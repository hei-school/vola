package school.hei.vola.service.sync.model;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RecoveryResult {
  private LocalDate date;
  private boolean isSuccessful;
  private int inserted;
  private String errorMessage;
}
