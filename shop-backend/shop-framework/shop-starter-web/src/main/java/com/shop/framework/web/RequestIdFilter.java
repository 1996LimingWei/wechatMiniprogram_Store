package com.shop.framework.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

public class RequestIdFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String MDC_KEY = "requestId";

    private static final Logger log = LoggerFactory.getLogger(RequestIdFilter.class);
    private static final int MAX_REQUEST_ID_LENGTH = 64;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestId = normalizeRequestId(request.getHeader(REQUEST_ID_HEADER));
        long startNanos = System.nanoTime();
        MDC.put(MDC_KEY, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
            int status = response.getStatus();
            if (status >= 500) {
                log.warn("请求完成 method={} uri={} status={} durationMs={}",
                        request.getMethod(), sanitizeUri(request.getRequestURI()), status, durationMs);
            } else {
                log.info("请求完成 method={} uri={} status={} durationMs={}",
                        request.getMethod(), sanitizeUri(request.getRequestURI()), status, durationMs);
            }
            MDC.remove(MDC_KEY);
        }
    }

    private String normalizeRequestId(String value) {
        if (value != null) {
            String trimmed = value.trim();
            if (trimmed.matches("[A-Za-z0-9._:-]{8,64}")) {
                return trimmed;
            }
        }
        return UUID.randomUUID().toString().replace("-", "").substring(0, MAX_REQUEST_ID_LENGTH / 2);
    }

    private String sanitizeUri(String uri) {
        if (uri == null || uri.isBlank()) {
            return "/";
        }
        return uri.length() <= 256 ? uri : uri.substring(0, 256);
    }
}
