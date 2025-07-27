package school.hei.vola.endpoint.rest.security;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = UNAUTHORIZED)
public class UnauthorizedException extends RuntimeException {}
