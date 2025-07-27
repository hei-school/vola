package school.hei.vola.repository;

import static java.util.UUID.randomUUID;
import static school.hei.vola.model.Time.millisNow;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;
import school.hei.vola.model.Payment;
import school.hei.vola.model.User;
import school.hei.vola.model.psp.PspType;
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

  public Payment createPayment(User payer, PspType pspType, String pspPaymentId) {
    var jUser = jUserRepository.findByEmail(payer.email());
    var toSaveJPayment =
        new JPayment(
            randomUUID().toString(), pspType, null, pspPaymentId, null, millisNow(), null, jUser);
    var savedJPayment = jPaymentRepository.save(toSaveJPayment);

    return jPaymentMapper.toDomain(savedJPayment);
  }

  public Payment findPaymentById(String id) {
    var jPayment = jPaymentRepository.findById(id).get();
    return jPaymentMapper.toDomain(jPayment);
  }

  public Payment save(Payment payment) {
    var jPayment = jPaymentMapper.toEntity(payment);
    return jPaymentMapper.toDomain(jPaymentRepository.save(jPayment));
  }
}
