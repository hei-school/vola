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

  private static final String PAYMENT_SELECT_QUERY =
      """
      SELECT p.id, p.user_id, p.psp_type, p.psp_payment_id, p.psp_creation_instant,
             p.amount, p.creation_instant, p.last_psp_verification_instant,
             p.application_id, p.verification_attempt_nb
      FROM payment p
      INNER JOIN "user" u ON p.user_id = u.id
      """;

  private final EntityManager entityManager;

  public JPaymentRepositoryCustomImpl(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  @Override
  public List<JPayment> findByPaymentInfos(List<PaymentInfo> paymentInfos) {
    if (isPaymentInfosNullOrEmpty(paymentInfos)) {
      return List.of();
    }

    String queryString = buildQuery(paymentInfos.size());
    Query query = createQueryWithParameters(queryString, paymentInfos);

    return getQueryResults(query);
  }

  private boolean isPaymentInfosNullOrEmpty(List<PaymentInfo> paymentInfos) {
    return paymentInfos == null || paymentInfos.isEmpty();
  }

  private String buildQuery(int paymentInfoCount) {
    StringBuilder whereClauseBuilder = new StringBuilder();

    for (int i = 0; i < paymentInfoCount; i++) {
      if (i > 0) {
        whereClauseBuilder.append(" OR ");
      }
      whereClauseBuilder.append(buildPaymentInfoCondition(i));
    }

    return PAYMENT_SELECT_QUERY + " WHERE " + whereClauseBuilder;
  }

  private String buildPaymentInfoCondition(int index) {
    int baseParamIndex = index * 3 + 1;
    int emailParamIndex = baseParamIndex;
    int pspTypeParamIndex = baseParamIndex + 1;
    int pspIdParamIndex = baseParamIndex + 2;

    return String.format(
        "(u.email = ?%d AND p.psp_type = ?%d AND p.psp_payment_id = ?%d)",
        emailParamIndex, pspTypeParamIndex, pspIdParamIndex);
  }

  private Query createQueryWithParameters(String queryString, List<PaymentInfo> paymentInfos) {
    Query query = entityManager.createNativeQuery(queryString, JPayment.class);
    setQueryParameters(query, paymentInfos);
    return query;
  }

  private void setQueryParameters(Query query, List<PaymentInfo> paymentInfos) {
    int paramPosition = 1;

    for (PaymentInfo paymentInfo : paymentInfos) {
      query.setParameter(paramPosition++, paymentInfo.payerEmail());
      query.setParameter(paramPosition++, paymentInfo.pspType().name());
      query.setParameter(paramPosition++, paymentInfo.pspPaymentId());
    }
  }

  @SuppressWarnings("unchecked")
  private List<JPayment> getQueryResults(Query query) {
    return query.getResultList();
  }
}
