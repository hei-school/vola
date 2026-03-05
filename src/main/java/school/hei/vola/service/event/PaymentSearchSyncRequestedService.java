package school.hei.vola.service.event;

import static school.hei.vola.model.Time.millisNow;
import static school.hei.vola.model.psp.PspType.ORANGE_MONEY;

import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import school.hei.vola.endpoint.event.model.PaymentSearchSyncRequested;
import school.hei.vola.model.PaymentInfo;
import school.hei.vola.model.psp.PspPayment;
import school.hei.vola.model.psp.orange.OrangeApiClient;
import school.hei.vola.repository.OrangePaymentRepository;
import school.hei.vola.repository.PaymentRepository;

@Service
@AllArgsConstructor
@Slf4j
public class PaymentSearchSyncRequestedService implements Consumer<PaymentSearchSyncRequested> {

  private final PaymentRepository paymentRepository;
  private final OrangePaymentRepository orangePaymentRepository;
  private final OrangeApiClient orangeApiClient;

  @Override
  public void accept(PaymentSearchSyncRequested event) {
    var date = event.getDate();
    var apiKey = event.getApiKey();
    var paymentInfos = event.getPaymentInfos();
    var pspPaymentIds =
        paymentInfos.stream().map(PaymentInfo::pspPaymentId).collect(Collectors.toSet());

    var transactions = orangeApiClient.transactionsOf(date).getTransactions();

    for (var ot : transactions) {
      if (!pspPaymentIds.contains(ot.getRef())) {
        continue;
      }

      var existing =
          paymentRepository.findPaymentByPspTypeAndPspPaymentId(ORANGE_MONEY, ot.getRef());
      if (existing.isPresent()) {
        log.info("[ASYNC-SYNC] Payment already exists for ref={}", ot.getRef());
        continue;
      }

      var matchingInfo =
          paymentInfos.stream()
              .filter(info -> info.pspPaymentId().equals(ot.getRef()))
              .findFirst()
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "No matching PaymentInfo found for ref=" + ot.getRef()));

      orangePaymentRepository.save(ot);

      var payment =
          paymentRepository.createPayment(
              apiKey, matchingInfo.payerEmail(), ORANGE_MONEY, ot.getRef());

      var verifiedPayment =
          payment.toBuilder()
              .pspPayment(
                  new PspPayment(ORANGE_MONEY, ot.getRef(), ot.getAmount(), ot.creationInstant()))
              .lastPspVerificationInstant(millisNow())
              .build();
      paymentRepository.save(verifiedPayment);

      log.info("[ASYNC-SYNC] Auto-created verified payment for ref={}", ot.getRef());
    }
  }
}
