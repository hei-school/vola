package school.hei.vola.endpoint.rest.controller;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import school.hei.vola.service.OrangeTransactionRecoveryService;
import school.hei.vola.service.sync.model.RecoveryResult;

@RestController
@RequestMapping("/orange")
@AllArgsConstructor
public class OrangeRecoveryController {

  private final OrangeTransactionRecoveryService recoveryService;

  /**
   * PUT /orange/sync?date=2025-12-17
   *
   * <p>This endpoint will: - fetch transactions from Orange for the given date, - insert any
   * missing orange_transaction rows, - trigger verification for matching payments, - return a JSON
   * summary per transaction.
   */
  @PutMapping("/sync")
  public RecoveryResult sync(
      @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

    return recoveryService.sync(date);
  }
}
