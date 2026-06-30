package school.hei.vola.unit;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import school.hei.vola.service.utils.DateParser;

class DateParserTest {
  @Test
  void parseDate_validInput_returnsLocalDate() {
    assertEquals(LocalDate.of(2024, 1, 15), DateParser.parseDate("2024-01-15"));
  }

  @Test
  void parseDate_nullInput_returnsNull() {
    assertNull(DateParser.parseDate(null));
  }

  @Test
  void parseDate_blankInput_returnsNull() {
    assertNull(DateParser.parseDate("  "));
  }
}
