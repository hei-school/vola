package school.hei.vola.repository.jpa.model;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.IDENTITY;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import school.hei.vola.model.Psp;

@Entity
@Table(name = "\"payment\"")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class JPayment {
  @Id
  @GeneratedValue(strategy = IDENTITY)
  private String id;

  private Integer amount;

  @Column(name = "\"psp\"")
  @Enumerated(STRING)
  @JdbcTypeCode(NAMED_ENUM)
  private Psp psp;

  private String pspPaymentId;

  @CreationTimestamp private Instant creationDatetime;
  @UpdateTimestamp private Instant updateDatetime;

  @ManyToOne
  @JoinColumn(name = "user_id")
  private JUser payer;
}
