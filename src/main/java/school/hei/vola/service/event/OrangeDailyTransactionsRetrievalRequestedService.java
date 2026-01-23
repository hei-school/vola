package school.hei.vola.service.event;

import static school.hei.vola.model.psp.PspType.ORANGE_MONEY;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import school.hei.vola.endpoint.event.model.OrangeDailyTransactionsRetrievalRequested;
import school.hei.vola.endpoint.event.model.PaymentVerificationRequested;
import school.hei.vola.model.psp.orange.OrangeApiClient;
import school.hei.vola.model.psp.orange.OrangeTransaction;
import school.hei.vola.repository.PaymentRepository;
import school.hei.vola.repository.jpa.JOrangeTransactionRepository;
import school.hei.vola.repository.jpa.model.JOrangeTransaction;

@Slf4j
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
    log.info("Processing OrangeDailyTransactionsRetrievalRequested for date: {}", date);

    var dailyTransactions = orangeApiClient.transactionsOf(date);
    log.info(
        "Retrieved {} transactions from Orange API", dailyTransactions.getTransactions().size());

    dailyTransactions
        .getTransactions()
        .forEach(
            ot -> {
              log.info("Processing Orange transaction with ref: {}", ot.getRef());
              jOrangeTransactionRepository.save(toJOrangeTransaction(ot));
              log.info("Saved Orange transaction: {}", ot.getRef());
              verifyCorrespondingPayment(ot);
            });

    log.info(
        "Completed processing {} Orange transactions", dailyTransactions.getTransactions().size());
  }

  private void verifyCorrespondingPayment(OrangeTransaction ot) {
    log.info("Looking for payment with ref: {}", ot.getRef());
    var paymentOpt =
        paymentRepository.findPaymentByPspTypeAndPspPaymentId(ORANGE_MONEY, ot.getRef());

    if (paymentOpt.isPresent()) {
      log.info("Found payment for ref: {}, triggering verification", ot.getRef());
      paymentOpt.ifPresent(
          payment ->
              paymentVerificationRequestedService.accept(
                  PaymentVerificationRequested.builder().payment(payment).build()));
    } else {
      log.info("No payment found for ref: {}", ot.getRef());
    }
  }

  private JOrangeTransaction toJOrangeTransaction(OrangeTransaction ot) {
    var jot = new JOrangeTransaction();
    jot.setRef(ot.getRef());
    try {
      jot.setOrangeApiRawResponse(OrangeApiClient.om.writeValueAsString(ot));
      return jot;
    } catch (JsonProcessingException e) {
      log.error("Failed to serialize Orange transaction: {}", ot.getRef(), e);
      throw new RuntimeException(e);
    }
  }
}
