package com.bankguard.masterdata.service;

import com.bankguard.common.api.PageResponse;
import com.bankguard.common.constant.ErrorCode;
import com.bankguard.common.exception.ApiException;
import com.bankguard.masterdata.cache.RedisCacheInvalidationService;
import com.bankguard.masterdata.dto.RegisterTrustedDeviceRequest;
import com.bankguard.masterdata.dto.TrustedDeviceResponse;
import com.bankguard.masterdata.dto.UpdateTrustedDeviceRequest;
import com.bankguard.masterdata.entity.CustomerDeviceEntity;
import com.bankguard.masterdata.repository.CustomerDeviceRepository;
import com.bankguard.masterdata.repository.CustomerRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrustedDeviceService {
    private final CustomerRepository customerRepository;
    private final CustomerDeviceRepository deviceRepository;
    private final RedisCacheInvalidationService cacheInvalidationService;
    private final Clock clock;

    public TrustedDeviceService(
            CustomerRepository customerRepository,
            CustomerDeviceRepository deviceRepository,
            RedisCacheInvalidationService cacheInvalidationService,
            Clock clock
    ) {
        this.customerRepository = customerRepository;
        this.deviceRepository = deviceRepository;
        this.cacheInvalidationService = cacheInvalidationService;
        this.clock = clock;
    }

    @Transactional
    public TrustedDeviceResponse register(Long customerId, RegisterTrustedDeviceRequest request) {
        ensureCustomerExists(customerId);
        if (deviceRepository.existsByCustomerIdAndDeviceId(customerId, request.deviceId())) {
            throw new ApiException(409, ErrorCode.DUPLICATE_RESOURCE, "Trusted device already exists");
        }
        CustomerDeviceEntity device = deviceRepository.save(new CustomerDeviceEntity(
                customerId,
                request.deviceId(),
                request.deviceName(),
                true,
                Instant.now(clock)
        ));
        cacheInvalidationService.invalidateTrustedDevice(customerId, device.getDeviceId());
        return toResponse(device);
    }

    @Transactional(readOnly = true)
    public PageResponse<TrustedDeviceResponse> list(Long customerId, int page, int size) {
        ensureCustomerExists(customerId);
        Page<CustomerDeviceEntity> result = deviceRepository.findByCustomerId(
                customerId,
                MasterDataPageable.of(page, size, Sort.by(Sort.Direction.DESC, "lastUsedAt"))
        );
        return PageResponse.of(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements()
        );
    }

    @Transactional
    public TrustedDeviceResponse update(Long customerId, String deviceId, UpdateTrustedDeviceRequest request) {
        ensureCustomerExists(customerId);
        CustomerDeviceEntity device = deviceRepository.findByCustomerIdAndDeviceId(customerId, deviceId)
                .orElseThrow(() -> new ApiException(404, ErrorCode.TRUSTED_DEVICE_NOT_FOUND, "Trusted device does not exist"));
        device.update(request.deviceName(), request.trusted(), Instant.now(clock));
        cacheInvalidationService.invalidateTrustedDevice(customerId, deviceId);
        return toResponse(device);
    }

    private void ensureCustomerExists(Long customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new ApiException(404, ErrorCode.CUSTOMER_NOT_FOUND, "Customer does not exist");
        }
    }

    private TrustedDeviceResponse toResponse(CustomerDeviceEntity device) {
        return new TrustedDeviceResponse(
                device.getCustomerId(),
                device.getDeviceId(),
                device.getDeviceName(),
                device.isTrusted(),
                device.getFirstSeenAt(),
                device.getLastUsedAt()
        );
    }
}
