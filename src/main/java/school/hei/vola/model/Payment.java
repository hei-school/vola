package school.hei.vola.model;

import static school.hei.vola.model.VerificationStatus.FAILED;
import static school.hei.vola.model.VerificationStatus.SUCCEEDED;
import static school.hei.vola.model.VerificationStatus.VERIFYING;

import java.time.Instant;
import lombok.Builder;
import school.hei.vola.model.psp.PspPayment;

@Builder(toBuilder = true)
public record Payment(
    String id,
    PspPayment pspPayment,
    Instant creationInstant,
    Instant lastPspVerificationInstant,
    int verificationAttemptNb,
    User payer,
    Application application) {

  private static final int MAX_VERIFICATION_ATTEMPT_NB = 5;

  public boolean hasNoMoreVerificationAttempt() {
    return verificationAttemptNb > MAX_VERIFICATION_ATTEMPT_NB;
  }

  public VerificationStatus getVerificationStatus() {
    if (pspPayment.amount() != null) {
      return SUCCEEDED;
    }
    if (hasNoMoreVerificationAttempt()) {
      return FAILED;
    }
    return VERIFYING;
  }
}
