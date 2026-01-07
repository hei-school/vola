package school.hei.vola.service;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static school.hei.vola.model.psp.PspType.ORANGE_MONEY;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import school.hei.vola.conf.FacadeIT;
import school.hei.vola.endpoint.event.EventProducer;
import school.hei.vola.model.PaymentInfo;
import school.hei.vola.repository.jpa.JApplicationRepository;
import school.hei.vola.repository.jpa.model.JApplication;

class PaymentServiceIT extends FacadeIT {
  @Autowired PaymentService subject;
  @MockBean EventProducer eventProducerMocked;
  @Autowired JApplicationRepository jApplicationRepository;

  private JApplication randomJApplication() {
    var app = new JApplication();
    app.setName(randomUUID().toString());
    app.setId(randomUUID().toString());
    app.setApiKey(randomUUID().toString());
    return jApplicationRepository.save(app);
  }

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

  @Test
  void find_existing_payment_by_info() {
    var apiKey = randomJApplication().getApiKey();
    var email = "lou@hei.school";
    var pspPaymentId = "MP250729.1216.D77954";
    subject.createPayment(apiKey, email, ORANGE_MONEY, pspPaymentId);

    var existingPaymentInfo =
        PaymentInfo.builder()
            .payerEmail(email)
            .pspPaymentId(pspPaymentId)
            .pspType(ORANGE_MONEY)
            .build();
    var paymentInfos = List.of(existingPaymentInfo);

    var retrieved = subject.findPaymentsByPaymentInfos(paymentInfos);

    assertEquals(1, retrieved.size());
    assertNotNull(retrieved.get(0).id());
  }

  @Test
  void skip_nonexistent_payments() {
    var nonExistentPaymentInfo =
        PaymentInfo.builder()
            .payerEmail(randomEmail())
            .pspPaymentId(randomUUID().toString())
            .pspType(ORANGE_MONEY)
            .build();

    var paymentInfos = List.of(nonExistentPaymentInfo);
    var retrieved = subject.findPaymentsByPaymentInfos(paymentInfos);

    assertTrue(retrieved.isEmpty());
  }

  private static String randomEmail() {
    return "lou+ " + randomUUID() + "@cute.dev";
  }
}
