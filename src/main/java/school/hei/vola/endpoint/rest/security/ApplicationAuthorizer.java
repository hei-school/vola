package school.hei.vola.endpoint.rest.security;

import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import school.hei.vola.repository.ApplicationRepository;

@Component
@AllArgsConstructor
public class ApplicationAuthorizer implements Consumer<String> {

  private final ApplicationRepository applicationRepository;

  @Override
  public void accept(String apiKey) {
    if (applicationRepository.findByApiKey(apiKey).isEmpty()) {
      throw new UnauthorizedException();
    }
  }
}
