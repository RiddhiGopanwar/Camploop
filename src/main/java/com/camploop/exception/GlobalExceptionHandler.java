package com.camploop.exception;

import com.camploop.dto.ApiMessageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiMessageResponse> handleApiException(ApiException ex) {
        return ResponseEntity.status(ex.getStatus()).body(new ApiMessageResponse(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiMessageResponse> handleValidation(MethodArgumentNotValidException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "Invalid request";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiMessageResponse(message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiMessageResponse> handleGeneral(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiMessageResponse("Something went wrong: " + ex.getMessage()));
    }
}
