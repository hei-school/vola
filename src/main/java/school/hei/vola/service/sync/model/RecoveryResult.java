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
  LocalDate date;
  boolean isSuccessful;
  int inserted;
  String errorMessage;
}
