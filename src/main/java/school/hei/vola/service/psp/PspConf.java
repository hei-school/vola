package school.hei.vola.service.psp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import school.hei.vola.model.psp.orange.OrangeApiClient;
import school.hei.vola.model.psp.orange.OrangePsp;

@Configuration
public class PspConf {

  private final String orangeApiUrl;

  public PspConf(@Value("${orange.api.url}") String orangeApiUrl) {
    this.orangeApiUrl = orangeApiUrl;
  }

  @Bean
  OrangePsp orangePsp() {
    return new OrangePsp(new OrangeApiClient(orangeApiUrl));
  }
}
