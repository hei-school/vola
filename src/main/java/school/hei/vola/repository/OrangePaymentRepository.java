package school.hei.vola.repository;

import static school.hei.vola.model.psp.PspType.ORANGE_MONEY;
import static school.hei.vola.model.psp.orange.OrangeTransaction.TransactionStatus.SUCCEEDED;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;
import school.hei.vola.model.psp.PspPayment;
import school.hei.vola.model.psp.orange.OrangeApiClient;
import school.hei.vola.model.psp.orange.OrangeTransaction;
import school.hei.vola.repository.jpa.JOrangeTransactionRepository;
import school.hei.vola.repository.jpa.model.JOrangeTransaction;
import school.hei.vola.repository.mapper.OrangeTransactionMapper;

@Repository
@AllArgsConstructor
public class OrangePaymentRepository {

  private final JOrangeTransactionRepository jOrangeTransactionRepository;
  private final OrangeTransactionMapper orangeTransactionMapper;

  public Optional<PspPayment> findById(String orangeRef) {
    var jOrangeTransactionOpt = jOrangeTransactionRepository.findById(orangeRef);
    return jOrangeTransactionOpt
        .map(JOrangeTransaction::getOrangeApiRawResponse)
        .map(this::typeRawOrangeApiResponse)
        // /!\ Other status have no test, yet critical. Do __NOT__ remove.
        .filter(ot -> SUCCEEDED.equals(ot.status()))
        .map(this::toPspPayment);
  }

  public OrangeTransaction save(OrangeTransaction ot) {
    try {
      var jOrangeTransaction = new JOrangeTransaction();
      jOrangeTransaction.setRef(ot.getRef());
      jOrangeTransaction.setOrangeApiRawResponse(OrangeApiClient.om.writeValueAsString(ot));
      return typeRawOrangeApiResponse(
          jOrangeTransactionRepository.save(jOrangeTransaction).getOrangeApiRawResponse());
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public List<OrangeTransaction> saveAll(List<OrangeTransaction> ots) {
    try {
      var jOrangeTransactions = orangeTransactionMapper.toEntities(ots);
      return typeRawOrangeApiResponses(jOrangeTransactionRepository.saveAll(jOrangeTransactions));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private OrangeTransaction typeRawOrangeApiResponse(String raw) {
    try {
      return OrangeApiClient.om.readValue(raw, OrangeTransaction.class);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  private List<OrangeTransaction> typeRawOrangeApiResponses(List<JOrangeTransaction> raws) {
    return raws.stream()
        .map(raw -> typeRawOrangeApiResponse(raw.getOrangeApiRawResponse()))
        .toList();
  }

  private PspPayment toPspPayment(OrangeTransaction orangeTransaction) {
    return new PspPayment(
        ORANGE_MONEY,
        orangeTransaction.getRef(),
        orangeTransaction.getAmount(),
        orangeTransaction.creationInstant());
  }
}
