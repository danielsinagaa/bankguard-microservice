package com.bankguard.masterdata.service;

import com.bankguard.common.constant.ErrorCode;
import com.bankguard.common.exception.ApiException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

final class MasterDataPageable {
    private static final int MAX_PAGE_SIZE = 100;

    private MasterDataPageable() {
    }

    static PageRequest of(int page, int size, Sort sort) {
        if (page < 0) {
            throw new ApiException(400, ErrorCode.VALIDATION_ERROR, "Page number must not be negative");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ApiException(400, ErrorCode.INVALID_PAGE_SIZE, "Page size must be between 1 and 100");
        }
        return PageRequest.of(page, size, sort);
    }
}
