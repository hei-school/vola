package school.hei.vola.repository.jpa;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import school.hei.vola.model.psp.PspType;
import school.hei.vola.repository.jpa.model.JPayment;

@Repository
public interface JPaymentRepository extends JpaRepository<JPayment, String> {
  Optional<JPayment> findByPspTypeAndPspPaymentId(PspType pspType, String pspPaymentId);

  List<JPayment> findByPspPaymentIdIn(List<String> pspPaymentIds);

  Optional<JPayment> findPaymentByPayerEmailAndPspTypeAndPspPaymentId(
      String payerEmail, PspType pspType, String pspPaymentId);

  List<JPayment> findByApplication_Name(String applicationName);

  @Query(
      "SELECT p FROM JPayment p WHERE p.application.name = :applicationName "
          + "AND p.creationInstant >= :start AND p.creationInstant < :end")
  List<JPayment> findByApplicationNameAndCreationInstantBetween(
      @Param("applicationName") String applicationName,
      @Param("start") Instant start,
      @Param("end") Instant end);
}
