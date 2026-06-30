package school.hei.vola.unit;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import school.hei.vola.model.psp.PspType;
import school.hei.vola.model.psp.orange.OrangePsp;

class OrangePspTest {
  @Test
  void type_returnsOrangeMoney() {
    var psp = new OrangePsp(null);
    assertEquals(PspType.ORANGE_MONEY, psp.type());
  }
}
