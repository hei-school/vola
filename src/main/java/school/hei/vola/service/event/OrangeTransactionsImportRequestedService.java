package school.hei.vola.service.event;

import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import school.hei.vola.endpoint.event.model.OrangeTransactionsImportRequested;
import school.hei.vola.repository.OrangePaymentRepository;

@Slf4j
@Service
@AllArgsConstructor
public class OrangeTransactionsImportRequestedService
    implements Consumer<OrangeTransactionsImportRequested> {
  private final OrangePaymentRepository orangePaymentRepository;

  @Override
  public void accept(OrangeTransactionsImportRequested orangeTransactionsImportFromXlsxRequested) {
    log.info(
        "Save valid transactions : "
            + orangeTransactionsImportFromXlsxRequested.getOrangeTransactions().toString());
    var savedOrangeTransactions =
        orangePaymentRepository.saveAll(
            orangeTransactionsImportFromXlsxRequested.getOrangeTransactions());
    log.info("Saved valid transactions : " + savedOrangeTransactions.toString());
  }
}
