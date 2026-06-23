package school.hei.vola.repository.jpa;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static school.hei.vola.model.psp.PspType.ORANGE_MONEY;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import school.hei.vola.conf.FacadeIT;
import school.hei.vola.repository.jpa.model.JApplication;
import school.hei.vola.repository.jpa.model.JPayment;
import school.hei.vola.repository.jpa.model.JUser;

class JPaymentRepositoryIT extends FacadeIT {
  @Autowired JPaymentRepository subject;
  @Autowired JApplicationRepository jApplicationRepository;
  @Autowired JUserRepository jUserRepository;

  JApplication app;
  JUser user;

  @BeforeEach
  void setUp() {
    app = new JApplication();
    app.setId(randomUUID().toString());
    app.setName(randomUUID().toString());
    app.setApiKey(randomUUID().toString());
    jApplicationRepository.save(app);

    user = new JUser(randomUUID().toString(), randomUUID() + "@test.com");
    jUserRepository.save(user);
  }

  @Test
  void findByApplicationNameAndCreationInstantBetween_returns_matching_payments() {
    var now = Instant.now();
    subject.save(paymentAt(app, user, now));

    var result =
        subject.findByApplicationNameAndCreationInstantBetween(
            app.getName(), Instant.EPOCH, Instant.parse("9999-12-31T23:59:59Z"));

    assertEquals(1, result.size());
  }

  @Test
  void findByApplicationNameAndCreationInstantBetween_excludes_outside_range() {
    subject.save(paymentAt(app, user, Instant.parse("2024-01-15T10:00:00Z")));

    var result =
        subject.findByApplicationNameAndCreationInstantBetween(
            app.getName(),
            Instant.parse("2025-01-01T00:00:00Z"),
            Instant.parse("2025-12-31T23:59:59Z"));

    assertTrue(result.isEmpty());
  }

  @Test
  void findByApplicationNameAndCreationInstantBetween_excludes_other_application() {
    var otherApp = new JApplication();
    otherApp.setId(randomUUID().toString());
    otherApp.setName(randomUUID().toString());
    otherApp.setApiKey(randomUUID().toString());
    jApplicationRepository.save(otherApp);

    subject.save(paymentAt(otherApp, user, Instant.now()));

    var result =
        subject.findByApplicationNameAndCreationInstantBetween(
            app.getName(), Instant.EPOCH, Instant.parse("9999-12-31T23:59:59Z"));

    assertTrue(result.isEmpty());
  }

  @Test
  void findByApplicationNameAndCreationInstantBetween_start_is_inclusive() {
    var instant = Instant.parse("2025-06-01T00:00:00Z");
    subject.save(paymentAt(app, user, instant));

    var result =
        subject.findByApplicationNameAndCreationInstantBetween(
            app.getName(), instant, Instant.parse("9999-12-31T23:59:59Z"));

    assertEquals(1, result.size());
  }

  @Test
  void findByApplicationNameAndCreationInstantBetween_end_is_exclusive() {
    var instant = Instant.parse("2025-06-01T00:00:00Z");
    subject.save(paymentAt(app, user, instant));

    var result =
        subject.findByApplicationNameAndCreationInstantBetween(
            app.getName(), Instant.EPOCH, instant);

    assertTrue(result.isEmpty());
  }

  @Test
  void findByApplicationNameAndCreationInstantBetween_returns_multiple_payments() {
    subject.save(paymentAt(app, user, Instant.parse("2025-01-01T00:00:00Z")));
    subject.save(paymentAt(app, user, Instant.parse("2025-06-15T12:00:00Z")));
    subject.save(paymentAt(app, user, Instant.parse("2025-12-31T23:59:59Z")));

    var result =
        subject.findByApplicationNameAndCreationInstantBetween(
            app.getName(),
            Instant.parse("2025-01-01T00:00:00Z"),
            Instant.parse("2026-01-01T00:00:00Z"));

    assertEquals(3, result.size());
  }

  private JPayment paymentAt(JApplication application, JUser payer, Instant creationInstant) {
    var p = new JPayment();
    p.setId(randomUUID().toString());
    p.setPspType(ORANGE_MONEY);
    p.setPspPaymentId(randomUUID().toString());
    p.setCreationInstant(creationInstant);
    p.setPayer(payer);
    p.setApplication(application);
    return p;
  }
}
