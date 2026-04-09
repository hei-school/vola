package school.hei.vola.service;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static school.hei.vola.model.psp.PspType.ORANGE_MONEY;

import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import school.hei.vola.conf.FacadeIT;
import school.hei.vola.endpoint.event.EventProducer;
import school.hei.vola.endpoint.event.model.PaymentVerificationRequested;
import school.hei.vola.model.PaymentInfo;
import school.hei.vola.repository.jpa.JApplicationRepository;
import school.hei.vola.repository.jpa.model.JApplication;

@Slf4j
class PaymentServiceIT extends FacadeIT {
  @Autowired PaymentService subject;
  @MockBean EventProducer eventProducerMocked;
  @Autowired JApplicationRepository jApplicationRepository;

  @Captor ArgumentCaptor<List<PaymentVerificationRequested>> eventCaptor;

  @BeforeEach
  void setUp() {
    reset(eventProducerMocked);
  }

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

    var retrieved = subject.findPaymentsByPaymentInfos(apiKey, paymentInfos);

    assertEquals(1, retrieved.size());
    assertNotNull(retrieved.getFirst().id());
  }

  @Test
  void skip_nonexistent_payments_and_trigger_creation() {
    var apiKey = randomJApplication().getApiKey();
    var nonExistentPaymentInfo = randomPaymentInfo();

    var paymentInfos = List.of(nonExistentPaymentInfo);
    var retrieved = subject.findPaymentsByPaymentInfos(apiKey, paymentInfos);

    assertTrue(retrieved.isEmpty());
    verify(eventProducerMocked).accept(eventCaptor.capture());
    var sentEvents = eventCaptor.getValue();
    assertEquals(1, sentEvents.size());
    assertEquals(
        nonExistentPaymentInfo.pspPaymentId(),
        sentEvents.getFirst().getPayment().pspPayment().id());
  }

  @Test
  void find_only_existing_payments_when_mixed_with_nonexistent() {
    var apiKey = randomJApplication().getApiKey();
    var existingPayments =
        List.of(randomPaymentInfo(), randomPaymentInfo(), randomPaymentInfo(), randomPaymentInfo());
    var nonExistentPayments =
        List.of(randomPaymentInfo(), randomPaymentInfo(), randomPaymentInfo());

    createPayments(apiKey, existingPayments);
    reset(eventProducerMocked);

    var allPaymentInfos = mergePaymentInfos(existingPayments, nonExistentPayments);
    var retrieved = subject.findPaymentsByPaymentInfos(apiKey, allPaymentInfos);

    assertEquals(existingPayments.size(), retrieved.size());
    assertNotNull(retrieved.getFirst().id());
    verify(eventProducerMocked).accept(eventCaptor.capture());
    var sentEvents = eventCaptor.getValue();
    assertEquals(nonExistentPayments.size(), sentEvents.size());
  }

  @Test
  void findPayments_matches_on_exact_triplet() {
    var apiKey = randomJApplication().getApiKey();
    var paymentOne = randomPaymentInfo();
    var compositePayment =
        PaymentInfo.builder()
            .payerEmail(paymentOne.payerEmail())
            .pspType(ORANGE_MONEY)
            .pspPaymentId(randomUUID().toString())
            .build();
    createPayments(apiKey, List.of(paymentOne));
    reset(eventProducerMocked);

    var retrieved = subject.findPaymentsByPaymentInfos(apiKey, List.of(compositePayment));

    assertTrue(retrieved.isEmpty());
    verify(eventProducerMocked).accept(eventCaptor.capture());
    assertEquals(1, eventCaptor.getValue().size());
  }

  @Test
  void findPaymentsByInfo_retrieve_the_exact_payment_in_base() {
    var apiKey = randomJApplication().getApiKey();
    var paymentInfo = randomPaymentInfo();
    createPayments(apiKey, List.of(paymentInfo));

    var expected =
        subject.findPaymentByPayerEmailAndPspTypeAndPspPaymentId(
            paymentInfo.payerEmail(), paymentInfo.pspType(), paymentInfo.pspPaymentId());

    var actual =
        Optional.of(subject.findPaymentsByPaymentInfos(apiKey, List.of(paymentInfo)).getFirst());

    assertEquals(expected, actual);
    assertEquals(expected.get().id(), actual.get().id());
  }

  @Test
  void all_existing_payments_triggers_no_creation() {
    var apiKey = randomJApplication().getApiKey();
    var paymentInfos = List.of(randomPaymentInfo(), randomPaymentInfo());
    createPayments(apiKey, paymentInfos);
    reset(eventProducerMocked);

    var retrieved = subject.findPaymentsByPaymentInfos(apiKey, paymentInfos);

    assertEquals(2, retrieved.size());
    verify(eventProducerMocked, never()).accept(any());
  }

  @Test
  void empty_payment_infos_returns_empty_and_no_creation() {
    var apiKey = randomJApplication().getApiKey();

    var retrieved = subject.findPaymentsByPaymentInfos(apiKey, List.of());

    assertTrue(retrieved.isEmpty());
    verify(eventProducerMocked, never()).accept(any());
  }

  @Test
  void all_missing_payments_returns_empty_and_creates_all() {
    var apiKey = randomJApplication().getApiKey();
    var missingInfos = List.of(randomPaymentInfo(), randomPaymentInfo(), randomPaymentInfo());

    var retrieved = subject.findPaymentsByPaymentInfos(apiKey, missingInfos);

    assertTrue(retrieved.isEmpty());
    verify(eventProducerMocked).accept(eventCaptor.capture());
    var sentEvents = eventCaptor.getValue();
    assertEquals(missingInfos.size(), sentEvents.size());
  }

  @Test
  void mixed_search_returns_only_existing_and_creates_only_missing() {
    var apiKey = randomJApplication().getApiKey();
    var existing = List.of(randomPaymentInfo(), randomPaymentInfo());
    var missing = List.of(randomPaymentInfo());
    createPayments(apiKey, existing);
    reset(eventProducerMocked);

    var allInfos = List.of(existing.getFirst(), missing.getFirst(), existing.get(1));
    var retrieved = subject.findPaymentsByPaymentInfos(apiKey, allInfos);

    assertEquals(2, retrieved.size());
    retrieved.forEach(p -> assertNotNull(p.id()));

    verify(eventProducerMocked).accept(eventCaptor.capture());
    var sentEvents = eventCaptor.getValue();
    assertEquals(1, sentEvents.size());
    assertEquals(
        missing.getFirst().pspPaymentId(), sentEvents.getFirst().getPayment().pspPayment().id());
  }

  @Test
  void createPayment_throws_on_duplicate_pspPaymentId() {
    var apiKey = randomJApplication().getApiKey();
    var paymentInfo = randomPaymentInfo();
    subject.createPayment(
        apiKey, paymentInfo.payerEmail(), paymentInfo.pspType(), paymentInfo.pspPaymentId());

    var exception =
        assertThrows(
            RuntimeException.class,
            () ->
                subject.createPayment(
                    apiKey, randomEmail(), paymentInfo.pspType(), paymentInfo.pspPaymentId()));
    assertInstanceOf(IllegalArgumentException.class, exception.getCause());
  }

  @Test
  void createPayments_skips_duplicate_pspPaymentIds() {
    var apiKey = randomJApplication().getApiKey();
    var existingInfo = randomPaymentInfo();
    subject.createPayment(
        apiKey, existingInfo.payerEmail(), existingInfo.pspType(), existingInfo.pspPaymentId());
    reset(eventProducerMocked);

    var newInfo = randomPaymentInfo();
    var duplicateInfo =
        PaymentInfo.builder()
            .payerEmail(randomEmail())
            .pspType(existingInfo.pspType())
            .pspPaymentId(existingInfo.pspPaymentId())
            .build();

    var created = subject.createPayments(apiKey, List.of(newInfo, duplicateInfo));

    assertEquals(1, created.size());
    assertEquals(newInfo.pspPaymentId(), created.getFirst().pspPayment().id());
    verify(eventProducerMocked).accept(eventCaptor.capture());
    assertEquals(1, eventCaptor.getValue().size());
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
