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

@Configuration
@EnableWebSecurity
public class SecurityConf {

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/ping",
                        "/health/**",
                        "/error",
                        "/payment",
                        "/payments/search",
                        "/payments/export/csv",
                        "/orange/**",
                        "/css/**",
                        "/js/**",
                        "/script/**",
                        "/style/**",
                        "/images/**")
                    .permitAll()
                    .requestMatchers("/payments/**")
                    .authenticated()
                    .anyRequest()
                    .denyAll())
        .formLogin(login -> login.usernameParameter("email").defaultSuccessUrl("/payments", true))
        .logout(logout -> logout.logoutSuccessUrl("/"))
        .csrf(csrf -> csrf.ignoringRequestMatchers("/payment", "/payments/search", "/orange/**"));
    return http.build();
  }

  @Bean
  public UserDetailsService users(Environment env, PasswordEncoder passwordEncoder) {
    return new InMemoryUserDetailsManager(
        User.builder()
            .username(env.getRequiredProperty("ADMIN_EMAIL"))
            .password(passwordEncoder.encode(env.getRequiredProperty("ADMIN_PASSWORD")))
            .build());
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }
}
