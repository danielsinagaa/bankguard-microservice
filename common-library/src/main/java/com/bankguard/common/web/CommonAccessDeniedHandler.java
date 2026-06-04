package com.bankguard.common.web;

import com.bankguard.common.constant.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class CommonAccessDeniedHandler implements AccessDeniedHandler {
    private final SecurityErrorWriter errorWriter;

    public CommonAccessDeniedHandler(SecurityErrorWriter errorWriter) {
        this.errorWriter = errorWriter;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException {
        errorWriter.write(request, response, HttpServletResponse.SC_FORBIDDEN, ErrorCode.FORBIDDEN, "Access is forbidden");
    }
}
