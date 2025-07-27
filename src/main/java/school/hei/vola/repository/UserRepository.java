package school.hei.vola.repository;

import static java.util.UUID.randomUUID;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;
import school.hei.vola.model.User;
import school.hei.vola.repository.jpa.JUserRepository;
import school.hei.vola.repository.jpa.mapper.JUserMapper;

@Repository
@AllArgsConstructor
public class UserRepository {
  private final JUserRepository jUserRepository;
  private final JUserMapper jUserMapper;

  public User save(User user) {
    return jUserMapper.toDomain(
        jUserRepository.save(jUserMapper.toEntity(user, randomUUID().toString())));
  }
}
