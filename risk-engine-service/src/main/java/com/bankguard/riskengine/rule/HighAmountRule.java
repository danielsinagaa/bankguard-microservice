package com.bankguard.riskengine.rule;

import com.bankguard.riskengine.dto.RiskFactor;
import com.bankguard.riskengine.dto.RuleConfig;
import com.bankguard.riskengine.dto.TransactionContext;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class HighAmountRule extends AbstractRiskRule {
    static final String RULE_CODE = "HIGH_AMOUNT";

    @Override
    public RiskFactor evaluate(TransactionContext context) {
        RuleConfig config = activeConfig(context, RULE_CODE);
        if (config == null || config.thresholdValue() == null) {
            return RiskFactor.notTriggered(RULE_CODE);
        }

        BigDecimal amount = context.transaction().getAmount();
        if (amount.compareTo(config.thresholdValue()) < 0) {
            return RiskFactor.notTriggered(RULE_CODE);
        }

        return RiskFactor.triggered(
                RULE_CODE,
                "Transaction amount is above configured threshold",
                config.score(),
                Map.of("threshold", config.thresholdValue(), "actualAmount", amount)
        );
    }
}
