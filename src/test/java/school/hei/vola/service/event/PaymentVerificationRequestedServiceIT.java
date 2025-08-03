package school.hei.vola.service.event;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static school.hei.vola.model.Time.millisNow;
import static school.hei.vola.model.psp.PspType.ORANGE_MONEY;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import school.hei.vola.conf.FacadeIT;
import school.hei.vola.endpoint.event.model.PaymentVerificationRequested;
import school.hei.vola.model.Application;
import school.hei.vola.model.Payment;
import school.hei.vola.model.User;
import school.hei.vola.model.psp.PspPayment;
import school.hei.vola.model.psp.orange.OrangePsp;
import school.hei.vola.repository.PaymentRepository;
import school.hei.vola.repository.UserRepository;
import school.hei.vola.repository.jpa.JApplicationRepository;
import school.hei.vola.repository.jpa.mapper.JApplicationMapper;
import school.hei.vola.repository.jpa.model.JApplication;

class PaymentVerificationRequestedServiceIT extends FacadeIT {
  @Autowired PaymentVerificationRequestedService subject;
  @MockBean OrangePsp orangePspMocked;

  @Autowired PaymentRepository paymentRepository;
  @Autowired UserRepository userRepository;

  @Autowired JApplicationRepository jApplicationRepository;
  @Autowired JApplicationMapper jApplicationMapper;

  Application randomApplication() {
    var jApplication = new JApplication();
    jApplication.setName(randomUUID().toString());
    jApplication.setId(randomUUID().toString());
    jApplication.setApiKey(randomUUID().toString());
    return jApplicationMapper.toDomain(jApplicationRepository.save(jApplication));
  }

  @Test
  void unverified_pspPayment_updates_lastPspVerificationInstant() {
    var randomLou = userRepository.save(randomLou());
    when(orangePspMocked.verify(any())).thenReturn(Optional.empty());
    var paymentId = randomId();
    var payment =
        new Payment(
            paymentId,
            new PspPayment(ORANGE_MONEY, randomId(), null, null),
            millisNow(),
            null,
            0,
            randomLou,
            randomApplication());
    var savedPayment = paymentRepository.save(payment);

    assertNull(savedPayment.pspPayment().amount());
    assertNull(savedPayment.lastPspVerificationInstant());
    assertThrows(
        NotYetVerifiedByPspException.class,
        () -> subject.accept(new PaymentVerificationRequested(payment)));

    var savedPaymentAfterVerification = paymentRepository.findPaymentById(paymentId);
    assertNull(savedPaymentAfterVerification.pspPayment().amount());
    assertNotNull(savedPaymentAfterVerification.lastPspVerificationInstant());
  }

  @Test
  void verified_pspPayment_updates_amount() {
    var randomLou = userRepository.save(randomLou());
    var pspPaymentId = randomId();
    when(orangePspMocked.verify(any()))
        .thenReturn(Optional.of(new PspPayment(ORANGE_MONEY, pspPaymentId, 100, millisNow())));
    var paymentId = randomId();
    var payment =
        new Payment(
            paymentId,
            new PspPayment(ORANGE_MONEY, pspPaymentId, null, null),
            millisNow(),
            null,
            0,
            randomLou,
            randomApplication());
    var savedPayment = paymentRepository.save(payment);

    assertNull(savedPayment.pspPayment().amount());
    assertNull(savedPayment.lastPspVerificationInstant());
    subject.accept(new PaymentVerificationRequested(payment));

    var savedPaymentAfterVerification = paymentRepository.findPaymentById(paymentId);
    assertEquals(100, savedPaymentAfterVerification.pspPayment().amount());
    assertNotNull(savedPaymentAfterVerification.lastPspVerificationInstant());
  }

  private static String randomId() {
    return randomUUID().toString();
  }

  private static User randomLou() {
    return new User("lou+ " + randomUUID() + "@cute.dev");
  }
}
