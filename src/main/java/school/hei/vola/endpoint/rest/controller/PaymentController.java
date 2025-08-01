package school.hei.vola.endpoint.rest.controller;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import school.hei.vola.model.Payment;
import school.hei.vola.model.User;
import school.hei.vola.model.psp.PspType;
import school.hei.vola.service.PaymentService;

@RestController
@AllArgsConstructor
public class PaymentController {

  private final PaymentService paymentService;

  @PostMapping("/payment")
  public Payment createPayment(User user, PspType pspType, String pspId) {
    return paymentService.createPayment(user, pspType, pspId);
  }
}
