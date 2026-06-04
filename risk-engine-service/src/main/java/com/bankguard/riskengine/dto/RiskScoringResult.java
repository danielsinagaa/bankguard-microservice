package com.bankguard.riskengine.dto;

import java.util.List;

public record RiskScoringResult(
        int totalScore,
        String decision,
        List<RiskFactor> riskFactors
) {
    public RiskScoringResult {
        riskFactors = riskFactors == null ? List.of() : List.copyOf(riskFactors);
    }
}
