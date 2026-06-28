package zone.hei.telecom.vola.service;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import zone.hei.telecom.vola.model.Payment;
import zone.hei.telecom.vola.repository.PaymentRepository;
import java.time.Instant;

@Service
@AllArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;

    public Payment savePayment(String studentRef, Double amount) {
        Payment payment = Payment.builder()
                .studentRef(studentRef)
                .amount(amount)
                .creationDatetime(Instant.now())
                .status("SUCCESS")
                .build();
        return paymentRepository.save(payment);
    }

    public Page<Payment> getAllPayments(String studentRef, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by("creationDatetime").descending());
        if (studentRef != null && !studentRef.isBlank()) {
            return paymentRepository.findByStudentRefContainingIgnoreCase(studentRef, pageable);
        }
        return paymentRepository.findAll(pageable);
    }
}
