package school.hei.vola.service;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static school.hei.vola.model.psp.PspType.ORANGE_MONEY;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import school.hei.vola.conf.FacadeIT;
import school.hei.vola.endpoint.event.EventProducer;
import school.hei.vola.endpoint.event.model.PaymentVerificationRequested;
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

  private static String randomEmail() {
    return "lou+ " + randomUUID() + "@cute.dev";
  }
}
