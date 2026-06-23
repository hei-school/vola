package school.hei.vola.endpoint.rest.controller;

import static org.springframework.format.annotation.DateTimeFormat.ISO;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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
      @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate endDate,
      Model model) {
    if (applicationName == null || applicationName.isBlank()) {
      model.addAttribute("applications", jApplicationRepository.findAll());
      return "payments";
    }

    Instant start = startDate != null ? startDate.atStartOfDay(ZoneOffset.UTC).toInstant() : Instant.EPOCH;
    Instant end = endDate != null
        ? endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()
        : Instant.parse("9999-12-31T23:59:59Z");

    var payments = paymentService.findPaymentsByApplicationNameAndDateRange(applicationName, start, end);

    long totalAmount = payments.stream()
        .filter(p -> p.getVerificationStatus() == VerificationStatus.SUCCEEDED)
        .mapToLong(p -> p.pspPayment().amount() != null ? p.pspPayment().amount() : 0)
        .sum();

    long pendingCount = payments.stream()
        .filter(p -> p.getVerificationStatus() == VerificationStatus.VERIFYING)
        .count();

    model.addAttribute("payments", payments);
    model.addAttribute("totalCollected", String.format("%,d Ar", totalAmount));
    model.addAttribute("pendingCount", pendingCount);
    model.addAttribute("applications", jApplicationRepository.findAll());
    model.addAttribute("selectedApplication", applicationName);
    model.addAttribute("selectedStartDate", startDate);
    model.addAttribute("selectedEndDate", endDate);
    return "payments";
  }
}
