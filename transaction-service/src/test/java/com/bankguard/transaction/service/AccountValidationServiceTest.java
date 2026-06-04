package com.bankguard.transaction.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bankguard.common.constant.ErrorCode;
import com.bankguard.common.exception.ApiException;
import com.bankguard.transaction.entity.AccountEntity;
import com.bankguard.transaction.entity.CustomerEntity;
import com.bankguard.transaction.repository.AccountRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;

class AccountValidationServiceTest {

    private final AccountRepository accountRepository = mock(AccountRepository.class);
    private final AccountValidationService service = new AccountValidationService(accountRepository);

    @Test
    void shouldReturnActiveAccount() {
        AccountEntity account = account("ACTIVE");
        when(accountRepository.findByAccountNumber("1234567890")).thenReturn(Optional.of(account));

        assertThat(service.validateSourceAccount("1234567890")).isSameAs(account);
    }

    @Test
    void shouldRejectMissingAccount() {
        when(accountRepository.findByAccountNumber("0000000000")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validateSourceAccount("0000000000"))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SOURCE_ACCOUNT_NOT_FOUND);
    }

    @Test
    void shouldRejectInactiveAccountStatuses() {
        when(accountRepository.findByAccountNumber("1234567890")).thenReturn(Optional.of(account("BLOCKED")));

        assertThatThrownBy(() -> service.validateSourceAccount("1234567890"))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SOURCE_ACCOUNT_INACTIVE);
    }

    private static AccountEntity account(String status) {
        CustomerEntity customer = new CustomerEntity("CIF001", "Daniel Sinaga", "daniel@example.com", "0811", "ACTIVE");
        return new AccountEntity(customer, "1234567890", "SAVINGS", "IDR", BigDecimal.TEN, status);
    }
}
