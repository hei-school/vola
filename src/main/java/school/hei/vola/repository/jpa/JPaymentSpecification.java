package school.hei.vola.repository.jpa;

import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import school.hei.vola.repository.jpa.model.JPayment;

public class JPaymentSpecification {

  public static Specification<JPayment> withFilters(
      String applicationName, String scope, Instant start, Instant end) {
    return (root, query, criteriaBuilder) -> {
      List<Predicate> predicates = new ArrayList<>();

      if (applicationName != null && !applicationName.isEmpty()) {
        predicates.add(criteriaBuilder.equal(root.get("application").get("name"), applicationName));
      }

      if (scope != null && !scope.isEmpty()) {
        predicates.add(
            criteriaBuilder.like(
                criteriaBuilder.lower(root.get("scope")), "%" + scope.toLowerCase() + "%"));
      }

      if (start != null) {
        predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("creationInstant"), start));
      }

      if (end != null) {
        predicates.add(criteriaBuilder.lessThan(root.get("creationInstant"), end));
      }

      return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    };
  }

  public static Specification<JPayment> withAmountNotNull() {
    return (root, query, criteriaBuilder) -> criteriaBuilder.isNotNull(root.get("amount"));
  }

  public static Specification<JPayment> withAmountNull() {
    return (root, query, criteriaBuilder) -> criteriaBuilder.isNull(root.get("amount"));
  }

  public static Specification<JPayment> withVerificationAttemptNbLessThanOrEqual(int maxAttempts) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.lessThanOrEqualTo(root.get("verificationAttemptNb"), maxAttempts);
  }
}
