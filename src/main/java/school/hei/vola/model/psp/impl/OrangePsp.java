package school.hei.vola.model.psp.impl;

import static school.hei.vola.model.psp.PspType.ORANGE_MONEY;

import java.util.Optional;
import school.hei.vola.model.psp.Psp;
import school.hei.vola.model.psp.PspPayment;
import school.hei.vola.model.psp.PspType;

public class OrangePsp implements Psp {
  @Override
  public PspType type() {
    return ORANGE_MONEY;
  }

  @Override
  public Optional<PspPayment> verify(String pspId) {
    throw new RuntimeException("TODO");
  }
}
