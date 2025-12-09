package school.hei.vola.endpoint.rest.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import school.hei.vola.service.OrangeTransactionRecoveryService;

@WebMvcTest(controllers = OrangeRecoveryController.class)
class OrangeRecoveryControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private OrangeTransactionRecoveryService recoveryService;

  @Test
  void postRecover_callsService_andReturnsJson() throws Exception {
    LocalDate date = LocalDate.of(2025, 9, 17);

    OrangeTransactionRecoveryService.RecoveryResult r1 =
        new OrangeTransactionRecoveryService.RecoveryResult(
            "MP250917.1604.D33118", true, true, "payment-1", 324000);
    OrangeTransactionRecoveryService.RecoveryResult r2 =
        new OrangeTransactionRecoveryService.RecoveryResult(
            "MP250917.1605.XYZ", false, false, null, 12000);

    when(recoveryService.recover(date)).thenReturn(List.of(r1, r2));

    mockMvc
        .perform(
            post("/api/orange/recover")
                .param("date", date.toString())
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$[0].ref").value("MP250917.1604.D33118"))
        .andExpect(jsonPath("$[0].inserted").value(true))
        .andExpect(jsonPath("$[0].paymentFound").value(true))
        .andExpect(jsonPath("$[1].ref").value("MP250917.1605.XYZ"))
        .andExpect(jsonPath("$[1].inserted").value(false));

    ArgumentCaptor<LocalDate> captor = ArgumentCaptor.forClass(LocalDate.class);
    verify(recoveryService).recover(captor.capture());
    assert captor.getValue().equals(date);
  }
}
