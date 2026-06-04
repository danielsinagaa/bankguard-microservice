package com.bankguard.masterdata.service;

import com.bankguard.common.constant.ErrorCode;
import com.bankguard.common.exception.ApiException;
import com.bankguard.masterdata.cache.RedisCacheInvalidationService;
import com.bankguard.masterdata.dto.RiskProfileResponse;
import com.bankguard.masterdata.dto.UpdateRiskProfileRequest;
import com.bankguard.masterdata.entity.CustomerRiskProfileEntity;
import com.bankguard.masterdata.repository.CustomerRepository;
import com.bankguard.masterdata.repository.CustomerRiskProfileRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerRiskProfileService {
    private static final Set<String> RISK_LEVELS = Set.of("LOW", "MEDIUM", "HIGH");

    private final CustomerRepository customerRepository;
    private final CustomerRiskProfileRepository profileRepository;
    private final RedisCacheInvalidationService cacheInvalidationService;
    private final Clock clock;

    public CustomerRiskProfileService(
            CustomerRepository customerRepository,
            CustomerRiskProfileRepository profileRepository,
            RedisCacheInvalidationService cacheInvalidationService,
            Clock clock
    ) {
        this.customerRepository = customerRepository;
        this.profileRepository = profileRepository;
        this.cacheInvalidationService = cacheInvalidationService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public RiskProfileResponse get(Long customerId) {
        ensureCustomerExists(customerId);
        return profileRepository.findByCustomerId(customerId)
                .map(this::toResponse)
                .orElseGet(() -> new RiskProfileResponse(customerId, "LOW", "Default profile", null, null, null));
    }

    @Transactional
    public RiskProfileResponse update(Long customerId, UpdateRiskProfileRequest request) {
        ensureCustomerExists(customerId);
        validateRiskLevel(request.riskLevel());
        Instant now = Instant.now(clock);
        CustomerRiskProfileEntity profile = profileRepository.findByCustomerId(customerId)
                .orElseGet(() -> new CustomerRiskProfileEntity(customerId, request.riskLevel(), request.riskReason(), now));
        profile.update(request.riskLevel(), request.riskReason(), now);
        CustomerRiskProfileEntity saved = profileRepository.save(profile);
        cacheInvalidationService.invalidateCustomerRiskProfile(customerId);
        return toResponse(saved);
    }

    private void ensureCustomerExists(Long customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new ApiException(404, ErrorCode.CUSTOMER_NOT_FOUND, "Customer does not exist");
        }
    }

    private static void validateRiskLevel(String riskLevel) {
        if (!RISK_LEVELS.contains(riskLevel)) {
            throw new ApiException(400, ErrorCode.VALIDATION_ERROR, "Risk level must be LOW, MEDIUM, or HIGH");
        }
    }

    private RiskProfileResponse toResponse(CustomerRiskProfileEntity profile) {
        return new RiskProfileResponse(
                profile.getCustomerId(),
                profile.getRiskLevel(),
                profile.getRiskReason(),
                profile.getLastReviewedAt(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}
