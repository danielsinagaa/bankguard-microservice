package com.bankguard.riskengine.rule;

import com.bankguard.riskengine.dto.RiskFactor;
import com.bankguard.riskengine.dto.RuleConfig;
import com.bankguard.riskengine.dto.TransactionContext;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class UnusualLocationRule extends AbstractRiskRule {
    static final String RULE_CODE = "UNUSUAL_LOCATION";

    @Override
    public RiskFactor evaluate(TransactionContext context) {
        RuleConfig config = activeConfig(context, RULE_CODE);
        String location = context.transaction().getLocation();
        if (config == null || location == null || location.isBlank() || context.knownLocation()) {
            return RiskFactor.notTriggered(RULE_CODE);
        }

        return RiskFactor.triggered(
                RULE_CODE,
                "Transaction location is not known for this customer",
                config.score(),
                Map.of("location", location)
        );
    }
}
