package school.hei.vola.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import school.hei.vola.endpoint.event.model.PaymentVerificationRequested;
import school.hei.vola.model.Payment;
import school.hei.vola.model.psp.PspType;
import school.hei.vola.model.psp.orange.OrangeApiClient;
import school.hei.vola.model.psp.orange.OrangeDailyTransactions;
import school.hei.vola.model.psp.orange.OrangeTransaction;
import school.hei.vola.repository.PaymentRepository;
import school.hei.vola.repository.jpa.JOrangeTransactionRepository;
import school.hei.vola.service.event.PaymentVerificationRequestedService;
import school.hei.vola.service.sync.model.RecoveryResult;

class OrangeSyncServiceTest {

  private final OrangeApiClient orangeApiClient = mock(OrangeApiClient.class);
  private final JOrangeTransactionRepository transactionRepository =
      mock(JOrangeTransactionRepository.class);
  private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
  private final PaymentVerificationRequestedService verificationService =
      mock(PaymentVerificationRequestedService.class);

  private final OrangeSyncService service =
      new OrangeSyncService(
          orangeApiClient, transactionRepository, paymentRepository, verificationService);

  @Test
  void sync_insertsNewTransaction_and_triggersVerification() throws Exception {
    LocalDate date = LocalDate.of(2025, 9, 17);
    OrangeTransaction transaction = mockTransaction("REF-1", 1000);
    OrangeDailyTransactions daily = mockDailyTransactions(List.of(transaction));

    when(orangeApiClient.transactionsOf(date)).thenReturn(daily);
    when(transactionRepository.existsById("REF-1")).thenReturn(false);

    Payment payment = mock(Payment.class);
    when(payment.id()).thenReturn("payment-1");
    when(paymentRepository.findPaymentByPspTypeAndPspPaymentId(PspType.ORANGE_MONEY, "REF-1"))
        .thenReturn(Optional.of(payment));

    RecoveryResult result = service.sync(date);

    verify(transactionRepository).save(any());
    verify(verificationService).accept(any(PaymentVerificationRequested.class));
    assertTrue(result.isSuccessful());
    assertEquals(1, result.getInserted());
  }

  @Test
  void sync_skipsExistingTransactions() throws Exception {
    LocalDate date = LocalDate.of(2025, 9, 17);
    OrangeTransaction transaction = mockTransaction("REF-EXISTS", 5000);
    OrangeDailyTransactions daily = mockDailyTransactions(List.of(transaction));

    when(orangeApiClient.transactionsOf(date)).thenReturn(daily);
    when(transactionRepository.existsById("REF-EXISTS")).thenReturn(true);

    RecoveryResult result = service.sync(date);

    verify(transactionRepository, never()).save(any());
    assertEquals(0, result.getInserted());
  }

  @Test
  void sync_returnsFailure_whenApiCallFails() {
    LocalDate date = LocalDate.of(2025, 9, 17);
    when(orangeApiClient.transactionsOf(date)).thenThrow(new RuntimeException("API error"));

    RecoveryResult result = service.sync(date);

    assertFalse(result.isSuccessful());
    assertEquals("API error", result.getErrorMessage());
  }

  private OrangeTransaction mockTransaction(String ref, Integer amount)
      throws JsonProcessingException {
    OrangeTransaction transaction = mock(OrangeTransaction.class);
    when(transaction.getRef()).thenReturn(ref);
    when(transaction.getAmount()).thenReturn(amount);
    when(OrangeApiClient.om.writeValueAsString(transaction))
        .thenReturn("{\"ref\":\"" + ref + "\"}");
    return transaction;
  }

  private OrangeDailyTransactions mockDailyTransactions(List<OrangeTransaction> transactions) {
    OrangeDailyTransactions daily = mock(OrangeDailyTransactions.class);
    when(daily.getTransactions()).thenReturn(transactions);
    return daily;
  }
}
