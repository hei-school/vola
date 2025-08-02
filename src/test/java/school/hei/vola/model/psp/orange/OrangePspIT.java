package school.hei.vola.model.psp.orange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import school.hei.vola.conf.FacadeIT;
import school.hei.vola.endpoint.event.model.OrangeDailyTransactionsRetrievalRequested;
import school.hei.vola.service.event.OrangeDailyTransactionsRetrievalRequestedService;

class OrangePspIT extends FacadeIT {

  @Autowired private OrangePsp subject;

  @Autowired
  private OrangeDailyTransactionsRetrievalRequestedService
      orangeDailyTransactionsRetrievalRequestedService;

  @Test
  void unknown_pspId_leadsTo_emptyPspPayment() {
    assertTrue(subject.verify("dummy").isEmpty());
    assertTrue(subject.verify("MP250729.1116.D77954").isEmpty());
  }

  @Disabled("Flaky")
  @Test
  void known_pspId_leadsTo_PspPayment() {
    orangeDailyTransactionsRetrievalRequestedService.accept(
        new OrangeDailyTransactionsRetrievalRequested(LocalDate.of(2025, 7, 29)));

    var pspPaymentOpt = subject.verify("MP250729.1216.D77954");
    assertEquals(324_000, pspPaymentOpt.get().amount());
  }
}
