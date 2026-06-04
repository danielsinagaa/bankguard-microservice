package com.bankguard.masterdata.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bankguard.common.exception.ApiException;
import com.bankguard.masterdata.cache.RedisCacheInvalidationService;
import com.bankguard.masterdata.dto.UpdateRiskProfileRequest;
import com.bankguard.masterdata.entity.CustomerRiskProfileEntity;
import com.bankguard.masterdata.repository.CustomerRepository;
import com.bankguard.masterdata.repository.CustomerRiskProfileRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CustomerRiskProfileServiceTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-04T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void shouldReturnDefaultProfileWhenCustomerHasNoProfile() {
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        CustomerRiskProfileRepository profileRepository = mock(CustomerRiskProfileRepository.class);
        when(customerRepository.existsById(1001L)).thenReturn(true);
        CustomerRiskProfileService service = new CustomerRiskProfileService(
                customerRepository,
                profileRepository,
                mock(RedisCacheInvalidationService.class),
                CLOCK
        );

        var response = service.get(1001L);

        assertThat(response.riskLevel()).isEqualTo("LOW");
        assertThat(response.riskReason()).isEqualTo("Default profile");
    }

    @Test
    void shouldUpdateProfileAndInvalidateCache() {
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        CustomerRiskProfileRepository profileRepository = mock(CustomerRiskProfileRepository.class);
        RedisCacheInvalidationService cache = mock(RedisCacheInvalidationService.class);
        CustomerRiskProfileEntity profile = new CustomerRiskProfileEntity(1001L, "LOW", "Default profile", Instant.parse("2026-06-04T09:00:00Z"));
        when(customerRepository.existsById(1001L)).thenReturn(true);
        when(profileRepository.findByCustomerId(1001L)).thenReturn(Optional.of(profile));
        when(profileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        CustomerRiskProfileService service = new CustomerRiskProfileService(customerRepository, profileRepository, cache, CLOCK);

        var response = service.update(1001L, new UpdateRiskProfileRequest("HIGH", "Confirmed suspicious activity"));

        assertThat(response.riskLevel()).isEqualTo("HIGH");
        assertThat(response.lastReviewedAt()).isEqualTo(Instant.parse("2026-06-04T10:00:00Z"));
        verify(cache).invalidateCustomerRiskProfile(1001L);
    }

    @Test
    void shouldRejectUnsupportedRiskLevel() {
        CustomerRepository customerRepository = mock(CustomerRepository.class);
        when(customerRepository.existsById(1001L)).thenReturn(true);
        CustomerRiskProfileService service = new CustomerRiskProfileService(
                customerRepository,
                mock(CustomerRiskProfileRepository.class),
                mock(RedisCacheInvalidationService.class),
                CLOCK
        );

        assertThatThrownBy(() -> service.update(1001L, new UpdateRiskProfileRequest("CRITICAL", "Nope")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("LOW, MEDIUM, or HIGH");
    }
}
