package com.bankguard.common.web;

import com.bankguard.common.constant.ApiHeaders;
import com.bankguard.common.constant.ErrorCode;
import com.bankguard.common.security.JwtClaims;
import com.bankguard.common.security.JwtTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenService jwtTokenService;
    private final SecurityErrorWriter errorWriter;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService, SecurityErrorWriter errorWriter) {
        this.jwtTokenService = jwtTokenService;
        this.errorWriter = errorWriter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader(ApiHeaders.AUTHORIZATION);
        if (authorization == null || authorization.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!authorization.startsWith(BEARER_PREFIX)) {
            errorWriter.write(request, response, HttpServletResponse.SC_UNAUTHORIZED, ErrorCode.UNAUTHORIZED, "Invalid bearer token");
            return;
        }

        Optional<JwtClaims> claims = jwtTokenService.validate(authorization.substring(BEARER_PREFIX.length()));
        if (claims.isEmpty()) {
            SecurityContextHolder.clearContext();
            errorWriter.write(request, response, HttpServletResponse.SC_UNAUTHORIZED, ErrorCode.UNAUTHORIZED, "Invalid or expired token");
            return;
        }

        JwtClaims jwtClaims = claims.get();
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                jwtClaims.username(),
                null,
                jwtClaims.roles().stream().map(SimpleGrantedAuthority::new).toList()
        );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}
