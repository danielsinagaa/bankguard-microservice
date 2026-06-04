package com.bankguard.riskengine.cache;

import java.math.BigDecimal;

public record VelocitySnapshot(
        long count,
        BigDecimal amount
) {
}
