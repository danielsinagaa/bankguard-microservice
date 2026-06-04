package com.bankguard.common.web;

import com.bankguard.common.constant.ApiHeaders;
import com.bankguard.common.util.CorrelationIdUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {
    public static final String REQUEST_ID_ATTRIBUTE = "bankguard.requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = CorrelationIdUtil.resolveRequestId(request.getHeader(ApiHeaders.REQUEST_ID), UUID::randomUUID);
        request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
        response.setHeader(ApiHeaders.REQUEST_ID, requestId);

        MDC.put("requestId", requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("requestId");
        }
    }

    public static String requestId(HttpServletRequest request) {
        Object value = request.getAttribute(REQUEST_ID_ATTRIBUTE);
        if (value instanceof String requestId && !requestId.isBlank()) {
            return requestId;
        }
        return CorrelationIdUtil.resolveRequestId(request.getHeader(ApiHeaders.REQUEST_ID), UUID::randomUUID);
    }
}
