package com.bankguard.common.event;

import java.util.Map;

public record RiskFactorPayload(
        String code,
        String description,
        int score,
        Map<String, Object> metadata
) {
    public RiskFactorPayload {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
