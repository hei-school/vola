package school.hei.vola.service.event;

import static java.nio.file.Files.readAllBytes;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import school.hei.vola.conf.FacadeIT;
import school.hei.vola.file.bucket.BucketComponent;
import school.hei.vola.repository.OrangePaymentRepository;
import school.hei.vola.service.MultipartFileConverter;
import school.hei.vola.service.utils.ExcelParser;

public class OrangeTransactionImportRequestedIT extends FacadeIT {
  @Autowired OrangePaymentRepository orangePaymentRepository;
  @MockBean BucketComponent bucketComponent;
  @Autowired MultipartFileConverter multipartFileConverter;
  @Autowired ExcelParser excelParser;

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
    when(bucketComponent.download(any())).thenReturn(file);
    var inOrder = inOrder(bucketComponent, excelParser, orangePaymentRepository);

    inOrder.verify(bucketComponent).download(bucketKey);
    inOrder.verify(excelParser).parseToOrangeTransaction(file);
    var validTransactions = excelParser.parseToOrangeTransaction(file).validTransactions();
    inOrder.verify(orangePaymentRepository).saveAll(validTransactions);
  }
}
