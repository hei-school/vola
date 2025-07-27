package school.hei.vola.service.event;

import java.time.Instant;
import lombok.ToString;
import school.hei.vola.model.Payment;

@ToString
public class NotYetVerifiedByPspException extends RuntimeException {
  private final Payment payment;
  private final Instant verificationInstant;

  public NotYetVerifiedByPspException(Payment payment, Instant verificationInstant) {
    super();
    this.payment = payment;
    this.verificationInstant = verificationInstant;
  }
}
