package school.hei.vola.service;

import static java.util.UUID.randomUUID;
import static school.hei.vola.model.Time.millisNow;
import static school.hei.vola.model.psp.PspType.ORANGE_MONEY;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import school.hei.vola.concurrency.Workers;
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
import school.hei.vola.repository.jpa.JApplicationRepository;
import school.hei.vola.repository.jpa.JOrangeTransactionRepository;
import school.hei.vola.repository.jpa.JPaymentRepository;
import school.hei.vola.repository.jpa.JUserRepository;
import school.hei.vola.repository.jpa.model.JOrangeTransaction;
import school.hei.vola.repository.jpa.model.JPayment;
import school.hei.vola.repository.jpa.model.JUser;

@Service
@AllArgsConstructor
@Slf4j
public class PaymentService {

  private final PaymentRepository paymentRepository;
  private final OrangePaymentRepository orangePaymentRepository;
  private final OrangeApiClient orangeApiClient;
  private final EventProducer eventProducer;
  private final Workers workers;
  private final JOrangeTransactionRepository jOrangeTransactionRepository;
  private final JUserRepository jUserRepository;
  private final JApplicationRepository jApplicationRepository;
  private final JPaymentRepository jPaymentRepository;

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

    var created = Collections.synchronizedList(new ArrayList<Payment>());

    List<Callable<Void>> callables =
        infosByDate.entrySet().stream()
            .map(
                entry ->
                    (Callable<Void>)
                        () -> {
                          var synced = syncForDate(apiKey, entry.getKey(), entry.getValue());
                          created.addAll(synced);
                          return null;
                        })
            .toList();

    workers.invokeAll(callables);

    return created;
  }

  private List<Payment> syncForDate(String apiKey, LocalDate date, List<PaymentInfo> infosForDate) {
    var pspPaymentIds =
        infosForDate.stream().map(PaymentInfo::pspPaymentId).collect(Collectors.toSet());
    var infoByRef =
        infosForDate.stream().collect(Collectors.toMap(PaymentInfo::pspPaymentId, info -> info));

    List<OrangeTransaction> transactions;
    try {
      transactions = orangeApiClient.transactionsOf(date).getTransactions();
    } catch (Exception e) {
      log.error("[SEARCH] Failed to fetch Orange transactions for date={}", date, e);
      return List.of();
    }

    var matchedTransactions =
        transactions.stream().filter(ot -> pspPaymentIds.contains(ot.getRef())).toList();

    if (matchedTransactions.isEmpty()) {
      return List.of();
    }

    // 1. Batch save all orange transactions
    var jOrangeTransactions = matchedTransactions.stream().map(this::toJOrangeTransaction).toList();
    jOrangeTransactionRepository.saveAll(jOrangeTransactions);

    // 2. Resolve or create all users in batch
    var emails =
        matchedTransactions.stream()
            .map(ot -> infoByRef.get(ot.getRef()).payerEmail())
            .distinct()
            .toList();
    var existingUsers = jUserRepository.findByEmailIn(emails);
    var userByEmail = existingUsers.stream().collect(Collectors.toMap(JUser::getEmail, u -> u));
    var newUsers = new ArrayList<JUser>();
    for (var email : emails) {
      if (!userByEmail.containsKey(email)) {
        var jUser = new JUser();
        jUser.setId(randomUUID().toString());
        jUser.setEmail(email);
        newUsers.add(jUser);
      }
    }
    if (!newUsers.isEmpty()) {
      jUserRepository.saveAll(newUsers).forEach(u -> userByEmail.put(u.getEmail(), u));
    }

    // 3. Load application once
    var jApplication = jApplicationRepository.findByApiKey(apiKey).get();

    // 4. Build and batch save all payments
    var now = millisNow();
    var jPayments =
        matchedTransactions.stream()
            .map(
                ot -> {
                  var info = infoByRef.get(ot.getRef());
                  var jUser = userByEmail.get(info.payerEmail());
                  return new JPayment(
                      randomUUID().toString(),
                      ORANGE_MONEY,
                      ot.getAmount(),
                      ot.getRef(),
                      ot.creationInstant(),
                      now,
                      now,
                      0,
                      jUser,
                      jApplication);
                })
            .toList();
    var savedJPayments = jPaymentRepository.saveAll(jPayments);

    log.info(
        "[SEARCH] Batch created {} verified payments from scrapper for date={}",
        savedJPayments.size(),
        date);

    return savedJPayments.stream()
        .map(
            jp ->
                new Payment(
                    jp.getId(),
                    new PspPayment(
                        jp.getPspType(),
                        jp.getPspPaymentId(),
                        jp.getAmount(),
                        jp.getPspCreationInstant()),
                    jp.getCreationInstant(),
                    jp.getLastPspVerificationInstant(),
                    jp.getVerificationAttemptNb(),
                    new school.hei.vola.model.User(jp.getPayer().getEmail()),
                    new school.hei.vola.model.Application(
                        jp.getApplication().getName(), jp.getApplication().getApiKey())))
        .toList();
  }

  private JOrangeTransaction toJOrangeTransaction(OrangeTransaction ot) {
    var jot = new JOrangeTransaction();
    jot.setRef(ot.getRef());
    try {
      jot.setOrangeApiRawResponse(OrangeApiClient.om.writeValueAsString(ot));
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
    return jot;
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
