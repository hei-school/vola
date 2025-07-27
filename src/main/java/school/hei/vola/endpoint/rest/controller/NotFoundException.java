package school.hei.vola.endpoint.rest.controller;

import static org.springframework.http.HttpStatus.NOT_FOUND;

import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = NOT_FOUND)
public class NotFoundException extends RuntimeException {}
