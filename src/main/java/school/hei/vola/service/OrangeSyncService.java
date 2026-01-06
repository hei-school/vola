package school.hei.vola.service;

import static school.hei.vola.model.psp.PspType.ORANGE_MONEY;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import school.hei.vola.endpoint.event.model.PaymentVerificationRequested;
import school.hei.vola.model.psp.orange.OrangeApiClient;
import school.hei.vola.model.psp.orange.OrangeTransaction;
import school.hei.vola.repository.OrangePaymentRepository;
import school.hei.vola.repository.PaymentRepository;
import school.hei.vola.service.event.PaymentVerificationRequestedService;
import school.hei.vola.service.sync.model.RecoveryResult;

@Service
@AllArgsConstructor
@Slf4j
public class OrangeSyncService {
  private final OrangeApiClient orangeApiClient;
  private final OrangePaymentRepository orangePaymentRepository;
  private final PaymentRepository paymentRepository;
  private final PaymentVerificationRequestedService verificationService;

  public RecoveryResult sync(LocalDate date) {
    try {
      var transactions = orangeApiClient.transactionsOf(date).getTransactions();
      int totalTransactions = transactions.size();
      int syncedTransactions =
          transactions.stream().mapToInt(ot -> persistAndVerifyTransaction(ot) ? 1 : 0).sum();

      return RecoveryResult.builder()
          .date(date)
          .isSuccessful(syncedTransactions == totalTransactions)
          .nbSyncedTransactions(syncedTransactions)
          .nbTotalTransactions(totalTransactions)
          .build();
    } catch (Exception e) {
      log.error("[SYNC] Failed to sync transactions for date={}", date, e);
      return RecoveryResult.builder()
          .date(date)
          .isSuccessful(false)
          .nbSyncedTransactions(0)
          .nbTotalTransactions(0)
          .errorMessage(e.getMessage())
          .build();
    }
  }

  private boolean persistAndVerifyTransaction(OrangeTransaction ot) {
    try {
      persistIfNew(ot);
      triggerVerificationIfExists(ot);
      return true;
    } catch (Exception e) {
      log.error("[SYNC] Failed to sync orange transaction ot={}", ot, e);
      return false;
    }
  }

  private void persistIfNew(OrangeTransaction ot) {
    if (orangePaymentRepository.findById(ot.getRef()).isPresent()) {
      log.debug("[SYNC] Transaction ref={} already exists, skipping", ot.getRef());
      return;
    }
    orangePaymentRepository.save(ot);
    log.info("[SYNC] Inserted ref={}", ot.getRef());
  }

  private void triggerVerificationIfExists(OrangeTransaction ot) {
    paymentRepository
        .findPaymentByPspTypeAndPspPaymentId(ORANGE_MONEY, ot.getRef())
        .ifPresent(
            p ->
                verificationService.accept(
                    PaymentVerificationRequested.builder().payment(p).build()));
  }
}
