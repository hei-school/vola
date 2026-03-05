package school.hei.vola.service;

import static school.hei.vola.model.Time.millisNow;
import static school.hei.vola.model.psp.PspType.ORANGE_MONEY;

import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import school.hei.vola.endpoint.event.EventProducer;
import school.hei.vola.endpoint.event.model.PaymentVerificationRequested;
import school.hei.vola.model.Payment;
import school.hei.vola.model.PaymentInfo;
import school.hei.vola.model.psp.PspPayment;
import school.hei.vola.model.psp.PspType;
import school.hei.vola.model.psp.orange.OrangeApiClient;
import school.hei.vola.model.psp.orange.OrangeTransaction;
import school.hei.vola.repository.OrangePaymentRepository;
import school.hei.vola.repository.PaymentRepository;

@Service
@AllArgsConstructor
@Slf4j
public class PaymentService {

  private final PaymentRepository paymentRepository;
  private final OrangePaymentRepository orangePaymentRepository;
  private final OrangeApiClient orangeApiClient;
  private final EventProducer eventProducer;

  @Transactional
  public Payment createPayment(
      String apiKey, String payerEmail, PspType pspType, String pspPaymentId) {
    var payment = paymentRepository.createPayment(apiKey, payerEmail, pspType, pspPaymentId);

    eventProducer.accept(List.of(new PaymentVerificationRequested(payment)));
    log.info("PaymentVerificationRequested event sent for payment={}", payment);

    return payment;
  }

  public Optional<Payment> findPaymentByPayerEmailAndPspTypeAndPspPaymentId(
      String payerEmail, PspType pspType, String pspPaymentId) {
    return paymentRepository.findPaymentByPayerEmailAndPspTypeAndPspPaymentId(
        payerEmail, pspType, pspPaymentId);
  }

  @Transactional
  public List<Payment> findPaymentsByPaymentInfos(String apiKey, List<PaymentInfo> paymentInfos) {
    var foundPayments = paymentRepository.findPaymentsByPaymentInfos(paymentInfos);
    var foundPspPaymentIds = foundPayments.stream().map(p -> p.pspPayment().id()).toList();

    var missingInfos =
        paymentInfos.stream()
            .filter(info -> !foundPspPaymentIds.contains(info.pspPaymentId()))
            .toList();

    if (missingInfos.isEmpty()) {
      return foundPayments;
    }

    var autoCreated = syncAndCreateFromScrapper(apiKey, missingInfos);

    var result = new ArrayList<>(foundPayments);
    result.addAll(autoCreated);
    return result;
  }

  @Transactional
  public List<Payment> createPayments(String apiKey, List<PaymentInfo> paymentInfos) {
    var result = new ArrayList<Payment>();
    var missingInfos = new ArrayList<PaymentInfo>();

    for (var info : paymentInfos) {
      var existing =
          paymentRepository.findPaymentByPspTypeAndPspPaymentId(
              info.pspType(), info.pspPaymentId());
      if (existing.isPresent()) {
        result.add(existing.get());
      } else {
        missingInfos.add(info);
      }
    }

    result.addAll(syncAndCreateFromScrapper(apiKey, missingInfos));
    return result;
  }

  private List<Payment> syncAndCreateFromScrapper(String apiKey, List<PaymentInfo> missingInfos) {
    var orangeInfos =
        missingInfos.stream().filter(info -> ORANGE_MONEY.equals(info.pspType())).toList();

    if (orangeInfos.isEmpty()) {
      return List.of();
    }

    Map<LocalDate, List<PaymentInfo>> infosByDate =
        orangeInfos.stream()
            .collect(
                Collectors.groupingBy(info -> extractDateFromPspPaymentId(info.pspPaymentId())));

    var created = new ArrayList<Payment>();

    for (var entry : infosByDate.entrySet()) {
      var date = entry.getKey();
      var infosForDate = entry.getValue();
      var pspPaymentIds =
          infosForDate.stream().map(PaymentInfo::pspPaymentId).collect(Collectors.toSet());

      List<OrangeTransaction> transactions;
      try {
        transactions = orangeApiClient.transactionsOf(date).getTransactions();
      } catch (Exception e) {
        log.error("[SEARCH] Failed to fetch Orange transactions for date={}", date, e);
        continue;
      }

      for (var ot : transactions) {
        if (!pspPaymentIds.contains(ot.getRef())) {
          continue;
        }

        var matchingInfo =
            infosForDate.stream()
                .filter(info -> info.pspPaymentId().equals(ot.getRef()))
                .findFirst()
                .orElseThrow(
                    () ->
                        new IllegalStateException(
                            "No matching PaymentInfo found for ref=" + ot.getRef()));

        orangePaymentRepository.save(ot);

        var payment =
            paymentRepository.createPayment(
                apiKey, matchingInfo.payerEmail(), ORANGE_MONEY, ot.getRef());

        var verifiedPayment =
            payment.toBuilder()
                .pspPayment(
                    new PspPayment(ORANGE_MONEY, ot.getRef(), ot.getAmount(), ot.creationInstant()))
                .lastPspVerificationInstant(millisNow())
                .build();
        var savedPayment = paymentRepository.save(verifiedPayment);
        created.add(savedPayment);

        log.info("[SEARCH] Auto-created verified payment from scrapper for ref={}", ot.getRef());
      }
    }

    return created;
  }

  static LocalDate extractDateFromPspPaymentId(String pspPaymentId) {
    // Format: MP[YYMMDD].HHMM.XXXXX → e.g. "MP260227.0706.A57074" = 2026-02-27
    var datePart = pspPaymentId.substring(2, 8);
    int year = 2000 + Integer.parseInt(datePart.substring(0, 2));
    int month = Integer.parseInt(datePart.substring(2, 4));
    int day = Integer.parseInt(datePart.substring(4, 6));
    return LocalDate.of(year, month, day);
  }
}
