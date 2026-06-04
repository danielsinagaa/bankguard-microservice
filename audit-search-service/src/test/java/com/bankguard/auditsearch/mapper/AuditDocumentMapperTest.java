package com.bankguard.auditsearch.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.bankguard.common.event.RiskFactorPayload;
import com.bankguard.common.event.TransactionRiskScoredPayload;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AuditDocumentMapperTest {

    @Test
    void shouldMaskAccountNumbersAndExtractRiskFactorCodes() {
        AuditDocumentMapper mapper = new AuditDocumentMapper();

        var document = mapper.toDocument(payload());

        assertThat(document.getTransactionRef()).isEqualTo("TRX-20260604-000001");
        assertThat(document.getSourceAccountMasked()).isEqualTo("123****890");
        assertThat(document.getDestinationAccountMasked()).isEqualTo("987****210");
        assertThat(document.getRiskFactors()).containsExactly("HIGH_AMOUNT", "NEW_DEVICE");
    }

    static TransactionRiskScoredPayload payload() {
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
                List.of(
                        new RiskFactorPayload("NEW_DEVICE", "Device is not trusted", 20, Map.of()),
                        new RiskFactorPayload("HIGH_AMOUNT", "High amount", 25, Map.of())
                ),
                Instant.parse("2026-06-04T10:00:00Z"),
                Instant.parse("2026-06-04T10:00:03Z")
        );
    }
}
