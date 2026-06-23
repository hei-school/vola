package school.hei.vola.endpoint.rest.controller;

import static org.springframework.format.annotation.DateTimeFormat.ISO;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import school.hei.vola.endpoint.rest.security.ApplicationAuthorizer;
import school.hei.vola.model.ImportedTransactionDetails;
import school.hei.vola.model.Payment;
import school.hei.vola.model.PaymentInfo;
import school.hei.vola.model.psp.PspType;
import school.hei.vola.service.MultipartFileConverter;
import school.hei.vola.service.OrangeSyncService;
import school.hei.vola.service.PaymentService;
import school.hei.vola.service.sync.model.RecoveryResult;
import java.io.IOException;

@RestController
@AllArgsConstructor
public class PaymentController {

  private final PaymentService paymentService;
  private final ApplicationAuthorizer applicationAuthorizer;
  private final OrangeSyncService recoveryService;
  private final MultipartFileConverter multipartFileConverter;

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
  public List<Payment> getPaymentsByApplication(
      @RequestParam String applicationName, @RequestParam(required = false) String apiKey) {
    if (apiKey != null) {
      applicationAuthorizer.accept(apiKey);
    }
    return paymentService.findPaymentsByApplicationName(applicationName);
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

  @PostMapping(value = "/orange/transactions/import", consumes = MULTIPART_FORM_DATA_VALUE)
  public ImportedTransactionDetails saveTransactions(
      @RequestPart MultipartFile excel, @RequestParam String apiKey) throws IOException {
    applicationAuthorizer.accept(apiKey);
    return paymentService.saveTransactionFromExcel(multipartFileConverter.apply(excel));
  }
}
