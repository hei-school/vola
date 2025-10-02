package school.hei.vola.service;

import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import school.hei.vola.endpoint.event.model.PaymentVerificationRequested;
import school.hei.vola.model.psp.PspType;
import school.hei.vola.model.psp.orange.OrangeApiClient;
import school.hei.vola.model.psp.orange.OrangeDailyTransactions;
import school.hei.vola.model.psp.orange.OrangeTransaction;
import school.hei.vola.repository.OrangePaymentRepository;
import school.hei.vola.repository.PaymentRepository;
import school.hei.vola.repository.jpa.JOrangeTransactionRepository;
import school.hei.vola.service.event.PaymentVerificationRequestedService;

/**
 * Unit test for OrangeTransactionRecoveryService. Uses mocks for all dependencies and a mocked
 * OrangeDailyTransactions instance.
 */
class OrangeTransactionRecoveryServiceTest {

  private final OrangeApiClient orangeApiClient = mock(OrangeApiClient.class);
  private final JOrangeTransactionRepository jOrangeTransactionRepository =
      mock(JOrangeTransactionRepository.class);
  private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
  private final PaymentVerificationRequestedService paymentVerificationRequestedService =
      mock(PaymentVerificationRequestedService.class);
  private final OrangePaymentRepository orangePaymentRepository =
      mock(OrangePaymentRepository.class);

  private final OrangeTransactionRecoveryService service =
      new OrangeTransactionRecoveryService(
          orangeApiClient,
          jOrangeTransactionRepository,
          paymentRepository,
          paymentVerificationRequestedService,
          orangePaymentRepository);

  @Test
  void recover_insertsMissingTransaction_and_triggersVerification_whenPaymentExists()
      throws Exception {
    LocalDate date = LocalDate.of(2025, 9, 17);

    // create two OrangeTransaction mocks
    OrangeTransaction ot1 = mock(OrangeTransaction.class);
    when(ot1.getRef()).thenReturn("REF-1");
    when(ot1.getAmount()).thenReturn(1000);

    OrangeTransaction ot2 = mock(OrangeTransaction.class);
    when(ot2.getRef()).thenReturn("REF-2");
    when(ot2.getAmount()).thenReturn(2000);

    // create a mocked OrangeDailyTransactions that returns our list
    OrangeDailyTransactions daily = mock(OrangeDailyTransactions.class);
    when(daily.getTransactions()).thenReturn(List.of(ot1, ot2));

    // stub the client to return our daily wrapper
    when(orangeApiClient.transactionsOf(date)).thenReturn(daily);

    // simulate: REF-1 does not exist in DB, REF-2 already exists
    when(jOrangeTransactionRepository.existsById("REF-1")).thenReturn(false);
    when(jOrangeTransactionRepository.existsById("REF-2")).thenReturn(true);

    // simulate a payment existing for REF-1 only
    school.hei.vola.model.Payment paymentForRef1 = mock(school.hei.vola.model.Payment.class);
    when(paymentForRef1.id()).thenReturn("payment-1");
    when(paymentRepository.findPaymentByPspTypeAndPspPaymentId(PspType.ORANGE_MONEY, "REF-1"))
        .thenReturn(Optional.of(paymentForRef1));
    when(paymentRepository.findPaymentByPspTypeAndPspPaymentId(PspType.ORANGE_MONEY, "REF-2"))
        .thenReturn(Optional.empty());

    // Call recover
    var results = service.recover(date);

    // Verify that REF-1 was saved
    verify(jOrangeTransactionRepository, times(1))
        .save(
            argThat(
                jot -> {
                  try {
                    var method = jot.getClass().getMethod("getRef");
                    Object val = method.invoke(jot);
                    return "REF-1".equals(val);
                  } catch (Exception e) {
                    return false;
                  }
                }));

    // Verify verification was requested for payment-1
    ArgumentCaptor<PaymentVerificationRequested> captor =
        ArgumentCaptor.forClass(PaymentVerificationRequested.class);
    verify(paymentVerificationRequestedService, times(1)).accept(captor.capture());
    PaymentVerificationRequested req = captor.getValue();
    // assert the payment passed in is the one we simulated
    assert req.getPayment().id().equals("payment-1");

    // Check results contains two entries and flags as expected
    assert results.size() == 2;
    var r1 = results.stream().filter(r -> r.getRef().equals("REF-1")).findFirst().get();
    var r2 = results.stream().filter(r -> r.getRef().equals("REF-2")).findFirst().get();
    assert r1.isInserted(); // inserted true for REF-1
    assert r1.isPaymentFound(); // payment found for REF-1
    assert !r2.isInserted(); // existing REF-2 => not inserted
  }
}
