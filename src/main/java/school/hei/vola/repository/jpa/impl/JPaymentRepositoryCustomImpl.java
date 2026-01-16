package school.hei.vola.repository.jpa.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.List;
import org.springframework.stereotype.Repository;
import school.hei.vola.model.PaymentInfo;
import school.hei.vola.repository.jpa.JPaymentRepositoryCustom;
import school.hei.vola.repository.jpa.model.JPayment;

@Repository
public class JPaymentRepositoryCustomImpl implements JPaymentRepositoryCustom {

  private final EntityManager entityManager;

  public JPaymentRepositoryCustomImpl(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  @Override
  public List<JPayment> findByPaymentInfos(List<PaymentInfo> paymentInfos) {
    if (paymentInfos == null || paymentInfos.isEmpty()) {
      return List.of();
    }

    String sql = buildQueryForPaymentInfos(paymentInfos.size());
    Query query = entityManager.createNativeQuery(sql, JPayment.class);

    int paramIndex = 1;
    for (PaymentInfo info : paymentInfos) {
      query.setParameter(paramIndex++, info.payerEmail());
      query.setParameter(paramIndex++, info.pspType().name());
      query.setParameter(paramIndex++, info.pspPaymentId());
    }

    return query.getResultList();
  }

  private String buildQueryForPaymentInfos(int count) {
    StringBuilder conditions = new StringBuilder();

    for (int i = 0; i < count; i++) {
      if (i > 0) {
        conditions.append(" OR ");
      }
      int base = i * 3 + 1;
      conditions.append(
          String.format(
              "(u.email = ?%d AND p.psp_type = ?%d AND p.psp_payment_id = ?%d)",
              base, base + 1, base + 2));
    }

    return """
           SELECT p.id, p.user_id, p.psp_type, p.psp_payment_id, p.psp_creation_instant,
                  p.amount, p.creation_instant, p.last_psp_verification_instant,
                  p.application_id, p.verification_attempt_nb
           FROM payment p
           INNER JOIN "user" u ON p.user_id = u.id
           WHERE """
        + conditions;
  }
}
