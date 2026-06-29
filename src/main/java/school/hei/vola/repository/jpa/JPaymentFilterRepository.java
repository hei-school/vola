package school.hei.vola.repository.jpa;

import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import school.hei.vola.repository.jpa.model.JPayment;

@Component
@RequiredArgsConstructor
public class JPaymentFilterRepository {

  private final JPaymentRepository jPaymentRepository;

  public List<JPayment> findByApplicationNameAndCreationInstantBetween(
      String applicationName, String scope, Instant start, Instant end) {
    Specification<JPayment> spec =
        JPaymentSpecification.withFilters(applicationName, scope, start, end);
    return jPaymentRepository.findAll(spec);
  }

  public Page<JPayment> findFilteredPage(
      String applicationName, String scope, Instant start, Instant end, Pageable pageable) {
    Specification<JPayment> spec =
        JPaymentSpecification.withFilters(applicationName, scope, start, end);
    return jPaymentRepository.findAll(spec, pageable);
  }

  public long countFiltered(String applicationName, String scope, Instant start, Instant end) {
    Specification<JPayment> spec =
        JPaymentSpecification.withFilters(applicationName, scope, start, end);
    return jPaymentRepository.count(spec);
  }

  public long sumAmountForSucceeded(
      String applicationName, String scope, Instant start, Instant end) {
    Specification<JPayment> spec =
        Specification.where(JPaymentSpecification.withFilters(applicationName, scope, start, end))
            .and(JPaymentSpecification.withAmountNotNull());

    return jPaymentRepository.findAll(spec).stream()
        .mapToLong(jPayment -> jPayment.getAmount() != null ? jPayment.getAmount() : 0L)
        .sum();
  }

  public long countPending(String applicationName, String scope, Instant start, Instant end) {
    Specification<JPayment> spec =
        Specification.where(JPaymentSpecification.withFilters(applicationName, scope, start, end))
            .and(JPaymentSpecification.withAmountNull())
            .and(JPaymentSpecification.withVerificationAttemptNbLessThanOrEqual(5));

    return jPaymentRepository.count(spec);
  }
}
