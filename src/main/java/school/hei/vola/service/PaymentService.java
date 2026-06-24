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

  public List<Payment> findAllPayments() {
    return paymentRepository.findAll();
  }

  public List<Payment> findPaymentsByApplicationName(String applicationName) {
    return paymentRepository.findByApplicationName(applicationName);
  }

  public List<Payment> findPaymentsByApplicationNameAndDateRange(
      String applicationName, String scope, Instant start, Instant end) {
    var effectiveApp = "all".equals(applicationName) ? null : applicationName;
    var effectiveScope = "all".equals(scope) ? null : scope;
    return paymentRepository.findByApplicationNameAndDateRange(
        effectiveApp, effectiveScope, start, end);
  }

  public Page<Payment> findFilteredPage(
      String applicationName, String scope, Instant start, Instant end, Pageable pageable) {
    var effectiveApp = "all".equals(applicationName) ? null : applicationName;
    var effectiveScope = "all".equals(scope) ? null : scope;
    return paymentRepository.findFilteredPage(effectiveApp, effectiveScope, start, end, pageable);
  }

  public long countFiltered(String applicationName, String scope, Instant start, Instant end) {
    var effectiveApp = "all".equals(applicationName) ? null : applicationName;
    var effectiveScope = "all".equals(scope) ? null : scope;
    return paymentRepository.countFiltered(effectiveApp, effectiveScope, start, end);
  }

  public long sumAmountForSucceeded(
      String applicationName, String scope, Instant start, Instant end) {
    var effectiveApp = "all".equals(applicationName) ? null : applicationName;
    var effectiveScope = "all".equals(scope) ? null : scope;
    return paymentRepository.sumAmountForSucceeded(effectiveApp, effectiveScope, start, end);
  }

  public long countPending(String applicationName, String scope, Instant start, Instant end) {
    var effectiveApp = "all".equals(applicationName) ? null : applicationName;
    var effectiveScope = "all".equals(scope) ? null : scope;
    return paymentRepository.countPending(effectiveApp, effectiveScope, start, end);
  }

  public String buildPaymentsCsv(String applicationName, String scope, Instant start, Instant end) {
    List<Payment> payments =
        findPaymentsByApplicationNameAndDateRange(applicationName, scope, start, end);
    var sb = new StringBuilder();
    sb.append(
        "Email payeur;PSP;Ref paiement;Montant (Ar);Statut;Date cr\u00e9ation;Derni\u00e8re"
            + " v\u00e9rification;Scope;Application\n");
    for (var p : payments) {
      var amount = p.pspPayment().amount();
      sb.append(escapeCsv(p.payer().email()))
          .append(';')
          .append(p.pspPayment().pspType())
          .append(';')
          .append(escapeCsv(p.pspPayment().id()))
          .append(';')
          .append(amount != null ? amount : "")
          .append(';')
          .append(statusLabel(p.getVerificationStatus()))
          .append(';')
          .append(p.creationInstant() != null ? p.creationInstant().toString() : "")
          .append(';')
          .append(
              p.lastPspVerificationInstant() != null
                  ? p.lastPspVerificationInstant().toString()
                  : "")
          .append(';')
          .append(escapeCsv(p.scope()))
          .append(';')
          .append(p.application().name())
          .append('\n');
    }
    return sb.toString();
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
      case VERIFYING -> "En vérification";
      case SUCCEEDED -> "Succès";
      case FAILED -> "Échoué";
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
