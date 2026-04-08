package school.hei.vola.model.psp.orange;

import static school.hei.vola.model.psp.orange.OrangeTransaction.TransactionStatus.FAILED;
import static school.hei.vola.model.psp.orange.OrangeTransaction.TransactionStatus.SUCCEEDED;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class OrangeTransaction {
  private int number;
  private String date;
  private String time;
  private String ref;
  private String status;
  private static final Pattern REF_PATTERN =
      Pattern.compile("^[A-Z]{2}\\d{6}\\.\\d{4}\\.[A-Z]\\d{5}$");
  private static final Pattern CLIENT_NUMBER_PATTERN = Pattern.compile("^03\\d{8}$");

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
    // Date is like 29/07/2025 while time is like 12:11:10
    String year = date.substring(6);
    String month = date.substring(3, 5);
    String day = date.substring(0, 2);
    return Instant.parse(String.format("%s-%s-%sT%sZ", year, month, day, time));
  }

  public enum TransactionStatus {
    SUCCEEDED,
    FAILED
  }

  public boolean validateClientNumber(String clientNumber) {
    return CLIENT_NUMBER_PATTERN.matcher(clientNumber).matches();
  }

  public boolean validateRef(String ref) {
    return ref != null && !ref.isEmpty() && REF_PATTERN.matcher(ref).matches();
  }
}
