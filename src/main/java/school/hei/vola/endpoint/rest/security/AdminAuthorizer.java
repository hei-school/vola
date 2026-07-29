package school.hei.vola.endpoint.rest.security;

import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import school.hei.vola.repository.ApplicationRepository;

@Component
@AllArgsConstructor
public class AdminAuthorizer implements Consumer<String> {

  private final ApplicationRepository applicationRepository;

  @Override
  public void accept(String apiKey) {
    var app = applicationRepository.findByApiKey(apiKey);
    if (app.isEmpty() || !"ADMIN".equals(app.get().name())) {
      throw new UnauthorizedException();
    }
  }
}
