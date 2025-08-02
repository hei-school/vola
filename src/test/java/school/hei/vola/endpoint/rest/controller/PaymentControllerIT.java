package school.hei.vola.endpoint.rest.controller;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static school.hei.vola.model.psp.PspType.ORANGE_MONEY;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import school.hei.vola.conf.FacadeIT;
import school.hei.vola.endpoint.event.EventProducer;
import school.hei.vola.endpoint.event.model.OrangeDailyTransactionsRetrievalRequested;
import school.hei.vola.model.User;
import school.hei.vola.service.event.OrangeDailyTransactionsRetrievalRequestedService;

class PaymentControllerIT extends FacadeIT {

  @Autowired PaymentController subject;
  @MockBean EventProducer eventProducerMocked;

  @Autowired
  private OrangeDailyTransactionsRetrievalRequestedService
      orangeDailyTransactionsRetrievalRequestedService;

  @Test
  void can_create_payment_then_retrieve_it() {
    var apiKey = "dummyApiKey";

    var createdPayment =
        subject.createPayment(apiKey, randomLou(), ORANGE_MONEY, "MP250729.1216.D77954");
    var paymentId = createdPayment.id();
    assertNotNull(paymentId);
    assertNull(createdPayment.pspPayment().amount());
    assertNull(createdPayment.lastPspVerificationInstant());

    var retrievedPayment = subject.getPayment(apiKey, paymentId);
    assertEquals(createdPayment, retrievedPayment);
  }

  @Test
  void can_create_payment_then_verify_it() {
    var apiKey = "dummyApiKey";

    var createdPayment =
        subject.createPayment(apiKey, randomLou(), ORANGE_MONEY, "MP250729.1216.D77954");
    var paymentId = createdPayment.id();
    assertNotNull(paymentId);
    assertNull(createdPayment.pspPayment().amount());
    assertNull(createdPayment.lastPspVerificationInstant());

    orangeDailyTransactionsRetrievalRequestedService.accept(
        new OrangeDailyTransactionsRetrievalRequested(LocalDate.of(2025, 7, 29)));

    var retrievedPayment = subject.getPayment(apiKey, paymentId);
    assertEquals(
        createdPayment.pspPayment().toBuilder()
            .amount(324_000)
            .creationInstant(Instant.parse("2025-07-29T12:16:58Z"))
            .build(),
        retrievedPayment.pspPayment());
    assertNotNull(retrievedPayment.lastPspVerificationInstant());
  }

  private static User randomLou() {
    return new User("lou+ " + randomUUID() + "@cute.dev");
  }
}
