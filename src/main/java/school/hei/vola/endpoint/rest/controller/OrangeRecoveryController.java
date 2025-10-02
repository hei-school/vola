package school.hei.vola.endpoint.rest.controller;

import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import school.hei.vola.service.OrangeTransactionRecoveryService;

@RestController
@RequestMapping("/api/orange")
@AllArgsConstructor
public class OrangeRecoveryController {

  private final OrangeTransactionRecoveryService recoveryService;

  /**
   * POST /api/orange/recover?date=2025-09-17
   *
   * <p>This endpoint will: - fetch transactions from Orange for the given date, - insert any
   * missing orange_transaction rows, - trigger verification for matching payments, - return a JSON
   * summary per transaction.
   */
  @PostMapping("/recover")
  public ResponseEntity<List<OrangeTransactionRecoveryService.RecoveryResult>> recover(
      @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

    List<OrangeTransactionRecoveryService.RecoveryResult> results = recoveryService.recover(date);
    return ResponseEntity.ok(results);
  }
}
