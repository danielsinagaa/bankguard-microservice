package com.bankguard.masterdata.service;

import com.bankguard.common.api.PageResponse;
import com.bankguard.common.constant.ErrorCode;
import com.bankguard.common.exception.ApiException;
import com.bankguard.masterdata.cache.RedisCacheInvalidationService;
import com.bankguard.masterdata.dto.RiskRuleResponse;
import com.bankguard.masterdata.dto.UpdateRiskRuleRequest;
import com.bankguard.masterdata.entity.RiskRuleConfigEntity;
import com.bankguard.masterdata.repository.RiskRuleConfigRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RiskRuleConfigService {
    private final RiskRuleConfigRepository repository;
    private final RedisCacheInvalidationService cacheInvalidationService;
    private final Clock clock;

    public RiskRuleConfigService(
            RiskRuleConfigRepository repository,
            RedisCacheInvalidationService cacheInvalidationService,
            Clock clock
    ) {
        this.repository = repository;
        this.cacheInvalidationService = cacheInvalidationService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<RiskRuleResponse> list(int page, int size) {
        Page<RiskRuleConfigEntity> result = repository.findAll(
                MasterDataPageable.of(page, size, Sort.by(Sort.Direction.ASC, "ruleCode"))
        );
        return PageResponse.of(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements()
        );
    }

    @Transactional(readOnly = true)
    public RiskRuleResponse get(String ruleCode) {
        return toResponse(find(ruleCode));
    }

    @Transactional
    public RiskRuleResponse update(String ruleCode, UpdateRiskRuleRequest request) {
        validateThreshold(request.thresholdValue());
        RiskRuleConfigEntity rule = find(ruleCode);
        rule.update(
                request.score(),
                request.thresholdValue(),
                request.active(),
                request.description(),
                Instant.now(clock)
        );
        cacheInvalidationService.invalidateRiskRuleConfig(ruleCode);
        return toResponse(rule);
    }

    private RiskRuleConfigEntity find(String ruleCode) {
        return repository.findByRuleCode(ruleCode)
                .orElseThrow(() -> new ApiException(404, ErrorCode.RISK_RULE_NOT_FOUND, "Risk rule does not exist"));
    }

    private static void validateThreshold(BigDecimal thresholdValue) {
        if (thresholdValue != null && thresholdValue.compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiException(400, ErrorCode.VALIDATION_ERROR, "Threshold value must not be negative");
        }
    }

    private RiskRuleResponse toResponse(RiskRuleConfigEntity entity) {
        return new RiskRuleResponse(
                entity.getRuleCode(),
                entity.getRuleName(),
                entity.getScore(),
                entity.getThresholdValue(),
                entity.isActive(),
                entity.getDescription()
        );
    }
}
