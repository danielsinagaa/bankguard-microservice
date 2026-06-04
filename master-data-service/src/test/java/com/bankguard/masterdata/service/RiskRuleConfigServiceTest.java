package com.bankguard.masterdata.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bankguard.common.exception.ApiException;
import com.bankguard.masterdata.cache.RedisCacheInvalidationService;
import com.bankguard.masterdata.dto.UpdateRiskRuleRequest;
import com.bankguard.masterdata.entity.RiskRuleConfigEntity;
import com.bankguard.masterdata.repository.RiskRuleConfigRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RiskRuleConfigServiceTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-04T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void shouldUpdateRiskRuleAndInvalidateCache() {
        RiskRuleConfigRepository repository = mock(RiskRuleConfigRepository.class);
        RedisCacheInvalidationService cache = mock(RedisCacheInvalidationService.class);
        RiskRuleConfigEntity rule = new RiskRuleConfigEntity(
                "HIGH_AMOUNT",
                "High Amount Transaction",
                25,
                new BigDecimal("10000000"),
                true,
                "Old description"
        );
        when(repository.findByRuleCode("HIGH_AMOUNT")).thenReturn(Optional.of(rule));
        RiskRuleConfigService service = new RiskRuleConfigService(repository, cache, CLOCK);

        var response = service.update("HIGH_AMOUNT", new UpdateRiskRuleRequest(
                30,
                new BigDecimal("15000000"),
                true,
                "New description"
        ));

        assertThat(response.score()).isEqualTo(30);
        assertThat(response.thresholdValue()).isEqualByComparingTo("15000000");
        assertThat(response.description()).isEqualTo("New description");
        verify(cache).invalidateRiskRuleConfig("HIGH_AMOUNT");
    }

    @Test
    void shouldRejectNegativeThreshold() {
        RiskRuleConfigService service = new RiskRuleConfigService(
                mock(RiskRuleConfigRepository.class),
                mock(RedisCacheInvalidationService.class),
                CLOCK
        );

        assertThatThrownBy(() -> service.update("HIGH_AMOUNT", new UpdateRiskRuleRequest(30, new BigDecimal("-1"), true, "Nope")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("must not be negative");
    }
}
