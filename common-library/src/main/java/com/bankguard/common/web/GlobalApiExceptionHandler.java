package com.bankguard.common.web;

import com.bankguard.common.api.CommonErrorResponse;
import com.bankguard.common.api.ErrorDetail;
import com.bankguard.common.constant.ErrorCode;
import com.bankguard.common.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<CommonErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ErrorDetail(error.getField(), error.getDefaultMessage()))
                .toList();
        return error(request, HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, "Request validation failed", details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<CommonErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        List<ErrorDetail> details = ex.getConstraintViolations().stream()
                .map(violation -> new ErrorDetail(violation.getPropertyPath().toString(), violation.getMessage()))
                .toList();
        return error(request, HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, "Request validation failed", details);
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<CommonErrorResponse> handleApiException(ApiException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatus());
        return error(request, status, ex.getErrorCode(), ex.getMessage(), ex.getDetails());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<CommonErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        return error(request, HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_SERVER_ERROR, "Unexpected server error", List.of());
    }

    private static ResponseEntity<CommonErrorResponse> error(
            HttpServletRequest request,
            HttpStatus status,
            ErrorCode errorCode,
            String message,
            List<ErrorDetail> details
    ) {
        return ResponseEntity.status(status)
                .body(CommonErrorResponse.of(
                        status.value(),
                        errorCode.name(),
                        message,
                        details,
                        request.getRequestURI(),
                        RequestIdFilter.requestId(request)
                ));
    }
}
