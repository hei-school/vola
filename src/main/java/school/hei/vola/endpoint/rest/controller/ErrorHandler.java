package school.hei.vola.endpoint.rest.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import school.hei.vola.endpoint.rest.security.UnauthorizedException;

@ControllerAdvice
public class ErrorHandler {

  @ExceptionHandler(NotFoundException.class)
  public String handleNotFoundException(Model model, HttpServletResponse response) {
    model.addAttribute("status", HttpServletResponse.SC_NOT_FOUND);
    model.addAttribute("error", "Not Found");
    model.addAttribute("errorMessage", "La ressource demandée est introuvable");
    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    return "error";
  }

  @ExceptionHandler(UnauthorizedException.class)
  public String handleUnauthorizedException(Model model, HttpServletResponse response) {
    model.addAttribute("status", HttpServletResponse.SC_UNAUTHORIZED);
    model.addAttribute("error", "Unauthorized");
    model.addAttribute("errorMessage", "Accès non autorisé");
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    return "error";
  }

  @ExceptionHandler(Exception.class)
  public String handleException(Exception ex, Model model, HttpServletResponse response) {
    var message = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
    model.addAttribute("errorMessage", message);
    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    return "error";
  }
}
