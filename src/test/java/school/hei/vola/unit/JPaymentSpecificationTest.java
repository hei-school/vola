package school.hei.vola.unit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.Test;
import school.hei.vola.repository.jpa.JPaymentSpecification;

class JPaymentSpecificationTest {
  @Test
  void withAmountNotNull_returnsSpec() {
    var root = mock(Root.class);
    var query = mock(CriteriaQuery.class);
    var cb = mock(CriteriaBuilder.class);
    when(root.get("amount")).thenReturn(mock(Path.class));
    when(cb.isNotNull(any())).thenReturn(mock(Predicate.class));

    var spec = JPaymentSpecification.withAmountNotNull();
    var predicate = spec.toPredicate(root, query, cb);

    assertNotNull(predicate);
    verify(cb).isNotNull(any());
  }

  @Test
  void withAmountNull_returnsSpec() {
    var root = mock(Root.class);
    var query = mock(CriteriaQuery.class);
    var cb = mock(CriteriaBuilder.class);
    when(root.get("amount")).thenReturn(mock(Path.class));
    when(cb.isNull(any())).thenReturn(mock(Predicate.class));

    var spec = JPaymentSpecification.withAmountNull();
    var predicate = spec.toPredicate(root, query, cb);

    assertNotNull(predicate);
    verify(cb).isNull(any());
  }

  @Test
  void withVerificationAttemptNbLessThanOrEqual_returnsSpec() {
    var root = mock(Root.class);
    var query = mock(CriteriaQuery.class);
    var cb = mock(CriteriaBuilder.class);
    when(root.get("verificationAttemptNb")).thenReturn(mock(Path.class));
    when(cb.lessThanOrEqualTo(any(), anyInt())).thenReturn(mock(Predicate.class));

    var spec = JPaymentSpecification.withVerificationAttemptNbLessThanOrEqual(5);
    var predicate = spec.toPredicate(root, query, cb);

    assertNotNull(predicate);
    verify(cb).lessThanOrEqualTo(any(), eq(5));
  }
}
