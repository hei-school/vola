package school.hei.vola.conf;

import org.springframework.test.context.DynamicPropertyRegistry;
import school.hei.vola.PojaGenerated;

@PojaGenerated
public class EmailConf {

  void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("aws.ses.source", () -> "dummy-ses-source");
  }
}
