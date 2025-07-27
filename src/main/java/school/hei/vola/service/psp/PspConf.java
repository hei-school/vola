package school.hei.vola.service.psp;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import school.hei.vola.model.psp.impl.OrangePsp;

@Configuration
public class PspConf {

  @Bean
  OrangePsp orangePsp() {
    return new OrangePsp();
  }
}
