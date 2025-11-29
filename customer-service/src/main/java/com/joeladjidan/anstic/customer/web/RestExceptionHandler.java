package com.joeladjidan.anstic.customer.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;
import java.util.Collections;
import java.util.stream.Collectors;

@ControllerAdvice
public class RestExceptionHandler {

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<Map<String,String>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
    String msg = "Request body is missing or malformed";
    if (ex.getCause() != null) {
      msg += ": " + ex.getCause().getMessage();
    } else if (ex.getMessage() != null) {
      msg += ": " + ex.getMessage();
    }
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", msg));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String,String>> handleValidation(MethodArgumentNotValidException ex) {
    String errors = ex.getBindingResult().getFieldErrors().stream()
        .map(fe -> fe.getField() + " " + fe.getDefaultMessage())
        .collect(Collectors.joining(", "));
    if (errors == null || errors.isBlank()) errors = "Validation failed";
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", errors));
  }
}
