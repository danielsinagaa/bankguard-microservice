package com.bankguard.transaction.service;

import com.bankguard.common.constant.ErrorCode;
import com.bankguard.common.exception.ApiException;
import com.bankguard.transaction.entity.AccountEntity;
import com.bankguard.transaction.repository.AccountRepository;
import org.springframework.stereotype.Service;

@Service
public class AccountValidationService {
    private static final String ACTIVE_STATUS = "ACTIVE";

    private final AccountRepository accountRepository;

    public AccountValidationService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public AccountEntity validateSourceAccount(String accountNumber) {
        AccountEntity account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ApiException(404, ErrorCode.SOURCE_ACCOUNT_NOT_FOUND, "Source account does not exist"));

        if (!ACTIVE_STATUS.equals(account.getStatus())) {
            throw new ApiException(409, ErrorCode.SOURCE_ACCOUNT_INACTIVE, "Source account is not active");
        }

        return account;
    }
}
