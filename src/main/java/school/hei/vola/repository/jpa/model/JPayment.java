package school.hei.vola.repository.jpa.model;

import static jakarta.persistence.EnumType.STRING;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import school.hei.vola.model.psp.PspType;

@Entity
@Table(name = "payment")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class JPayment {
  @Id private String id;

  @Column(name = "psp_type")
  @Enumerated(STRING)
  private PspType pspType;

  private Integer amount;
  private String pspPaymentId;
  private Instant pspCreationInstant;

  private Instant creationInstant;
  private Instant lastPspVerificationInstant;

  @ManyToOne
  @JoinColumn(name = "user_id")
  private JUser payer;

  @ManyToOne
  @JoinColumn(name = "application_id")
  private JApplication application;
}
