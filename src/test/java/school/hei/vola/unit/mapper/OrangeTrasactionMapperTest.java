package school.hei.vola.unit.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
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
}
