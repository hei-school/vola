package school.hei.vola.endpoint.rest.controller;

import static java.nio.file.Files.readAllBytes;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.test.annotation.DirtiesContext.MethodMode.BEFORE_METHOD;
import static school.hei.vola.conf.TestData.ORANGE_REF_SUCCEEDED;
import static school.hei.vola.model.VerificationStatus.FAILED;
import static school.hei.vola.model.VerificationStatus.SUCCEEDED;
import static school.hei.vola.model.VerificationStatus.VERIFYING;
import static school.hei.vola.model.psp.PspType.ORANGE_MONEY;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import school.hei.vola.conf.FacadeIT;
import school.hei.vola.endpoint.event.EventProducer;
import school.hei.vola.endpoint.event.model.OrangeDailyTransactionsRetrievalRequested;
import school.hei.vola.endpoint.event.model.OrangeTransactionsImportRequested;
import school.hei.vola.endpoint.event.model.PaymentVerificationRequested;
import school.hei.vola.file.bucket.BucketComponent;
import school.hei.vola.repository.jpa.JApplicationRepository;
import school.hei.vola.repository.jpa.model.JApplication;
import school.hei.vola.service.event.OrangeDailyTransactionsRetrievalRequestedService;
import school.hei.vola.service.event.PaymentVerificationRequestedService;

@Slf4j
class PaymentControllerIT extends FacadeIT {

  @Autowired PaymentController subject;
  @MockBean EventProducer eventProducerMocked;
  @MockBean BucketComponent bucketComponent;
  @Captor ArgumentCaptor<List<OrangeTransactionsImportRequested>> eventCaptor;

  @Autowired
  private OrangeDailyTransactionsRetrievalRequestedService
      orangeDailyTransactionsRetrievalRequestedService;

  @Autowired private PaymentVerificationRequestedService paymentVerificationRequestedService;

  @Autowired JApplicationRepository jApplicationRepository;

  JApplication randomJApplication() {
    var jApplication = new JApplication();
    jApplication.setName(randomUUID().toString());
    jApplication.setId(randomUUID().toString());
    jApplication.setApiKey(randomUUID().toString());
    jApplicationRepository.save(jApplication);
    return jApplication;
  }

  @DirtiesContext(methodMode = BEFORE_METHOD) // note(unique_pspPayment)
  @Test
  void can_create_payment_then_retrieve_it() {
    var apiKey = randomJApplication().getApiKey();
    var email = randomEmail();
    var pspType = ORANGE_MONEY;
    var pspPaymentId = ORANGE_REF_SUCCEEDED;

    var createdPayment = subject.createPayment(apiKey, email, pspType, pspPaymentId, null);
    assertNotNull(createdPayment.id());
    assertNull(createdPayment.pspPayment().amount());
    assertNull(createdPayment.lastPspVerificationInstant());
    assertEquals(VERIFYING, createdPayment.getVerificationStatus());

    var retrievedPayment = subject.getPayment(apiKey, email, pspType, pspPaymentId);
    assertEquals(createdPayment, retrievedPayment);
  }

  @Disabled("Requires real Orange API key")
  @DirtiesContext(methodMode = BEFORE_METHOD) // note(unique_pspPayment)
  @Test
  void can_create_payment_beforeOrangeDailyRetrieval_then_verify_it() {
    var apiKey = randomJApplication().getApiKey();
    var email = randomEmail();
    var pspType = ORANGE_MONEY;
    var pspPaymentId = ORANGE_REF_SUCCEEDED;

    var createdPayment = subject.createPayment(apiKey, email, pspType, pspPaymentId, null);
    assertNotNull(createdPayment.id());
    assertNull(createdPayment.pspPayment().amount());
    assertNull(createdPayment.lastPspVerificationInstant());
    assertEquals(VERIFYING, createdPayment.getVerificationStatus());

    orangeDailyTransactionsRetrievalRequestedService.accept(
        new OrangeDailyTransactionsRetrievalRequested(LocalDate.of(2026, 5, 20)));

    var retrievedPayment = subject.getPayment(apiKey, email, pspType, pspPaymentId);
    assertEquals(
        createdPayment.pspPayment().toBuilder()
            .amount(316800)
            .creationInstant(Instant.parse("2026-05-20T06:31:26Z"))
            .build(),
        retrievedPayment.pspPayment());
    assertNotNull(retrievedPayment.lastPspVerificationInstant());
    assertEquals(SUCCEEDED, retrievedPayment.getVerificationStatus());
  }

  @Disabled("Requires real Orange API key")
  @DirtiesContext(methodMode = BEFORE_METHOD) // note(unique_pspPayment)
  @Test
  void can_create_payment_afterOrangeDailyRetrieval_then_verify_it() {
    var apiKey = randomJApplication().getApiKey();
    var email = randomEmail();
    var pspType = ORANGE_MONEY;
    var pspPaymentId = ORANGE_REF_SUCCEEDED;

    try {
      orangeDailyTransactionsRetrievalRequestedService.accept(
          new OrangeDailyTransactionsRetrievalRequested(LocalDate.of(2026, 5, 20)));

    } catch (Exception e) {
      throw new RuntimeException("The error is ", e);
    }

    var createdPayment = subject.createPayment(apiKey, email, pspType, pspPaymentId, null);
    assertNotNull(createdPayment.id());
    assertNull(createdPayment.pspPayment().amount());
    assertNull(createdPayment.lastPspVerificationInstant());
    assertEquals(VERIFYING, createdPayment.getVerificationStatus());

    var retrievedPayment = subject.getPayment(apiKey, email, pspType, pspPaymentId);
    assertEquals(createdPayment, retrievedPayment);

    ArgumentCaptor<List<PaymentVerificationRequested>> captor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMocked, times(1)).accept(captor.capture());
    List<PaymentVerificationRequested> captured = captor.getValue();
    assertEquals(1, captured.size());
    paymentVerificationRequestedService.accept(captured.get(0));

    retrievedPayment = subject.getPayment(apiKey, email, pspType, pspPaymentId);
    assertEquals(
        createdPayment.pspPayment().toBuilder()
            .amount(316800)
            .creationInstant(Instant.parse("2026-05-20T06:31:26Z"))
            .build(),
        retrievedPayment.pspPayment());
    assertNotNull(retrievedPayment.lastPspVerificationInstant());
    assertEquals(SUCCEEDED, retrievedPayment.getVerificationStatus());
  }

  @Test
  void can_create_payment_then_fail_it() {
    var apiKey = randomJApplication().getApiKey();
    var email = randomEmail();
    var pspType = ORANGE_MONEY;
    var pspPaymentId = "non-existing";

    var createdPayment = subject.createPayment(apiKey, email, pspType, pspPaymentId, null);
    assertNotNull(createdPayment.id());
    assertNull(createdPayment.pspPayment().amount());
    assertNull(createdPayment.lastPspVerificationInstant());
    assertEquals(VERIFYING, createdPayment.getVerificationStatus());

    ArgumentCaptor<List<PaymentVerificationRequested>> captor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMocked, times(1)).accept(captor.capture());
    var captured = captor.getValue();
    assertEquals(1, captured.size());
    assertEquals(createdPayment, captured.get(0).getPayment());
    paymentVerificationRequestedService.accept(
        new PaymentVerificationRequested(createdPayment) {
          @Override
          public int getAttemptNb() {
            return 7;
          }
        });

    var retrievedPayment = subject.getPayment(apiKey, email, pspType, pspPaymentId);
    assertEquals(FAILED, retrievedPayment.getVerificationStatus());
  }

  @Test
  void save_transactions_from_xls_file_OK() throws IOException {
    var apiKey = randomJApplication().getApiKey();
    var path = Paths.get("src/test/resources/mock/transaction-to-save.xls");
    var file =
        new MockMultipartFile(
            "transaction-to-save.xls",
            "transaction-to-save.xls",
            "application/vnd.ms-excel",
            readAllBytes(path));
    var bucketKey = "/TRANSACTIONS_XLS_IMPORT/" + file.getName();
    subject.saveTransactions(file, apiKey);

    verify(eventProducerMocked).accept(eventCaptor.capture());

    var events = eventCaptor.getValue();
    assertEquals(1, events.size());
    assertTrue(events.getFirst().getBucketKey().contains(bucketKey));
  }

  @Test
  void save_transactions_from_xls_file_K0() throws IOException {
    var apiKey = randomJApplication().getApiKey();
    var path = Paths.get("src/test/resources/mock/bad-transactions-data.xls");
    var file =
        new MockMultipartFile(
            "bad-transactions-data.xls",
            "bad-transactions-data.xls",
            "application/vnd.ms-excel",
            readAllBytes(path));

    var bucketKey = "/TRANSACTIONS_XLS_IMPORT/" + file.getName();
    subject.saveTransactions(file, apiKey);

    verify(eventProducerMocked).accept(eventCaptor.capture());

    var events = eventCaptor.getValue();
    assertEquals(1, events.size());
    assertTrue(events.getFirst().getBucketKey().contains(bucketKey));
  }

  @Test
  void exportPaymentsCsv_returns_csv_with_correct_headers() {
    var app = randomJApplication();
    var apiKey = app.getApiKey();
    var appName = app.getName();
    var email = randomEmail();

    subject.createPayment(apiKey, email, ORANGE_MONEY, randomUUID().toString(), null);

    var response = subject.exportPaymentsCsv(appName, null, null, null);

    assertEquals(200, response.getStatusCodeValue());
    assertTrue(response.getHeaders().get(CONTENT_DISPOSITION).getFirst().contains("attachment"));
    assertTrue(response.getHeaders().getContentType().toString().contains("text/csv"));
    var body = new String(response.getBody());
    assertTrue(body.contains("Email payeur"));
    assertTrue(body.contains(email));
    assertTrue(body.contains(appName));
  }

  @Test
  void exportPaymentsCsv_filters_by_date_range() {
    var app = randomJApplication();
    var apiKey = app.getApiKey();
    var appName = app.getName();
    var email = randomEmail();

    subject.createPayment(apiKey, email, ORANGE_MONEY, randomUUID().toString(), null);

    var response =
        subject.exportPaymentsCsv(
            appName, null, LocalDate.of(2020, 1, 1), LocalDate.of(2099, 1, 1));

    assertEquals(200, response.getStatusCodeValue());
    var body = new String(response.getBody());
    assertTrue(body.contains(email));
  }

  @Test
  void exportPaymentsCsv_outside_date_range_returns_header_only() {
    var app = randomJApplication();
    var apiKey = app.getApiKey();
    var appName = app.getName();

    subject.createPayment(apiKey, randomEmail(), ORANGE_MONEY, randomUUID().toString(), null);

    var response =
        subject.exportPaymentsCsv(
            appName, null, LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 2));

    assertEquals(200, response.getStatusCodeValue());
    var body = new String(response.getBody());
    var lines = body.split("\n");
    assertEquals(1, lines.length);
  }

  @Test
  void exportPaymentsCsv_filename_contains_application_name() {
    var app = randomJApplication();
    var apiKey = app.getApiKey();
    var appName = app.getName();

    subject.createPayment(apiKey, randomEmail(), ORANGE_MONEY, randomUUID().toString(), null);

    var response = subject.exportPaymentsCsv(appName, null, null, null);

    var disposition = response.getHeaders().get(CONTENT_DISPOSITION).getFirst();
    assertTrue(disposition.contains("payments_" + appName + ".csv"));
  }

  @Test
  void exportPaymentsCsv_with_all_returns_payments_from_all_apps() {
    var app1 = randomJApplication();
    var app2 = randomJApplication();
    var email1 = randomEmail();
    var email2 = randomEmail();

    subject.createPayment(app1.getApiKey(), email1, ORANGE_MONEY, randomUUID().toString(), null);
    subject.createPayment(app2.getApiKey(), email2, ORANGE_MONEY, randomUUID().toString(), null);

    var response = subject.exportPaymentsCsv("all", null, null, null);

    assertEquals(200, response.getStatusCodeValue());
    var body = new String(response.getBody());
    assertTrue(body.contains(email1));
    assertTrue(body.contains(email2));
  }

  @Test
  void exportPaymentsCsv_with_all_uses_all_in_filename() {
    var app = randomJApplication();
    subject.createPayment(
        app.getApiKey(), randomEmail(), ORANGE_MONEY, randomUUID().toString(), null);

    var response = subject.exportPaymentsCsv("all", null, null, null);

    var disposition = response.getHeaders().get(CONTENT_DISPOSITION).getFirst();
    assertTrue(disposition.contains("payments_all.csv"));
  }

  private static String randomEmail() {
    return "lou+" + randomUUID() + "@cute.dev";
  }
}
