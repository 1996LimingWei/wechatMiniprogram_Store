package com.shop.framework.security;

import com.shop.common.config.DeliveryMode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

/** 录入模式在应用层封闭全部用户入口和交易后台，不依赖菜单隐藏或反向代理。 */
public class DeliveryModeFilter extends OncePerRequestFilter {
    private final DeliveryMode mode;
    private static final List<String> CATALOG_ADMIN_PREFIXES = List.of(
            "/admin-api/auth/", "/admin-api/product/", "/admin-api/category/",
            "/admin-api/content/", "/admin-api/material/", "/admin-api/system/");

    public DeliveryModeFilter(DeliveryMode mode) { this.mode = mode; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String servletPath = request.getServletPath();
        final String path = servletPath.isEmpty() ? request.getRequestURI() : servletPath;
        if (!mode.isCommerce() && (path.startsWith("/app-api/")
                || (path.startsWith("/admin-api/")
                && CATALOG_ADMIN_PREFIXES.stream().noneMatch(path::startsWith)))) {
            response.setStatus(503);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":503,\"msg\":\"商城交易尚未开放，请联系管理员\",\"data\":null}");
            return;
        }
        chain.doFilter(request, response);
    }
}
