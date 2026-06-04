package com.bankguard.transaction.dto.response;

import java.util.Map;

public record RiskFactorResponse(
        String code,
        String description,
        int score,
        Map<String, Object> metadata
) {
    public RiskFactorResponse {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
