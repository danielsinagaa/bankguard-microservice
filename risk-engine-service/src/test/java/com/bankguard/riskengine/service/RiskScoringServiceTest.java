package com.bankguard.riskengine.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bankguard.riskengine.dto.RiskFactor;
import com.bankguard.riskengine.rule.RiskRule;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RiskScoringServiceTest {

    @ParameterizedTest
    @CsvSource({
            "49, APPROVED",
            "50, REVIEW",
            "79, REVIEW",
            "80, BLOCKED"
    })
    void shouldMapDecisionBoundaries(int score, String decision) {
        RiskScoringService service = new RiskScoringService(List.of());

        assertThat(service.decision(score)).isEqualTo(decision);
    }

    @Test
    void shouldSumTriggeredFactorsOnly() {
        RiskRule highAmount = context -> RiskFactor.triggered("HIGH_AMOUNT", "High amount", 25, Map.of());
        RiskRule noRisk = context -> RiskFactor.notTriggered("NEW_DEVICE");
        RiskRule newLocation = context -> RiskFactor.triggered("UNUSUAL_LOCATION", "Unknown location", 20, Map.of());
        RiskScoringService service = new RiskScoringService(List.of(highAmount, noRisk, newLocation));

        var result = service.score(null);

        assertThat(result.totalScore()).isEqualTo(45);
        assertThat(result.decision()).isEqualTo("APPROVED");
        assertThat(result.riskFactors()).extracting(RiskFactor::code)
                .containsExactly("HIGH_AMOUNT", "UNUSUAL_LOCATION");
    }
}
