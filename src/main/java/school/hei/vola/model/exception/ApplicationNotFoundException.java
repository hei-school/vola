package school.hei.vola.model.exception;

public class ApplicationNotFoundException extends RuntimeException {
  public ApplicationNotFoundException(String message) {
    super(message);
  }
}
