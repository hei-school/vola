package school.hei.vola.service.psp;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import school.hei.vola.model.psp.Psp;
import school.hei.vola.model.psp.PspType;
import school.hei.vola.model.psp.orange.OrangePsp;

@Component
@AllArgsConstructor
public class PspProvider {

  private final OrangePsp orangePsp;

  public Psp pspOfType(PspType pspType) {
    return switch (pspType) {
      case ORANGE_MONEY -> orangePsp;
    };
  }
}
