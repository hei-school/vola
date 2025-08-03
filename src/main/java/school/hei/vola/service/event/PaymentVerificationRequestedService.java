package school.hei.vola.service.event;

import static school.hei.vola.model.Time.millisNow;

import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import school.hei.vola.endpoint.event.model.PaymentVerificationRequested;
import school.hei.vola.repository.PaymentRepository;
import school.hei.vola.service.psp.PspProvider;

@Service
@AllArgsConstructor
public class PaymentVerificationRequestedService implements Consumer<PaymentVerificationRequested> {

  private final PaymentRepository paymentRepository;
  private final PspProvider pspProvider;

  @Override
  public void accept(PaymentVerificationRequested paymentVerificationRequested) {
    var paymentToSave =
        paymentVerificationRequested.getPayment().toBuilder()
            .lastPspVerificationInstant(millisNow())
            .verificationAttemptNb(paymentVerificationRequested.getAttemptNb())
            .build();

    var pspType = paymentToSave.pspPayment().pspType();
    var psp = pspProvider.pspOfType(pspType);
    var verifiedPspPaymentOpt = psp.verify(paymentToSave.pspPayment().id());
    if (verifiedPspPaymentOpt.isEmpty()) {
      var savedPayment = paymentRepository.save(paymentToSave);
      if (paymentToSave.hasNoMoreVerificationAttempt()) {
        return;
      }
      throw new NotYetVerifiedByPspException(savedPayment);
    }

    var verifiedPspPayment = verifiedPspPaymentOpt.get();
    paymentRepository.save(paymentToSave.toBuilder().pspPayment(verifiedPspPayment).build());
  }
}
