package school.hei.vola.service.sync.model;

import java.time.LocalDate;
import lombok.Builder;

@Builder
public record RecoveryResult(
    LocalDate date, boolean isSuccessful, int inserted, String errorMessage) {}
