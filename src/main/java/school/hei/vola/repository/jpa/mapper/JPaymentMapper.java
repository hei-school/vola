package school.hei.vola.repository.jpa.mapper;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import school.hei.vola.model.Payment;
import school.hei.vola.model.psp.PspPayment;
import school.hei.vola.repository.jpa.JApplicationRepository;
import school.hei.vola.repository.jpa.JUserRepository;
import school.hei.vola.repository.jpa.model.JPayment;

@Component
@AllArgsConstructor
public class JPaymentMapper {
  private final JUserRepository jUserRepository;
  private final JUserMapper jUserMapper;

  private final JApplicationRepository jApplicationRepository;
  private final JApplicationMapper jApplicationMapper;

  public Payment toDomain(JPayment jPayment) {
    var pspPayment =
        new PspPayment(
            jPayment.getPspType(),
            jPayment.getPspPaymentId(),
            jPayment.getAmount(),
            jPayment.getPspCreationInstant());
    var payer = jUserMapper.toDomain(jPayment.getPayer());
    var application = jApplicationMapper.toDomain(jPayment.getApplication());
    return new Payment(
        jPayment.getId(),
        pspPayment,
        jPayment.getCreationInstant(),
        jPayment.getLastPspVerificationInstant(),
        jPayment.getVerificationAttemptNb(),
        payer,
        application,
        jPayment.getScope());
  }

  public JPayment toEntity(Payment payment) {
    var pspPayment = payment.pspPayment();
    var jPayer = jUserRepository.findByEmail(payment.payer().email());
    var jApplication = jApplicationRepository.findByApiKey(payment.application().apiKey()).get();
    return new JPayment(
        payment.id(),
        pspPayment.pspType(),
        pspPayment.amount(),
        pspPayment.id(),
        pspPayment.creationInstant(),
        payment.creationInstant(),
        payment.lastPspVerificationInstant(),
        payment.verificationAttemptNb(),
        payment.scope(),
        jPayer,
        jApplication);
  }
}
