package com.bankguard.transaction.dto.response;

import java.util.List;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        List<String> roles
) {
    public LoginResponse {
        roles = roles == null ? List.of() : List.copyOf(roles);
    }
}
