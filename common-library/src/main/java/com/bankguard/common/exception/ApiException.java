package com.bankguard.common.exception;

import com.bankguard.common.api.ErrorDetail;
import com.bankguard.common.constant.ErrorCode;
import java.util.List;

public class ApiException extends RuntimeException {
    private final int status;
    private final ErrorCode errorCode;
    private final List<ErrorDetail> details;

    public ApiException(int status, ErrorCode errorCode, String message) {
        this(status, errorCode, message, List.of());
    }

    public ApiException(int status, ErrorCode errorCode, String message, List<ErrorDetail> details) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public int getStatus() {
        return status;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public List<ErrorDetail> getDetails() {
        return details;
    }
}
