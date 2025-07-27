package school.hei.vola.service;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import school.hei.vola.model.Payment;
import school.hei.vola.model.Psp;
import school.hei.vola.model.User;
import school.hei.vola.repository.PaymentRepository;

@Service
@AllArgsConstructor
public class PaymentService {

  private final PaymentRepository paymentRepository;

  @Transactional
  public Payment createPayment(User payer, Psp psp, String pspPaymentId) {
    var payment = paymentRepository.createPayment(payer, psp, pspPaymentId);
    return payment;
  }
}
