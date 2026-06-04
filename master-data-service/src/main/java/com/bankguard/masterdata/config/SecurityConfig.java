package com.bankguard.masterdata.config;

import com.bankguard.common.web.CommonAccessDeniedHandler;
import com.bankguard.common.web.CommonAuthenticationEntryPoint;
import com.bankguard.common.web.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
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
                        .requestMatchers("/actuator/health", "/actuator/info", "/actuator/metrics").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/customers/*/risk-profile").hasAnyAuthority("ROLE_ADMIN", "ROLE_FRAUD_ANALYST")
                        .requestMatchers(HttpMethod.GET, "/api/v1/risk-rules", "/api/v1/risk-rules/*").hasAnyAuthority("ROLE_ADMIN", "ROLE_FRAUD_ANALYST")
                        .requestMatchers("/api/v1/blacklisted-accounts/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/v1/customers/*/trusted-devices/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/customers/*/risk-profile").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/risk-rules/*").hasAuthority("ROLE_ADMIN")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
