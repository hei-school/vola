package school.hei.vola.model.psp;

import java.time.Instant;
import lombok.Builder;

@Builder(toBuilder = true)
public record PspPayment(PspType pspType, String id, Integer amount, Instant creationInstant) {}
