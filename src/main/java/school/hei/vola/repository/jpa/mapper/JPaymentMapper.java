package school.hei.vola.repository.jpa.mapper;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import school.hei.vola.model.Payment;
import school.hei.vola.repository.jpa.model.JPayment;

@Component
@AllArgsConstructor
public class JPaymentMapper {
  private final JUserMapper jUserMapper;

  public Payment toDomain(JPayment jPayment) {
    var payer = jUserMapper.toDomain(jPayment.getPayer());
    return new Payment(
        jPayment.getId(),
        jPayment.getAmount(),
        jPayment.getPsp(),
        jPayment.getPspPaymentId(),
        jPayment.getCreationDatetime(),
        jPayment.getUpdateDatetime(),
        payer);
  }
}
