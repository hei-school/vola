package school.hei.vola.service;

import jakarta.transaction.Transactional;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import school.hei.vola.endpoint.event.EventProducer;
import school.hei.vola.endpoint.event.model.PaymentVerificationRequested;
import school.hei.vola.model.Payment;
import school.hei.vola.model.User;
import school.hei.vola.model.psp.PspType;
import school.hei.vola.repository.PaymentRepository;

@Service
@AllArgsConstructor
@Slf4j
public class PaymentService {

  private final PaymentRepository paymentRepository;
  private final EventProducer eventProducer;

  @Transactional
  public Payment createPayment(User payer, PspType pspType, String pspPaymentId) {
    var payment = paymentRepository.createPayment(payer, pspType, pspPaymentId);

    eventProducer.accept(List.of(new PaymentVerificationRequested(payment)));
    log.info("PaymentVerificationRequested event sent for payment={}", payment);

    return payment;
  }

  public Payment findPaymentById(String id) {
    return paymentRepository.findPaymentById(id);
  }
}
