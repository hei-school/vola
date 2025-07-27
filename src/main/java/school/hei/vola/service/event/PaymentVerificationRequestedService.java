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
    var payment = paymentVerificationRequested.getPayment();
    var verificationInstant = millisNow();
    payment = payment.toBuilder().lastPspVerificationInstant(verificationInstant).build();

    var pspType = payment.pspPayment().pspType();
    var psp = pspProvider.pspOfType(pspType);
    var verifiedPspPaymentOpt = psp.verify(payment.pspPayment().id());
    if (verifiedPspPaymentOpt.isEmpty()) {
      paymentRepository.save(payment);
      throw new NotYetVerifiedByPspException(payment, verificationInstant);
    }

    var verifiedPspPayment = verifiedPspPaymentOpt.get();
    paymentRepository.save(payment.toBuilder().pspPayment(verifiedPspPayment).build());
  }
}
