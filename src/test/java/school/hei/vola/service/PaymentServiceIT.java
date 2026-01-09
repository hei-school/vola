package school.hei.vola.service;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static school.hei.vola.model.psp.PspType.ORANGE_MONEY;

import java.util.List;
import java.util.Optional;
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

  @Test
  void createdPayment_canBe_retrieved() {
    var email = randomEmail();
    var apiKey = randomJApplication().getApiKey();
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
    var email = "lou@hei.school";
    var apiKey = randomJApplication().getApiKey();
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
    assertNotNull(retrieved.getFirst().id());
  }

  @Test
  void skip_nonexistent_payments() {
    var nonExistentPaymentInfo = randomPaymentInfo();

    var paymentInfos = List.of(nonExistentPaymentInfo);
    var retrieved = subject.findPaymentsByPaymentInfos(paymentInfos);

    assertTrue(retrieved.isEmpty());
  }

  @Test
  void find_only_existing_payments_when_mixed_with_nonexistent() {
    var apiKey = randomJApplication().getApiKey();
    var existingPayments =
        List.of(randomPaymentInfo(), randomPaymentInfo(), randomPaymentInfo(), randomPaymentInfo());
    var nonExistentPayments =
        List.of(randomPaymentInfo(), randomPaymentInfo(), randomPaymentInfo());

    createPayments(apiKey, existingPayments);

    var allPaymentInfos = mergePaymentInfos(existingPayments, nonExistentPayments);
    var retrieved = subject.findPaymentsByPaymentInfos(allPaymentInfos);

    assertEquals(existingPayments.size(), retrieved.size());
    assertNotNull(retrieved.getFirst().id());
  }

  @Test
  void findPaymentsByInfo_retireve_the_exact_payment_in_base() {
    var apiKey = randomJApplication().getApiKey();
    var paymentInfo = randomPaymentInfo();
    createPayments(apiKey, List.of(paymentInfo));

    var expected =
        subject.findPaymentByPayerEmailAndPspTypeAndPspPaymentId(
            paymentInfo.payerEmail(), paymentInfo.pspType(), paymentInfo.pspPaymentId());

    var actual = Optional.of(subject.findPaymentsByPaymentInfos(List.of(paymentInfo)).getFirst());

    assertEquals(expected, actual);
    assertEquals(expected.get().id(), actual.get().id());
  }

  private void createPayments(String apiKey, List<PaymentInfo> paymentInfos) {
    paymentInfos.forEach(
        info ->
            subject.createPayment(apiKey, info.payerEmail(), info.pspType(), info.pspPaymentId()));
  }

  private List<PaymentInfo> mergePaymentInfos(
      List<PaymentInfo> existing, List<PaymentInfo> nonExistent) {
    return List.of(
        existing.get(1),
        existing.getFirst(),
        existing.get(2),
        existing.get(3),
        nonExistent.getFirst(),
        nonExistent.get(1),
        nonExistent.get(2));
  }

  private JApplication randomJApplication() {
    var app = new JApplication();
    app.setName(randomUUID().toString());
    app.setId(randomUUID().toString());
    app.setApiKey(randomUUID().toString());
    return jApplicationRepository.save(app);
  }

  private static PaymentInfo randomPaymentInfo() {
    return PaymentInfo.builder()
        .payerEmail(randomEmail())
        .pspType(ORANGE_MONEY)
        .pspPaymentId(randomUUID().toString())
        .build();
  }

  private static String randomEmail() {
    return "lou+" + randomUUID() + "@cute.dev";
  }
}
