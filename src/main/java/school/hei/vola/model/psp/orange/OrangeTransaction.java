package school.hei.vola.model.psp.orange;

import static school.hei.vola.model.psp.orange.OrangeTransaction.TransactionStatus.FAILED;
import static school.hei.vola.model.psp.orange.OrangeTransaction.TransactionStatus.SUCCEEDED;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@EqualsAndHashCode
public class OrangeTransaction {
  private int number;
  private String date;
  private String time;
  private String ref;
  private String status;

  @JsonProperty("client_number")
  private String clientNumber;

  private int amount;

  public TransactionStatus status() {
    return switch (status) {
      case "Succès" -> SUCCEEDED;
      case "Echec" -> FAILED;
      default -> throw new RuntimeException("Unexpected status: " + status);
    };
  }

  public Instant creationInstant() {
    return Instant.parse(date + time);
  }

  public enum TransactionStatus {
    SUCCEEDED,
    FAILED
  }
}
