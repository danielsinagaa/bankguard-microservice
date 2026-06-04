package com.bankguard.common.api;

import java.util.List;

public record PageResponse<T>(
        List<T> data,
        int page,
        int size,
        long total
) {
    public PageResponse {
        data = data == null ? List.of() : List.copyOf(data);
    }

    public static <T> PageResponse<T> of(List<T> data, int page, int size, long total) {
        return new PageResponse<>(data, page, size, total);
    }
}
