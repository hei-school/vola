package school.hei.vola.endpoint.rest.controller;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import school.hei.vola.endpoint.Authorizer;
import school.hei.vola.model.Payment;
import school.hei.vola.model.User;
import school.hei.vola.model.psp.PspType;
import school.hei.vola.service.PaymentService;

@RestController
@AllArgsConstructor
public class PaymentController {

  private final PaymentService paymentService;
  private final Authorizer authorizer;

  @PostMapping("/payment")
  public Payment createPayment(String apiKey, User user, PspType pspType, String pspId) {
    authorizer.accept(apiKey);
    return paymentService.createPayment(user, pspType, pspId);
  }

  @GetMapping("/payment/{id}")
  public Payment getPayment(@RequestParam String apiKey, @PathVariable String id) {
    authorizer.accept(apiKey);
    return paymentService.findPaymentById(id);
  }
}
