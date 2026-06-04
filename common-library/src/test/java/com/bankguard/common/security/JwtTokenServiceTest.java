package com.bankguard.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class JwtTokenServiceTest {

    @Test
    void shouldCreateAndValidateToken() {
        JwtTokenService jwtTokenService = new JwtTokenService(
                "test-secret",
                3600,
                Clock.fixed(Instant.parse("2026-06-04T10:00:00Z"), ZoneOffset.UTC)
        );

        String token = jwtTokenService.createToken("admin", List.of("ROLE_ADMIN"));

        assertThat(jwtTokenService.validate(token))
                .hasValueSatisfying(claims -> {
                    assertThat(claims.username()).isEqualTo("admin");
                    assertThat(claims.roles()).containsExactly("ROLE_ADMIN");
                    assertThat(claims.expiresAt()).isEqualTo(Instant.parse("2026-06-04T11:00:00Z"));
                });
    }

    @Test
    void shouldRejectExpiredToken() {
        JwtTokenService issuer = new JwtTokenService(
                "test-secret",
                1,
                Clock.fixed(Instant.parse("2026-06-04T10:00:00Z"), ZoneOffset.UTC)
        );
        String token = issuer.createToken("admin", List.of("ROLE_ADMIN"));

        JwtTokenService validator = new JwtTokenService(
                "test-secret",
                1,
                Clock.fixed(Instant.parse("2026-06-04T10:00:02Z"), ZoneOffset.UTC)
        );

        assertThat(validator.validate(token)).isEmpty();
    }

    @Test
    void shouldRejectTokenSignedWithDifferentSecret() {
        JwtTokenService issuer = new JwtTokenService(
                "test-secret",
                3600,
                Clock.fixed(Instant.parse("2026-06-04T10:00:00Z"), ZoneOffset.UTC)
        );
        String token = issuer.createToken("admin", List.of("ROLE_ADMIN"));

        JwtTokenService validator = new JwtTokenService(
                "other-secret",
                3600,
                Clock.fixed(Instant.parse("2026-06-04T10:00:01Z"), ZoneOffset.UTC)
        );

        assertThat(validator.validate(token)).isEmpty();
    }
}
