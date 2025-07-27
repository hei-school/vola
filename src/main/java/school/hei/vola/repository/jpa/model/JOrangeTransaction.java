package school.hei.vola.repository.jpa.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Table(name = "\"orange_transaction\"")
@Entity
@Getter
@Setter
public class JOrangeTransaction {
  @Id private String ref;

  private String orangeApiRawResponse;
}
