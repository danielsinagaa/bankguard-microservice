package com.bankguard.riskengine.dto;

import java.util.Map;

public record RiskFactor(
        String code,
        String description,
        int score,
        Map<String, Object> metadata,
        boolean triggered
) {
    public RiskFactor {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public static RiskFactor triggered(String code, String description, int score, Map<String, Object> metadata) {
        return new RiskFactor(code, description, score, metadata, true);
    }

    public static RiskFactor notTriggered(String code) {
        return new RiskFactor(code, "", 0, Map.of(), false);
    }
}
