package school.hei.vola.model.psp.orange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import school.hei.vola.conf.FacadeIT;

class OrangePspIT extends FacadeIT {

  @Autowired private OrangePsp subject;

  @Test
  void unknown_pspId_leadsTo_emptyPspPayment() {
    assertTrue(subject.verify("dummy").isEmpty());
    assertTrue(subject.verify("MP250729.1116.D77954").isEmpty());
  }

  @Test
  void known_pspId_leadsTo_PspPayment() {
    var pspPaymentOpt = subject.verify("MP250729.1216.D77954");
    assertEquals(324_000, pspPaymentOpt.get().amount());
  }
}
