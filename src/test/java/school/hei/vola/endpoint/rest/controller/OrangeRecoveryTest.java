package school.hei.vola.endpoint.rest.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import school.hei.vola.service.OrangeTransactionRecoveryService;
import school.hei.vola.service.sync.model.RecoveryResult;

@WebMvcTest(controllers = PaymentController.class)
class OrangeRecoveryTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private OrangeTransactionRecoveryService recoveryService;

  @Test
  void putSync_callsService_andReturnsJson() throws Exception {
    LocalDate date = LocalDate.of(2025, 9, 17);

    RecoveryResult successResult =
        RecoveryResult.builder()
            .date(date)
            .isSuccessful(true)
            .inserted(2)
            .errorMessage(null)
            .build();

    when(recoveryService.sync(date)).thenReturn(successResult);

    mockMvc
        .perform(
            put("/orange/sync")
                .param("date", date.toString())
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.date").value("2025-09-17"))
        .andExpect(jsonPath("$.successful").value(true))
        .andExpect(jsonPath("$.inserted").value(2))
        .andExpect(jsonPath("$.errorMessage").isEmpty());

    ArgumentCaptor<LocalDate> captor = ArgumentCaptor.forClass(LocalDate.class);
    verify(recoveryService).sync(captor.capture());
    assert captor.getValue().equals(date);
  }

  @Test
  void putSync_whenServiceFails_returnsFailureResult() throws Exception {
    LocalDate date = LocalDate.of(2025, 9, 18);

    RecoveryResult failureResult =
        RecoveryResult.builder()
            .date(date)
            .isSuccessful(false)
            .inserted(0)
            .errorMessage("Orange API connection failed")
            .build();

    when(recoveryService.sync(date)).thenReturn(failureResult);

    mockMvc
        .perform(
            put("/orange/sync")
                .param("date", date.toString())
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.date").value("2025-09-18"))
        .andExpect(jsonPath("$.successful").value(false))
        .andExpect(jsonPath("$.inserted").value(0))
        .andExpect(jsonPath("$.errorMessage").value("Orange API connection failed"));

    verify(recoveryService).sync(date);
  }

  @Test
  void putSync_withPartialSuccess_returnsCorrectMetrics() throws Exception {
    LocalDate date = LocalDate.of(2025, 9, 19);

    RecoveryResult partialResult =
        RecoveryResult.builder()
            .date(date)
            .isSuccessful(true)
            .inserted(5)
            .errorMessage(null)
            .build();

    when(recoveryService.sync(date)).thenReturn(partialResult);

    mockMvc
        .perform(
            put("/orange/sync")
                .param("date", date.toString())
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.successful").value(true))
        .andExpect(jsonPath("$.inserted").value(5));

    verify(recoveryService).sync(date);
  }
}
