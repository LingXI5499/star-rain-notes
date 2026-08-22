package com.starrainnotes.common.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Map;

/**
 * Global MVC exception translation to RFC 9457 Problem Details.
 *
 * <p>Security filter-chain failures (401/403/CSRF) are handled separately in
 * {@code common.security} so they produce the same format. Client-facing
 * responses never include stack traces, SQL messages or password material.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ProblemDetail handleApiException(ApiException ex, HttpServletRequest request) {
        return ApiProblem.create(ex.getStatus(), ex.getCode(), ex.getTitle(), ex.getMessage(), request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        return ApiProblem.create(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
                "Invalid username or password",
                "The provided credentials could not be authenticated.", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<Map<String, String>> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(this::violation)
                .toList();
        ProblemDetail problem = ApiProblem.create(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                "Request validation failed", "One or more fields are invalid.", request);
        problem.setProperty("violations", violations);
        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        return ApiProblem.create(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                "Request validation failed", ex.getMessage(), request);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            MissingRequestHeaderException.class
    })
    public ProblemDetail handleMalformed(Exception ex, HttpServletRequest request) {
        return ApiProblem.create(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST",
                "Malformed request", "The request could not be understood.", request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNotFound(NoResourceFoundException ex, HttpServletRequest request) {
        return ApiProblem.create(HttpStatus.NOT_FOUND, "NOT_FOUND",
                "Resource not found", "The requested resource does not exist.", request);
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ProblemDetail handleMethodNotAllowed(org.springframework.web.HttpRequestMethodNotSupportedException ex,
                                                HttpServletRequest request) {
        return ApiProblem.create(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED",
                "Method not allowed", "The HTTP method is not supported for this resource.", request);
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ProblemDetail handleUploadTooLarge(org.springframework.web.multipart.MaxUploadSizeExceededException ex,
                                              HttpServletRequest request) {
        return ApiProblem.create(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE",
                "File too large", "The uploaded file exceeds the maximum allowed size.", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataConflict(DataIntegrityViolationException ex, HttpServletRequest request) {
        return ApiProblem.create(HttpStatus.CONFLICT, "DATA_CONFLICT",
                "Data conflict", "The request conflicts with the current data state.", request);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return ApiProblem.create(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Internal server error", "An unexpected error occurred.", request);
    }

    private Map<String, String> violation(FieldError error) {
        return Map.of(
                "field", error.getField(),
                "message", error.getDefaultMessage() == null ? "invalid value" : error.getDefaultMessage());
    }
}
