package school.hei.vola.service.event;

import lombok.ToString;
import school.hei.vola.model.Payment;

@ToString
public class NotYetVerifiedByPspException extends RuntimeException {
  private final Payment payment;

  public NotYetVerifiedByPspException(Payment payment) {
    super();
    this.payment = payment;
  }
}
