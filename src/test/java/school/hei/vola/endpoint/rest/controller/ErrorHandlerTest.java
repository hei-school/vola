package school.hei.vola.endpoint.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ui.Model;

class ErrorHandlerTest {

  private ErrorHandler errorHandler;
  private Model model;
  private HttpServletResponse response;

  @BeforeEach
  void setUp() {
    errorHandler = new ErrorHandler();
    model = mock(Model.class);
    response = mock(HttpServletResponse.class);
  }

  @Test
  void handleException_setsStatus500_andErrorMessage() {
    var exception = new RuntimeException("Test error");

    var result = errorHandler.handleException(exception, model, response);

    assertEquals("error", result);
    verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    verify(model).addAttribute("errorMessage", "Test error");
  }

  @Test
  void handleException_withNullMessage_usesClassName() {
    var exception = new RuntimeException((String) null);

    var result = errorHandler.handleException(exception, model, response);

    assertEquals("error", result);
    verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    verify(model).addAttribute("errorMessage", "RuntimeException");
  }

  @Test
  void handleNotFoundException_setsStatus404() {
    var result = errorHandler.handleNotFoundException(model, response);

    assertEquals("error", result);
    verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
    verify(model).addAttribute("status", HttpServletResponse.SC_NOT_FOUND);
    verify(model).addAttribute("error", "Not Found");
    verify(model).addAttribute("errorMessage", "La ressource demandée est introuvable");
  }

  @Test
  void handleUnauthorizedException_setsStatus401() {
    var result = errorHandler.handleUnauthorizedException(model, response);

    assertEquals("error", result);
    verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    verify(model).addAttribute("status", HttpServletResponse.SC_UNAUTHORIZED);
    verify(model).addAttribute("error", "Unauthorized");
    verify(model).addAttribute("errorMessage", "Accès non autorisé");
  }
}
