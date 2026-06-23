package school.hei.vola.conf;

import org.springframework.test.context.DynamicPropertyRegistry;

public class EnvConf {
  void configureProperties(DynamicPropertyRegistry registry) {
    var apiUrl = System.getenv("ORANGE_API_URL");
    registry.add("env", () -> "test");
    registry.add(
        "spring.datasource.url", () -> "jdbc:h2:mem:testdb;CASE_INSENSITIVE_IDENTIFIERS=TRUE");
    registry.add("spring.datasource.driverClassName", () -> "org.h2.Driver");
    registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.H2Dialect");
    registry.add("orange.api.url", () -> apiUrl != null ? apiUrl : "http://dummy.orange.api");
    registry.add(
        "spring.security.oauth2.client.registration.casdoor.client-id", () -> "dummy");
    registry.add(
        "spring.security.oauth2.client.registration.casdoor.client-secret", () -> "dummy");
    registry.add(
        "spring.security.oauth2.client.registration.casdoor.authorization-grant-type",
        () -> "authorization_code");
    registry.add(
        "spring.security.oauth2.client.registration.casdoor.redirect-uri",
        () -> "{baseUrl}/login/oauth2/code/casdoor");
    registry.add(
        "spring.security.oauth2.client.provider.casdoor.authorization-uri", () -> "dummy");
    registry.add(
        "spring.security.oauth2.client.provider.casdoor.token-uri", () -> "dummy");
    registry.add("vola.admins", () -> "admin@test.com");
  }
}
