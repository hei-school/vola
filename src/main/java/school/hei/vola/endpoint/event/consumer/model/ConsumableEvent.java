package school.hei.vola.endpoint.event.consumer.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import school.hei.vola.PojaGenerated;

@PojaGenerated
@AllArgsConstructor
public class ConsumableEvent {
  @Getter private final TypedEvent event;
  private final Runnable acknowledger;
  private final Runnable randomVisibilityTimeoutSetter;

  public void ack() {
    acknowledger.run();
  }

  public void newRandomVisibilityTimeout() {
    randomVisibilityTimeoutSetter.run();
  }
}
