package school.hei.vola.model.psp.orange;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import school.hei.vola.conf.FacadeIT;

class OrangePspIT extends FacadeIT {

  @Autowired private OrangePsp subject;

  @Test
  void unknown_pspId_leadsTo_emptyPspPayment() {
    var pspPaymentOpt = subject.verify("dummy");

    assertTrue(pspPaymentOpt.isEmpty());
  }

  @Disabled("TODO")
  @Test
  void known_pspId_leadsTo_PspPayment() {
    var pspPaymentOpt = subject.verify("MP250725.0944.D02365");

    assertFalse(pspPaymentOpt.isEmpty());
  }
}
