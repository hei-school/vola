package school.hei.vola.model.psp.orange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.annotation.DirtiesContext.MethodMode.BEFORE_METHOD;
import static school.hei.vola.conf.TestData.ORANGE_REF_SUCCEEDED;

import java.time.LocalDate;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import school.hei.vola.conf.FacadeIT;
import school.hei.vola.endpoint.event.model.OrangeDailyTransactionsRetrievalRequested;
import school.hei.vola.service.event.OrangeDailyTransactionsRetrievalRequestedService;

class OrangePspIT extends FacadeIT {

  @Autowired private OrangePsp subject;

  @Autowired
  private OrangeDailyTransactionsRetrievalRequestedService
      orangeDailyTransactionsRetrievalRequestedService;

  @Test
  void unknown_pspPaymentId_leadsTo_emptyPspPayment() {
    assertTrue(subject.verify("dummy").isEmpty());
    assertTrue(subject.verify("MP250729.1116.D77954").isEmpty());
  }

  @DirtiesContext(
      // note(unique_pspPayment): due SQL unicity constraint
      methodMode = BEFORE_METHOD) // due to unique_pspPayment SQL constraint
  @Disabled("Flaky")
  @Test
  void known_pspPaymentId_leadsTo_PspPayment() {
    orangeDailyTransactionsRetrievalRequestedService.accept(
        new OrangeDailyTransactionsRetrievalRequested(LocalDate.of(2025, 7, 29)));

    var pspPaymentOpt = subject.verify(ORANGE_REF_SUCCEEDED);
    assertEquals(324_000, pspPaymentOpt.get().amount());
  }
}
