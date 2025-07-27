package school.hei.vola.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import school.hei.vola.repository.jpa.model.JUser;

@Repository
public interface JUserRepository extends JpaRepository<JUser, String> {
  JUser findByEmail(String email);
}
