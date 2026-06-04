package com.bankguard.riskengine.service;

import com.bankguard.common.constant.RiskDecision;
import com.bankguard.riskengine.dto.RiskFactor;
import com.bankguard.riskengine.dto.RiskScoringResult;
import com.bankguard.riskengine.dto.TransactionContext;
import com.bankguard.riskengine.rule.RiskRule;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RiskScoringService {
    private final List<RiskRule> riskRules;

    public RiskScoringService(List<RiskRule> riskRules) {
        this.riskRules = riskRules;
    }

    public RiskScoringResult score(TransactionContext context) {
        List<RiskFactor> triggeredFactors = riskRules.stream()
                .map(rule -> rule.evaluate(context))
                .filter(RiskFactor::triggered)
                .sorted(Comparator.comparing(RiskFactor::code))
                .toList();
        int totalScore = triggeredFactors.stream().mapToInt(RiskFactor::score).sum();
        return new RiskScoringResult(totalScore, decision(totalScore), triggeredFactors);
    }

    public String decision(int totalScore) {
        if (totalScore < 50) {
            return RiskDecision.APPROVED.name();
        }
        if (totalScore < 80) {
            return RiskDecision.REVIEW.name();
        }
        return RiskDecision.BLOCKED.name();
    }
}
