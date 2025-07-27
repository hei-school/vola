package school.hei.vola.service.event;

import java.time.Instant;
import school.hei.vola.model.Payment;

public class NotYetVerifiedByPspException extends RuntimeException {
  private final Payment payment;
  private final Instant verificationInstant;

  public NotYetVerifiedByPspException(Payment payment, Instant verificationInstant) {
    super();
    this.payment = payment;
    this.verificationInstant = verificationInstant;
  }
}
