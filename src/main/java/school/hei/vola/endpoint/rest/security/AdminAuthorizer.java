package school.hei.vola.endpoint.rest.security;

import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AdminAuthorizer implements Consumer<String> {

  private final String adminApiKey;

  public AdminAuthorizer(@Value("${ADMIN_API_KEY}") String adminApiKey) {
    this.adminApiKey = adminApiKey;
  }

  @Override
  public void accept(String apiKey) {
    if (!adminApiKey.equals(apiKey)) {
      throw new UnauthorizedException();
    }
  }
}
