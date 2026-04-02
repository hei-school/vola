package school.hei.vola.repository.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import org.springframework.stereotype.Component;
import school.hei.vola.model.psp.orange.OrangeApiClient;
import school.hei.vola.model.psp.orange.OrangeTransaction;
import school.hei.vola.repository.jpa.model.JOrangeTransaction;

@Component
public class OrangeTransactionMapper {
  public JOrangeTransaction toEntity(OrangeTransaction ot) throws JsonProcessingException {
    var jOrangeTransaction = new JOrangeTransaction();
    jOrangeTransaction.setRef(ot.getRef());
    jOrangeTransaction.setOrangeApiRawResponse(OrangeApiClient.om.writeValueAsString(ot));
    return jOrangeTransaction;
  }

  public List<JOrangeTransaction> toEntity(List<OrangeTransaction> ots)
      throws JsonProcessingException {
    return ots.stream()
        .map(
            ot -> {
              try {
                return toEntity(ot);
              } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
              }
            })
        .toList();
  }
}
