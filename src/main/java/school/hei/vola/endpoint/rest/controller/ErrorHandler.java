package school.hei.vola.endpoint.rest.controller;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ErrorHandler {

  @ExceptionHandler(Exception.class)
  public String handleException(Exception ex, Model model) {
    model.addAttribute("errorMessage", ex.getMessage());
    return "error";
  }
}
