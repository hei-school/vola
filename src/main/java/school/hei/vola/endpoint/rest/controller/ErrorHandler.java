package school.hei.vola.endpoint.rest.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ErrorHandler {

  @ExceptionHandler(Exception.class)
  public String handleException(Exception ex, Model model, HttpServletResponse response) {
    var message = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
    model.addAttribute("errorMessage", message);
    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    return "error";
  }
}
