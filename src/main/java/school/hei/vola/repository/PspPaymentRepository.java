package school.hei.vola.repository;

import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;
import school.hei.vola.model.psp.PspPayment;
import school.hei.vola.model.psp.PspType;

@Repository
@AllArgsConstructor
public class PspPaymentRepository {

  private final OrangePaymentRepository orangePaymentRepository;

  public Optional<PspPayment> findById(PspType pspType, String pspPaymentId) {
    return switch (pspType) {
      case ORANGE_MONEY -> orangePaymentRepository.findById(pspPaymentId);
    };
  }
}
