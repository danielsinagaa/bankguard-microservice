package com.bankguard.transaction.service;

import com.bankguard.transaction.dto.response.SubmitTransactionResponse;

public record SubmitTransactionResult(
        SubmitTransactionResponse response,
        boolean created
) {
}
