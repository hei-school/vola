package school.hei.vola.service;

import static school.hei.vola.model.psp.PspType.ORANGE_MONEY;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.LocalDate;
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
      var inserted = (int) transactions.stream().filter(this::processTransaction).count();
      return RecoveryResult.builder().date(date).isSuccessful(true).inserted(inserted).build();
    } catch (Exception e) {
      log.error("[SYNC] Failed date={}", date, e);
      return RecoveryResult.builder()
          .date(date)
          .isSuccessful(false)
          .errorMessage(e.getMessage())
          .build();
    }
  }

  private boolean processTransaction(OrangeTransaction tx) {
    try {
      boolean inserted = persistIfNew(tx);
      triggerVerificationIfExists(tx);
      return inserted;
    } catch (Exception e) {
      log.error("[SYNC] Failed ref={}", tx.getRef(), e);
      return false;
    }
  }

  private boolean persistIfNew(OrangeTransaction tx) throws JsonProcessingException {
    if (transactionRepository.existsById(tx.getRef())) return false;
    var entity = new JOrangeTransaction();
    entity.setRef(tx.getRef());
    entity.setOrangeApiRawResponse(OrangeApiClient.om.writeValueAsString(tx));
    transactionRepository.save(entity);
    log.info("[SYNC] Inserted ref={}", tx.getRef());
    return true;
  }

  private void triggerVerificationIfExists(OrangeTransaction tx) {
    paymentRepository
        .findPaymentByPspTypeAndPspPaymentId(ORANGE_MONEY, tx.getRef())
        .ifPresent(
            p ->
                verificationService.accept(
                    PaymentVerificationRequested.builder().payment(p).build()));
  }
}
