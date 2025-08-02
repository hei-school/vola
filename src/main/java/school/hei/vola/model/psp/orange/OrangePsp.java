package school.hei.vola.model.psp.orange;

import static school.hei.vola.model.psp.PspType.ORANGE_MONEY;

import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import school.hei.vola.model.psp.Psp;
import school.hei.vola.model.psp.PspPayment;
import school.hei.vola.model.psp.PspType;
import school.hei.vola.repository.PspPaymentRepository;

@AllArgsConstructor
@Slf4j
public class OrangePsp implements Psp {

  private final PspPaymentRepository pspPaymentRepository;

  @Override
  public PspType type() {
    return ORANGE_MONEY;
  }

  @Override
  public Optional<PspPayment> verify(String pspPaymentId) {
    return pspPaymentRepository.findById(ORANGE_MONEY, pspPaymentId);
  }
}
