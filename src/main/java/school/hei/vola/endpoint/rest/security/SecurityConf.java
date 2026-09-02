package school.hei.vola.endpoint.rest.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConf {

  @Bean
  public SecurityFilterChain filterChain(
      HttpSecurity http, ApiKeyAuthenticationFilter apiKeyAuthenticationFilter) throws Exception {
    http.addFilterBefore(apiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/ping",
                        "/health/**",
                        "/error",
                        "/login",
                        "/orange/sync",
                        "/css/**",
                        "/js/**",
                        "/script/**",
                        "/style/**",
                        "/images/**")
                    .permitAll()
                    .requestMatchers("/payment", "/payments/search", "/orange/transactions/import")
                    .hasRole("APP")
                    .requestMatchers("/payments/**")
                    .hasRole("ADMIN")
                    .anyRequest()
                    .denyAll())
        .formLogin(login -> login.usernameParameter("email").defaultSuccessUrl("/payments", true))
        .logout(logout -> logout.logoutSuccessUrl("/"))
        .csrf(csrf -> csrf.ignoringRequestMatchers("/payment", "/payments/search", "/orange/**"));
    return http.build();
  }

  @Bean
  public ApiKeyAuthenticationFilter apiKeyAuthenticationFilter(
      ApplicationAuthorizer applicationAuthorizer, AdminAuthorizer adminAuthorizer) {
    return new ApiKeyAuthenticationFilter(applicationAuthorizer, adminAuthorizer);
  }

  @Bean
  public UserDetailsService users(Environment env, PasswordEncoder passwordEncoder) {
    return new InMemoryUserDetailsManager(
        User.builder()
            .username(env.getRequiredProperty("ADMIN_EMAIL"))
            .password(passwordEncoder.encode(env.getRequiredProperty("ADMIN_PASSWORD")))
            .roles("ADMIN")
            .build());
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }
}
