package school.hei.vola.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import school.hei.vola.endpoint.event.model.PaymentVerificationRequested;
import school.hei.vola.model.Payment;
import school.hei.vola.model.psp.PspType;
import school.hei.vola.model.psp.orange.OrangeApiClient;
import school.hei.vola.model.psp.orange.OrangeDailyTransactions;
import school.hei.vola.model.psp.orange.OrangeTransaction;
import school.hei.vola.repository.PaymentRepository;
import school.hei.vola.repository.jpa.JOrangeTransactionRepository;
import school.hei.vola.repository.jpa.model.JOrangeTransaction;
import school.hei.vola.service.event.PaymentVerificationRequestedService;
import school.hei.vola.service.sync.model.RecoveryResult;

class OrangeTransactionRecoveryServiceTest {

  private final OrangeApiClient orangeApiClient = mock(OrangeApiClient.class);
  private final JOrangeTransactionRepository jOrangeTransactionRepository =
      mock(JOrangeTransactionRepository.class);
  private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
  private final PaymentVerificationRequestedService paymentVerificationRequestedService =
      mock(PaymentVerificationRequestedService.class);

  private final OrangeTransactionRecoveryService service =
      new OrangeTransactionRecoveryService(
          orangeApiClient,
          jOrangeTransactionRepository,
          paymentRepository,
          paymentVerificationRequestedService);

  @Test
  void sync_insertsNewTransaction_and_triggersVerification_whenPaymentExists() throws Exception {
    LocalDate date = LocalDate.of(2025, 9, 17);

    OrangeTransaction transaction1 = createMockTransaction("REF-1", 1000);
    OrangeTransaction transaction2 = createMockTransaction("REF-2", 2000);

    OrangeDailyTransactions dailyTransactions = mock(OrangeDailyTransactions.class);
    when(dailyTransactions.getTransactions()).thenReturn(List.of(transaction1, transaction2));
    when(orangeApiClient.transactionsOf(date)).thenReturn(dailyTransactions);

    when(jOrangeTransactionRepository.existsById("REF-1")).thenReturn(false);
    when(jOrangeTransactionRepository.existsById("REF-2")).thenReturn(true);

    Payment payment1 = mock(Payment.class);
    when(payment1.id()).thenReturn("payment-1");
    when(paymentRepository.findPaymentByPspTypeAndPspPaymentId(PspType.ORANGE_MONEY, "REF-1"))
        .thenReturn(Optional.of(payment1));

    when(paymentRepository.findPaymentByPspTypeAndPspPaymentId(PspType.ORANGE_MONEY, "REF-2"))
        .thenReturn(Optional.empty());

    RecoveryResult result = service.sync(date);

    ArgumentCaptor<JOrangeTransaction> transactionCaptor =
        ArgumentCaptor.forClass(JOrangeTransaction.class);
    verify(jOrangeTransactionRepository, times(1)).save(transactionCaptor.capture());

    JOrangeTransaction savedTransaction = transactionCaptor.getValue();
    assertEquals("REF-1", savedTransaction.getRef());
    assertNotNull(savedTransaction.getOrangeApiRawResponse());

    ArgumentCaptor<PaymentVerificationRequested> verificationCaptor =
        ArgumentCaptor.forClass(PaymentVerificationRequested.class);
    verify(paymentVerificationRequestedService, times(1)).accept(verificationCaptor.capture());

    PaymentVerificationRequested verificationRequest = verificationCaptor.getValue();
    assertEquals("payment-1", verificationRequest.getPayment().id());

    assertTrue(result.isSuccessful());
    assertEquals(date, result.getDate());
    assertEquals(1, result.getInserted());
    assertNull(result.getErrorMessage());
  }

  @Test
  void sync_skipsExistingTransactions() throws Exception {
    LocalDate date = LocalDate.of(2025, 9, 17);

    OrangeTransaction transaction = createMockTransaction("REF-EXISTING", 5000);

    OrangeDailyTransactions dailyTransactions = mock(OrangeDailyTransactions.class);
    when(dailyTransactions.getTransactions()).thenReturn(List.of(transaction));
    when(orangeApiClient.transactionsOf(date)).thenReturn(dailyTransactions);

    when(jOrangeTransactionRepository.existsById("REF-EXISTING")).thenReturn(true);

    when(paymentRepository.findPaymentByPspTypeAndPspPaymentId(
            PspType.ORANGE_MONEY, "REF-EXISTING"))
        .thenReturn(Optional.empty());

    RecoveryResult result = service.sync(date);

    verify(jOrangeTransactionRepository, never()).save(any());
    assertTrue(result.isSuccessful());
    assertEquals(0, result.getInserted());
  }

  @Test
  void sync_continuesProcessing_whenOneTransactionFails() throws Exception {
    LocalDate date = LocalDate.of(2025, 9, 17);

    OrangeTransaction transaction1 = createMockTransaction("REF-1", 1000);
    OrangeTransaction transaction2 = createMockTransaction("REF-2", 2000);

    OrangeDailyTransactions dailyTransactions = mock(OrangeDailyTransactions.class);
    when(dailyTransactions.getTransactions()).thenReturn(List.of(transaction1, transaction2));
    when(orangeApiClient.transactionsOf(date)).thenReturn(dailyTransactions);

    when(jOrangeTransactionRepository.existsById("REF-1")).thenReturn(false);
    when(jOrangeTransactionRepository.existsById("REF-2")).thenReturn(false);

    when(jOrangeTransactionRepository.save(argThat(jot -> "REF-1".equals(jot.getRef()))))
        .thenThrow(new RuntimeException("Database error"));

    when(paymentRepository.findPaymentByPspTypeAndPspPaymentId(any(), any()))
        .thenReturn(Optional.empty());

    RecoveryResult result = service.sync(date);

    verify(jOrangeTransactionRepository, times(2)).save(any());
    assertTrue(result.isSuccessful());
    assertEquals(1, result.getInserted()); // Only REF-2 succeeded
  }

  @Test
  void sync_returnsFailure_whenApiCallFails() {
    LocalDate date = LocalDate.of(2025, 9, 17);
    String errorMessage = "Orange API connection timeout";

    when(orangeApiClient.transactionsOf(date)).thenThrow(new RuntimeException(errorMessage));

    RecoveryResult result = service.sync(date);

    assertFalse(result.isSuccessful());
    assertEquals(date, result.getDate());
    assertEquals(0, result.getInserted());
    assertEquals(errorMessage, result.getErrorMessage());

    verify(jOrangeTransactionRepository, never()).save(any());
    verify(paymentVerificationRequestedService, never()).accept(any());
  }

  @Test
  void sync_doesNotTriggerVerification_whenNoPaymentFound() throws Exception {
    LocalDate date = LocalDate.of(2025, 9, 17);

    OrangeTransaction transaction = createMockTransaction("REF-NO-PAYMENT", 3000);

    OrangeDailyTransactions dailyTransactions = mock(OrangeDailyTransactions.class);
    when(dailyTransactions.getTransactions()).thenReturn(List.of(transaction));
    when(orangeApiClient.transactionsOf(date)).thenReturn(dailyTransactions);

    when(jOrangeTransactionRepository.existsById("REF-NO-PAYMENT")).thenReturn(false);
    when(paymentRepository.findPaymentByPspTypeAndPspPaymentId(
            PspType.ORANGE_MONEY, "REF-NO-PAYMENT"))
        .thenReturn(Optional.empty());

    RecoveryResult result = service.sync(date);

    verify(jOrangeTransactionRepository, times(1)).save(any());
    verify(paymentVerificationRequestedService, never()).accept(any());
    assertTrue(result.isSuccessful());
    assertEquals(1, result.getInserted());
  }

  @Test
  void sync_handlesEmptyTransactionList() throws Exception {
    LocalDate date = LocalDate.of(2025, 9, 17);

    OrangeDailyTransactions dailyTransactions = mock(OrangeDailyTransactions.class);
    when(dailyTransactions.getTransactions()).thenReturn(List.of());
    when(orangeApiClient.transactionsOf(date)).thenReturn(dailyTransactions);

    RecoveryResult result = service.sync(date);

    verify(jOrangeTransactionRepository, never()).save(any());
    verify(paymentVerificationRequestedService, never()).accept(any());
    assertTrue(result.isSuccessful());
    assertEquals(0, result.getInserted());
  }

  private OrangeTransaction createMockTransaction(String ref, Integer amount)
      throws JsonProcessingException {
    OrangeTransaction transaction = mock(OrangeTransaction.class);
    when(transaction.getRef()).thenReturn(ref);
    when(transaction.getAmount()).thenReturn(amount);

    when(OrangeApiClient.om.writeValueAsString(transaction))
        .thenReturn("{\"ref\":\"" + ref + "\",\"amount\":" + amount + "}");

    return transaction;
  }
}
