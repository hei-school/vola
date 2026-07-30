package school.hei.vola.endpoint.rest.controller;

import static java.time.ZoneOffset.UTC;

import java.time.Instant;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import school.hei.vola.repository.jpa.JApplicationRepository;
import school.hei.vola.service.PaymentService;

@Controller
public class PaymentViewController {

  private final PaymentService paymentService;
  private final JApplicationRepository jApplicationRepository;
  private final String adminKey;

  public PaymentViewController(
      PaymentService paymentService,
      JApplicationRepository jApplicationRepository,
      @Value("${ADMIN_API_KEY}") String adminKey) {
    this.paymentService = paymentService;
    this.jApplicationRepository = jApplicationRepository;
    this.adminKey = adminKey;
  }

  @GetMapping("/")
  public String index() {
    return "redirect:/payments";
  }

  @GetMapping("/payments")
  public String paymentsPage(
      @RequestParam(required = false) String applicationName,
      @RequestParam(required = false) String scope,
      @RequestParam(required = false) String startDate,
      @RequestParam(required = false) String endDate,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "15") int size,
      Model model) {
    model.addAttribute("applications", jApplicationRepository.findAll());

    var effectiveApp = normalizeFilter(applicationName);
    var effectiveScope = normalizeFilter(scope);
    var parsedStartDate = parseDate(startDate);
    var parsedEndDate = parseDate(endDate);

    var start =
        parsedStartDate != null ? parsedStartDate.atStartOfDay(UTC).toInstant() : Instant.EPOCH;
    var end =
        parsedEndDate != null
            ? parsedEndDate.plusDays(1).atStartOfDay(UTC).toInstant()
            : Instant.now();

    var totalAmount =
        paymentService.sumAmountForSucceeded(effectiveApp, effectiveScope, start, end);
    var pendingCount = paymentService.countPending(effectiveApp, effectiveScope, start, end);
    var totalCount = paymentService.countFiltered(effectiveApp, effectiveScope, start, end);

    var paymentsPage =
        paymentService.findFilteredPage(
            effectiveApp,
            effectiveScope,
            start,
            end,
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "creationInstant")));

    model.addAttribute("payments", paymentsPage.getContent());
    model.addAttribute("totalCollected", String.format("%,d Ar", totalAmount));
    model.addAttribute("pendingCount", pendingCount);
    model.addAttribute("totalCount", totalCount);
    model.addAttribute("currentPage", page);
    model.addAttribute("totalPages", paymentsPage.getTotalPages());
    model.addAttribute("pageSize", size);
    model.addAttribute("scopes", paymentService.findDistinctScopes(effectiveApp));
    model.addAttribute("selectedApplication", effectiveApp);
    model.addAttribute("selectedScope", effectiveScope);
    model.addAttribute("selectedStartDate", parsedStartDate);
    model.addAttribute("selectedEndDate", parsedEndDate);
    model.addAttribute("adminKey", adminKey);
    return "payments";
  }

  private static String normalizeFilter(String value) {
    return (value == null || value.isBlank() || "all".equals(value)) ? null : value;
  }

  private static LocalDate parseDate(String dateStr) {
    return (dateStr == null || dateStr.isBlank()) ? null : LocalDate.parse(dateStr);
  }
}
