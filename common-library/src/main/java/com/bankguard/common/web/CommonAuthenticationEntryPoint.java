package com.bankguard.common.web;

import com.bankguard.common.constant.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class CommonAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final SecurityErrorWriter errorWriter;

    public CommonAuthenticationEntryPoint(SecurityErrorWriter errorWriter) {
        this.errorWriter = errorWriter;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        errorWriter.write(request, response, HttpServletResponse.SC_UNAUTHORIZED, ErrorCode.UNAUTHORIZED, "Authentication is required");
    }
}
