package school.hei.vola.endpoint.rest.security;

import jakarta.annotation.PostConstruct;
import java.sql.SQLException;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

@Configuration
@Slf4j
public class SessionConfig {

  private final DataSource dataSource;

  public SessionConfig(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @PostConstruct
  public void initSessionSchema() {
    var populator = new ResourceDatabasePopulator();
    populator.addScript(new ClassPathResource(schemaPath()));
    populator.setContinueOnError(true);
    DatabasePopulatorUtils.execute(populator, dataSource);
    log.info("Spring Session schema initialized using {}", schemaPath());
  }

  private String schemaPath() {
    try (var conn = dataSource.getConnection()) {
      var productName = conn.getMetaData().getDatabaseProductName();
      if ("H2".equalsIgnoreCase(productName)) {
        return "org/springframework/session/jdbc/schema-h2.sql";
      }
    } catch (SQLException e) {
      log.warn("Could not detect database product name, falling back to PostgreSQL schema", e);
    }
    return "org/springframework/session/jdbc/schema-postgresql.sql";
  }
}
