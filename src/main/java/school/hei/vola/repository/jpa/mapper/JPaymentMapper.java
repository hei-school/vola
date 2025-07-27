package school.hei.vola.repository.jpa.mapper;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import school.hei.vola.model.Payment;
import school.hei.vola.model.psp.PspPayment;
import school.hei.vola.repository.jpa.JUserRepository;
import school.hei.vola.repository.jpa.model.JPayment;

@Component
@AllArgsConstructor
public class JPaymentMapper {
  private final JUserMapper jUserMapper;
  private final JUserRepository jUserRepository;

  public Payment toDomain(JPayment jPayment) {
    var payer = jUserMapper.toDomain(jPayment.getPayer());
    var pspPayment =
        new PspPayment(
            jPayment.getPspType(),
            jPayment.getPspPaymentId(),
            jPayment.getAmount(),
            jPayment.getPspCreationInstant());
    return new Payment(
        jPayment.getId(),
        pspPayment,
        jPayment.getCreationInstant(),
        jPayment.getLastPspVerificationInstant(),
        payer);
  }

  public JPayment toEntity(Payment payment) {
    var jPayer = jUserRepository.findByEmail(payment.payer().email());
    var pspPayment = payment.pspPayment();
    return new JPayment(
        payment.id(),
        pspPayment.pspType(),
        pspPayment.amount(),
        pspPayment.id(),
        pspPayment.creationInstant(),
        payment.creationInstant(),
        payment.lastPspVerificationInstant(),
        jPayer);
  }
}
