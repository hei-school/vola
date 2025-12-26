package school.hei.vola.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import school.hei.vola.model.Payment;
import school.hei.vola.model.psp.PspType;
import school.hei.vola.model.psp.orange.OrangeApiClient;
import school.hei.vola.model.psp.orange.OrangeDailyTransactions;
import school.hei.vola.model.psp.orange.OrangeTransaction;
import school.hei.vola.repository.OrangePaymentRepository;
import school.hei.vola.repository.PaymentRepository;
import school.hei.vola.service.event.PaymentVerificationRequestedService;

class OrangeSyncServiceTest {

  private final OrangeApiClient api = mock(OrangeApiClient.class);
  private final OrangePaymentRepository orangeRepo = mock(OrangePaymentRepository.class);
  private final PaymentRepository paymentRepo = mock(PaymentRepository.class);
  private final PaymentVerificationRequestedService verifier =
          mock(PaymentVerificationRequestedService.class);
  private final OrangeSyncService service =
          new OrangeSyncService(api, orangeRepo, paymentRepo, verifier);

  @Test
  void sync_insertsAndTriggersVerification() {
    var date = LocalDate.of(2025, 9, 17);
    var ot = mock(OrangeTransaction.class);
    when(ot.getRef()).thenReturn("REF-1");

    var daily = mock(OrangeDailyTransactions.class);
    when(daily.getTransactions()).thenReturn(List.of(ot));
    when(api.transactionsOf(date)).thenReturn(daily);

    when(orangeRepo.findById("REF-1")).thenReturn(Optional.empty());

    var payment = mock(Payment.class);
    when(paymentRepo.findPaymentByPspTypeAndPspPaymentId(PspType.ORANGE_MONEY, "REF-1"))
            .thenReturn(Optional.of(payment));

    var result = service.sync(date);

    InOrder inOrder = inOrder(orangeRepo, verifier);
    inOrder.verify(orangeRepo).findById("REF-1");
    inOrder.verify(orangeRepo).save(ot);
    inOrder.verify(verifier).accept(any());

    assertTrue(result.isSuccessful());
    assertEquals(1, result.nbSyncedTransactions());
    assertEquals(1, result.nbTotalTransactions());
  }
}
