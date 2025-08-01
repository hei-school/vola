package school.hei.vola.endpoint.rest.controller;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static school.hei.vola.model.psp.PspType.ORANGE_MONEY;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import school.hei.vola.conf.FacadeIT;
import school.hei.vola.endpoint.event.EventProducer;
import school.hei.vola.model.User;

class PaymentControllerIT extends FacadeIT {

  @Autowired PaymentController subject;
  @MockBean EventProducer eventProducerMocked;

  @Test
  void can_create_payment_then_retrieve_it() {
    var apiKey = "dummyApiKey";

    var createdPayment = subject.createPayment(apiKey, randomLou(), ORANGE_MONEY, "pspId");
    var paymentId = createdPayment.id();
    assertNotNull(paymentId);
    assertNull(createdPayment.pspPayment().amount());

    var retrievedPayment = subject.getPayment(apiKey, paymentId);
    assertEquals(createdPayment, retrievedPayment);
  }

  private static User randomLou() {
    return new User("lou+ " + randomUUID() + "@cute.dev");
  }
}
