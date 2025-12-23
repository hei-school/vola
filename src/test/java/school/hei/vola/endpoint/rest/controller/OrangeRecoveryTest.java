package school.hei.vola.endpoint.rest.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import school.hei.vola.endpoint.rest.security.ApplicationAuthorizer;
import school.hei.vola.service.OrangeSyncService;
import school.hei.vola.service.PaymentService;
import school.hei.vola.service.sync.model.RecoveryResult;

@WebMvcTest(controllers = PaymentController.class)
class OrangeRecoveryTest {

  @Autowired private MockMvc mockMvc;
  @MockBean private OrangeSyncService recoveryService;
  @MockBean private PaymentService paymentService;
  @MockBean private ApplicationAuthorizer applicationAuthorizer;

  @Test
  void putSync_success() throws Exception {
    LocalDate date = LocalDate.of(2025, 9, 17);
    when(recoveryService.sync(date))
        .thenReturn(RecoveryResult.builder().date(date).isSuccessful(true).inserted(2).build());

    mockMvc
        .perform(put("/orange/sync").param("date", "2025-09-17"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.successful").value(true))
        .andExpect(jsonPath("$.inserted").value(2));

    verify(recoveryService).sync(date);
  }
}