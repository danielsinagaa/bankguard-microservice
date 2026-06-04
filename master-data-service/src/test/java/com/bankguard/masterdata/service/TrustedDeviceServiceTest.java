package com.bankguard.masterdata.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bankguard.common.exception.ApiException;
import com.bankguard.masterdata.cache.RedisCacheInvalidationService;
import com.bankguard.masterdata.dto.RegisterTrustedDeviceRequest;
import com.bankguard.masterdata.dto.UpdateTrustedDeviceRequest;
import com.bankguard.masterdata.entity.CustomerDeviceEntity;
import com.bankguard.masterdata.repository.CustomerDeviceRepository;
import com.bankguard.masterdata.repository.CustomerRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TrustedDeviceServiceTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-04T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void shouldRegisterTrustedDeviceAndInvalidateCache() {
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        CustomerDeviceRepository deviceRepository = mock(CustomerDeviceRepository.class);
        RedisCacheInvalidationService cache = mock(RedisCacheInvalidationService.class);
        when(customerRepository.existsById(1001L)).thenReturn(true);
        when(deviceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        TrustedDeviceService service = new TrustedDeviceService(customerRepository, deviceRepository, cache, CLOCK);

        var response = service.register(1001L, new RegisterTrustedDeviceRequest("DEVICE-1", "Primary phone"));

        assertThat(response.customerId()).isEqualTo(1001L);
        assertThat(response.trusted()).isTrue();
        assertThat(response.firstSeenAt()).isEqualTo(Instant.parse("2026-06-04T10:00:00Z"));
        verify(cache).invalidateTrustedDevice(1001L, "DEVICE-1");
    }

    @Test
    void shouldRejectMissingCustomer() {
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        TrustedDeviceService service = new TrustedDeviceService(
                customerRepository,
                mock(CustomerDeviceRepository.class),
                mock(RedisCacheInvalidationService.class),
                CLOCK
        );

        assertThatThrownBy(() -> service.register(1001L, new RegisterTrustedDeviceRequest("DEVICE-1", "Phone")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Customer does not exist");
    }

    @Test
    void shouldUpdateTrustedDeviceAndInvalidateCache() {
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        CustomerDeviceRepository deviceRepository = mock(CustomerDeviceRepository.class);
        RedisCacheInvalidationService cache = mock(RedisCacheInvalidationService.class);
        CustomerDeviceEntity device = new CustomerDeviceEntity(1001L, "DEVICE-1", "Phone", true, Instant.parse("2026-06-04T09:00:00Z"));
        when(customerRepository.existsById(1001L)).thenReturn(true);
        when(deviceRepository.findByCustomerIdAndDeviceId(1001L, "DEVICE-1")).thenReturn(Optional.of(device));
        TrustedDeviceService service = new TrustedDeviceService(customerRepository, deviceRepository, cache, CLOCK);

        var response = service.update(1001L, "DEVICE-1", new UpdateTrustedDeviceRequest("Primary phone", false));

        assertThat(response.deviceName()).isEqualTo("Primary phone");
        assertThat(response.trusted()).isFalse();
        assertThat(response.lastUsedAt()).isEqualTo(Instant.parse("2026-06-04T10:00:00Z"));
        verify(cache).invalidateTrustedDevice(1001L, "DEVICE-1");
    }
}
