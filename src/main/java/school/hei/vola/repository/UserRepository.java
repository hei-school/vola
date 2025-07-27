package school.hei.vola.repository;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;
import school.hei.vola.repository.jpa.JUserRepository;

@Repository
@AllArgsConstructor
public class UserRepository {
  private final JUserRepository jUserRepository;
}
