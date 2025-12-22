package school.hei.vola.service;

import static school.hei.vola.model.psp.PspType.ORANGE_MONEY;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import school.hei.vola.endpoint.event.model.PaymentVerificationRequested;
import school.hei.vola.model.psp.orange.OrangeApiClient;
import school.hei.vola.model.psp.orange.OrangeTransaction;
import school.hei.vola.repository.PaymentRepository;
import school.hei.vola.repository.jpa.JOrangeTransactionRepository;
import school.hei.vola.repository.jpa.model.JOrangeTransaction;
import school.hei.vola.service.event.PaymentVerificationRequestedService;
import school.hei.vola.service.sync.model.RecoveryResult;

@Service
@AllArgsConstructor
@Slf4j
public class OrangeTransactionRecoveryService {

  private final OrangeApiClient orangeApiClient;
  private final JOrangeTransactionRepository jOrangeTransactionRepository;
  private final PaymentRepository paymentRepository;
  private final PaymentVerificationRequestedService paymentVerificationRequestedService;

  public RecoveryResult sync(LocalDate date) {
    try {

      var dailyTransactions = orangeApiClient.transactionsOf(date);
      var transactions = dailyTransactions.getTransactions();
      int insertedCount = processTransactions(transactions);

      return RecoveryResult.builder()
          .date(date)
          .isSuccessful(true)
          .inserted(insertedCount)
          .errorMessage(null)
          .build();

    } catch (Exception e) {
      return RecoveryResult.builder()
          .date(date)
          .isSuccessful(false)
          .inserted(0)
          .errorMessage(e.getMessage())
          .build();
    }
  }

  private int processTransactions(List<OrangeTransaction> transactions) {
    int insertedCount = 0;

    for (OrangeTransaction transaction : transactions) {
      try {
        boolean wasInserted = persistTransactionIfNew(transaction);
        if (wasInserted) {
          insertedCount++;
        }

        triggerPaymentVerificationIfExists(transaction);

      } catch (Exception e) {
        log.error("[SYNC] Failed to process transaction ref={}", transaction.getRef(), e);
      }
    }

    return insertedCount;
  }

  private boolean persistTransactionIfNew(OrangeTransaction transaction)
      throws JsonProcessingException {
    String ref = transaction.getRef();

    if (jOrangeTransactionRepository.existsById(ref)) {
      log.debug("[SYNC] Transaction ref={} already exists in database, skipping", ref);
      return false;
    }

    JOrangeTransaction jTransaction = createJOrangeTransaction(transaction);
    jOrangeTransactionRepository.save(jTransaction);

    log.info("[SYNC] Inserted new orange_transaction with ref={}", ref);
    return true;
  }

  private JOrangeTransaction createJOrangeTransaction(OrangeTransaction transaction)
      throws JsonProcessingException {
    JOrangeTransaction jTransaction = new JOrangeTransaction();
    jTransaction.setRef(transaction.getRef());

    String serializedTransaction = OrangeApiClient.om.writeValueAsString(transaction);
    jTransaction.setOrangeApiRawResponse(serializedTransaction);

    return jTransaction;
  }

  private void triggerPaymentVerificationIfExists(OrangeTransaction transaction) {
    String ref = transaction.getRef();

    var paymentOptional = paymentRepository.findPaymentByPspTypeAndPspPaymentId(ORANGE_MONEY, ref);

    if (paymentOptional.isEmpty()) {
      log.debug("[SYNC] No payment found for transaction ref={}", ref);
      return;
    }

    var payment = paymentOptional.get();
    var verificationRequest = PaymentVerificationRequested.builder().payment(payment).build();

    paymentVerificationRequestedService.accept(verificationRequest);
  }
}
