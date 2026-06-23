package school.hei.vola.endpoint.rest.security;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class VolaAdminChecker {
  @Value("${vola.admins}")
  private String admins;

  public boolean isAdmin(String email) {
    if (email == null || admins == null) {
      return false;
    }
    return Arrays.stream(admins.split(","))
        .map(String::trim)
        .map(String::toLowerCase)
        .anyMatch(admin -> admin.equals(email.toLowerCase()));
  }
}
