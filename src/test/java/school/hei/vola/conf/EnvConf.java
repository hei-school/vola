package school.hei.vola.conf;

import org.springframework.test.context.DynamicPropertyRegistry;

public class EnvConf {
  void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("env", () -> "test");
    registry.add("vola.api.key", () -> "dummyApiKey");
    registry.add(
        "spring.datasource.url", () -> "jdbc:h2:mem:testdb;CASE_INSENSITIVE_IDENTIFIERS=TRUE");
    registry.add("spring.datasource.driverClassName", () -> "org.h2.Driver");
    registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.H2Dialect");
    registry.add(
        "orange.api.url", () -> "https://cbf314fda3.execute-api.eu-west-3.amazonaws.com/Prod");
  }
}
