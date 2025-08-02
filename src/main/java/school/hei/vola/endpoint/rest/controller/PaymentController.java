package school.hei.vola.endpoint.rest.controller;

import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import school.hei.vola.endpoint.Authorizer;
import school.hei.vola.model.Payment;
import school.hei.vola.model.psp.PspType;
import school.hei.vola.service.PaymentService;

@RestController
@AllArgsConstructor
public class PaymentController {

  private final PaymentService paymentService;
  private final Authorizer authorizer;

  @PostMapping("/payment")
  public Payment createPayment(
      String apiKey, String payerEmail, PspType pspType, String pspPaymentId) {
    authorizer.accept(apiKey);
    return paymentService.createPayment(payerEmail, pspType, pspPaymentId);
  }

  @GetMapping("/payment")
  public Optional<Payment> getPayment(
      @RequestParam String apiKey,
      @RequestParam String payerEmail,
      @RequestParam PspType pspType,
      @RequestParam String pspPaymentId) {
    authorizer.accept(apiKey);
    return paymentService.findPaymentByPayerEmailAndPspTypeAndPspPaymentId(
        payerEmail, pspType, pspPaymentId);
  }
}
