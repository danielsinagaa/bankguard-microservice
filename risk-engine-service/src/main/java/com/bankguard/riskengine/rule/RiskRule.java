package com.bankguard.riskengine.rule;

import com.bankguard.riskengine.dto.RiskFactor;
import com.bankguard.riskengine.dto.TransactionContext;

public interface RiskRule {
    RiskFactor evaluate(TransactionContext context);
}
