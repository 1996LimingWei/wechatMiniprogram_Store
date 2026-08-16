package com.shop.framework.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.common.pojo.CommonResult;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        HttpServletRequest auditRequest = shouldCacheBody(request) ? new CachedBodyRequest(request) : request;
        Map<String, Object> requestBody = parseBody(auditRequest);
        OperationDescriptor operation = describeOperation(auditRequest.getMethod(), uri);
        String businessRef = businessReference(auditRequest, requestBody);
        String beforeSnapshot = operation.highRisk() ? entitySnapshot(operation, businessRef, requestBody) : "";
        if (!hasPermission(loginUser.getUserId(), request.getMethod(), uri)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(CommonResult.error(403, "没有该管理操作权限")));
            recordOperation(loginUser.getUserId(), auditRequest, operation, businessRef, beforeSnapshot, "", false, 0, "权限不足");
            return;
        }
        long startedAt = System.currentTimeMillis();
        try {
            filterChain.doFilter(auditRequest, response);
        } finally {
            String afterSnapshot = operation.highRisk()
                    ? entitySnapshot(operation, businessRef, requestBody)
                    : requestSummary(requestBody);
            recordOperation(loginUser.getUserId(), auditRequest, operation, businessRef, beforeSnapshot, afterSnapshot, response.getStatus() < 400,
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

    private void recordOperation(Long adminUserId, HttpServletRequest request, OperationDescriptor operation,
                                 String businessRef, String beforeSnapshot, String afterSnapshot, boolean success,
                                 long durationMs, String message) {
        try {
            jdbcTemplate.update("""
                    INSERT INTO sys_operation_log
                        (admin_user_id, admin_role_codes, method, request_uri, operation_type, high_risk,
                         business_ref, success, ip, user_agent, duration_ms, message, before_snapshot, after_snapshot)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, adminUserId, roleCodes(adminUserId), request.getMethod(), request.getRequestURI(),
                    operation.operationType(), operation.highRisk() ? 1 : 0, businessRef, success ? 1 : 0,
                    clientIp(request), truncateHeader(request.getHeader("User-Agent"), 512), durationMs, message,
                    truncateText(beforeSnapshot, 1024), truncateText(afterSnapshot, 1024));
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

    private String roleCodes(Long adminUserId) {
        String roles = jdbcTemplate.query("""
                SELECT GROUP_CONCAT(DISTINCT r.code ORDER BY r.code SEPARATOR ',')
                  FROM sys_admin_user_role ur
                  JOIN sys_role r ON r.id = ur.role_id AND r.deleted = b'0'
                 WHERE ur.admin_user_id = ?
                """, rs -> rs.next() ? rs.getString(1) : "", adminUserId);
        return truncateText(roles == null ? "" : roles, 255);
    }

    private String businessReference(HttpServletRequest request, Map<String, Object> body) {
        try {
            String value = firstNonBlank(request.getParameter("orderNo"), request.getParameter("orderSn"),
                    request.getParameter("orderId"), request.getParameter("afterSaleNo"),
                    request.getParameter("afterSaleId"), request.getParameter("spuId"),
                    request.getParameter("skuId"), request.getParameter("payOrderId"),
                    request.getParameter("exceptionId"), request.getParameter("differenceId"),
                    request.getParameter("ruleId"), request.getParameter("id"));
            if (value != null) return truncate(value);
            for (String key : List.of("orderNo", "orderSn", "orderId", "afterSaleNo", "afterSaleId",
                    "spuId", "skuId", "payOrderId", "exceptionId", "differenceId", "ruleId", "id")) {
                Object candidate = body.get(key);
                if (candidate != null && !String.valueOf(candidate).isBlank()) return truncate(String.valueOf(candidate));
            }
            Object spu = body.get("spu");
            if (spu instanceof Map<?, ?> spuMap && spuMap.get("id") != null) {
                return truncate(String.valueOf(spuMap.get("id")));
            }
            Object ids = body.get("ids");
            if (ids instanceof List<?> list && !list.isEmpty()) return truncate("ids=" + list);
            if (body.containsKey("content")) return "批量文件";
        } catch (Exception ignored) {
            // 业务审计只提取关联编号，不解析或记录完整请求内容。
        }
        return null;
    }

    private OperationDescriptor describeOperation(String method, String uri) {
        if (pathMatcher.match("/admin-api/system/admin-user/save", uri)) return highRisk("保存管理员");
        if (pathMatcher.match("/admin-api/system/admin-user/status", uri)) return highRisk("启停管理员");
        if (pathMatcher.match("/admin-api/system/admin-user/reset-password", uri)) return highRisk("重置管理员密码");
        if (pathMatcher.match("/admin-api/system/role/save", uri)) return highRisk("修改角色权限");
        if (pathMatcher.match("/admin-api/system/role/status", uri)) return highRisk("启停角色");
        if (pathMatcher.match("/admin-api/material/upload", uri)) return highRisk("上传商品素材");
        if (pathMatcher.match("/admin-api/material/delete", uri)) return highRisk("删除素材");
        if (pathMatcher.match("/admin-api/product/spu/import-confirm", uri)) return highRisk("导入商品");
        if (pathMatcher.match("/admin-api/product/spu/save", uri)
                || pathMatcher.match("/admin-api/product/spu/create", uri)
                || pathMatcher.match("/admin-api/product/spu/update", uri)) return highRisk("新建商品或修改商品价格");
        if (pathMatcher.match("/admin-api/product/spu/delete", uri)) return highRisk("删除商品");
        if (pathMatcher.match("/admin-api/product/spu/batch/status", uri)) return highRisk("批量上下架");
        if (pathMatcher.match("/admin-api/product/spu/batch/price", uri)) return highRisk("批量调价");
        if (pathMatcher.match("/admin-api/product/spu/batch/stock", uri)
                || pathMatcher.match("/admin-api/product/inventory/adjust", uri)) return highRisk("人工调整库存");
        if (pathMatcher.match("/admin-api/product/inventory/warning-stock", uri)) return highRisk("修改库存预警");
        if (pathMatcher.match("/admin-api/marketing/coupon-template/create", uri)
                || pathMatcher.match("/admin-api/marketing/coupon-template/update-status", uri)
                || pathMatcher.match("/admin-api/marketing/coupon-template/delete", uri)) return highRisk("创建或停用优惠券");
        if (pathMatcher.match("/admin-api/marketing/promotion/create", uri)
                || pathMatcher.match("/admin-api/marketing/promotion/update-status", uri)
                || pathMatcher.match("/admin-api/marketing/promotion/delete", uri)) return highRisk("创建或停用满减");
        if (pathMatcher.match("/admin-api/marketing/shipping/create", uri)
                || pathMatcher.match("/admin-api/marketing/shipping/update", uri)
                || pathMatcher.match("/admin-api/marketing/shipping/update-status", uri)) return highRisk("修改运费或包邮规则");
        if (pathMatcher.match("/admin-api/trade/order/ship", uri)) return highRisk("发货");
        if (pathMatcher.match("/admin-api/trade/order/batch-ship/import", uri)) return highRisk("批量发货");
        if (pathMatcher.match("/admin-api/trade/after-sale/approve", uri)) return highRisk("售后同意");
        if (pathMatcher.match("/admin-api/trade/after-sale/reject", uri)) return highRisk("售后拒绝");
        if (pathMatcher.match("/admin-api/trade/after-sale/receive", uri)) return highRisk("退货收货");
        if (pathMatcher.match("/admin-api/trade/pay/order/sync", uri)) return highRisk("人工同步支付");
        if (pathMatcher.match("/admin-api/trade/pay/exception/handle", uri)) return highRisk("处理支付异常");
        if (pathMatcher.match("/admin-api/trade/refund/sync", uri)
                || pathMatcher.match("/admin-api/trade/refund/retry", uri)) return highRisk("人工同步退款");
        if (pathMatcher.match("/admin-api/trade/refund/handle", uri)) return highRisk("处理退款异常");
        if (pathMatcher.match("/admin-api/trade/reconcile/run", uri)) return highRisk("手动触发对账");
        if (pathMatcher.match("/admin-api/trade/reconcile/difference/handle", uri)) return highRisk("标记对账差异");
        if (pathMatcher.match("/admin-api/trade/order/export", uri)) return highRisk("导出订单");
        if (pathMatcher.match("/admin-api/trade/reconcile/export", uri)) return highRisk("导出对账");
        if (!"GET".equalsIgnoreCase(method)) return new OperationDescriptor("后台写操作", false);
        return new OperationDescriptor("后台查询", false);
    }

    private OperationDescriptor highRisk(String operationType) {
        return new OperationDescriptor(operationType, true);
    }

    private String entitySnapshot(OperationDescriptor operation, String businessRef, Map<String, Object> body) {
        String uri = operation.operationType();
        try {
            if (Set.of("保存管理员", "启停管理员", "重置管理员密码").contains(uri)) return orSummary(adminUserSnapshot(longValue(body, "id")), body);
            if (Set.of("修改角色权限", "启停角色").contains(uri)) return orSummary(roleSnapshot(longValue(body, "id")), body);
            if (Set.of("删除素材").contains(uri)) return orSummary(materialSnapshot(longValue(body, "id")), body);
            if (Set.of("新建商品或修改商品价格", "删除商品").contains(uri)) return orSummary(productSnapshot(firstLong(longValue(body, "id"), nestedLong(body, "spu", "id"))), body);
            if (Set.of("批量上下架", "批量调价", "人工调整库存").contains(uri) && body.get("ids") != null) return requestSummary(body);
            if (Set.of("人工调整库存", "修改库存预警").contains(uri)) return orSummary(skuSnapshot(longValue(body, "skuId")), body);
            if (Set.of("创建或停用优惠券").contains(uri)) return orSummary(marketingSnapshot("marketing_coupon_template", longValue(body, "id")), body);
            if (Set.of("创建或停用满减").contains(uri)) return orSummary(marketingSnapshot("marketing_promotion_rule", longValue(body, "id")), body);
            if (Set.of("修改运费或包邮规则").contains(uri)) return orSummary(shippingSnapshot(longValue(body, "id")), body);
            if (Set.of("发货", "导出订单").contains(uri)) return orSummary(orderSnapshot(firstLong(longValue(body, "orderId"), parseLong(businessRef))), body);
            if (Set.of("售后同意", "售后拒绝", "退货收货", "人工同步退款", "处理退款异常").contains(uri)) return orSummary(afterSaleSnapshot(longValue(body, "afterSaleId")), body);
            if (Set.of("人工同步支付").contains(uri)) return orSummary(payOrderSnapshot(longValue(body, "payOrderId")), body);
            if (Set.of("处理支付异常").contains(uri)) return orSummary(payExceptionSnapshot(longValue(body, "exceptionId")), body);
            if (Set.of("标记对账差异").contains(uri)) return orSummary(reconcileDifferenceSnapshot(longValue(body, "differenceId")), body);
        } catch (Exception ignored) {
            return requestSummary(body);
        }
        return requestSummary(body);
    }

    private String adminUserSnapshot(Long id) {
        if (id == null || id <= 0) return "";
        return queryOne("""
                SELECT u.id, u.username, u.nickname, u.status,
                       COALESCE(GROUP_CONCAT(r.code ORDER BY r.code SEPARATOR ','), '') roleCodes
                  FROM sys_admin_user u
                  LEFT JOIN sys_admin_user_role ur ON ur.admin_user_id = u.id
                  LEFT JOIN sys_role r ON r.id = ur.role_id AND r.deleted = b'0'
                 WHERE u.id = ? AND u.deleted = b'0'
                 GROUP BY u.id, u.username, u.nickname, u.status
                """, id);
    }

    private String roleSnapshot(Long id) {
        if (id == null || id <= 0) return "";
        return queryOne("""
                SELECT r.id, r.code, r.name, r.status,
                       COALESCE(GROUP_CONCAT(p.code ORDER BY p.code SEPARATOR ','), '') permissionCodes
                  FROM sys_role r
                  LEFT JOIN sys_role_permission rp ON rp.role_id = r.id
                  LEFT JOIN sys_permission p ON p.id = rp.permission_id AND p.deleted = b'0'
                 WHERE r.id = ? AND r.deleted = b'0'
                 GROUP BY r.id, r.code, r.name, r.status
                """, id);
    }

    private String materialSnapshot(Long id) {
        if (id == null || id <= 0) return "";
        return queryOne("SELECT id, file_name, biz_type, reference_count, deleted FROM material_asset WHERE id = ?", id);
    }

    private String productSnapshot(Long id) {
        if (id == null || id <= 0) return "";
        return queryOne("""
                SELECT s.id, s.name, s.category_id categoryId, s.price, s.stock, s.status,
                       COUNT(k.id) skuCount, MIN(k.price) minSkuPrice, MAX(k.price) maxSkuPrice, SUM(k.stock) skuStock
                  FROM product_spu s LEFT JOIN product_sku k ON k.spu_id = s.id AND k.deleted = b'0'
                 WHERE s.id = ? AND s.deleted = b'0'
                 GROUP BY s.id, s.name, s.category_id, s.price, s.stock, s.status
                """, id);
    }

    private String skuSnapshot(Long id) {
        if (id == null || id <= 0) return "";
        return queryOne("SELECT id skuId, spu_id spuId, sku_code skuCode, price, stock, warning_stock warningStock FROM product_sku WHERE id = ? AND deleted = b'0'", id);
    }

    private String marketingSnapshot(String table, Long id) {
        if (id == null || id <= 0) return "";
        return queryOne("SELECT id, name, type, threshold_amount thresholdAmount, discount_amount discountAmount, status FROM " + table + " WHERE id = ? AND deleted = b'0'", id);
    }

    private String shippingSnapshot(Long id) {
        if (id == null || id <= 0) return "";
        return queryOne("SELECT id, name, free_threshold freeThreshold, base_fee baseFee, status, start_time startTime, end_time endTime FROM marketing_shipping_rule WHERE id = ? AND deleted = b'0'", id);
    }

    private String orderSnapshot(Long id) {
        if (id == null || id <= 0) return "";
        return queryOne("SELECT id, order_sn orderSn, status, pay_status payStatus, actual_price actualPrice, refunded_amount refundedAmount, admin_remark adminRemark FROM trade_order WHERE id = ? AND deleted = b'0'", id);
    }

    private String afterSaleSnapshot(Long id) {
        if (id == null || id <= 0) return "";
        return queryOne("SELECT id, after_sale_sn afterSaleSn, order_id orderId, status, refund_amount refundAmount, refund_channel_state refundChannelState, refund_exception_code refundExceptionCode, refund_handled refundHandled FROM trade_after_sale WHERE id = ? AND deleted = b'0'", id);
    }

    private String payOrderSnapshot(Long id) {
        if (id == null || id <= 0) return "";
        return queryOne("SELECT id, pay_sn paySn, order_id orderId, order_sn orderSn, status, amount, wechat_trade_state wechatTradeState, wechat_amount wechatAmount FROM pay_order WHERE id = ? AND deleted = b'0'", id);
    }

    private String payExceptionSnapshot(Long id) {
        if (id == null || id <= 0) return "";
        return queryOne("SELECT id, pay_order_id payOrderId, order_sn orderSn, pay_sn paySn, reason_code reasonCode, handled, local_status localStatus, wechat_trade_state wechatTradeState FROM pay_exception WHERE id = ? AND deleted = b'0'", id);
    }

    private String reconcileDifferenceSnapshot(Long id) {
        if (id == null || id <= 0) return "";
        return queryOne("SELECT id, batch_id batchId, diff_type diffType, business_type businessType, business_sn businessSn, order_sn orderSn, local_amount localAmount, channel_amount channelAmount, handled FROM trade_reconcile_difference WHERE id = ? AND deleted = b'0'", id);
    }

    private String queryOne(String sql, Object... args) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, args);
        if (rows.isEmpty()) return "";
        return toJson(rows.getFirst());
    }

    private Map<String, Object> parseBody(HttpServletRequest request) {
        try {
            if (request instanceof CachedBodyRequest cached && cached.body.length > 0) {
                return objectMapper.readValue(cached.body, Map.class);
            }
        } catch (Exception ignored) {
            return Map.of();
        }
        return Map.of();
    }

    private String requestSummary(Map<String, Object> body) {
        if (body == null || body.isEmpty()) return "";
        Map<String, Object> summary = new LinkedHashMap<>();
        for (String key : List.of("id", "ids", "orderId", "orderNo", "orderSn", "afterSaleId", "skuId",
                "payOrderId", "exceptionId", "differenceId", "status", "reason", "remark", "rejectReason",
                "receiveRemark", "confirmCount", "priceAdjustType", "priceAdjustValue", "stockDelta",
                "changeQuantity", "warningStock", "name", "type", "thresholdAmount", "discountAmount",
                "freeThreshold", "baseFee", "startTime", "endTime")) {
            if (body.containsKey(key)) summary.put(key, maskValue(key, body.get(key)));
        }
        Object spu = body.get("spu");
        if (spu instanceof Map<?, ?> spuMap) {
            Map<String, Object> spuSummary = new LinkedHashMap<>();
            for (String key : List.of("id", "name", "categoryId", "price", "stock", "status", "sort")) {
                if (spuMap.containsKey(key)) spuSummary.put(key, maskValue(key, spuMap.get(key)));
            }
            summary.put("spu", spuSummary);
        }
        return toJson(summary);
    }

    private String orSummary(String snapshot, Map<String, Object> body) {
        return snapshot == null || snapshot.isBlank() ? requestSummary(body) : snapshot;
    }

    private String toJson(Object value) {
        try {
            return truncateText(objectMapper.writeValueAsString(value), 1024);
        } catch (Exception ignored) {
            return "";
        }
    }

    private Object maskValue(String key, Object value) {
        if (value == null) return null;
        String lower = key.toLowerCase();
        if (lower.contains("password") || lower.contains("token") || lower.contains("secret")
                || lower.contains("session") || lower.contains("key") || lower.contains("certificate")) return "***";
        String text = String.valueOf(value);
        if (lower.contains("mobile") || lower.contains("phone")) return text.length() < 7 ? "***" : text.substring(0, 3) + "****" + text.substring(text.length() - 4);
        if (lower.contains("address") || lower.contains("openid")) return "***";
        return value;
    }

    private boolean shouldCacheBody(HttpServletRequest request) {
        String contentType = request.getContentType();
        if (contentType == null) return false;
        String lower = contentType.toLowerCase();
        return lower.contains(MediaType.APPLICATION_JSON_VALUE);
    }

    private Long longValue(Map<String, Object> body, String key) {
        return parseLong(body.get(key));
    }

    private Long nestedLong(Map<String, Object> body, String parent, String key) {
        Object value = body.get(parent);
        if (value instanceof Map<?, ?> map) return parseLong(map.get(key));
        return null;
    }

    private Long firstLong(Long... values) {
        for (Long value : values) if (value != null && value > 0) return value;
        return null;
    }

    private Long parseLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.longValue();
        try {
            String text = String.valueOf(value);
            if (text == null || text.isBlank() || text.startsWith("ids=")) return null;
            return Long.parseLong(text.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value;
        return null;
    }

    private String truncate(String value) {
        return value.length() <= 128 ? value : value.substring(0, 128);
    }

    private String truncateHeader(String value, int maxLength) {
        return value == null ? "" : truncateText(value, maxLength);
    }

    private String truncateText(String value, int maxLength) {
        if (value == null) return "";
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private record PermissionRule(String pathPattern, String httpMethod) {
    }

    private record OperationDescriptor(String operationType, boolean highRisk) {
    }

    private static class CachedBodyRequest extends HttpServletRequestWrapper {
        private final byte[] body;

        CachedBodyRequest(HttpServletRequest request) throws IOException {
            super(request);
            this.body = request.getInputStream().readAllBytes();
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream input = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public int read() {
                    return input.read();
                }

                @Override
                public boolean isFinished() {
                    return input.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
