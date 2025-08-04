package school.hei.vola.endpoint.event.model;

import static java.time.LocalDate.now;
import static school.hei.vola.endpoint.event.EventStack.EVENT_STACK_2;

import java.time.Duration;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import school.hei.vola.endpoint.event.EventStack;

@AllArgsConstructor
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class OrangeDailyTransactionsRetrievalRequested extends PojaEvent {
  private LocalDate date;

  public OrangeDailyTransactionsRetrievalRequested() {
    date =
        now()
            // Transactions of current day are only available tomorrow
            .minusDays(1);
  }

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(5);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofHours(1);
  }

  @Override
  public EventStack getEventStack() {
    return EVENT_STACK_2;
  }
}
