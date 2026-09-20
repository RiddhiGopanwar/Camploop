package com.camploop.exception;

import org.springframework.http.HttpStatus;

/** Generic exception carrying an HTTP status, thrown by services and translated by GlobalExceptionHandler. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
