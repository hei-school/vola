package school.hei.vola.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.annotation.DirtiesContext.MethodMode.BEFORE_METHOD;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.ui.Model;
import org.springframework.validation.support.BindingAwareModelMap;
import school.hei.vola.conf.FacadeIT;
import school.hei.vola.repository.jpa.JApplicationRepository;
import school.hei.vola.repository.jpa.model.JApplication;

class PaymentViewControllerIT extends FacadeIT {

  @Autowired private PaymentViewController subject;

  @Autowired private JApplicationRepository jApplicationRepository;

  @DirtiesContext(methodMode = BEFORE_METHOD)
  @Test
  void payments_page_returns_payments_view_with_default_filters() {
    var app = new JApplication();
    app.setName("test-app");
    app.setId(UUID.randomUUID().toString());
    app.setApiKey(UUID.randomUUID().toString());
    jApplicationRepository.save(app);

    Model model = new BindingAwareModelMap();
    String viewName = subject.paymentsPage(null, null, null, null, 0, model);

    assertEquals("payments", viewName);
    assertEquals(0, model.getAttribute("currentPage"));
    assertNotNull(model.getAttribute("applications"));
    assertNotNull(model.getAttribute("totalCollected"));
    assertNotNull(model.getAttribute("totalCount"));
    assertNotNull(model.getAttribute("pendingCount"));
    assertNotNull(model.getAttribute("payments"));
    assertNotNull(model.getAttribute("scopes"));
  }
}
