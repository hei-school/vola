package school.hei.vola.endpoint;

import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Authorizer implements Consumer<String> {

  private final String apiKey;

  public Authorizer(@Value("${vola.api.key}") String apiKey) {
    this.apiKey = apiKey;
  }

  @Override
  public void accept(String apiKey) {
    if (!this.apiKey.equals(apiKey)) {
      throw new RuntimeException("Bad credentials");
    }
  }
}
