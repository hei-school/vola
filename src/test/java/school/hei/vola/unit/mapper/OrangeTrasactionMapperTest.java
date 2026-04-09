package school.hei.vola.unit.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import org.junit.jupiter.api.Test;
import school.hei.vola.model.psp.orange.OrangeTransaction;
import school.hei.vola.repository.jpa.model.JOrangeTransaction;
import school.hei.vola.repository.mapper.OrangeTransactionMapper;

public class OrangeTrasactionMapperTest {
  private OrangeTransactionMapper orangeTransactionMapper = new OrangeTransactionMapper();

  @Test
  public void to_entity_test_ok() throws JsonProcessingException {
    var subject =
        new OrangeTransaction(
            1, "29/07/2025", "12:16:58", "MP250729.1216.D77954", "Succès", "0322797107", 324000);
    var expected = new JOrangeTransaction();
    expected.setRef("MP250729.1216.D77954");
    expected.setOrangeApiRawResponse(
        "{\"number\":1,\"date\":\"29/07/2025\",\"time\":\"12:16:58\",\"ref\":\"MP250729.1216.D77954\",\"status\":\"Succès\",\"amount\":324000,\"client_number\":\"0322797107\"}");

    var actual = orangeTransactionMapper.toEntity(subject);

    assertEquals(actual.getRef(), expected.getRef());
    assertEquals(actual.getOrangeApiRawResponse(), expected.getOrangeApiRawResponse());
  }

  @Test
  public void to_entities_test_ok() throws JsonProcessingException {
    var toAdd1 =
        new OrangeTransaction(
            1, "29/07/2025", "12:16:58", "MP250729.1216.D77954", "Succès", "0322797107", 324000);
    var toAdd2 =
        new OrangeTransaction(
            1, "05/08/2025", "09:18:47", "MP250805.0918.C21980", "Succès", "0320755515", 291500);
    var subject = List.of(toAdd1, toAdd2);

    var expected1 = new JOrangeTransaction();
    expected1.setRef("MP250729.1216.D77954");
    expected1.setOrangeApiRawResponse(
        "{\"number\":1,\"date\":\"29/07/2025\",\"time\":\"12:16:58\",\"ref\":\"MP250729.1216.D77954\",\"status\":\"Succès\",\"amount\":324000,\"client_number\":\"0322797107\"}");

    var expected2 = new JOrangeTransaction();
    expected2.setRef("MP250805.0918.C21980");
    expected2.setOrangeApiRawResponse(
        "{\"number\":1,\"date\":\"05/08/2025\",\"time\":\"09:18:47\",\"ref\":\"MP250805.0918.C21980\",\"status\":\"Succès\",\"amount\":291500,\"client_number\":\"0320755515\"}");

    var actual = orangeTransactionMapper.toEntities(List.of(toAdd1, toAdd2));

    assertEquals(actual.size(), subject.size());
    assertEquals(actual.get(0).getOrangeApiRawResponse(), expected1.getOrangeApiRawResponse());
  }
}
