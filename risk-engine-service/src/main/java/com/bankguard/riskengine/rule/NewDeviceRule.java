package com.bankguard.riskengine.rule;

import com.bankguard.riskengine.dto.RiskFactor;
import com.bankguard.riskengine.dto.RuleConfig;
import com.bankguard.riskengine.dto.TransactionContext;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class NewDeviceRule extends AbstractRiskRule {
    static final String RULE_CODE = "NEW_DEVICE";

    @Override
    public RiskFactor evaluate(TransactionContext context) {
        RuleConfig config = activeConfig(context, RULE_CODE);
        String deviceId = context.transaction().getDeviceId();
        if (config == null || deviceId == null || deviceId.isBlank() || (context.knownDevice() && context.trustedDevice())) {
            return RiskFactor.notTriggered(RULE_CODE);
        }

        return RiskFactor.triggered(
                RULE_CODE,
                "Device is not trusted for this customer",
                config.score(),
                Map.of("deviceId", deviceId)
        );
    }
}
