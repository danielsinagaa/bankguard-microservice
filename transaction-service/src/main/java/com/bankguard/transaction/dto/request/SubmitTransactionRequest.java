package com.bankguard.transaction.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record SubmitTransactionRequest(
        @NotBlank(message = "Source account number is required")
        String sourceAccountNumber,

        @NotBlank(message = "Destination account number is required")
        String destinationAccountNumber,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        BigDecimal amount,

        @NotBlank(message = "Currency is required")
        String currency,

        @NotBlank(message = "Channel is required")
        String channel,

        @Size(max = 100, message = "Device ID must not exceed 100 characters")
        String deviceId,

        @Size(max = 50, message = "IP address must not exceed 50 characters")
        String ipAddress,

        @Size(max = 100, message = "Location must not exceed 100 characters")
        String location
) {
}
