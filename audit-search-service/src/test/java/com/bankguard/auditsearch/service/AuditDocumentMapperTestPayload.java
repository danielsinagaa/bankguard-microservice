package com.bankguard.auditsearch.service;

import com.bankguard.common.event.RiskFactorPayload;
import com.bankguard.common.event.TransactionRiskScoredPayload;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class AuditDocumentMapperTestPayload {
    private AuditDocumentMapperTestPayload() {
    }

    public static TransactionRiskScoredPayload payload() {
        return new TransactionRiskScoredPayload(
                "TRX-20260604-000001",
                "CIF001",
                "Daniel Sinaga",
                "1234567890",
                "9876543210",
                new BigDecimal("25000000"),
                "IDR",
                "MOBILE_BANKING",
                "DEVICE-1",
                "36.77.88.12",
                "Jakarta",
                72,
                "REVIEW",
                List.of(new RiskFactorPayload("HIGH_AMOUNT", "High amount", 25, Map.of())),
                Instant.parse("2026-06-04T10:00:00Z"),
                Instant.parse("2026-06-04T10:00:03Z")
        );
    }
}
