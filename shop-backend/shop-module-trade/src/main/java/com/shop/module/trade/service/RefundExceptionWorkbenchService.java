package com.shop.module.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.shop.common.exception.ServerException;
import com.shop.common.pojo.PageResult;
import com.shop.module.trade.dal.dataobject.PayOrderDO;
import com.shop.module.trade.dal.dataobject.TradeAfterSaleDO;
import com.shop.module.trade.dal.dataobject.TradeOrderDO;
import com.shop.module.trade.dal.mysql.PayOrderMapper;
import com.shop.module.trade.dal.mysql.TradeAfterSaleMapper;
import com.shop.module.trade.dal.mysql.TradeOrderMapper;
import com.shop.module.trade.service.provider.TradeRefundProvider;
import com.shop.module.trade.service.provider.TradeRefundProviderService;
import com.shop.module.trade.util.TradeMoneyUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RefundExceptionWorkbenchService {

    public static final String REASON_REFUND_FAILED = "REFUND_FAILED";
    public static final String REASON_REFUND_TIMEOUT = "REFUND_TIMEOUT";
    public static final String REASON_LOCAL_SUCCESS_CHANNEL_NOT_SUCCESS = "LOCAL_SUCCESS_CHANNEL_NOT_SUCCESS";
    public static final String REASON_QUERY_FAILED = "REFUND_QUERY_FAILED";
    public static final String REASON_RETRY_EXHAUSTED = "RETRY_EXHAUSTED";

    private static final int MAX_PAGE_SIZE = 100;
    private static final int STATUS_REFUNDED = 1;
    private static final int STATUS_REFUNDING = 4;
    private static final int STATUS_REFUND_FAILED = 5;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter QUERY_TIME_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("uuuu-MM-dd HH:mm:ss")
            .toFormatter()
            .withResolverStyle(ResolverStyle.STRICT);

    private final JdbcTemplate jdbcTemplate;
    private final TradeAfterSaleMapper tradeAfterSaleMapper;
    private final TradeOrderMapper tradeOrderMapper;
    private final PayOrderMapper payOrderMapper;
    private final TradeRefundExecutionService tradeRefundExecutionService;
    private final TradeRefundProviderService tradeRefundProviderService;

    @Value("${trade.refund.max-auto-attempts:12}")
    private int maxAutoAttempts = 12;

    @Value("${trade.refund.exception-timeout-minutes:30}")
    private int exceptionTimeoutMinutes = 30;

    public PageResult<Map<String, Object>> getRefundPage(int page, int size, String refundSn,
                                                          String afterSaleSn, String orderSn,
                                                          Integer status, Integer exceptionOnly,
                                                          String createTimeStart, String createTimeEnd) {
        int finalPage = normalizePage(page);
        int finalSize = normalizeSize(size);
        LocalDateTime start = parseTime(createTimeStart, "createTimeStart");
        LocalDateTime end = parseTime(createTimeEnd, "createTimeEnd");
        validateTimeRange(start, end);

        StringBuilder where = new StringBuilder(" WHERE a.deleted = b'0'");
        List<Object> args = new ArrayList<>();
        if (hasText(refundSn)) {
            where.append(" AND (a.provider_refund_no = ? OR a.after_sale_sn = ?)");
            args.add(refundSn.trim());
            args.add(refundSn.trim());
        }
        if (hasText(afterSaleSn)) {
            where.append(" AND a.after_sale_sn = ?");
            args.add(afterSaleSn.trim());
        }
        if (hasText(orderSn)) {
            where.append(" AND o.order_sn = ?");
            args.add(orderSn.trim());
        }
        if (status != null) {
            where.append(" AND a.status = ?");
            args.add(status);
        }
        if (exceptionOnly != null && exceptionOnly == 1) {
            where.append("""
                     AND (a.status = 5
                          OR a.refund_exception_code <> ''
                          OR (a.status = 4 AND a.refund_attempt_count >= ?)
                          OR (a.status = 4 AND COALESCE(a.refund_last_attempt_time, a.audit_time, a.create_time) < ?))
                    """);
            args.add(Math.max(maxAutoAttempts, 1));
            args.add(Timestamp.valueOf(LocalDateTime.now().minusMinutes(Math.max(exceptionTimeoutMinutes, 1))));
        }
        if (start != null) {
            where.append(" AND a.create_time >= ?");
            args.add(Timestamp.valueOf(start));
        }
        if (end != null) {
            where.append(" AND a.create_time < ?");
            args.add(Timestamp.valueOf(end));
        }

        Long total = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM trade_after_sale a
                  LEFT JOIN trade_order o ON o.id = a.order_id
                """ + where, Long.class, args.toArray());
        args.add((finalPage - 1) * finalSize);
        args.add(finalSize);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(baseSelect() + where
                + " ORDER BY a.create_time DESC, a.id DESC LIMIT ?, ?", args.toArray());
        return new PageResult<>(rows.stream().map(this::toRefundResp).toList(), total == null ? 0 : total);
    }

    public Map<String, Object> getRefundDetail(Long afterSaleId) {
        if (afterSaleId == null || afterSaleId <= 0) {
            throw new ServerException(400, "售后单ID不能为空");
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(baseSelect()
                + " WHERE a.deleted = b'0' AND a.id = ? LIMIT 1", afterSaleId);
        if (rows.isEmpty()) {
            throw new ServerException(404, "售后单不存在");
        }
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("refund", toRefundResp(rows.get(0)));
        detail.put("taskRecords", buildTaskRecords(rows.get(0)));
        detail.put("callbackRecords", buildCallbackRecords(text(rows.get(0).get("after_sale_sn"))));
        return detail;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> syncRefund(Long adminId, Long afterSaleId) {
        requireAdmin(adminId);
        TradeAfterSaleDO afterSale = getAfterSale(afterSaleId);
        if (Integer.valueOf(STATUS_REFUNDING).equals(afterSale.getStatus())) {
            tradeRefundExecutionService.execute(afterSaleId, TradeOrderLogService.OPERATOR_ADMIN, adminId, true);
            evaluateException(getAfterSale(afterSaleId));
            return getRefundDetail(afterSaleId);
        }
        if (hasText(afterSale.getProviderRefundNo())) {
            queryAndApplyChannelStatus(afterSale, adminId);
            return getRefundDetail(afterSaleId);
        }
        evaluateException(afterSale);
        return getRefundDetail(afterSaleId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> retryRefund(Long adminId, Long afterSaleId) {
        requireAdmin(adminId);
        TradeAfterSaleDO afterSale = getAfterSale(afterSaleId);
        if (!Integer.valueOf(STATUS_REFUND_FAILED).equals(afterSale.getStatus())
                && !Integer.valueOf(STATUS_REFUNDING).equals(afterSale.getStatus())) {
            throw new ServerException(400, "当前退款状态不允许重试");
        }
        TradeOrderDO order = tradeOrderMapper.selectById(afterSale.getOrderId());
        PayOrderDO payOrder = getLatestPayOrder(order);
        validateRefundAmount(afterSale, payOrder);
        if (Integer.valueOf(STATUS_REFUND_FAILED).equals(afterSale.getStatus())) {
            tradeOrderMapper.update(null, new LambdaUpdateWrapper<TradeOrderDO>()
                    .eq(TradeOrderDO::getId, order.getId())
                    .eq(TradeOrderDO::getPayStatus, TradeOrderPayStatus.PAID)
                    .set(TradeOrderDO::getStatus, 5));
            tradeAfterSaleMapper.update(null, new LambdaUpdateWrapper<TradeAfterSaleDO>()
                    .eq(TradeAfterSaleDO::getId, afterSaleId)
                    .eq(TradeAfterSaleDO::getStatus, STATUS_REFUND_FAILED)
                    .set(TradeAfterSaleDO::getStatus, STATUS_REFUNDING)
                    .set(TradeAfterSaleDO::getRefundMessage, "人工重试退款任务已提交")
                    .set(TradeAfterSaleDO::getRefundNextAttemptTime, null)
                    .set(TradeAfterSaleDO::getRefundClaimUntil, null)
                    .set(TradeAfterSaleDO::getRefundLastError, "")
                    .set(TradeAfterSaleDO::getRefundExceptionCode, "")
                    .set(TradeAfterSaleDO::getRefundExceptionMessage, "")
                    .set(TradeAfterSaleDO::getRefundHandled, 0));
        }
        tradeRefundExecutionService.execute(afterSaleId, TradeOrderLogService.OPERATOR_ADMIN, adminId, true);
        evaluateException(getAfterSale(afterSaleId));
        return getRefundDetail(afterSaleId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean handleRefund(Long adminId, Long afterSaleId, String remark) {
        requireAdmin(adminId);
        String finalRemark = remark == null ? "" : remark.trim();
        if (finalRemark.length() < 4 || finalRemark.length() > 200) {
            throw new ServerException(400, "处理备注长度应为 4 至 200 个字符");
        }
        int updated = tradeAfterSaleMapper.update(null, new LambdaUpdateWrapper<TradeAfterSaleDO>()
                .eq(TradeAfterSaleDO::getId, afterSaleId)
                .ne(TradeAfterSaleDO::getRefundExceptionCode, "")
                .set(TradeAfterSaleDO::getRefundHandled, 1)
                .set(TradeAfterSaleDO::getRefundHandleRemark, finalRemark)
                .set(TradeAfterSaleDO::getRefundHandleAdminId, adminId)
                .set(TradeAfterSaleDO::getRefundHandleTime, LocalDateTime.now()));
        if (updated != 1) {
            throw new ServerException(400, "退款异常不存在或已处理");
        }
        return true;
    }

    private void queryAndApplyChannelStatus(TradeAfterSaleDO afterSale, Long adminId) {
        TradeOrderDO order = tradeOrderMapper.selectById(afterSale.getOrderId());
        PayOrderDO payOrder = getLatestPayOrder(order);
        try {
            TradeRefundProvider.RefundResult result = tradeRefundProviderService.query(
                    new TradeRefundProvider.RefundQuery(afterSale.getAfterSaleSn(),
                            afterSale.getProviderRefundNo(), payOrder.getPaySn(), afterSale.getRefundAmount()));
            tradeAfterSaleMapper.update(null, new LambdaUpdateWrapper<TradeAfterSaleDO>()
                    .eq(TradeAfterSaleDO::getId, afterSale.getId())
                    .set(TradeAfterSaleDO::getRefundChannelState, result.status().name())
                    .set(TradeAfterSaleDO::getRefundMessage, result.message()));
            if (Integer.valueOf(STATUS_REFUNDING).equals(afterSale.getStatus())) {
                tradeRefundExecutionService.execute(afterSale.getId(),
                        TradeOrderLogService.OPERATOR_ADMIN, adminId, true);
            } else if (Integer.valueOf(STATUS_REFUNDED).equals(afterSale.getStatus())
                    && result.status() != TradeRefundProvider.RefundStatus.SUCCESS) {
                markException(afterSale.getId(), result.status().name(),
                        REASON_LOCAL_SUCCESS_CHANNEL_NOT_SUCCESS, "本地已退款但渠道未返回成功");
            }
        } catch (Exception exception) {
            markException(afterSale.getId(), "QUERY_FAILED", REASON_QUERY_FAILED,
                    safeMessage(exception));
        }
    }

    private void evaluateException(TradeAfterSaleDO afterSale) {
        if (afterSale == null) {
            return;
        }
        if (Integer.valueOf(STATUS_REFUND_FAILED).equals(afterSale.getStatus())) {
            markException(afterSale.getId(), state(afterSale), REASON_REFUND_FAILED,
                    hasText(afterSale.getRefundMessage()) ? afterSale.getRefundMessage() : "退款渠道返回失败");
            return;
        }
        if (Integer.valueOf(STATUS_REFUNDING).equals(afterSale.getStatus())) {
            int attempts = afterSale.getRefundAttemptCount() == null ? 0 : afterSale.getRefundAttemptCount();
            if (attempts >= Math.max(maxAutoAttempts, 1)) {
                markException(afterSale.getId(), state(afterSale), REASON_RETRY_EXHAUSTED, "自动退款重试次数已耗尽");
                return;
            }
            LocalDateTime referenceTime = firstTime(afterSale.getRefundLastAttemptTime(),
                    afterSale.getAuditTime(), afterSale.getCreateTime());
            if (referenceTime != null
                    && referenceTime.plusMinutes(Math.max(exceptionTimeoutMinutes, 1)).isBefore(LocalDateTime.now())) {
                markException(afterSale.getId(), state(afterSale), REASON_REFUND_TIMEOUT, "退款处理中超时");
            }
        }
    }

    private void markException(Long afterSaleId, String channelState, String code, String message) {
        tradeAfterSaleMapper.update(null, new LambdaUpdateWrapper<TradeAfterSaleDO>()
                .eq(TradeAfterSaleDO::getId, afterSaleId)
                .set(TradeAfterSaleDO::getRefundChannelState, channelState == null ? "" : trim(channelState, 32))
                .set(TradeAfterSaleDO::getRefundExceptionCode, code)
                .set(TradeAfterSaleDO::getRefundExceptionMessage, trim(message, 255))
                .set(TradeAfterSaleDO::getRefundHandled, 0));
    }

    private String baseSelect() {
        return """
                SELECT a.id, a.after_sale_sn, a.order_id, o.order_sn, a.user_id, a.type, a.status,
                       a.refund_amount, a.reason, a.refund_provider, a.provider_refund_no,
                       a.refund_message, a.refund_attempt_count, a.refund_last_attempt_time,
                       a.refund_next_attempt_time, a.refund_claim_until, a.refund_last_error,
                       a.refund_channel_state, a.refund_exception_code, a.refund_exception_message,
                       a.refund_handled, a.refund_handle_remark, a.refund_handle_admin_id,
                       a.refund_handle_time, a.apply_time, a.audit_time, a.refund_time,
                       a.create_time, p.pay_sn, p.amount AS pay_amount, p.refunded_amount AS pay_refunded_amount
                  FROM trade_after_sale a
                  LEFT JOIN trade_order o ON o.id = a.order_id
                  LEFT JOIN pay_order p ON p.order_id = a.order_id AND p.user_id = a.user_id
                """;
    }

    private Map<String, Object> toRefundResp(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", longValue(row.get("id")));
        result.put("afterSaleSn", text(row.get("after_sale_sn")));
        result.put("refundSn", hasText(text(row.get("provider_refund_no"))) ? text(row.get("provider_refund_no")) : text(row.get("after_sale_sn")));
        result.put("orderId", longValue(row.get("order_id")));
        result.put("orderSn", text(row.get("order_sn")));
        result.put("userId", longValue(row.get("user_id")));
        result.put("type", intValue(row.get("type")));
        result.put("status", intValue(row.get("status")));
        result.put("statusText", statusText(intValue(row.get("status"))));
        result.put("refundAmount", TradeMoneyUtils.formatYuan(intValue(row.get("refund_amount"))));
        result.put("payAmount", TradeMoneyUtils.formatYuan(intValue(row.get("pay_amount"))));
        result.put("payRefundedAmount", TradeMoneyUtils.formatYuan(intValue(row.get("pay_refunded_amount"))));
        result.put("reason", text(row.get("reason")));
        result.put("refundProvider", text(row.get("refund_provider")));
        result.put("providerRefundNo", text(row.get("provider_refund_no")));
        result.put("refundMessage", text(row.get("refund_message")));
        result.put("refundAttemptCount", intValue(row.get("refund_attempt_count")));
        result.put("refundLastAttemptTime", format(row.get("refund_last_attempt_time")));
        result.put("refundNextAttemptTime", format(row.get("refund_next_attempt_time")));
        result.put("refundClaimUntil", format(row.get("refund_claim_until")));
        result.put("refundLastError", text(row.get("refund_last_error")));
        result.put("refundChannelState", text(row.get("refund_channel_state")));
        result.put("refundExceptionCode", text(row.get("refund_exception_code")));
        result.put("refundExceptionMessage", text(row.get("refund_exception_message")));
        result.put("refundHandled", intValue(row.get("refund_handled")));
        result.put("refundHandleRemark", text(row.get("refund_handle_remark")));
        result.put("refundHandleAdminId", longValue(row.get("refund_handle_admin_id")));
        result.put("refundHandleTime", format(row.get("refund_handle_time")));
        result.put("applyTime", format(row.get("apply_time")));
        result.put("auditTime", format(row.get("audit_time")));
        result.put("refundTime", format(row.get("refund_time")));
        result.put("createTime", format(row.get("create_time")));
        result.put("canRetry", canRetry(intValue(row.get("status")), intValue(row.get("refund_attempt_count"))));
        return result;
    }

    private List<Map<String, Object>> buildTaskRecords(Map<String, Object> row) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("refundAttemptCount", intValue(row.get("refund_attempt_count")));
        record.put("lastAttemptTime", format(row.get("refund_last_attempt_time")));
        record.put("nextAttemptTime", format(row.get("refund_next_attempt_time")));
        record.put("claimUntil", format(row.get("refund_claim_until")));
        record.put("lastError", text(row.get("refund_last_error")));
        record.put("message", text(row.get("refund_message")));
        return List.of(record);
    }

    private List<Map<String, Object>> buildCallbackRecords(String afterSaleSn) {
        if (!hasText(afterSaleSn)) {
            return List.of();
        }
        List<Map<String, Object>> records = new ArrayList<>();
        records.addAll(jdbcTemplate.queryForList("""
                SELECT create_time, notification_id, event_type, refund_status, status, message
                  FROM refund_notify_log
                 WHERE after_sale_sn = ? AND deleted = b'0'
                 ORDER BY create_time DESC, id DESC
                 LIMIT 20
                """, afterSaleSn).stream().map(row -> {
                    Map<String, Object> record = new LinkedHashMap<>();
                    record.put("type", "SUCCESS");
                    record.put("notificationId", text(row.get("notification_id")));
                    record.put("eventType", text(row.get("event_type")));
                    record.put("refundStatus", text(row.get("refund_status")));
                    record.put("status", intValue(row.get("status")));
                    record.put("message", text(row.get("message")));
                    record.put("createTime", format(row.get("create_time")));
                    return record;
                }).toList());
        records.addAll(jdbcTemplate.queryForList("""
                SELECT create_time, notification_id, error_message
                  FROM refund_notify_failure_log
                 WHERE after_sale_sn = ?
                 ORDER BY create_time DESC, id DESC
                 LIMIT 20
                """, afterSaleSn).stream().map(row -> {
                    Map<String, Object> record = new LinkedHashMap<>();
                    record.put("type", "FAILURE");
                    record.put("notificationId", text(row.get("notification_id")));
                    record.put("eventType", "");
                    record.put("refundStatus", "FAIL");
                    record.put("status", 0);
                    record.put("message", text(row.get("error_message")));
                    record.put("createTime", format(row.get("create_time")));
                    return record;
                }).toList());
        records.sort((left, right) -> text(right.get("createTime")).compareTo(text(left.get("createTime"))));
        return records.size() <= 20 ? records : records.subList(0, 20);
    }

    private boolean canRetry(Integer status, Integer attempts) {
        if (status == null) {
            return false;
        }
        return status == STATUS_REFUND_FAILED || status == STATUS_REFUNDING
                || (attempts != null && attempts >= Math.max(maxAutoAttempts, 1));
    }

    private void validateRefundAmount(TradeAfterSaleDO afterSale, PayOrderDO payOrder) {
        if (payOrder == null || payOrder.getAmount() == null || afterSale.getRefundAmount() == null
                || afterSale.getRefundAmount() <= 0) {
            throw new ServerException(400, "退款金额异常");
        }
        int refunded = payOrder.getRefundedAmount() == null ? 0 : payOrder.getRefundedAmount();
        if (Math.addExact(refunded, afterSale.getRefundAmount()) > payOrder.getAmount()) {
            throw new ServerException(400, "退款金额超过支付单剩余可退金额");
        }
    }

    private PayOrderDO getLatestPayOrder(TradeOrderDO order) {
        if (order == null) {
            throw new ServerException(404, "退款关联订单不存在");
        }
        return payOrderMapper.selectOne(new LambdaQueryWrapper<PayOrderDO>()
                .eq(PayOrderDO::getOrderId, order.getId())
                .eq(PayOrderDO::getUserId, order.getUserId())
                .orderByDesc(PayOrderDO::getUpdateTime)
                .last("LIMIT 1"));
    }

    private TradeAfterSaleDO getAfterSale(Long afterSaleId) {
        if (afterSaleId == null || afterSaleId <= 0) {
            throw new ServerException(400, "售后单ID不能为空");
        }
        TradeAfterSaleDO afterSale = tradeAfterSaleMapper.selectById(afterSaleId);
        if (afterSale == null) {
            throw new ServerException(404, "售后单不存在");
        }
        return afterSale;
    }

    private void requireAdmin(Long adminId) {
        if (adminId == null || adminId <= 0) {
            throw new ServerException(401, "管理员身份无效");
        }
    }

    private int normalizePage(int page) {
        return Math.max(page, 1);
    }

    private int normalizeSize(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }

    private LocalDateTime parseTime(String value, String field) {
        if (!hasText(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim(), QUERY_TIME_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new ServerException(400, field + "格式必须为 yyyy-MM-dd HH:mm:ss");
        }
    }

    private void validateTimeRange(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && !start.isBefore(end)) {
            throw new ServerException(400, "createTimeStart必须早于createTimeEnd");
        }
    }

    private String statusText(Integer status) {
        return switch (status == null ? 0 : status) {
            case 1 -> "已退款";
            case 4 -> "退款处理中";
            case 5 -> "退款失败";
            case 6 -> "待买家寄回";
            case 7 -> "待商家收货";
            default -> "待审核";
        };
    }

    private LocalDateTime firstTime(LocalDateTime... values) {
        for (LocalDateTime value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String state(TradeAfterSaleDO afterSale) {
        if (hasText(afterSale.getRefundChannelState())) {
            return afterSale.getRefundChannelState();
        }
        return Integer.valueOf(STATUS_REFUNDED).equals(afterSale.getStatus()) ? "SUCCESS" : "PROCESSING";
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
        String normalized = value.trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();
        if (!hasText(message)) {
            message = exception.getClass().getSimpleName();
        }
        return trim(message, 255);
    }

    private Integer intValue(Object value) {
        if (value == null) {
            return null;
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
