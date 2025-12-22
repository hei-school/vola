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
import school.hei.vola.repository.OrangePaymentRepository;
import school.hei.vola.repository.PaymentRepository;
import school.hei.vola.repository.jpa.JOrangeTransactionRepository;
import school.hei.vola.repository.jpa.model.JOrangeTransaction;
import school.hei.vola.service.event.PaymentVerificationRequestedService;
import school.hei.vola.service.sync.model.RecoveryResult;

/**
 * Service responsible for recovering Orange Money transactions from the Orange API
 * and synchronizing them with our payment system.
 */
@Service
@AllArgsConstructor
@Slf4j
public class OrangeTransactionRecoveryService {

    private final OrangeApiClient orangeApiClient;
    private final JOrangeTransactionRepository jOrangeTransactionRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentVerificationRequestedService paymentVerificationRequestedService;
    private final OrangePaymentRepository orangePaymentRepository;

    /**
     * Synchronizes Orange transactions for a specific date.
     *
     * Process:
     * 1. Fetches all transactions from Orange API for the given date
     * 2. Persists new transactions to our database
     * 3. Triggers payment verification for any matching payments
     *
     * @param date The date to sync transactions for
     * @return RecoveryResult containing sync status and metrics
     */
    public RecoveryResult sync(LocalDate date) {
        log.info("[SYNC] Starting sync for date {}", date);

        try {
            var dailyTransactions = orangeApiClient.transactionsOf(date);
            var transactions = dailyTransactions.getTransactions();

            log.info("[SYNC] Fetched {} transactions from Orange API for date {}",
                    transactions.size(), date);

            int insertedCount = processTransactions(transactions);

            log.info("[SYNC] Successfully completed sync for date {}. Inserted {} new transactions",
                    date, insertedCount);

            return buildSuccessResult(date, insertedCount);

        } catch (Exception e) {
            log.error("[SYNC] Failed to sync transactions for date {}", date, e);
            return buildFailureResult(date, e);
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
                log.error("[SYNC] Failed to process transaction ref={}",
                        transaction.getRef(), e);
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

        var paymentOptional = paymentRepository.findPaymentByPspTypeAndPspPaymentId(
                ORANGE_MONEY, ref);

        if (paymentOptional.isEmpty()) {
            log.debug("[SYNC] No payment found for transaction ref={}", ref);
            return;
        }

        var payment = paymentOptional.get();
        log.info("[SYNC] Found payment id={} for transaction ref={}, triggering verification",
                payment.id(), ref);

        var verificationRequest = PaymentVerificationRequested.builder()
                .payment(payment)
                .build();

        paymentVerificationRequestedService.accept(verificationRequest);
    }

    private RecoveryResult buildSuccessResult(LocalDate date, int insertedCount) {
        return RecoveryResult.builder()
                .date(date)
                .isSuccessful(true)
                .inserted(insertedCount)
                .errorMessage(null)
                .build();
    }

    private RecoveryResult buildFailureResult(LocalDate date, Exception exception) {
        return RecoveryResult.builder()
                .date(date)
                .isSuccessful(false)
                .inserted(0)
                .errorMessage(exception.getMessage())
                .build();
    }
}