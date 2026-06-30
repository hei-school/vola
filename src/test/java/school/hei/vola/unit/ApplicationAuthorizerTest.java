package school.hei.vola.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import school.hei.vola.endpoint.rest.security.ApplicationAuthorizer;
import school.hei.vola.endpoint.rest.security.UnauthorizedException;
import school.hei.vola.model.Application;
import school.hei.vola.repository.ApplicationRepository;

class ApplicationAuthorizerTest {
  @Test
  void accept_validApiKey_doesNotThrow() {
    var repo = mock(ApplicationRepository.class);
    when(repo.findByApiKey("valid-key"))
        .thenReturn(Optional.of(new Application("app", "valid-key")));
    var authorizer = new ApplicationAuthorizer(repo);
    assertDoesNotThrow(() -> authorizer.accept("valid-key"));
  }

  @Test
  void accept_invalidApiKey_throwsUnauthorized() {
    var repo = mock(ApplicationRepository.class);
    when(repo.findByApiKey("bad-key")).thenReturn(Optional.empty());
    var authorizer = new ApplicationAuthorizer(repo);
    assertThrows(UnauthorizedException.class, () -> authorizer.accept("bad-key"));
  }
}
