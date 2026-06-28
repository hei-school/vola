package zone.hei.telecom.vola.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import zone.hei.telecom.vola.model.Payment;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Page<Payment> findByStudentRefContainingIgnoreCase(String studentRef, Pageable pageable);
}
