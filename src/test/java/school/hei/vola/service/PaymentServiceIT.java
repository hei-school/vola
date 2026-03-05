package school.hei.vola.service;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static school.hei.vola.model.VerificationStatus.SUCCEEDED;
import static school.hei.vola.model.psp.PspType.ORANGE_MONEY;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import school.hei.vola.conf.FacadeIT;
import school.hei.vola.endpoint.event.EventProducer;
import school.hei.vola.model.PaymentInfo;
import school.hei.vola.model.psp.orange.OrangeApiClient;
import school.hei.vola.model.psp.orange.OrangeDailyTransactions;
import school.hei.vola.model.psp.orange.OrangeTransaction;
import school.hei.vola.repository.jpa.JApplicationRepository;
import school.hei.vola.repository.jpa.model.JApplication;

class PaymentServiceIT extends FacadeIT {
  @Autowired PaymentService subject;
  @MockBean EventProducer eventProducerMocked;
  @MockBean OrangeApiClient orangeApiClientMocked;
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
    when(orangeApiClientMocked.transactionsOf(any()))
        .thenReturn(new OrangeDailyTransactions(null, List.of()));
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
  void skip_nonexistent_payments() {
    when(orangeApiClientMocked.transactionsOf(any()))
        .thenReturn(new OrangeDailyTransactions(null, List.of()));
    var apiKey = randomJApplication().getApiKey();
    var nonExistentPaymentInfo = randomPaymentInfo();

    var paymentInfos = List.of(nonExistentPaymentInfo);
    var retrieved = subject.findPaymentsByPaymentInfos(apiKey, paymentInfos);

    assertTrue(retrieved.isEmpty());
  }

  @Test
  void find_only_existing_payments_when_mixed_with_nonexistent() {
    when(orangeApiClientMocked.transactionsOf(any()))
        .thenReturn(new OrangeDailyTransactions(null, List.of()));
    var apiKey = randomJApplication().getApiKey();
    var existingPayments =
        List.of(randomPaymentInfo(), randomPaymentInfo(), randomPaymentInfo(), randomPaymentInfo());
    var nonExistentPayments =
        List.of(randomPaymentInfo(), randomPaymentInfo(), randomPaymentInfo());

    createPayments(apiKey, existingPayments);

    var allPaymentInfos = mergePaymentInfos(existingPayments, nonExistentPayments);
    var retrieved = subject.findPaymentsByPaymentInfos(apiKey, allPaymentInfos);

    assertEquals(existingPayments.size(), retrieved.size());
    assertNotNull(retrieved.getFirst().id());
  }

  @Test
  void findPayments_use_the_exact_same_process_as_findPaymentMethod() {
    when(orangeApiClientMocked.transactionsOf(any()))
        .thenReturn(new OrangeDailyTransactions(null, List.of()));
    var apiKey = randomJApplication().getApiKey();
    var paymentOne = randomPaymentInfo();
    var paymentTwo = randomPaymentInfo();
    var compositePayment =
        PaymentInfo.builder()
            .payerEmail(paymentOne.payerEmail())
            .pspType(ORANGE_MONEY)
            .pspPaymentId(paymentTwo.pspPaymentId())
            .build();
    var persistedPaymentList = List.of(paymentOne, paymentTwo);
    createPayments(apiKey, persistedPaymentList);

    assertEquals(List.of(), subject.findPaymentsByPaymentInfos(apiKey, List.of(compositePayment)));
    assertNotNull(subject.findPaymentsByPaymentInfos(apiKey, persistedPaymentList));
  }

  @Test
  void findPaymentsByInfo_retireve_the_exact_payment_in_base() {
    when(orangeApiClientMocked.transactionsOf(any()))
        .thenReturn(new OrangeDailyTransactions(null, List.of()));
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
  void extractDateFromPspPaymentId_parses_correctly() {
    assertEquals(
        LocalDate.of(2026, 2, 27),
        PaymentService.extractDateFromPspPaymentId("MP260227.0706.A57074"));
    assertEquals(
        LocalDate.of(2025, 7, 29),
        PaymentService.extractDateFromPspPaymentId("MP250729.1216.D77954"));
    assertEquals(
        LocalDate.of(2026, 3, 5),
        PaymentService.extractDateFromPspPaymentId("MP260305.1430.B12345"));
  }

  @Test
  void createPayments_returns_existing_when_already_in_db() {
    when(orangeApiClientMocked.transactionsOf(any()))
        .thenReturn(new OrangeDailyTransactions(null, List.of()));
    var apiKey = randomJApplication().getApiKey();
    var paymentInfo = randomPaymentInfo();
    createPayments(apiKey, List.of(paymentInfo));

    var result = subject.createPayments(apiKey, List.of(paymentInfo));

    assertEquals(1, result.size());
    assertNotNull(result.getFirst().id());
  }

  @Test
  void createPayments_auto_creates_from_scrapper_when_not_in_db() {
    var pspPaymentId = "MP260305.1200.C99999";
    var apiKey = randomJApplication().getApiKey();
    var email = randomEmail();
    var paymentInfo =
        PaymentInfo.builder()
            .payerEmail(email)
            .pspType(ORANGE_MONEY)
            .pspPaymentId(pspPaymentId)
            .build();

    var orangeTransaction =
        OrangeTransaction.builder()
            .number(1)
            .date("05/03/2026")
            .time("12:00:00")
            .ref(pspPaymentId)
            .status("Succès")
            .clientNumber("0340000000")
            .amount(50_000)
            .build();

    when(orangeApiClientMocked.transactionsOf(eq(LocalDate.of(2026, 3, 5))))
        .thenReturn(new OrangeDailyTransactions(null, List.of(orangeTransaction)));

    var result = subject.createPayments(apiKey, List.of(paymentInfo));

    assertEquals(1, result.size());
    var created = result.getFirst();
    assertNotNull(created.id());
    assertEquals(50_000, created.pspPayment().amount());
    assertEquals(ORANGE_MONEY, created.pspPayment().pspType());
    assertEquals(pspPaymentId, created.pspPayment().id());
    assertNotNull(created.lastPspVerificationInstant());
    assertEquals(SUCCEEDED, created.getVerificationStatus());
  }

  @Test
  void createPayments_mixes_existing_and_auto_created() {
    var apiKey = randomJApplication().getApiKey();
    var existingInfo = randomPaymentInfo();
    createPayments(apiKey, List.of(existingInfo));

    var newPspPaymentId = "MP260305.1500.D11111";
    var newInfo =
        PaymentInfo.builder()
            .payerEmail(randomEmail())
            .pspType(ORANGE_MONEY)
            .pspPaymentId(newPspPaymentId)
            .build();

    var orangeTransaction =
        OrangeTransaction.builder()
            .number(2)
            .date("05/03/2026")
            .time("15:00:00")
            .ref(newPspPaymentId)
            .status("Succès")
            .clientNumber("0340000001")
            .amount(75_000)
            .build();

    when(orangeApiClientMocked.transactionsOf(eq(LocalDate.of(2026, 3, 5))))
        .thenReturn(new OrangeDailyTransactions(null, List.of(orangeTransaction)));

    var result = subject.createPayments(apiKey, List.of(existingInfo, newInfo));

    assertEquals(2, result.size());
  }

  @Test
  void createPayments_empty_list_returns_empty() {
    when(orangeApiClientMocked.transactionsOf(any()))
        .thenReturn(new OrangeDailyTransactions(null, List.of()));
    var apiKey = randomJApplication().getApiKey();

    var result = subject.createPayments(apiKey, List.of());

    assertTrue(result.isEmpty());
  }

  @Test
  void findPayments_auto_creates_missing_from_scrapper() {
    var pspPaymentId = "MP260305.0900.E22222";
    var apiKey = randomJApplication().getApiKey();
    var email = randomEmail();
    var paymentInfo =
        PaymentInfo.builder()
            .payerEmail(email)
            .pspType(ORANGE_MONEY)
            .pspPaymentId(pspPaymentId)
            .build();

    var orangeTransaction =
        OrangeTransaction.builder()
            .number(3)
            .date("05/03/2026")
            .time("09:00:00")
            .ref(pspPaymentId)
            .status("Succès")
            .clientNumber("0340000002")
            .amount(100_000)
            .build();

    when(orangeApiClientMocked.transactionsOf(eq(LocalDate.of(2026, 3, 5))))
        .thenReturn(new OrangeDailyTransactions(null, List.of(orangeTransaction)));

    var result = subject.findPaymentsByPaymentInfos(apiKey, List.of(paymentInfo));

    assertEquals(1, result.size());
    var payment = result.getFirst();
    assertEquals(100_000, payment.pspPayment().amount());
    assertEquals(SUCCEEDED, payment.getVerificationStatus());
    assertNotNull(payment.lastPspVerificationInstant());
  }

  @Test
  void findPayments_ignores_missing_when_scrapper_has_no_match() {
    var apiKey = randomJApplication().getApiKey();
    var paymentInfo =
        PaymentInfo.builder()
            .payerEmail(randomEmail())
            .pspType(ORANGE_MONEY)
            .pspPaymentId("MP260305.1100.F33333")
            .build();

    when(orangeApiClientMocked.transactionsOf(eq(LocalDate.of(2026, 3, 5))))
        .thenReturn(new OrangeDailyTransactions(null, List.of()));

    var result = subject.findPaymentsByPaymentInfos(apiKey, List.of(paymentInfo));

    assertTrue(result.isEmpty());
  }

  @Test
  void createPayments_gracefully_handles_scrapper_failure() {
    var apiKey = randomJApplication().getApiKey();
    var paymentInfo =
        PaymentInfo.builder()
            .payerEmail(randomEmail())
            .pspType(ORANGE_MONEY)
            .pspPaymentId("MP260305.1300.G44444")
            .build();

    when(orangeApiClientMocked.transactionsOf(any()))
        .thenThrow(new RuntimeException("Orange API down"));

    var result = subject.createPayments(apiKey, List.of(paymentInfo));

    assertTrue(result.isEmpty());
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
        .pspPaymentId(randomOrangePspPaymentId())
        .build();
  }

  private static String randomOrangePspPaymentId() {
    // Format: MP[YYMMDD].[HHMM].[XXXXXX]
    return String.format(
        "MP260305.%04d.%s",
        (int) (Math.random() * 2400), randomUUID().toString().substring(0, 6).toUpperCase());
  }

  private static String randomEmail() {
    return "lou+" + randomUUID() + "@cute.dev";
  }
}
