package school.hei.vola.endpoint.rest.controller;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import school.hei.vola.model.VerificationStatus;
import school.hei.vola.repository.jpa.JApplicationRepository;
import school.hei.vola.service.PaymentService;

@Controller
@RequiredArgsConstructor
public class PaymentViewController {

  private final PaymentService paymentService;
  private final JApplicationRepository jApplicationRepository;

  @GetMapping("/")
  public String index() {
    return "redirect:/payments";
  }

  @GetMapping("/payments")
  public String paymentsPage(
      @RequestParam(required = false) String applicationName,
      @RequestParam(required = false) String startDate,
      @RequestParam(required = false) String endDate,
      Model model) {
    model.addAttribute("applications", jApplicationRepository.findAll());

    var parsedStartDate = parseDate(startDate);
    var parsedEndDate = parseDate(endDate);

    var start =
        parsedStartDate != null
            ? parsedStartDate.atStartOfDay(ZoneOffset.UTC).toInstant()
            : Instant.EPOCH;
    var end =
        parsedEndDate != null
            ? parsedEndDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()
            : Instant.parse("9999-12-31T23:59:59Z");

    var effectiveApp = "all".equals(applicationName) ? null : applicationName;
    var payments =
        paymentService.findPaymentsByApplicationNameAndDateRange(effectiveApp, start, end);

    var totalAmount =
        payments.stream()
            .filter(p -> p.getVerificationStatus() == VerificationStatus.SUCCEEDED)
            .mapToLong(p -> p.pspPayment().amount() != null ? p.pspPayment().amount() : 0)
            .sum();

    model.addAttribute("payments", payments);
    model.addAttribute("totalCollected", String.format("%,d Ar", totalAmount));
    model.addAttribute(
        "pendingCount",
        payments.stream()
            .filter(p -> p.getVerificationStatus() == VerificationStatus.VERIFYING)
            .count());
    model.addAttribute("selectedApplication", applicationName);
    model.addAttribute("selectedStartDate", parsedStartDate);
    model.addAttribute("selectedEndDate", parsedEndDate);
    return "payments";
  }

  private static LocalDate parseDate(String dateStr) {
    if (dateStr == null || dateStr.isBlank()) {
      return null;
    }
    return LocalDate.parse(dateStr);
  }

}
