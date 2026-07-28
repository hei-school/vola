package school.hei.vola.endpoint.rest.controller;

import static java.nio.file.Files.readAllBytes;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.annotation.DirtiesContext.MethodMode.BEFORE_METHOD;
import static school.hei.vola.conf.TestData.ORANGE_REF_SUCCEEDED;
import static school.hei.vola.model.VerificationStatus.FAILED;
import static school.hei.vola.model.VerificationStatus.SUCCEEDED;
import static school.hei.vola.model.VerificationStatus.VERIFYING;
import static school.hei.vola.model.psp.PspType.ORANGE_MONEY;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import school.hei.vola.conf.FacadeIT;
import school.hei.vola.endpoint.event.EventProducer;
import school.hei.vola.endpoint.event.model.OrangeDailyTransactionsRetrievalRequested;
import school.hei.vola.endpoint.event.model.OrangeTransactionsImportRequested;
import school.hei.vola.endpoint.event.model.PaymentVerificationRequested;
import school.hei.vola.file.bucket.BucketComponent;
import school.hei.vola.model.Application;
import school.hei.vola.model.Payment;
import school.hei.vola.model.User;
import school.hei.vola.model.psp.PspPayment;
import school.hei.vola.repository.PaymentRepository;
import school.hei.vola.repository.UserRepository;
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

  @Autowired private PaymentRepository paymentRepository;
  @Autowired private UserRepository userRepository;

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
        new OrangeDailyTransactionsRetrievalRequested(LocalDate.of(2026, 7, 10)));

    var retrievedPayment = subject.getPayment(apiKey, email, pspType, pspPaymentId);
    assertEquals(
        createdPayment.pspPayment().toBuilder()
            .amount(356400)
            .creationInstant(Instant.parse("2026-07-10T14:55:57Z"))
            .build(),
        retrievedPayment.pspPayment());
    assertNotNull(retrievedPayment.lastPspVerificationInstant());
    assertEquals(SUCCEEDED, retrievedPayment.getVerificationStatus());
  }

  @DirtiesContext(methodMode = BEFORE_METHOD) // note(unique_pspPayment)
  @Test
  void can_create_payment_afterOrangeDailyRetrieval_then_verify_it() {
    var apiKey = randomJApplication().getApiKey();
    var email = randomEmail();
    var pspType = ORANGE_MONEY;
    var pspPaymentId = ORANGE_REF_SUCCEEDED;
    try {
      orangeDailyTransactionsRetrievalRequestedService.accept(
          new OrangeDailyTransactionsRetrievalRequested(LocalDate.of(2026, 7, 10)));

    } catch (Exception e) {
      log.error("Failed to retrieve transactions, an error occured: ", e.getMessage());
      throw new RuntimeException("The error is " + e.getMessage());
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
            .amount(356400)
            .creationInstant(Instant.parse("2026-07-10T14:55:57Z"))
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

  @DirtiesContext(methodMode = BEFORE_METHOD)
  @Test
  void exportPaymentsCsv_with_data_matches_expected() throws IOException {
    var klioba = new JApplication();
    klioba.setName("klioba");
    klioba.setId("app-klioba");
    klioba.setApiKey("klioba-api-key");
    jApplicationRepository.save(klioba);

    var tsinjo = new JApplication();
    tsinjo.setName("tsinjo");
    tsinjo.setId("app-tsinjo");
    tsinjo.setApiKey("tsinjo-api-key");
    jApplicationRepository.save(tsinjo);

    userRepository.save(new User("mata@cu.te"));

    paymentRepository.save(
        Payment.builder()
            .id("p1")
            .pspPayment(
                PspPayment.builder().pspType(ORANGE_MONEY).id("MP260715.1234.A1B2C3").build())
            .creationInstant(Instant.parse("2026-01-15T10:30:00Z"))
            .verificationAttemptNb(0)
            .payer(new User("mata@cu.te"))
            .application(new Application("klioba", "klioba-api-key"))
            .build());

    paymentRepository.save(
        Payment.builder()
            .id("p2")
            .pspPayment(
                PspPayment.builder().pspType(ORANGE_MONEY).id("MP260715.5678.D9E0F1").build())
            .creationInstant(Instant.parse("2026-01-15T10:30:00Z"))
            .verificationAttemptNb(0)
            .payer(new User("mata@cu.te"))
            .application(new Application("tsinjo", "tsinjo-api-key"))
            .build());

    var response = subject.exportPaymentsCsv(null, null, null, null);
    assertNotNull(response.getBody());
    var csv = new String(response.getBody(), StandardCharsets.UTF_8);

    assertEquals(200, response.getStatusCodeValue());
    assertEquals(readResource(), csv);
  }

  @DirtiesContext(methodMode = BEFORE_METHOD)
  @Test
  void exportPaymentsCsv_empty_returns_header_only() throws IOException {
    var app = new JApplication();
    app.setName("EmptyApp");
    app.setId("app-empty");
    app.setApiKey("empty-api-key");
    jApplicationRepository.save(app);

    var response = subject.exportPaymentsCsv("EmptyApp", null, null, null);
    assertNotNull(response.getBody());
    var csv = new String(response.getBody(), StandardCharsets.UTF_8);

    assertEquals(200, response.getStatusCodeValue());
    assertEquals(
        "Payer email;PSP;Payment ref;Amount (Ar);Status;Creation date;Last"
            + " verification;Scope;Application\n",
        csv);
  }

  private String readResource() throws IOException {
    var resource = new ClassPathResource("csv/expected-export.csv");
    try (var is = resource.getInputStream()) {
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private static String randomEmail() {
    return "lou+" + randomUUID() + "@cute.dev";
  }
}
