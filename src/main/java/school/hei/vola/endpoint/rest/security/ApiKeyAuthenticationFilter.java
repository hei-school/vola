package school.hei.vola.endpoint.rest.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

  private final ApplicationAuthorizer applicationAuthorizer;
  private final AdminAuthorizer adminAuthorizer;

  public ApiKeyAuthenticationFilter(
      ApplicationAuthorizer applicationAuthorizer, AdminAuthorizer adminAuthorizer) {
    this.applicationAuthorizer = applicationAuthorizer;
    this.adminAuthorizer = adminAuthorizer;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    var apiKey = request.getParameter("apiKey");
    var adminKey = request.getParameter("adminKey");

    try {
      if (apiKey != null && !apiKey.isBlank()) {
        applicationAuthorizer.accept(apiKey);
        SecurityContextHolder.getContext()
            .setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                    apiKey, null, List.of(new SimpleGrantedAuthority("ROLE_APP"))));
      } else if (adminKey != null && !adminKey.isBlank()) {
        adminAuthorizer.accept(adminKey);
        SecurityContextHolder.getContext()
            .setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                    adminKey, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
      }
    } catch (UnauthorizedException e) {
      SecurityContextHolder.clearContext();
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid key");
      return;
    }

    chain.doFilter(request, response);
  }
}
