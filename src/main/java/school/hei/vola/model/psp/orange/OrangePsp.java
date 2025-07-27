package school.hei.vola.model.psp.orange;

import static java.time.temporal.ChronoUnit.DAYS;
import static school.hei.vola.model.Time.millisNow;
import static school.hei.vola.model.psp.PspType.ORANGE_MONEY;
import static school.hei.vola.model.psp.orange.OrangeTransaction.TransactionStatus.SUCCEEDED;

import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import school.hei.vola.model.psp.Psp;
import school.hei.vola.model.psp.PspPayment;
import school.hei.vola.model.psp.PspType;

@AllArgsConstructor
@Slf4j
public class OrangePsp implements Psp {

  private final OrangeApiClient orangeApiClient;
  private final int NB_OF_PAST_DAYS_TO_VERIFY = 7;

  @Override
  public PspType type() {
    return ORANGE_MONEY;
  }

  @Override
  public Optional<PspPayment> verify(String pspId) {
    var now = millisNow();
    for (var minusDay = 0; minusDay <= NB_OF_PAST_DAYS_TO_VERIFY; minusDay++) {
      var orangeDailyTransactions = orangeApiClient.transactionsOf(now.minus(minusDay, DAYS));

      var verifiedTransactions =
          orangeDailyTransactions.getTransactions().stream()
              .filter(ot -> pspId.equals(ot.getRef()) && SUCCEEDED.equals(ot.status()))
              .toList();
      if (!verifiedTransactions.isEmpty()) {
        if (verifiedTransactions.size() > 1) {
          log.error(
              "There more than 1 transaction that matched the pspId to verify: {}",
              verifiedTransactions);
        }
        return Optional.of(toPspPayment(verifiedTransactions.stream().findFirst().get()));
      }
    }

    return Optional.empty();
  }

  private PspPayment toPspPayment(OrangeTransaction ot) {
    return new PspPayment(ORANGE_MONEY, ot.getRef(), ot.getAmount(), ot.creationInstant());
  }
}
