package school.hei.vola.service.event;

import static school.hei.vola.model.psp.PspType.ORANGE_MONEY;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import school.hei.vola.endpoint.event.model.OrangeDailyTransactionsRetrievalRequested;
import school.hei.vola.endpoint.event.model.PaymentVerificationRequested;
import school.hei.vola.model.psp.orange.OrangeApiClient;
import school.hei.vola.model.psp.orange.OrangeTransaction;
import school.hei.vola.repository.PaymentRepository;
import school.hei.vola.repository.jpa.JOrangeTransactionRepository;
import school.hei.vola.repository.jpa.model.JOrangeTransaction;

@Component
@AllArgsConstructor
public class OrangeDailyTransactionsRetrievalRequestedService
    implements Consumer<OrangeDailyTransactionsRetrievalRequested> {

  private final OrangeApiClient orangeApiClient;
  private final JOrangeTransactionRepository jOrangeTransactionRepository;

  private final PaymentRepository paymentRepository;
  private final PaymentVerificationRequestedService paymentVerificationRequestedService;

  @Override
  public void accept(
      OrangeDailyTransactionsRetrievalRequested orangeDailyTransactionsRetrievalRequested) {
    var date = orangeDailyTransactionsRetrievalRequested.getDate();
    var dailyTransactions = orangeApiClient.transactionsOf(date);

    dailyTransactions
        .getTransactions()
        .forEach(
            ot -> {
              jOrangeTransactionRepository.save(toJOrangeTransaction(ot));
              verifyCorrespondingPayment(ot);
            });
  }

  private void verifyCorrespondingPayment(OrangeTransaction ot) {
    var paymentOpt =
        paymentRepository.findPaymentByPspTypeAndPspPaymentId(ORANGE_MONEY, ot.getRef());
    paymentOpt.ifPresent(
        payment ->
            paymentVerificationRequestedService.accept(
                PaymentVerificationRequested.builder().payment(payment).build()));
  }

  private JOrangeTransaction toJOrangeTransaction(OrangeTransaction ot) {
    var jot = new JOrangeTransaction();
    jot.setRef(ot.getRef());
    try {
      jot.setOrangeApiRawResponse(OrangeApiClient.om.writeValueAsString(ot));
      return jot;
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
