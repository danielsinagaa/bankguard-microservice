package com.bankguard.masterdata.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bankguard.common.exception.ApiException;
import com.bankguard.masterdata.cache.RedisCacheInvalidationService;
import com.bankguard.masterdata.dto.CreateBlacklistedAccountRequest;
import com.bankguard.masterdata.dto.UpdateBlacklistedAccountRequest;
import com.bankguard.masterdata.entity.BlacklistedAccountEntity;
import com.bankguard.masterdata.repository.BlacklistedAccountRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class BlacklistedAccountServiceTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-04T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void shouldCreateBlacklistAndInvalidateCache() {
        BlacklistedAccountRepository repository = mock(BlacklistedAccountRepository.class);
        RedisCacheInvalidationService cache = mock(RedisCacheInvalidationService.class);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        BlacklistedAccountService service = new BlacklistedAccountService(repository, cache, CLOCK);

        var response = service.create(new CreateBlacklistedAccountRequest("9876543210", "Reported mule account"));

        assertThat(response.accountNumber()).isEqualTo("9876543210");
        assertThat(response.active()).isTrue();
        assertThat(response.createdAt()).isEqualTo(Instant.parse("2026-06-04T10:00:00Z"));
        verify(cache).invalidateBlacklistedAccount("9876543210");
    }

    @Test
    void shouldRejectDuplicateBlacklistAccount() {
        BlacklistedAccountRepository repository = mock(BlacklistedAccountRepository.class);
        when(repository.existsByAccountNumber("9876543210")).thenReturn(true);
        BlacklistedAccountService service = new BlacklistedAccountService(repository, mock(RedisCacheInvalidationService.class), CLOCK);

        assertThatThrownBy(() -> service.create(new CreateBlacklistedAccountRequest("9876543210", "Duplicate")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void shouldUpdateBlacklistAndInvalidateCache() {
        BlacklistedAccountRepository repository = mock(BlacklistedAccountRepository.class);
        RedisCacheInvalidationService cache = mock(RedisCacheInvalidationService.class);
        BlacklistedAccountEntity entity = new BlacklistedAccountEntity("9876543210", "Reported", "admin", Instant.parse("2026-06-04T09:00:00Z"));
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        BlacklistedAccountService service = new BlacklistedAccountService(repository, cache, CLOCK);

        var response = service.update(1L, new UpdateBlacklistedAccountRequest("Confirmed suspicious", false));

        assertThat(response.reason()).isEqualTo("Confirmed suspicious");
        assertThat(response.active()).isFalse();
        assertThat(response.updatedAt()).isEqualTo(Instant.parse("2026-06-04T10:00:00Z"));
        verify(cache).invalidateBlacklistedAccount("9876543210");
    }
}
