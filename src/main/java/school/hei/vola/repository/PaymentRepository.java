package school.hei.vola.repository;

import static java.util.UUID.randomUUID;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;
import school.hei.vola.model.Payment;
import school.hei.vola.model.Psp;
import school.hei.vola.model.User;
import school.hei.vola.repository.jpa.JPaymentRepository;
import school.hei.vola.repository.jpa.JUserRepository;
import school.hei.vola.repository.jpa.mapper.JPaymentMapper;
import school.hei.vola.repository.jpa.model.JPayment;

@Repository
@AllArgsConstructor
public class PaymentRepository {
  private final JPaymentRepository jPaymentRepository;
  private final JUserRepository jUserRepository;
  private final JPaymentMapper jPaymentMapper;

  public Payment createPayment(User payer, Psp psp, String pspPaymentId) {
    var jUser = jUserRepository.findByEmail(payer.email());
    var jpayment =
        new JPayment(randomUUID().toString(), null, psp, pspPaymentId, null, null, jUser);
    jPaymentRepository.save(jpayment);

    return jPaymentMapper.toDomain(jpayment);
  }
}
