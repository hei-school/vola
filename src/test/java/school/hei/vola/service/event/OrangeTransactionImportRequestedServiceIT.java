package school.hei.vola.service.event;

import static java.nio.file.Files.readAllBytes;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import school.hei.vola.conf.FacadeIT;
import school.hei.vola.endpoint.event.model.OrangeTransactionsImportRequested;
import school.hei.vola.file.bucket.BucketComponent;
import school.hei.vola.model.ImportedTransactionDetails;
import school.hei.vola.repository.OrangePaymentRepository;
import school.hei.vola.service.MultipartFileConverter;
import school.hei.vola.service.utils.ExcelParser;

public class OrangeTransactionImportRequestedServiceIT extends FacadeIT {
  @MockBean BucketComponent bucketComponent;
  @Autowired MultipartFileConverter multipartFileConverter;
  @MockBean ExcelParser excelParser;
  @Autowired OrangeTransactionsImportRequestedService orangeTransactionsImportRequestedService;
  @MockBean OrangePaymentRepository orangePaymentRepository;

  @Test
  void save_transactions_imported() throws IOException {
    var path = Paths.get("src/test/resources/mock/bad-transactions-data.xls");
    var multipartFile =
        new MockMultipartFile(
            "bad-transactions-data.xls",
            "bad-transactions-data.xls",
            "application/vnd.ms-excel",
            readAllBytes(path));
    var file = multipartFileConverter.apply(multipartFile);
    var bucketKey = "/TRANSACTIONS_IMPORT_XLS/" + file.getName();
    var result = new ImportedTransactionDetails(List.of(), List.of());
    when(bucketComponent.download(any())).thenReturn(file);
    when(excelParser.parseToOrangeTransaction(file)).thenReturn(result);
    when(orangePaymentRepository.saveAll(anyList())).thenReturn(List.of());

    orangeTransactionsImportRequestedService.accept(
        new OrangeTransactionsImportRequested(bucketKey));

    verify(excelParser).parseToOrangeTransaction(file);
    verify(bucketComponent).download(bucketKey);
    verify(orangePaymentRepository).saveAll(List.of());
  }
}
