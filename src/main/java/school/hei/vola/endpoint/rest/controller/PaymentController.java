package school.hei.vola.endpoint.rest.controller;

import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import school.hei.vola.endpoint.rest.security.ApplicationAuthorizer;
import school.hei.vola.model.Payment;
import school.hei.vola.model.PaymentInfo;
import school.hei.vola.model.psp.PspType;
import school.hei.vola.service.PaymentService;

@RestController
@AllArgsConstructor
public class PaymentController {

  private final PaymentService paymentService;
  private final ApplicationAuthorizer applicationAuthorizer;

  @PostMapping("/payment")
  public Payment createPayment(
      String apiKey, String payerEmail, PspType pspType, String pspPaymentId) {
    applicationAuthorizer.accept(apiKey);
    return paymentService.createPayment(apiKey, payerEmail, pspType, pspPaymentId);
  }

  @GetMapping("/payment")
  public Payment getPayment(
      @RequestParam String apiKey,
      @RequestParam String payerEmail,
      @RequestParam PspType pspType,
      @RequestParam String pspPaymentId) {
    applicationAuthorizer.accept(apiKey);
    return paymentService
        .findPaymentByPayerEmailAndPspTypeAndPspPaymentId(payerEmail, pspType, pspPaymentId)
        .orElseThrow(NotFoundException::new);
  }

  @GetMapping("/payments")
  public List<Payment> getPayments(
          @RequestParam String apiKey, @RequestBody List<PaymentInfo> paymentInfos) {
    applicationAuthorizer.accept(apiKey);
    return paymentService.findPaymentsByPaymentInfos(paymentInfos);
  }
}
