package com.starrainnotes.common.error;

import org.springframework.http.HttpStatus;

/**
 * Business rule failure carrying an RFC 9457 Problem Details mapping.
 * Thrown by services for frozen business rules; translated by
 * {@link GlobalExceptionHandler} into {@code application/problem+json}.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final String title;

    public ApiException(HttpStatus status, String code, String title, String detail) {
        super(detail);
        this.status = status;
        this.code = code;
        this.title = title;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }
}
