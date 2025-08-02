package school.hei.vola.repository.jpa.mapper;

import org.springframework.stereotype.Component;
import school.hei.vola.model.Application;
import school.hei.vola.repository.jpa.model.JApplication;

@Component
public class JApplicationMapper {
  public Application toDomain(JApplication jApplication) {
    return new Application(jApplication.getName(), jApplication.getApiKey());
  }
}
