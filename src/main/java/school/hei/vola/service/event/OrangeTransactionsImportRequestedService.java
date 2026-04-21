package school.hei.vola.service.event;

import java.io.IOException;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import school.hei.vola.endpoint.event.model.OrangeTransactionsImportRequested;
import school.hei.vola.file.bucket.BucketComponent;
import school.hei.vola.repository.OrangePaymentRepository;
import school.hei.vola.service.utils.ExcelParser;

@Slf4j
@Service
@AllArgsConstructor
public class OrangeTransactionsImportRequestedService
    implements Consumer<OrangeTransactionsImportRequested> {
  private final OrangePaymentRepository orangePaymentRepository;
  private final BucketComponent bucketComponent;
  private ExcelParser excelParser;

  @Override
  public void accept(OrangeTransactionsImportRequested event) {
    var file = bucketComponent.download(event.getBucketKey());
    try {
      var orangeTransactions = excelParser.parseToOrangeTransaction(file);
      var validTransactions = orangeTransactions.validOrangeTransactions();
      log.info("Save valid transactions : " + validTransactions.toString());
      var savedOrangeTransactions = orangePaymentRepository.saveAll(validTransactions);
      log.info("Saved valid transactions : " + savedOrangeTransactions.toString());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
