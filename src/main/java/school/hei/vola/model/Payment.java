package school.hei.vola.model;

import java.time.Instant;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Payment {
  private final String id;
  private final Integer amount;
  private final Psp psp;
  private final String pspPaymentId;
  private final Instant creationDatetime;
  private final Instant updateDatetime;
  private final User payer;
}
