package com.bankguard.riskengine.rule;

import com.bankguard.riskengine.dto.RiskFactor;
import com.bankguard.riskengine.dto.RuleConfig;
import com.bankguard.riskengine.dto.TransactionContext;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class HighFrequencyTransactionRule extends AbstractRiskRule {
    static final String COUNT_RULE_CODE = "HIGH_FREQUENCY_TRANSACTION_COUNT";
    static final String AMOUNT_RULE_CODE = "HIGH_FREQUENCY_TRANSACTION_AMOUNT";
    static final String FACTOR_CODE = "HIGH_FREQUENCY_TRANSACTION";

    @Override
    public RiskFactor evaluate(TransactionContext context) {
        RuleConfig countConfig = activeConfig(context, COUNT_RULE_CODE);
        RuleConfig amountConfig = activeConfig(context, AMOUNT_RULE_CODE);
        boolean countTriggered = countConfig != null
                && countConfig.thresholdValue() != null
                && BigDecimal.valueOf(context.velocityCount10m()).compareTo(countConfig.thresholdValue()) >= 0;
        boolean amountTriggered = amountConfig != null
                && amountConfig.thresholdValue() != null
                && context.velocityAmount10m().compareTo(amountConfig.thresholdValue()) >= 0;

        if (!countTriggered && !amountTriggered) {
            return RiskFactor.notTriggered(FACTOR_CODE);
        }

        int score = Math.max(countConfig == null ? 0 : countConfig.score(), amountConfig == null ? 0 : amountConfig.score());
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("velocityCount10m", context.velocityCount10m());
        metadata.put("velocityAmount10m", context.velocityAmount10m());
        if (countConfig != null) {
            metadata.put("countThreshold", countConfig.thresholdValue());
        }
        if (amountConfig != null) {
            metadata.put("amountThreshold", amountConfig.thresholdValue());
        }

        return RiskFactor.triggered(FACTOR_CODE, "Source account has high transaction velocity", score, metadata);
    }
}
