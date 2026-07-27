package school.hei.vola.unit;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import school.hei.vola.model.psp.orange.OrangeTransaction;

class OrangeTransactionTest {
  @Test
  void status_succeeded_returnsSucceeded() {
    var ot = new OrangeTransaction();
    ot.setStatus("Succès");
    assertEquals(OrangeTransaction.TransactionStatus.SUCCEEDED, ot.status());
  }

  @Test
  void status_failed_returnsFailed() {
    var ot = new OrangeTransaction();
    ot.setStatus("Echec");
    assertEquals(OrangeTransaction.TransactionStatus.FAILED, ot.status());
  }

  @Test
  void status_unexpected_throwsException() {
    var ot = new OrangeTransaction();
    ot.setStatus("Inconnu");
    assertThrows(RuntimeException.class, ot::status);
  }
}
