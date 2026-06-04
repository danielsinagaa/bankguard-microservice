package com.bankguard.riskengine.rule;

import com.bankguard.riskengine.dto.RiskFactor;
import com.bankguard.riskengine.dto.RuleConfig;
import com.bankguard.riskengine.dto.TransactionContext;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class HighRiskCustomerProfileRule extends AbstractRiskRule {
    static final String RULE_CODE = "HIGH_RISK_CUSTOMER_PROFILE";

    @Override
    public RiskFactor evaluate(TransactionContext context) {
        RuleConfig config = activeConfig(context, RULE_CODE);
        if (config == null || !"HIGH".equals(context.customerRiskLevel())) {
            return RiskFactor.notTriggered(RULE_CODE);
        }

        return RiskFactor.triggered(
                RULE_CODE,
                "Customer profile is marked as high risk",
                config.score(),
                Map.of("riskLevel", context.customerRiskLevel())
        );
    }
}
