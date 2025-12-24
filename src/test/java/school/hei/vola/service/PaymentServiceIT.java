package school.hei.vola.service;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static school.hei.vola.model.psp.PspType.ORANGE_MONEY;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import school.hei.vola.conf.FacadeIT;
import school.hei.vola.endpoint.event.EventProducer;
import school.hei.vola.repository.jpa.JApplicationRepository;
import school.hei.vola.repository.jpa.model.JApplication;

class PaymentServiceIT extends FacadeIT {

  @Autowired PaymentService subject;
  @MockBean EventProducer eventProducerMocked;
  @Autowired JApplicationRepository jApplicationRepository;

  @Test
  void createdPayment_canBe_retrieved() {
    var apiKey = randomJApplication().getApiKey();
    var email = randomEmail();
    var pspPaymentId = randomUUID().toString();

    var created = subject.createPayment(apiKey, email, ORANGE_MONEY, pspPaymentId);
    var retrieved =
        subject
            .findPaymentByPayerEmailAndPspTypeAndPspPaymentId(email, ORANGE_MONEY, pspPaymentId)
            .get();

    assertEquals(created, retrieved);
    assertNotNull(retrieved.id());
  }

  private JApplication randomJApplication() {
    var app = new JApplication();
    app.setName(randomUUID().toString());
    app.setId(randomUUID().toString());
    app.setApiKey(randomUUID().toString());
    return jApplicationRepository.save(app);
  }

  private static String randomEmail() {
    return "lou+ " + randomUUID() + "@cute.dev";
  }
}
