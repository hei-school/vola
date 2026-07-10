package school.hei.vola.repository.jpa;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import school.hei.vola.repository.jpa.model.JApplication;

@Repository
public interface JApplicationRepository extends JpaRepository<JApplication, String> {
  Optional<JApplication> findByApiKey(String apiKey);
}
