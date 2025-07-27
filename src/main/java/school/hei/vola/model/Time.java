package school.hei.vola.model;

import static java.time.temporal.ChronoUnit.MILLIS;

import java.time.Instant;

public class Time {
  /* Many systems does _not_ support time precision over millis.
   * This is the case for H2 db and Postgres by default for example.
   * Since Instant::now's precision is nanoseconds,
   * its excessive precision can cause comparison issue with less
   * precise systems. */
  public static Instant millisNow() {
    return Instant.now().truncatedTo(MILLIS);
  }
}
