package school.hei.vola.model;

import java.time.Instant;
import lombok.Builder;
import school.hei.vola.model.psp.PspPayment;

@Builder(toBuilder = true)
public record Payment(
    String id,
    PspPayment pspPayment,
    Instant creationInstant,
    Instant lastPspVerificationInstant,
    User payer,
    Application application) {}
