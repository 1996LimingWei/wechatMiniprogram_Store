package com.shop.framework.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.common.pojo.CommonResult;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class AdminSecurityFilter extends OncePerRequestFilter {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (!uri.startsWith("/admin-api/") || uri.equals("/admin-api/auth/login")) {
            filterChain.doFilter(request, response);
            return;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)
                || !Integer.valueOf(2).equals(loginUser.getUserType())) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!hasPermission(loginUser.getUserId(), request.getMethod(), uri)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(CommonResult.error(403, "没有该管理操作权限")));
            recordOperation(loginUser.getUserId(), request, null, false, 0, "权限不足");
            return;
        }
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request, 8192);
        long startedAt = System.currentTimeMillis();
        try {
            filterChain.doFilter(wrappedRequest, response);
        } finally {
            recordOperation(loginUser.getUserId(), wrappedRequest, businessReference(wrappedRequest), response.getStatus() < 400,
                    System.currentTimeMillis() - startedAt,
                    response.getStatus() < 400 ? "" : "HTTP " + response.getStatus());
        }
    }

    private boolean hasPermission(Long adminUserId, String method, String uri) {
        if ("POST".equalsIgnoreCase(method) && "/admin-api/system/password/change".equals(uri)) {
            Integer enabled = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sys_admin_user WHERE id = ? AND status = 1 AND deleted = b'0'",
                    Integer.class, adminUserId);
            return enabled != null && enabled == 1;
        }
        Integer superAdmin = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM sys_admin_user u
                  JOIN sys_admin_user_role ur ON ur.admin_user_id = u.id
                  JOIN sys_role r ON r.id = ur.role_id AND r.status = 1 AND r.deleted = b'0'
                 WHERE u.id = ? AND u.status = 1 AND u.deleted = b'0'
                   AND r.code = 'SUPER_ADMIN'
                """, Integer.class, adminUserId);
        if (superAdmin != null && superAdmin > 0) return true;
        List<PermissionRule> rules = jdbcTemplate.query("""
                SELECT DISTINCT p.path_pattern, p.http_method
                  FROM sys_admin_user u
                  JOIN sys_admin_user_role ur ON ur.admin_user_id = u.id
                  JOIN sys_role r ON r.id = ur.role_id AND r.status = 1 AND r.deleted = b'0'
                  JOIN sys_role_permission rp ON rp.role_id = r.id
                  JOIN sys_permission p ON p.id = rp.permission_id AND p.status = 1 AND p.deleted = b'0'
                 WHERE u.id = ? AND u.status = 1 AND u.deleted = b'0'
                """, (rs, index) -> new PermissionRule(
                        rs.getString("path_pattern"), rs.getString("http_method")), adminUserId);
        return rules.stream().anyMatch(rule ->
                ("*".equals(rule.httpMethod()) || method.equalsIgnoreCase(rule.httpMethod()))
                        && pathMatcher.match(rule.pathPattern(), uri));
    }

    private void recordOperation(Long adminUserId, HttpServletRequest request, String businessRef, boolean success,
                                 long durationMs, String message) {
        try {
            jdbcTemplate.update("""
                    INSERT INTO sys_operation_log
                        (admin_user_id, method, request_uri, business_ref, success, ip, duration_ms, message)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """, adminUserId, request.getMethod(), request.getRequestURI(), businessRef, success ? 1 : 0,
                    clientIp(request), durationMs, message);
        } catch (Exception ignored) {
            // 审计落库异常不能覆盖原业务响应，监控层会捕获数据库错误。
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip = forwarded == null || forwarded.isBlank()
                ? request.getRemoteAddr() : forwarded.split(",")[0].trim();
        return ip.length() <= 64 ? ip : ip.substring(0, 64);
    }

    private String businessReference(ContentCachingRequestWrapper request) {
        try {
            String value = firstNonBlank(request.getParameter("orderNo"), request.getParameter("orderId"),
                    request.getParameter("afterSaleNo"), request.getParameter("afterSaleId"),
                    request.getParameter("spuId"), request.getParameter("id"));
            if (value != null) return truncate(value);
            byte[] content = request.getContentAsByteArray();
            if (content.length == 0 || content.length > 8192) return null;
            Map<String, Object> body = objectMapper.readValue(content, Map.class);
            for (String key : List.of("orderNo", "orderId", "afterSaleNo", "afterSaleId", "spuId", "id")) {
                Object candidate = body.get(key);
                if (candidate != null && !String.valueOf(candidate).isBlank()) return truncate(String.valueOf(candidate));
            }
        } catch (Exception ignored) {
            // 业务审计只提取关联编号，不解析或记录完整请求内容。
        }
        return null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value;
        return null;
    }

    private String truncate(String value) {
        return value.length() <= 128 ? value : value.substring(0, 128);
    }

    private record PermissionRule(String pathPattern, String httpMethod) {
    }
}
