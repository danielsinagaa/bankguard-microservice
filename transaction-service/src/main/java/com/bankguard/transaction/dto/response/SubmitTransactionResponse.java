package com.bankguard.transaction.dto.response;

public record SubmitTransactionResponse(
        String transactionRef,
        String status,
        String message
) {
}
