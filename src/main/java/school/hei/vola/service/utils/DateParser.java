package school.hei.vola.service.utils;

import java.time.LocalDate;

public class DateParser {

  private DateParser() {}

  public static LocalDate parseDate(String dateStr) {
    if (dateStr == null || dateStr.isBlank()) {
      return null;
    }
    return LocalDate.parse(dateStr);
  }
}
