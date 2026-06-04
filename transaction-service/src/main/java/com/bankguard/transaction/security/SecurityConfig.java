package com.bankguard.transaction.security;

import com.bankguard.common.web.CommonAccessDeniedHandler;
import com.bankguard.common.web.CommonAuthenticationEntryPoint;
import com.bankguard.common.web.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            CommonAuthenticationEntryPoint authenticationEntryPoint,
            CommonAccessDeniedHandler accessDeniedHandler
    ) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/login", "/actuator/health", "/actuator/info", "/actuator/metrics").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/transactions").hasAnyAuthority("ROLE_ADMIN", "ROLE_BACKOFFICE")
                        .requestMatchers(HttpMethod.GET, "/api/v1/transactions/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_BACKOFFICE", "ROLE_FRAUD_ANALYST")
                        .requestMatchers(HttpMethod.GET, "/api/v1/reports/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_FRAUD_ANALYST")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
