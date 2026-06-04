package com.bankguard.riskengine.rule;

import com.bankguard.riskengine.dto.RiskFactor;
import com.bankguard.riskengine.dto.RuleConfig;
import com.bankguard.riskengine.dto.TransactionContext;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class BlacklistedDestinationRule extends AbstractRiskRule {
    static final String RULE_CODE = "BLACKLISTED_DESTINATION";

    @Override
    public RiskFactor evaluate(TransactionContext context) {
        RuleConfig config = activeConfig(context, RULE_CODE);
        if (config == null || !context.blacklistedDestination()) {
            return RiskFactor.notTriggered(RULE_CODE);
        }

        return RiskFactor.triggered(
                RULE_CODE,
                "Destination account is blacklisted",
                config.score(),
                Map.of("destinationAccountNumber", context.transaction().getDestinationAccountNumber())
        );
    }
}
