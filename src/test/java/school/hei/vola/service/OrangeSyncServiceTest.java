package school.hei.vola.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import school.hei.vola.model.Payment;
import school.hei.vola.model.psp.PspType;
import school.hei.vola.model.psp.orange.OrangeApiClient;
import school.hei.vola.model.psp.orange.OrangeDailyTransactions;
import school.hei.vola.model.psp.orange.OrangeTransaction;
import school.hei.vola.repository.PaymentRepository;
import school.hei.vola.repository.jpa.JOrangeTransactionRepository;
import school.hei.vola.service.event.PaymentVerificationRequestedService;

class OrangeSyncServiceTest {

  private final OrangeApiClient api = mock(OrangeApiClient.class);
  private final JOrangeTransactionRepository txRepo = mock(JOrangeTransactionRepository.class);
  private final PaymentRepository paymentRepo = mock(PaymentRepository.class);
  private final PaymentVerificationRequestedService verifier =
      mock(PaymentVerificationRequestedService.class);
  private final OrangeSyncService service =
      new OrangeSyncService(api, txRepo, paymentRepo, verifier);

  @Test
  void sync_success() throws Exception {
    var date = LocalDate.of(2025, 9, 17);
    var tx = mock(OrangeTransaction.class);
    when(tx.getRef()).thenReturn("REF-1");
    when(OrangeApiClient.om.writeValueAsString(tx)).thenReturn("{}");

    var daily = mock(OrangeDailyTransactions.class);
    when(daily.getTransactions()).thenReturn(List.of(tx));
    when(api.transactionsOf(date)).thenReturn(daily);
    when(txRepo.existsById("REF-1")).thenReturn(false);

    var payment = mock(Payment.class);
    when(paymentRepo.findPaymentByPspTypeAndPspPaymentId(PspType.ORANGE_MONEY, "REF-1"))
        .thenReturn(Optional.of(payment));

    var result = service.sync(date);

    verify(txRepo).save(any());
    verify(verifier).accept(any());
    assertTrue(result.isSuccessful());
    assertEquals(1, result.inserted());
  }

  @Test
  void sync_failure() {
    var date = LocalDate.of(2025, 9, 17);
    when(api.transactionsOf(date)).thenThrow(new RuntimeException("API error"));

    var result = service.sync(date);

    assertFalse(result.isSuccessful());
    assertEquals("API error", result.errorMessage());
  }
}
