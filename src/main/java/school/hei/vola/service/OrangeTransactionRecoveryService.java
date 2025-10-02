package school.hei.vola.service;

import static school.hei.vola.model.psp.PspType.ORANGE_MONEY;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import school.hei.vola.endpoint.event.model.PaymentVerificationRequested;
import school.hei.vola.model.psp.PspPayment;
import school.hei.vola.model.psp.orange.OrangeApiClient;
import school.hei.vola.model.psp.orange.OrangeTransaction;
import school.hei.vola.repository.OrangePaymentRepository;
import school.hei.vola.repository.PaymentRepository;
import school.hei.vola.repository.jpa.JOrangeTransactionRepository;
import school.hei.vola.repository.jpa.model.JOrangeTransaction;
import school.hei.vola.service.event.PaymentVerificationRequestedService;

@Service
@AllArgsConstructor
@Slf4j
public class OrangeTransactionRecoveryService {

  private final OrangeApiClient orangeApiClient;
  private final JOrangeTransactionRepository jOrangeTransactionRepository;
  private final PaymentRepository paymentRepository;
  private final PaymentVerificationRequestedService paymentVerificationRequestedService;
  private final OrangePaymentRepository orangePaymentRepository;

  @Value
  public static class RecoveryResult {
    String ref;
    boolean inserted;
    boolean paymentFound;
    String paymentId;
    Integer amount;
  }

  /**
   * Recover Orange transactions for a given date: - insert any transaction not yet present in
   * orange_transaction - trigger verification for any matching payments - return a list of
   * RecoveryResult for each transaction processed
   */
  public List<RecoveryResult> recover(LocalDate date) {
    log.info("[RECOVERY] Starting recovery for date {}", date);
    var daily = orangeApiClient.transactionsOf(date);
    var transactions = daily.getTransactions();

    log.info(
        "[RECOVERY] {} transactions fetched from Orange for date {}", transactions.size(), date);

    List<RecoveryResult> results = new ArrayList<>();

    for (OrangeTransaction ot : transactions) {
      String ref = ot.getRef();
      boolean inserted = false;
      Integer amount = ot.getAmount();

      if (jOrangeTransactionRepository.existsById(ref)) {
        log.debug("[RECOVERY] transaction ref={} already exists, skipping insert", ref);
      } else {
        // persist the transaction
        JOrangeTransaction jot = new JOrangeTransaction();
        jot.setRef(ref);
        try {
          jot.setOrangeApiRawResponse(OrangeApiClient.om.writeValueAsString(ot));
        } catch (JsonProcessingException e) {
          log.error("[RECOVERY] Failed to serialize OrangeTransaction ref={}", ref, e);
          // continue to next transaction to avoid failing whole recover
          results.add(new RecoveryResult(ref, false, false, null, amount));
          continue;
        }
        jOrangeTransactionRepository.save(jot);
        inserted = true;
        log.info("[RECOVERY] Inserted orange_transaction ref={}", ref);
      }

      // Check for a corresponding payment and trigger verification if present
      var paymentOpt = paymentRepository.findPaymentByPspTypeAndPspPaymentId(ORANGE_MONEY, ref);
      if (paymentOpt.isPresent()) {
        var payment = paymentOpt.get();
        log.info(
            "[RECOVERY] Found payment id={} for ref={}, requesting verification",
            payment.id(),
            ref);
        paymentVerificationRequestedService.accept(
            PaymentVerificationRequested.builder().payment(payment).build());
        results.add(new RecoveryResult(ref, inserted, true, payment.id(), amount));
      } else {
        log.warn("[RECOVERY] No payment found for ref={}", ref);
        // Also attempt to map the saved orange_transaction to a PspPayment (via
        // OrangePaymentRepository)
        var pspPaymentOpt = orangePaymentRepository.findById(ref);
        if (pspPaymentOpt.isPresent()) {
          PspPayment psp = pspPaymentOpt.get();
          results.add(new RecoveryResult(ref, inserted, false, null, psp.amount()));
        } else {
          results.add(new RecoveryResult(ref, inserted, false, null, amount));
        }
      }
    }

    log.info(
        "[RECOVERY] Finished recovery for date {}, processed {} transactions",
        date,
        results.size());
    return results;
  }
}
