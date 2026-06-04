package com.bankguard.riskengine.rule;

import com.bankguard.riskengine.dto.RuleConfig;
import com.bankguard.riskengine.dto.TransactionContext;

abstract class AbstractRiskRule implements RiskRule {
    protected RuleConfig activeConfig(TransactionContext context, String ruleCode) {
        RuleConfig config = context.config(ruleCode);
        return config != null && config.active() ? config : null;
    }
}
