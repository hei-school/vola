package school.hei.vola.service;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static school.hei.vola.model.VerificationStatus.FAILED;
import static school.hei.vola.model.psp.PspType.ORANGE_MONEY;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import school.hei.vola.conf.FacadeIT;
import school.hei.vola.endpoint.event.EventProducer;
import school.hei.vola.endpoint.event.model.PaymentVerificationRequested;
import school.hei.vola.model.PaymentInfo;
import school.hei.vola.model.psp.PspPayment;
import school.hei.vola.repository.jpa.JApplicationRepository;
import school.hei.vola.repository.jpa.model.JApplication;

class PaymentServiceIT extends FacadeIT {

  @Autowired PaymentService subject;
  @MockBean EventProducer eventProducerMocked;

  @Autowired JApplicationRepository jApplicationRepository;

  JApplication randomJApplication() {
    var jApplication = new JApplication();
    jApplication.setName(randomUUID().toString());
    jApplication.setId(randomUUID().toString());
    jApplication.setApiKey(randomUUID().toString());
    return jApplicationRepository.save(jApplication);
  }

  @Test
  void createdPayment_canBe_retrieved() {
    var apiKey = randomJApplication().getApiKey();
    var email = randomEmail();
    var pspType = ORANGE_MONEY;
    var pspPaymentId = randomUUID().toString();
    var createdPayment = subject.createPayment(apiKey, email, pspType, pspPaymentId);

    var retrievedPayment =
        subject
            .findPaymentByPayerEmailAndPspTypeAndPspPaymentId(email, pspType, pspPaymentId)
            .get();
    assertEquals(createdPayment, retrievedPayment);
    assertNotNull(retrievedPayment.id());
    assertEquals(email, retrievedPayment.payer().email());
    assertEquals(
        new PspPayment(ORANGE_MONEY, pspPaymentId, null, null), retrievedPayment.pspPayment());
    assertNotNull(retrievedPayment.creationInstant());
    assertNull(retrievedPayment.lastPspVerificationInstant());
  }

  @Test
  void createdPayment_triggers_verificationEvent() {
    var apiKey = randomJApplication().getApiKey();
    var email = randomEmail();
    var createdPayment =
        subject.createPayment(apiKey, email, ORANGE_MONEY, randomUUID().toString());

    ArgumentCaptor<List<PaymentVerificationRequested>> captor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMocked, times(1)).accept(captor.capture());
    List<PaymentVerificationRequested> captured = captor.getValue();
    assertEquals(1, captured.size());
    assertEquals(createdPayment, captured.getFirst().getPayment());
  }

  @Test
  void createdPayment_cannotBe_recreated() {
    var apiKey = randomJApplication().getApiKey();
    var email = randomEmail();
    var pspPaymentId = randomUUID().toString();
    subject.createPayment(apiKey, email, ORANGE_MONEY, pspPaymentId);
    assertThrows(
        RuntimeException.class,
        () -> subject.createPayment(apiKey, email, ORANGE_MONEY, pspPaymentId));
    assertThrows(
        RuntimeException.class,
        () -> subject.createPayment(apiKey, randomEmail(), ORANGE_MONEY, pspPaymentId));
  }

  @Test
  void can_find_multiple_payments_by_paymentInfos() {
    var apiKey = randomJApplication().getApiKey();
    var email1 = randomEmail();
    var email2 = randomEmail();
    var email3 = randomEmail();
    var pspPaymentId1 = randomUUID().toString();
    var pspPaymentId2 = randomUUID().toString();
    var pspPaymentId3 = randomUUID().toString();

    var payment1 = subject.createPayment(apiKey, email1, ORANGE_MONEY, pspPaymentId1);
    var payment2 = subject.createPayment(apiKey, email2, ORANGE_MONEY, pspPaymentId2);
    var payment3 = subject.createPayment(apiKey, email3, ORANGE_MONEY, pspPaymentId3);

    var paymentInfos =
        List.of(
            new PaymentInfo(email1, ORANGE_MONEY, pspPaymentId1),
            new PaymentInfo(email2, ORANGE_MONEY, pspPaymentId2),
            new PaymentInfo(email3, ORANGE_MONEY, pspPaymentId3));

    var retrievedPayments = subject.findPaymentsByPaymentInfos(paymentInfos);

    assertEquals(3, retrievedPayments.size());
    assertTrue(retrievedPayments.contains(payment1));
    assertTrue(retrievedPayments.contains(payment2));
    assertTrue(retrievedPayments.contains(payment3));
  }

  @Test
  void can_find_partial_payments_by_paymentInfos() {
    var apiKey = randomJApplication().getApiKey();
    var email1 = randomEmail();
    var email2 = randomEmail();
    var pspPaymentId1 = randomUUID().toString();
    var pspPaymentId2 = randomUUID().toString();
    var nonExistingPspPaymentId = randomUUID().toString();
    var nonExistingEmail = randomEmail();

    var payment1 = subject.createPayment(apiKey, email1, ORANGE_MONEY, pspPaymentId1);
    var payment2 = subject.createPayment(apiKey, email2, ORANGE_MONEY, pspPaymentId2);

    var paymentInfos =
        List.of(
            new PaymentInfo(email1, ORANGE_MONEY, pspPaymentId1),
            new PaymentInfo(email2, ORANGE_MONEY, pspPaymentId2),
            new PaymentInfo(nonExistingEmail, ORANGE_MONEY, nonExistingPspPaymentId));

    var retrievedPayments = subject.findPaymentsByPaymentInfos(paymentInfos);

    assertEquals(3, retrievedPayments.size());
    assertTrue(retrievedPayments.contains(payment1));
    assertTrue(retrievedPayments.contains(payment2));

    // Vérifier que le payment non trouvé est retourné avec le statut FAILED
    var notFoundPayment =
        retrievedPayments.stream()
            .filter(p -> p.payer().email().equals(nonExistingEmail))
            .findFirst()
            .orElseThrow();

    assertNull(notFoundPayment.id());
    assertEquals(ORANGE_MONEY, notFoundPayment.pspPayment().pspType());
    assertEquals(nonExistingPspPaymentId, notFoundPayment.pspPayment().id());
    assertEquals(nonExistingEmail, notFoundPayment.payer().email());
    assertEquals(FAILED, notFoundPayment.getVerificationStatus());
  }

  @Test
  void findPaymentsByPaymentInfos_returns_failed_payments_when_no_match() {
    var email1 = randomEmail();
    var email2 = randomEmail();
    var pspPaymentId1 = randomUUID().toString();
    var pspPaymentId2 = randomUUID().toString();

    var paymentInfos =
        List.of(
            new PaymentInfo(email1, ORANGE_MONEY, pspPaymentId1),
            new PaymentInfo(email2, ORANGE_MONEY, pspPaymentId2));

    var retrievedPayments = subject.findPaymentsByPaymentInfos(paymentInfos);

    assertEquals(2, retrievedPayments.size());

    retrievedPayments.forEach(
        payment -> {
          assertNull(payment.id());
          assertEquals(ORANGE_MONEY, payment.pspPayment().pspType());
          assertEquals(FAILED, payment.getVerificationStatus());
        });

    var emails = retrievedPayments.stream().map(p -> p.payer().email()).toList();
    assertTrue(emails.contains(email1));
    assertTrue(emails.contains(email2));

    var pspPaymentIds = retrievedPayments.stream().map(p -> p.pspPayment().id()).toList();
    assertTrue(pspPaymentIds.contains(pspPaymentId1));
    assertTrue(pspPaymentIds.contains(pspPaymentId2));
  }

  private static String randomEmail() {
    return "lou+ " + randomUUID() + "@cute.dev";
  }
}
