package com.insurance.vib.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Validation failed", errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
        // Unreadable bodies (e.g. an unknown enum value such as a bad policy type) are client errors
        return build(HttpStatus.BAD_REQUEST, "Malformed request body", List.of());
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleHandlerMethodValidation(HandlerMethodValidationException ex) {
        // Constraints on controller method parameters (e.g. a query param range) name the parameter
        List<String> errors = ex.getParameterValidationResults().stream()
                .map(result -> result.getMethodParameter().getParameterName() + ": "
                        + result.getResolvableErrors().stream()
                                .map(error -> error.getDefaultMessage())
                                .findFirst()
                                .orElse("invalid value"))
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Validation failed", errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        List<String> errors = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Validation failed", errors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        // Spring MVC exceptions (404, 405, ResponseStatusException, ...) carry their own status
        if (ex instanceof org.springframework.web.ErrorResponse springError) {
            HttpStatus status = HttpStatus.valueOf(springError.getStatusCode().value());
            String detail = springError.getBody().getDetail();
            return build(status, detail != null ? detail : status.getReasonPhrase(), List.of());
        }
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", List.of(String.valueOf(ex.getMessage())));
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, List<String> errors) {
        return ResponseEntity.status(status).body(new ErrorResponse(status.value(), message, errors));
    }
}
