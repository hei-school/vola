package school.hei.vola.repository.jpa.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Table(name = "application")
@Entity
@Getter
@Setter
public class JApplication {
  @Id private String id;
  private String name;
  private String apiKey;
}
