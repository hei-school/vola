package school.hei.vola.repository.jpa.model;

import static jakarta.persistence.GenerationType.IDENTITY;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "\"user\"")
@Data
public class JUser {
  @Id
  @GeneratedValue(strategy = IDENTITY)
  private String id;

  private String email;
}
