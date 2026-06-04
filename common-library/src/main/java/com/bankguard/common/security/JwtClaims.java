package com.bankguard.common.security;

import java.time.Instant;
import java.util.List;

public record JwtClaims(
        String username,
        List<String> roles,
        Instant issuedAt,
        Instant expiresAt
) {
    public JwtClaims {
        roles = roles == null ? List.of() : List.copyOf(roles);
    }
}
