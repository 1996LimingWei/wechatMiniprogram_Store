package com.shop.module.trade.service;

import com.shop.module.trade.vo.ObservabilitySummaryRespVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradeObservabilityService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    public ObservabilitySummaryRespVO getSummary(String orderSn) {
        List<ObservabilitySummaryRespVO.HealthItem> health = checkHealth();
        refreshAlerts(health);
        ObservabilitySummaryRespVO result = new ObservabilitySummaryRespVO();
        result.setHealth(health);
        result.setMetrics(buildMetrics());
        result.setAlerts(listActiveAlerts());
        result.setJobs(listJobs());
        if (hasText(orderSn)) {
            result.setOrderTrace(traceOrder(orderSn.trim()));
        }
        return result;
    }

    public List<ObservabilitySummaryRespVO.AlertItem> getActiveAlerts() {
        refreshAlerts(checkHealth());
        return listActiveAlerts();
    }

    public ObservabilitySummaryRespVO.OrderTrace traceOrder(String orderSn) {
        if (!hasText(orderSn)) {
            return null;
        }
        List<Map<String, Object>> orders = jdbcTemplate.queryForList("""
                SELECT id, order_sn, user_id, status, pay_status
                  FROM trade_order
                 WHERE order_sn = ? AND deleted = b'0'
                 LIMIT 1
                """, orderSn.trim());
        if (orders.isEmpty()) {
            return null;
        }
        Map<String, Object> order = orders.get(0);
        Long orderId = longValue(order.get("id"));
        ObservabilitySummaryRespVO.OrderTrace trace = new ObservabilitySummaryRespVO.OrderTrace();
        trace.setOrderId(orderId);
        trace.setOrderSn(text(order.get("order_sn")));
        trace.setUserId(longValue(order.get("user_id")));
        trace.setOrderStatus(intValue(order.get("status")));
        trace.setPayStatus(intValue(order.get("pay_status")));
        fillPayAndRefundTrace(trace, orderId);
        trace.setTradeLogs(listTradeLogs(orderId));
        List<ObservabilitySummaryRespVO.TraceItem> payLogs = new ArrayList<>(listPayLogs(trace.getPaySn()));
        payLogs.addAll(listRefundNotifyLogs(trace.getAfterSaleSn()));
        trace.setPayLogs(payLogs);
        trace.setAuditLogs(listAuditLogs(trace));
        return trace;
    }

    public void recordJobResult(String jobName, boolean success, int processedCount, String message) {
        String safeJobName = trim(jobName, 64);
        String safeMessage = trim(message, 255);
        Integer existing = jdbcTemplate.query("""
                SELECT COUNT(*) FROM sys_job_execution_metric WHERE job_name = ? AND deleted = b'0'
                """, rs -> rs.next() ? rs.getInt(1) : 0, safeJobName);
        if (existing != null && existing > 0) {
            jdbcTemplate.update("""
                    UPDATE sys_job_execution_metric
                       SET last_status = ?, last_message = ?, processed_count = ?,
                           success_count = success_count + ?,
                           failure_count = failure_count + ?,
                           consecutive_failures = CASE WHEN ? = 1 THEN 0 ELSE consecutive_failures + 1 END,
                           last_run_time = NOW()
                     WHERE job_name = ? AND deleted = b'0'
                    """, success ? "SUCCESS" : "FAIL", safeMessage, processedCount,
                    success ? 1 : 0, success ? 0 : 1, success ? 1 : 0, safeJobName);
            return;
        }
        jdbcTemplate.update("""
                INSERT INTO sys_job_execution_metric
                    (job_name, last_status, last_message, processed_count, success_count,
                     failure_count, consecutive_failures, last_run_time)
                VALUES (?, ?, ?, ?, ?, ?, ?, NOW())
                """, safeJobName, success ? "SUCCESS" : "FAIL", safeMessage, processedCount,
                success ? 1 : 0, success ? 0 : 1, success ? 0 : 1);
    }

    private List<ObservabilitySummaryRespVO.HealthItem> checkHealth() {
        List<ObservabilitySummaryRespVO.HealthItem> result = new ArrayList<>();
        result.add(checkDb());
        result.add(checkRedis());
        return result;
    }

    private ObservabilitySummaryRespVO.HealthItem checkDb() {
        ObservabilitySummaryRespVO.HealthItem item = health("database", "UP", "数据库连接正常");
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        } catch (Exception exception) {
            item.setStatus("DOWN");
            item.setMessage("数据库不可用：" + trim(exception.getMessage(), 120));
        }
        return item;
    }

    private ObservabilitySummaryRespVO.HealthItem checkRedis() {
        ObservabilitySummaryRespVO.HealthItem item = health("redis", "UP", "Redis 连接正常");
        try {
            StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
            if (redisTemplate == null || redisTemplate.getConnectionFactory() == null) {
                item.setStatus("DOWN");
                item.setMessage("Redis 客户端未初始化");
                return item;
            }
            try (var connection = redisTemplate.getConnectionFactory().getConnection()) {
                String pong = connection.ping();
                if (!"PONG".equalsIgnoreCase(pong)) {
                    item.setStatus("DOWN");
                    item.setMessage("Redis PING 异常：" + pong);
                }
            }
        } catch (Exception exception) {
            item.setStatus("DOWN");
            item.setMessage("Redis 不可用：" + trim(exception.getMessage(), 120));
        }
        return item;
    }

    private ObservabilitySummaryRespVO.HealthItem health(String component, String status, String message) {
        ObservabilitySummaryRespVO.HealthItem item = new ObservabilitySummaryRespVO.HealthItem();
        item.setComponent(component);
        item.setStatus(status);
        item.setMessage(message);
        item.setLastCheckTime(now());
        return item;
    }

    private List<ObservabilitySummaryRespVO.MetricItem> buildMetrics() {
        List<ObservabilitySummaryRespVO.MetricItem> metrics = new ArrayList<>();
        metrics.add(metric("order_success", "下单成功数", count("""
                SELECT COUNT(*) FROM trade_order
                 WHERE deleted = b'0' AND create_time >= CURDATE()
                """), "单", "INFO"));
        metrics.add(metric("order_failure", "下单失败数", count("""
                SELECT COUNT(*) FROM trade_order
                 WHERE deleted = b'0' AND status = 4 AND create_time >= CURDATE()
                """), "单", "WARN"));
        metrics.add(metric("payment_success", "支付成功数", count("""
                SELECT COUNT(*) FROM pay_order
                 WHERE deleted = b'0' AND status = 1 AND pay_time >= CURDATE()
                """), "笔", "INFO"));
        metrics.add(metric("payment_failure", "支付失败数", count("""
                SELECT COUNT(*) FROM pay_notify_failure_log
                 WHERE create_time >= CURDATE()
                """), "次", "WARN"));
        metrics.add(metric("payment_exception", "支付异常数", count("""
                SELECT COUNT(*) FROM pay_exception
                 WHERE deleted = b'0' AND handled = 0
                """), "个", "WARN"));
        metrics.add(metric("refund_success", "退款成功数", count("""
                SELECT COUNT(*) FROM trade_after_sale
                 WHERE deleted = b'0' AND status = 1 AND refund_time >= CURDATE()
                """), "笔", "INFO"));
        metrics.add(metric("refund_failure", "退款失败数", count("""
                SELECT (
                    (SELECT COUNT(*) FROM trade_after_sale
                      WHERE deleted = b'0' AND status = 5 AND update_time >= CURDATE())
                    +
                    (SELECT COUNT(*) FROM refund_notify_failure_log
                      WHERE create_time >= CURDATE())
                )
                """), "笔", "WARN"));
        metrics.add(metric("refund_exception", "退款异常数", count("""
                SELECT COUNT(*) FROM trade_after_sale
                 WHERE deleted = b'0' AND refund_exception_code <> '' AND refund_handled = 0
                """), "个", "WARN"));
        metrics.add(metric("reconcile_difference", "对账差异数", count("""
                SELECT COUNT(*) FROM trade_reconcile_difference
                 WHERE deleted = b'0' AND handled = 0 AND diff_type <> 'BALANCED'
                """), "条", "WARN"));
        metrics.add(metric("stock_deduct_failure", "库存扣减失败数", count("""
                SELECT COUNT(*) FROM sys_operation_log
                 WHERE success = 0 AND message LIKE '%库存%' AND create_time >= CURDATE()
                """), "次", "WARN"));
        metrics.add(metric("job_failure", "定时任务失败数", count("""
                SELECT COUNT(*) FROM sys_job_execution_metric
                 WHERE deleted = b'0' AND last_status = 'FAIL'
                """), "个", "WARN"));
        return metrics;
    }

    private void refreshAlerts(List<ObservabilitySummaryRespVO.HealthItem> health) {
        Set<String> activeTypes = new LinkedHashSet<>();
        for (ObservabilitySummaryRespVO.HealthItem item : health) {
            if ("DOWN".equals(item.getStatus())) {
                String type = "database".equals(item.getComponent()) ? "DB_UNAVAILABLE" : "REDIS_UNAVAILABLE";
                activeTypes.add(type);
                upsertAlert(type, "CRITICAL", item.getComponent() + "不可用", item.getMessage(), "", 1, 1);
            }
        }
        activateWhenPositive(activeTypes, "PAY_CALLBACK_FAILURE", "支付回调失败",
                "近 10 分钟存在支付回调验签或处理失败", count("""
                        SELECT COUNT(*) FROM pay_notify_failure_log
                         WHERE create_time >= DATE_SUB(NOW(), INTERVAL 10 MINUTE)
                        """));
        activateWhenPositive(activeTypes, "REFUND_CALLBACK_FAILURE", "退款回调或同步失败",
                "近 30 分钟存在退款渠道失败或查询失败", count("""
                        SELECT (
                            (SELECT COUNT(*) FROM refund_notify_failure_log
                              WHERE create_time >= DATE_SUB(NOW(), INTERVAL 30 MINUTE))
                            +
                            (SELECT COUNT(*) FROM trade_after_sale
                              WHERE deleted = b'0'
                                AND refund_exception_code IN ('REFUND_FAILED', 'REFUND_QUERY_FAILED', 'REFUND_NOTIFY_FAILED')
                                AND update_time >= DATE_SUB(NOW(), INTERVAL 30 MINUTE))
                        )
                        """));
        activateWhenPositive(activeTypes, "PAY_EXCEPTION_BACKLOG", "支付异常积压",
                "存在未处理支付异常", count("""
                        SELECT COUNT(*) FROM pay_exception
                         WHERE deleted = b'0' AND handled = 0
                        """));
        activateWhenPositive(activeTypes, "REFUND_EXCEPTION_BACKLOG", "退款异常积压",
                "存在未处理退款异常", count("""
                        SELECT COUNT(*) FROM trade_after_sale
                         WHERE deleted = b'0' AND refund_exception_code <> '' AND refund_handled = 0
                        """));
        activateWhenPositive(activeTypes, "RECONCILE_DIFFERENCE", "对账差异",
                "存在未处理日终对账差异", count("""
                        SELECT COUNT(*) FROM trade_reconcile_difference
                         WHERE deleted = b'0' AND handled = 0 AND diff_type <> 'BALANCED'
                        """));
        activateWhenPositive(activeTypes, "JOB_CONSECUTIVE_FAILURE", "任务连续失败",
                "存在连续失败 3 次及以上的定时任务", count("""
                        SELECT COUNT(*) FROM sys_job_execution_metric
                         WHERE deleted = b'0' AND consecutive_failures >= 3
                        """));
        resolveInactiveAlerts(activeTypes);
    }

    private void activateWhenPositive(Set<String> activeTypes, String type, String title, String message, int currentValue) {
        if (currentValue > 0) {
            activeTypes.add(type);
            upsertAlert(type, "WARN", title, message, "", currentValue, 1);
        }
    }

    private void upsertAlert(String type, String level, String title, String message,
                             String businessRef, int currentValue, int threshold) {
        Long activeId = jdbcTemplate.query("""
                SELECT id FROM sys_observability_alert
                 WHERE alert_type = ? AND business_ref = ? AND status = 0 AND deleted = b'0'
                 ORDER BY id DESC LIMIT 1
                """, rs -> rs.next() ? rs.getLong("id") : null, type, businessRef);
        if (activeId != null) {
            jdbcTemplate.update("""
                    UPDATE sys_observability_alert
                       SET level = ?, title = ?, message = ?, current_value = ?,
                           threshold_value = ?, last_trigger_time = NOW(),
                           trigger_count = trigger_count + 1
                     WHERE id = ?
                    """, level, title, trim(message, 512), currentValue, threshold, activeId);
            return;
        }
        jdbcTemplate.update("""
                INSERT INTO sys_observability_alert
                    (alert_type, level, title, message, business_ref, current_value, threshold_value)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, type, level, title, trim(message, 512), trim(businessRef, 128), currentValue, threshold);
    }

    private void resolveInactiveAlerts(Set<String> activeTypes) {
        List<String> knownTypes = List.of("DB_UNAVAILABLE", "REDIS_UNAVAILABLE", "PAY_CALLBACK_FAILURE",
                "REFUND_CALLBACK_FAILURE", "PAY_EXCEPTION_BACKLOG", "REFUND_EXCEPTION_BACKLOG",
                "RECONCILE_DIFFERENCE", "JOB_CONSECUTIVE_FAILURE");
        for (String type : knownTypes) {
            if (!activeTypes.contains(type)) {
                jdbcTemplate.update("""
                        UPDATE sys_observability_alert
                           SET status = 1, resolve_time = NOW()
                         WHERE alert_type = ? AND status = 0 AND deleted = b'0'
                        """, type);
            }
        }
    }

    private List<ObservabilitySummaryRespVO.AlertItem> listActiveAlerts() {
        return jdbcTemplate.queryForList("""
                SELECT id, alert_type, level, title, message, business_ref, current_value,
                       threshold_value, status, first_trigger_time, last_trigger_time, trigger_count
                  FROM sys_observability_alert
                 WHERE deleted = b'0' AND status = 0
                 ORDER BY FIELD(level, 'CRITICAL', 'WARN', 'INFO'), last_trigger_time DESC, id DESC
                """).stream().map(this::toAlert).toList();
    }

    private List<ObservabilitySummaryRespVO.JobItem> listJobs() {
        return jdbcTemplate.queryForList("""
                SELECT job_name, last_status, last_message, processed_count, success_count,
                       failure_count, consecutive_failures, last_run_time
                  FROM sys_job_execution_metric
                 WHERE deleted = b'0'
                 ORDER BY last_run_time DESC, job_name
                """).stream().map(this::toJob).toList();
    }

    private void fillPayAndRefundTrace(ObservabilitySummaryRespVO.OrderTrace trace, Long orderId) {
        List<Map<String, Object>> pays = jdbcTemplate.queryForList("""
                SELECT pay_sn FROM pay_order
                 WHERE order_id = ? AND deleted = b'0'
                 ORDER BY update_time DESC, id DESC LIMIT 1
                """, orderId);
        if (!pays.isEmpty()) {
            trace.setPaySn(text(pays.get(0).get("pay_sn")));
        }
        List<Map<String, Object>> refunds = jdbcTemplate.queryForList("""
                SELECT after_sale_sn, provider_refund_no FROM trade_after_sale
                 WHERE order_id = ? AND deleted = b'0'
                 ORDER BY update_time DESC, id DESC LIMIT 1
                """, orderId);
        if (!refunds.isEmpty()) {
            trace.setAfterSaleSn(text(refunds.get(0).get("after_sale_sn")));
            trace.setProviderRefundNo(text(refunds.get(0).get("provider_refund_no")));
        }
    }

    private List<ObservabilitySummaryRespVO.TraceItem> listTradeLogs(Long orderId) {
        return jdbcTemplate.queryForList("""
                SELECT create_time, action, remark, from_status, to_status, from_pay_status, to_pay_status
                  FROM trade_order_log
                 WHERE order_id = ? AND deleted = b'0'
                 ORDER BY create_time ASC, id ASC
                """, orderId).stream().map(row -> traceItem(
                "订单日志", "", text(row.get("action")),
                "订单 " + text(row.get("from_status")) + "->" + text(row.get("to_status"))
                        + "，支付 " + text(row.get("from_pay_status")) + "->" + text(row.get("to_pay_status"))
                        + "，" + text(row.get("remark")),
                row.get("create_time"))).toList();
    }

    private List<ObservabilitySummaryRespVO.TraceItem> listPayLogs(String paySn) {
        if (!hasText(paySn)) {
            return List.of();
        }
        List<ObservabilitySummaryRespVO.TraceItem> logs = new ArrayList<>();
        logs.addAll(jdbcTemplate.queryForList("""
                SELECT create_time, notification_id, event_type, status, message
                  FROM pay_notify_log
                 WHERE pay_sn = ? AND deleted = b'0'
                 ORDER BY create_time ASC, id ASC
                """, paySn).stream().map(row -> traceItem("支付回调", text(row.get("notification_id")),
                text(row.get("status")), text(row.get("event_type")) + " " + text(row.get("message")),
                row.get("create_time"))).toList());
        logs.addAll(jdbcTemplate.queryForList("""
                SELECT create_time, notification_id, error_message
                  FROM pay_notify_failure_log
                 WHERE pay_sn = ?
                 ORDER BY create_time ASC, id ASC
                """, paySn).stream().map(row -> traceItem("支付回调失败", text(row.get("notification_id")),
                "FAIL", text(row.get("error_message")), row.get("create_time"))).toList());
        logs.addAll(jdbcTemplate.queryForList("""
                SELECT create_time, reason_code, reason, handled
                  FROM pay_exception
                 WHERE pay_sn = ? AND deleted = b'0'
                 ORDER BY create_time ASC, id ASC
                """, paySn).stream().map(row -> traceItem("支付异常", text(row.get("reason_code")),
                intValue(row.get("handled")) == 1 ? "已处理" : "待处理", text(row.get("reason")),
                row.get("create_time"))).toList());
        return logs;
    }

    private List<ObservabilitySummaryRespVO.TraceItem> listRefundNotifyLogs(String afterSaleSn) {
        if (!hasText(afterSaleSn)) {
            return List.of();
        }
        List<ObservabilitySummaryRespVO.TraceItem> logs = new ArrayList<>();
        logs.addAll(jdbcTemplate.queryForList("""
                SELECT create_time, notification_id, event_type, refund_status, message
                  FROM refund_notify_log
                 WHERE after_sale_sn = ? AND deleted = b'0'
                 ORDER BY create_time ASC, id ASC
                """, afterSaleSn).stream().map(row -> traceItem("退款回调", text(row.get("notification_id")),
                text(row.get("refund_status")), text(row.get("event_type")) + " " + text(row.get("message")),
                row.get("create_time"))).toList());
        logs.addAll(jdbcTemplate.queryForList("""
                SELECT create_time, notification_id, error_message
                  FROM refund_notify_failure_log
                 WHERE after_sale_sn = ?
                 ORDER BY create_time ASC, id ASC
                """, afterSaleSn).stream().map(row -> traceItem("退款回调失败", text(row.get("notification_id")),
                "FAIL", text(row.get("error_message")), row.get("create_time"))).toList());
        return logs;
    }

    private List<ObservabilitySummaryRespVO.TraceItem> listAuditLogs(ObservabilitySummaryRespVO.OrderTrace trace) {
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE deleted = b'0' AND (business_ref = ?");
        args.add(trace.getOrderSn());
        if (hasText(trace.getPaySn())) {
            where.append(" OR business_ref = ?");
            args.add(trace.getPaySn());
        }
        if (hasText(trace.getAfterSaleSn())) {
            where.append(" OR business_ref = ?");
            args.add(trace.getAfterSaleSn());
        }
        where.append(")");
        return jdbcTemplate.queryForList("""
                SELECT create_time, operation_type, request_uri, success, message
                  FROM sys_operation_log
                """ + where + " ORDER BY create_time ASC, id ASC LIMIT 50", args.toArray())
                .stream().map(row -> traceItem("后台审计", text(row.get("request_uri")),
                        intValue(row.get("success")) == 1 ? "成功" : "失败",
                        text(row.get("operation_type")) + " " + text(row.get("message")),
                        row.get("create_time"))).toList();
    }

    private ObservabilitySummaryRespVO.MetricItem metric(String code, String label, int value, String unit, String level) {
        ObservabilitySummaryRespVO.MetricItem item = new ObservabilitySummaryRespVO.MetricItem();
        item.setCode(code);
        item.setLabel(label);
        item.setValue(value);
        item.setUnit(unit);
        item.setLevel(value > 0 ? level : "INFO");
        return item;
    }

    private ObservabilitySummaryRespVO.AlertItem toAlert(Map<String, Object> row) {
        ObservabilitySummaryRespVO.AlertItem item = new ObservabilitySummaryRespVO.AlertItem();
        item.setId(longValue(row.get("id")));
        item.setAlertType(text(row.get("alert_type")));
        item.setLevel(text(row.get("level")));
        item.setTitle(text(row.get("title")));
        item.setMessage(text(row.get("message")));
        item.setBusinessRef(text(row.get("business_ref")));
        item.setCurrentValue(intValue(row.get("current_value")));
        item.setThresholdValue(intValue(row.get("threshold_value")));
        item.setStatus(intValue(row.get("status")));
        item.setFirstTriggerTime(format(row.get("first_trigger_time")));
        item.setLastTriggerTime(format(row.get("last_trigger_time")));
        item.setTriggerCount(intValue(row.get("trigger_count")));
        return item;
    }

    private ObservabilitySummaryRespVO.JobItem toJob(Map<String, Object> row) {
        ObservabilitySummaryRespVO.JobItem item = new ObservabilitySummaryRespVO.JobItem();
        item.setJobName(text(row.get("job_name")));
        item.setLastStatus(text(row.get("last_status")));
        item.setLastMessage(text(row.get("last_message")));
        item.setProcessedCount(intValue(row.get("processed_count")));
        item.setSuccessCount(intValue(row.get("success_count")));
        item.setFailureCount(intValue(row.get("failure_count")));
        item.setConsecutiveFailures(intValue(row.get("consecutive_failures")));
        item.setLastRunTime(format(row.get("last_run_time")));
        return item;
    }

    private ObservabilitySummaryRespVO.TraceItem traceItem(String type, String ref, String status,
                                                           String message, Object time) {
        ObservabilitySummaryRespVO.TraceItem item = new ObservabilitySummaryRespVO.TraceItem();
        item.setTime(format(time));
        item.setType(type);
        item.setRef(ref);
        item.setStatus(status);
        item.setMessage(trim(message, 255));
        return item;
    }

    private int count(String sql) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class);
        return value == null ? 0 : value;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isBlank();
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String trim(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String normalized = value.replaceAll("[\\r\\n\\t]", " ").trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    private Integer intValue(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private Long longValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private String now() {
        return LocalDateTime.now().format(TIME_FORMATTER);
    }

    private String format(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().format(TIME_FORMATTER);
        }
        if (value instanceof LocalDateTime time) {
            return time.format(TIME_FORMATTER);
        }
        return String.valueOf(value);
    }
}
