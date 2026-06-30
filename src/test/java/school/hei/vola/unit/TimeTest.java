package school.hei.vola.unit;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import school.hei.vola.model.Time;

class TimeTest {
  @Test
  void millisNow_precisionIsMillis() {
    var result = Time.millisNow();
    var expected = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    assertEquals(expected, result);
  }

  @Test
  void millisNow_notNull() {
    assertNotNull(Time.millisNow());
  }
}
