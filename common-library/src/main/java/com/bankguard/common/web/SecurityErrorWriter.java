package com.bankguard.common.web;

import com.bankguard.common.api.CommonErrorResponse;
import com.bankguard.common.constant.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class SecurityErrorWriter {
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public void write(HttpServletRequest request, HttpServletResponse response, int status, ErrorCode errorCode, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), CommonErrorResponse.of(
                status,
                errorCode.name(),
                message,
                List.of(),
                request.getRequestURI(),
                RequestIdFilter.requestId(request)
        ));
    }
}
