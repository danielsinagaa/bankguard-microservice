package com.bankguard.common.exception;

import com.bankguard.common.constant.ErrorCode;

public class InvalidKafkaEventException extends ApiException {
    public InvalidKafkaEventException(String message) {
        super(400, ErrorCode.VALIDATION_ERROR, message);
    }
}
