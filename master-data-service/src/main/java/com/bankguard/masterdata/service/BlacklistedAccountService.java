package com.bankguard.masterdata.service;

import com.bankguard.common.api.PageResponse;
import com.bankguard.common.constant.ErrorCode;
import com.bankguard.common.exception.ApiException;
import com.bankguard.masterdata.cache.RedisCacheInvalidationService;
import com.bankguard.masterdata.dto.BlacklistedAccountResponse;
import com.bankguard.masterdata.dto.CreateBlacklistedAccountRequest;
import com.bankguard.masterdata.dto.UpdateBlacklistedAccountRequest;
import com.bankguard.masterdata.entity.BlacklistedAccountEntity;
import com.bankguard.masterdata.repository.BlacklistedAccountRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BlacklistedAccountService {
    private final BlacklistedAccountRepository repository;
    private final RedisCacheInvalidationService cacheInvalidationService;
    private final Clock clock;

    public BlacklistedAccountService(
            BlacklistedAccountRepository repository,
            RedisCacheInvalidationService cacheInvalidationService,
            Clock clock
    ) {
        this.repository = repository;
        this.cacheInvalidationService = cacheInvalidationService;
        this.clock = clock;
    }

    @Transactional
    public BlacklistedAccountResponse create(CreateBlacklistedAccountRequest request) {
        if (repository.existsByAccountNumber(request.accountNumber())) {
            throw new ApiException(409, ErrorCode.DUPLICATE_RESOURCE, "Account already exists in blacklist");
        }
        BlacklistedAccountEntity entity = repository.save(new BlacklistedAccountEntity(
                request.accountNumber(),
                request.reason(),
                currentUsername(),
                Instant.now(clock)
        ));
        cacheInvalidationService.invalidateBlacklistedAccount(entity.getAccountNumber());
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public PageResponse<BlacklistedAccountResponse> list(Boolean active, String accountNumber, int page, int size) {
        Page<BlacklistedAccountEntity> result = repository.search(
                active,
                blankToNull(accountNumber),
                MasterDataPageable.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return PageResponse.of(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements()
        );
    }

    @Transactional(readOnly = true)
    public BlacklistedAccountResponse get(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public BlacklistedAccountResponse update(Long id, UpdateBlacklistedAccountRequest request) {
        BlacklistedAccountEntity entity = find(id);
        entity.update(request.reason(), request.active(), Instant.now(clock));
        cacheInvalidationService.invalidateBlacklistedAccount(entity.getAccountNumber());
        return toResponse(entity);
    }

    @Transactional
    public void deactivate(Long id) {
        BlacklistedAccountEntity entity = find(id);
        entity.deactivate(Instant.now(clock));
        cacheInvalidationService.invalidateBlacklistedAccount(entity.getAccountNumber());
    }

    private BlacklistedAccountEntity find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ApiException(404, ErrorCode.BLACKLISTED_ACCOUNT_NOT_FOUND, "Blacklisted account does not exist"));
    }

    private BlacklistedAccountResponse toResponse(BlacklistedAccountEntity entity) {
        return new BlacklistedAccountResponse(
                entity.getId(),
                entity.getAccountNumber(),
                entity.getReason(),
                entity.isActive(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private static String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null || authentication.getName() == null ? "system" : authentication.getName();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
