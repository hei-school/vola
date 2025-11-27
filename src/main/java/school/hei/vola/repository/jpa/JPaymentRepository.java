package school.hei.vola.repository.jpa;

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

  Optional<JPayment> findPaymentByPayerEmailAndPspTypeAndPspPaymentId(
      String payerEmail, PspType pspType, String pspPaymentId);


  @Query("SELECT p FROM JPayment p WHERE " +
          "(p.payer.email = :email AND p.pspType = :pspType AND p.pspPaymentId = :pspPaymentId)")
  List<JPayment> findPaymentsByPaymentInfosCustom(
          @Param("email") String email,
          @Param("pspType") PspType pspType,
          @Param("pspPaymentId") String pspPaymentId);

}
