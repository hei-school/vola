package school.hei.vola.repository.jpa;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import school.hei.vola.model.PaymentInfo;
import school.hei.vola.model.psp.PspType;
import school.hei.vola.repository.jpa.model.JPayment;

@Repository
public interface JPaymentRepository extends JpaRepository<JPayment, String> {
  Optional<JPayment> findByPspTypeAndPspPaymentId(PspType pspType, String pspPaymentId);

  Optional<JPayment> findPaymentByPayerEmailAndPspTypeAndPspPaymentId(
      String payerEmail, PspType pspType, String pspPaymentId);

  default List<JPayment> findByPaymentInfos(List<PaymentInfo> paymentInfos) {
    if (paymentInfos.isEmpty()) {
      return List.of();
    }

    List<String> compositeKeys = buildCompositeKeys(paymentInfos);
    return findByPaymentInfosBatch(compositeKeys);
  }

  @Query(
      value =
          """
SELECT p.id, p.user_id, p.psp_type, p.psp_payment_id, p.psp_creation_instant, p.amount, p.creation_instant, p.last_psp_verification_instant, p.application_id, p.verification_attempt_nb
FROM payment p
INNER JOIN "user" u ON p.user_id = u.id
WHERE CONCAT(u.email, '|', p.psp_type, '|', p.psp_payment_id) IN (:compositeKeys)
""",
      nativeQuery = true)
  List<JPayment> findByPaymentInfosBatch(@Param("compositeKeys") List<String> compositeKeys);

  private List<String> buildCompositeKeys(List<PaymentInfo> paymentInfos) {
    return paymentInfos.stream()
        .map(info -> buildCompositeKey(info.payerEmail(), info.pspType(), info.pspPaymentId()))
        .toList();
  }

  private String buildCompositeKey(String email, PspType pspType, String pspPaymentId) {
    return String.format("%s|%s|%s", email, pspType.name(), pspPaymentId);
  }
}
