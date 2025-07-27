package school.hei.vola.model.psp.orange;

import static java.lang.Integer.parseInt;
import static school.hei.vola.model.psp.PspType.ORANGE_MONEY;

import java.time.LocalDate;
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

  @Override
  public PspType type() {
    return ORANGE_MONEY;
  }

  @Override
  public Optional<PspPayment> verify(String pspId) {
    var dateOpt = extractDate(pspId);
    if (dateOpt.isEmpty()) {
      return Optional.empty();
    }

    var transactions = orangeApiClient.transactionsOf(dateOpt.get());
    return transactions.getTransactions().stream()
        .filter(t -> pspId.equals(t.getRef()))
        .map(this::toPspPayment)
        .findFirst();
  }

  private PspPayment toPspPayment(OrangeTransaction orangeTransaction) {
    return new PspPayment(
        ORANGE_MONEY,
        orangeTransaction.getRef(),
        orangeTransaction.getAmount(),
        orangeTransaction.creationInstant());
  }

  private Optional<LocalDate> extractDate(String orangeRef) {
    try {
      /* orangeRef is like:
       * PP250730.0811.D71066
       * MP250730.1234.D58892 */
      int year = parseInt(20 + orangeRef.substring(2, 4));
      int month = parseInt(orangeRef.substring(4, 6));
      int day = parseInt(orangeRef.substring(6, 8));
      return Optional.of(LocalDate.of(year, month, day));
    } catch (Exception e) {
      return Optional.empty();
    }
  }
}
