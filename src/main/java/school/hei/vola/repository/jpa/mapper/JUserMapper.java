package school.hei.vola.repository.jpa.mapper;

import org.springframework.stereotype.Component;
import school.hei.vola.model.User;
import school.hei.vola.repository.jpa.model.JUser;

@Component
public class JUserMapper {
  public User toDomain(JUser jUser) {
    return new User(jUser.getEmail());
  }

  public JUser toEntity(User user, String id) {
    return new JUser(id, user.email());
  }
}
