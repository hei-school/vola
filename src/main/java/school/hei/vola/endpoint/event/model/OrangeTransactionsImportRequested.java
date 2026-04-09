package school.hei.vola.endpoint.event.model;

import static java.time.Duration.ofHours;
import static java.time.Duration.ofMinutes;

import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class OrangeTransactionsImportRequested extends PojaEvent {
  private String bucketKey;

  @Override
  public Duration maxConsumerDuration() {
    return ofMinutes(5);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return ofHours(12);
  }
}
