package school.hei.vola.service;

import static java.time.Instant.now;

import jakarta.transaction.Transactional;
import java.io.File;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import school.hei.vola.endpoint.event.EventProducer;
import school.hei.vola.endpoint.event.model.OrangeTransactionsImportRequested;
import school.hei.vola.endpoint.event.model.PaymentVerificationRequested;
import school.hei.vola.file.bucket.BucketComponent;
import school.hei.vola.model.ImportedTransactionDetails;
import school.hei.vola.model.Payment;
import school.hei.vola.model.PaymentInfo;
import school.hei.vola.model.VerificationStatus;
import school.hei.vola.model.psp.PspType;
import school.hei.vola.repository.OrangePaymentRepository;
import school.hei.vola.repository.PaymentRepository;
import school.hei.vola.service.utils.ExcelParser;

@Service
@AllArgsConstructor
@Slf4j
public class PaymentService {

  private static final String TRANSACTIONS_XLS_IMPORT_BUCKET_KEY = "/TRANSACTIONS_XLS_IMPORT/";

  private final PaymentRepository paymentRepository;
  private final EventProducer eventProducer;
  private final OrangePaymentRepository orangePaymentRepository;
  private final ExcelParser excelParser;
  private final BucketComponent bucketComponent;

  @Transactional
  public Payment createPayment(
      String apiKey, String payerEmail, PspType pspType, String pspPaymentId, String scope) {
    var payment = paymentRepository.createPayment(apiKey, payerEmail, pspType, pspPaymentId, scope);

    eventProducer.accept(List.of(new PaymentVerificationRequested(payment)));
    log.info("PaymentVerificationRequested event sent for payment={}", payment);

    return payment;
  }

  public List<Payment> createPayments(String apiKey, List<PaymentInfo> paymentInfos) {
    var payments = paymentRepository.createPayments(apiKey, paymentInfos);
    if (payments.isEmpty()) {
      return List.of();
    }

    var paymentRequests = payments.stream().map(PaymentVerificationRequested::new).toList();
    eventProducer.accept(paymentRequests);
    log.info("PaymentVerificationRequested event sent for {} payments", payments.size());

    return payments;
  }

  public Optional<Payment> findPaymentByPayerEmailAndPspTypeAndPspPaymentId(
      String payerEmail, PspType pspType, String pspPaymentId) {
    return paymentRepository.findPaymentByPayerEmailAndPspTypeAndPspPaymentId(
        payerEmail, pspType, pspPaymentId);
  }

  public List<Payment> findPaymentsByPaymentInfos(String apiKey, List<PaymentInfo> paymentInfos) {
    var foundPayments = paymentRepository.findPaymentsByPaymentInfos(paymentInfos);
    var foundPaymentInfos =
        new HashSet<>(
            foundPayments.stream()
                .map(
                    p ->
                        new PaymentInfo(
                            p.payer().email(), p.pspPayment().pspType(), p.pspPayment().id()))
                .toList());
    var missingPaymentInfos =
        paymentInfos.stream().filter(info -> !foundPaymentInfos.contains(info)).toList();
    if (!missingPaymentInfos.isEmpty()) {
      createPayments(apiKey, missingPaymentInfos);
    }
    return foundPayments;
  }

  public List<Payment> findAllPayments(Pageable pageable) {
    return paymentRepository.findAll(pageable);
  }

  public List<Payment> findPaymentsByApplicationName(String applicationName) {
    return paymentRepository.findByApplicationName(applicationName);
  }

  public List<Payment> findPaymentsByApplicationNameAndDateRange(
      String applicationName, String scope, Instant start, Instant end) {
    return paymentRepository.findByApplicationNameAndDateRange(applicationName, scope, start, end);
  }

  public Page<Payment> findFilteredPage(
      String applicationName, String scope, Instant start, Instant end, Pageable pageable) {
    return paymentRepository.findFilteredPage(applicationName, scope, start, end, pageable);
  }

  public long countFiltered(String applicationName, String scope, Instant start, Instant end) {
    return paymentRepository.countFiltered(applicationName, scope, start, end);
  }

  public long sumAmountForSucceeded(
      String applicationName, String scope, Instant start, Instant end) {
    return paymentRepository.sumAmountForSucceeded(applicationName, scope, start, end);
  }

  public long countPending(String applicationName, String scope, Instant start, Instant end) {
    return paymentRepository.countPending(applicationName, scope, start, end);
  }

  public List<String> findDistinctScopes(String applicationName) {
    return paymentRepository.findDistinctScopes(applicationName);
  }

  public String buildPaymentsCsv(String applicationName, String scope, Instant start, Instant end) {
    List<Payment> payments =
        findPaymentsByApplicationNameAndDateRange(applicationName, scope, start, end);

    var header =
        "Payer email;PSP;Payment ref;Amount (Ar);Status;Creation date;Last"
            + " verification;Scope;Application\n";
    var builder = new StringBuilder(header);

    var format = "%s;%s;%s;%s;%s;%s;%s;%s;%s\n";

    for (var p : payments) {
      var amount = p.pspPayment().amount();
      var line =
          String.format(
              format,
              escapeCsv(p.payer().email()),
              p.pspPayment().pspType(),
              escapeCsv(p.pspPayment().id()),
              amount != null ? amount : "",
              statusLabel(p.getVerificationStatus()),
              p.creationInstant() != null ? p.creationInstant().toString() : "",
              p.lastPspVerificationInstant() != null
                  ? p.lastPspVerificationInstant().toString()
                  : "",
              escapeCsv(p.scope()),
              p.application().name());
      builder.append(line);
    }
    return builder.toString();
  }

  private String escapeCsv(String value) {
    if (value == null) return "";
    if (value.contains(";") || value.contains("\"") || value.contains("\n")) {
      return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
  }

  private String statusLabel(VerificationStatus status) {
    return switch (status) {
      case VERIFYING -> "Verifying";
      case SUCCEEDED -> "Succeeded";
      case FAILED -> "Failed";
    };
  }

  public ImportedTransactionDetails saveTransactionFromExcel(File excel) {
    log.info("File name : " + excel.getName());
    var bucketKey = TRANSACTIONS_XLS_IMPORT_BUCKET_KEY + excel.getName();
    bucketComponent.upload(excel, bucketKey);
    eventProducer.accept(List.of(new OrangeTransactionsImportRequested(bucketKey)));
    return new ImportedTransactionDetails(bucketKey, now(), excel.getName());
  }
}
