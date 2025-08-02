package school.hei.vola.service.psp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import school.hei.vola.model.psp.orange.OrangeApiClient;
import school.hei.vola.model.psp.orange.OrangePsp;
import school.hei.vola.repository.PspPaymentRepository;

@Configuration
public class PspConf {

  private final String orangeApiUrl;
  private final PspPaymentRepository pspPaymentRepository;

  public PspConf(
      @Value("${orange.api.url}") String orangeApiUrl, PspPaymentRepository pspPaymentRepository) {
    this.orangeApiUrl = orangeApiUrl;
    this.pspPaymentRepository = pspPaymentRepository;
  }

  @Bean
  OrangePsp orangePsp() {
    return new OrangePsp(pspPaymentRepository);
  }

  @Bean
  OrangeApiClient orangeApiClient() {
    return new OrangeApiClient(orangeApiUrl);
  }
}
