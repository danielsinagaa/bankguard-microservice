package com.bankguard.transaction.service;

import com.bankguard.common.constant.ErrorCode;
import com.bankguard.common.exception.ApiException;
import com.bankguard.common.security.JwtTokenService;
import com.bankguard.transaction.dto.request.LoginRequest;
import com.bankguard.transaction.dto.response.LoginResponse;
import com.bankguard.transaction.entity.RoleEntity;
import com.bankguard.transaction.entity.UserEntity;
import com.bankguard.transaction.repository.UserRepository;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private static final String TOKEN_TYPE = "Bearer";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenService jwtTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByUsernameAndActiveTrue(request.username())
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
                .orElseThrow(() -> new ApiException(401, ErrorCode.UNAUTHORIZED, "Invalid username or password"));

        List<String> roles = user.getRoles().stream()
                .map(RoleEntity::getRoleCode)
                .sorted()
                .toList();

        return new LoginResponse(
                jwtTokenService.createToken(user.getUsername(), roles),
                TOKEN_TYPE,
                jwtTokenService.expirationSeconds(),
                roles
        );
    }
}
