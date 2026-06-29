package school.hei.vola.endpoint.rest.controller;

import java.time.Instant;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import school.hei.vola.repository.jpa.JApplicationRepository;
import school.hei.vola.service.PaymentService;
import school.hei.vola.service.utils.DateParser;

@Controller
@RequiredArgsConstructor
public class PaymentViewController {

  private final PaymentService paymentService;
  private final JApplicationRepository jApplicationRepository;

  @GetMapping("/")
  public String index() {
    return "redirect:/payments";
  }

  private static final int PAGE_SIZE = 15;

  @GetMapping("/payments")
  public String paymentsPage(
      @RequestParam(required = false) String applicationName,
      @RequestParam(required = false) String scope,
      @RequestParam(required = false) String startDate,
      @RequestParam(required = false) String endDate,
      @RequestParam(defaultValue = "0") int page,
      Model model) {
    model.addAttribute("applications", jApplicationRepository.findAll());

    var effectiveApp =
        (applicationName == null || applicationName.isBlank() || "all".equals(applicationName))
            ? null
            : applicationName;
    var effectiveScope = (scope == null || scope.isBlank() || "all".equals(scope)) ? null : scope;

    var parsedStartDate = DateParser.parseDate(startDate);
    var parsedEndDate = DateParser.parseDate(endDate);

    var start =
        parsedStartDate != null
            ? parsedStartDate.atStartOfDay(ZoneOffset.UTC).toInstant()
            : Instant.EPOCH;
    var end =
        parsedEndDate != null
            ? parsedEndDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()
            : Instant.parse("9999-12-31T23:59:59Z");

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
            PageRequest.of(page, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "creationInstant")));

    model.addAttribute("payments", paymentsPage.getContent());
    model.addAttribute("totalCollected", String.format("%,d Ar", totalAmount));
    model.addAttribute("pendingCount", pendingCount);
    model.addAttribute("totalCount", totalCount);
    model.addAttribute("currentPage", page);
    model.addAttribute("totalPages", paymentsPage.getTotalPages());
    model.addAttribute("pageSize", PAGE_SIZE);
    model.addAttribute("selectedApplication", effectiveApp);
    model.addAttribute("selectedScope", effectiveScope);
    model.addAttribute("selectedStartDate", parsedStartDate);
    model.addAttribute("selectedEndDate", parsedEndDate);
    return "payments";
  }
}
