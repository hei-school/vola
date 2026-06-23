package school.hei.vola.repository;

import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;
import school.hei.vola.model.Application;
import school.hei.vola.repository.jpa.JApplicationRepository;
import school.hei.vola.repository.jpa.mapper.JApplicationMapper;

@Repository
@AllArgsConstructor
public class ApplicationRepository {
  private final JApplicationRepository jApplicationRepository;
  private final JApplicationMapper jApplicationMapper;

  public Optional<Application> findByApiKey(String apiKey) {
    return jApplicationRepository.findByApiKey(apiKey).map(jApplicationMapper::toDomain);
  }

  public List<Application> findAll() {
    return jApplicationRepository.findAll().stream().map(jApplicationMapper::toDomain).toList();
  }
}
