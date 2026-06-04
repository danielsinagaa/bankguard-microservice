package com.bankguard.transaction.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bankguard.common.constant.ErrorCode;
import com.bankguard.common.exception.ApiException;
import com.bankguard.common.security.JwtTokenService;
import com.bankguard.transaction.dto.request.LoginRequest;
import com.bankguard.transaction.entity.RoleEntity;
import com.bankguard.transaction.entity.UserEntity;
import com.bankguard.transaction.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtTokenService jwtTokenService = new JwtTokenService(
            "test-secret",
            3600,
            Clock.fixed(Instant.parse("2026-06-04T10:00:00Z"), ZoneOffset.UTC)
    );

    @Mock
    private UserRepository userRepository;

    @Test
    void shouldLoginActiveUserWithValidPassword() {
        UserEntity user = new UserEntity(
                "admin",
                passwordEncoder.encode("admin123"),
                "System Administrator",
                true,
                Set.of(new RoleEntity("ROLE_ADMIN", "Administrator"))
        );
        when(userRepository.findByUsernameAndActiveTrue("admin")).thenReturn(Optional.of(user));

        AuthService authService = new AuthService(userRepository, passwordEncoder, jwtTokenService);

        var response = authService.login(new LoginRequest("admin", "admin123"));

        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600);
        assertThat(response.roles()).containsExactly("ROLE_ADMIN");
        assertThat(jwtTokenService.validate(response.accessToken())).isPresent();
    }

    @Test
    void shouldRejectInvalidPassword() {
        UserEntity user = new UserEntity(
                "admin",
                passwordEncoder.encode("admin123"),
                "System Administrator",
                true,
                Set.of(new RoleEntity("ROLE_ADMIN", "Administrator"))
        );
        when(userRepository.findByUsernameAndActiveTrue("admin")).thenReturn(Optional.of(user));

        AuthService authService = new AuthService(userRepository, passwordEncoder, jwtTokenService);

        assertThatThrownBy(() -> authService.login(new LoginRequest("admin", "wrong")))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }
}
