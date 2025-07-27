package school.hei.vola.model.psp;

import java.util.Optional;

public interface Psp {
  PspType type();

  Optional<PspPayment> verify(String pspId);
}
