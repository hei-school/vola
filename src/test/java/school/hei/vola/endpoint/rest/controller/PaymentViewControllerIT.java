package school.hei.vola.endpoint.rest.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.test.web.servlet.MockMvc;
import school.hei.vola.repository.jpa.JApplicationRepository;
import school.hei.vola.service.PaymentService;

@WebMvcTest(
    controllers = PaymentViewController.class,
    excludeAutoConfiguration = OAuth2ClientAutoConfiguration.class)
class PaymentViewControllerIT {

  @Autowired private MockMvc mockMvc;

  @MockBean private PaymentService paymentService;

  @MockBean private JApplicationRepository jApplicationRepository;

  @BeforeEach
  void setUp() {
    when(paymentService.findFilteredPage(any(), any(), any(), any(), any()))
        .thenReturn(Page.empty());
    when(paymentService.sumAmountForSucceeded(any(), any(), any(), any())).thenReturn(0L);
    when(paymentService.countPending(any(), any(), any(), any())).thenReturn(0L);
    when(paymentService.countFiltered(any(), any(), any(), any())).thenReturn(0L);
  }

  @Test
  void payments_page_contains_logout_dialog() throws Exception {
    mockMvc
        .perform(get("/payments").with(oidcLogin()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("logout-dialog")))
        .andExpect(content().string(containsString("Confirmer la déconnexion")))
        .andExpect(content().string(containsString("Oui, me déconnecter")))
        .andExpect(content().string(containsString("Non, rester connecté")));
  }
}
