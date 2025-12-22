package school.hei.vola.service.sync.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RecoveryResult {
    LocalDate date;
    boolean isSuccessful;
    int inserted;
    String errorMessage;
}
