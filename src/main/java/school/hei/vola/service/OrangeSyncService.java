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
public class OrangeSyncService {

  private final OrangeApiClient orangeApiClient;
  private final JOrangeTransactionRepository transactionRepository;
  private final PaymentRepository paymentRepository;
  private final PaymentVerificationRequestedService verificationService;

  public RecoveryResult sync(LocalDate date) {
    try {
      var transactions = orangeApiClient.transactionsOf(date).getTransactions();
      int inserted = processTransactions(transactions);

      return RecoveryResult.builder().date(date).isSuccessful(true).inserted(inserted).build();

    } catch (Exception e) {
      log.error("[SYNC] Failed for date {}", date, e);
      return RecoveryResult.builder().date(date).isSuccessful(false).errorMessage(e.getMessage()).build();
    }
  }

  private int processTransactions(List<OrangeTransaction> transactions) {
    int inserted = 0;
    for (OrangeTransaction transaction : transactions) {
      try {
        if (persistIfNew(transaction)) inserted++;
        triggerVerificationIfExists(transaction);
      } catch (Exception e) {
        log.error("[SYNC] Failed transaction ref={}", transaction.getRef(), e);
      }
    }
    return inserted;
  }

  private boolean persistIfNew(OrangeTransaction transaction) throws JsonProcessingException {
    String ref = transaction.getRef();

    if (transactionRepository.existsById(ref)) {
      return false;
    }

    var entity = new JOrangeTransaction();
    entity.setRef(ref);
    entity.setOrangeApiRawResponse(OrangeApiClient.om.writeValueAsString(transaction));
    transactionRepository.save(entity);

    log.info("[SYNC] Inserted ref={}", ref);
    return true;
  }

  private void triggerVerificationIfExists(OrangeTransaction transaction) {
    paymentRepository
        .findPaymentByPspTypeAndPspPaymentId(ORANGE_MONEY, transaction.getRef())
        .ifPresent(
            payment -> {
              var request = PaymentVerificationRequested.builder().payment(payment).build();
              verificationService.accept(request);
            });
  }
}
