package school.hei.vola.endpoint.rest.controller;

import static org.springframework.format.annotation.DateTimeFormat.ISO;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import school.hei.vola.endpoint.rest.security.ApplicationAuthorizer;
import school.hei.vola.model.ImportedTransactionDetails;
import school.hei.vola.model.Payment;
import school.hei.vola.model.PaymentInfo;
import school.hei.vola.model.psp.PspType;
import school.hei.vola.service.OrangeSyncService;
import school.hei.vola.service.PaymentService;
import school.hei.vola.service.sync.model.RecoveryResult;

@RestController
@AllArgsConstructor
public class PaymentController {

  private final PaymentService paymentService;
  private final ApplicationAuthorizer applicationAuthorizer;
  private final OrangeSyncService recoveryService;

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

  @PutMapping("/payments/search")
  public List<Payment> getPayments(
      @RequestParam String apiKey, @RequestBody List<PaymentInfo> paymentSearch) {
    applicationAuthorizer.accept(apiKey);
    return paymentService.findPaymentsByPaymentInfos(apiKey, paymentSearch);
  }

  @PutMapping("/orange/sync")
  public RecoveryResult sync(@RequestParam("date") @DateTimeFormat(iso = ISO.DATE) LocalDate date) {
    return recoveryService.sync(date);
  }

  @PutMapping(" /orange/transactions/import")
  public ImportedTransactionDetails saveTransaction(@RequestPart MultipartFile excel)
      throws IOException {
    return paymentService.saveTransactionFromExcel(excel);
  }
}
