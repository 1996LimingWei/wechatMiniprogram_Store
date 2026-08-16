package com.shop.module.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.shop.common.exception.ServerException;
import com.shop.common.pojo.PageResult;
import com.shop.module.trade.dal.dataobject.PayNotifyLogDO;
import com.shop.module.trade.dal.dataobject.PayOrderDO;
import com.shop.module.trade.dal.dataobject.TradeOrderDO;
import com.shop.module.trade.dal.mysql.PayNotifyLogMapper;
import com.shop.module.trade.dal.mysql.PayOrderMapper;
import com.shop.module.trade.dal.mysql.TradeOrderMapper;
import com.shop.module.trade.util.TradeMoneyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class PayExceptionWorkbenchService {

    public static final String REASON_WECHAT_PAID_LOCAL_UNPAID = "WECHAT_PAID_LOCAL_UNPAID";
    public static final String REASON_WECHAT_PAID_LOCAL_CLOSED = "WECHAT_PAID_LOCAL_CLOSED";
    public static final String REASON_PENDING_TIMEOUT = "PENDING_TIMEOUT";
    public static final String REASON_NOTIFY_VERIFY_FAILED = "NOTIFY_VERIFY_FAILED";
    public static final String REASON_AMOUNT_MISMATCH = "AMOUNT_MISMATCH";
    public static final String REASON_QUERY_FAILED = "QUERY_FAILED";

    private static final int MAX_PAGE_SIZE = 100;
    private static final int AUTO_SCAN_LIMIT = 100;
    private static final int PENDING_TIMEOUT_MINUTES = 30;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter QUERY_TIME_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("uuuu-MM-dd HH:mm:ss")
            .toFormatter()
            .withResolverStyle(ResolverStyle.STRICT);

    private final JdbcTemplate jdbcTemplate;
    private final PayOrderMapper payOrderMapper;
    private final PayNotifyLogMapper payNotifyLogMapper;
    private final TradeOrderMapper tradeOrderMapper;
    private final PayOrderService payOrderService;
    private final WechatPayService wechatPayService;

    public PageResult<Map<String, Object>> getPayOrderPage(int page, int size, String paySn, String orderSn,
                                                            Integer status, String createTimeStart,
                                                            String createTimeEnd) {
        int finalPage = normalizePage(page);
        int finalSize = normalizeSize(size);
        LocalDateTime start = parseTime(createTimeStart, "createTimeStart");
        LocalDateTime end = parseTime(createTimeEnd, "createTimeEnd");
        validateTimeRange(start, end);

        StringBuilder where = new StringBuilder(" WHERE p.deleted = b'0'");
        List<Object> args = new ArrayList<>();
        if (hasText(paySn)) {
            where.append(" AND p.pay_sn = ?");
            args.add(paySn.trim());
        }
        if (hasText(orderSn)) {
            where.append(" AND o.order_sn = ?");
            args.add(orderSn.trim());
        }
        if (status != null) {
            where.append(" AND p.status = ?");
            args.add(status);
        }
        if (start != null) {
            where.append(" AND p.create_time >= ?");
            args.add(Timestamp.valueOf(start));
        }
        if (end != null) {
            where.append(" AND p.create_time < ?");
            args.add(Timestamp.valueOf(end));
        }

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pay_order p LEFT JOIN trade_order o ON o.id = p.order_id" + where,
                Long.class, args.toArray());
        args.add((finalPage - 1) * finalSize);
        args.add(finalSize);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT p.id, p.pay_sn, p.order_id, o.order_sn, p.user_id, p.amount, p.channel,
                       p.channel_trade_no, p.status, p.pay_time, p.last_query_time,
                       p.wechat_trade_state, p.wechat_amount, p.sync_message, p.create_time,
                       o.pay_status AS order_pay_status, o.status AS order_status
                  FROM pay_order p
                  LEFT JOIN trade_order o ON o.id = p.order_id
                """ + where + " ORDER BY p.create_time DESC, p.id DESC LIMIT ?, ?", args.toArray());
        return new PageResult<>(rows.stream().map(this::toPayOrderResp).toList(), total == null ? 0 : total);
    }

    public Map<String, Object> getPayOrderDetail(Long payOrderId) {
        if (payOrderId == null || payOrderId <= 0) {
            throw new ServerException(400, "支付单ID不能为空");
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT p.id, p.pay_sn, p.order_id, o.order_sn, p.user_id, p.amount, p.channel,
                       p.channel_trade_no, p.status, p.pay_time, p.last_query_time,
                       p.wechat_trade_state, p.wechat_amount, p.sync_message, p.create_time,
                       o.pay_status AS order_pay_status, o.status AS order_status,
                       o.actual_price, o.goods_price, o.freight_price, o.coupon_price
                  FROM pay_order p
                  LEFT JOIN trade_order o ON o.id = p.order_id
                 WHERE p.deleted = b'0' AND p.id = ?
                 LIMIT 1
                """, payOrderId);
        if (rows.isEmpty()) {
            throw new ServerException(404, "支付单不存在");
        }
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("payOrder", toPayOrderResp(rows.get(0)));
        detail.put("order", toOrderResp(rows.get(0)));
        detail.put("notifyLogs", listNotifyLogs(payOrderId));
        detail.put("exceptions", listExceptions(payOrderId));
        return detail;
    }

    public PageResult<Map<String, Object>> getExceptionPage(int page, int size, String paySn, String orderSn,
                                                            String reasonCode, Integer handled,
                                                            String createTimeStart, String createTimeEnd) {
        int finalPage = normalizePage(page);
        int finalSize = normalizeSize(size);
        LocalDateTime start = parseTime(createTimeStart, "createTimeStart");
        LocalDateTime end = parseTime(createTimeEnd, "createTimeEnd");
        validateTimeRange(start, end);

        StringBuilder where = new StringBuilder(" WHERE deleted = b'0'");
        List<Object> args = new ArrayList<>();
        if (hasText(paySn)) {
            where.append(" AND pay_sn = ?");
            args.add(paySn.trim());
        }
        if (hasText(orderSn)) {
            where.append(" AND order_sn = ?");
            args.add(orderSn.trim());
        }
        if (hasText(reasonCode)) {
            where.append(" AND reason_code = ?");
            args.add(reasonCode.trim());
        }
        if (handled != null) {
            where.append(" AND handled = ?");
            args.add(handled);
        }
        if (start != null) {
            where.append(" AND create_time >= ?");
            args.add(Timestamp.valueOf(start));
        }
        if (end != null) {
            where.append(" AND create_time < ?");
            args.add(Timestamp.valueOf(end));
        }

        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pay_exception" + where,
                Long.class, args.toArray());
        args.add((finalPage - 1) * finalSize);
        args.add(finalSize);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, pay_order_id, pay_sn, order_id, order_sn, user_id, reason_code, reason,
                       wechat_trade_state, wechat_amount, channel_trade_no, local_status,
                       order_pay_status, handled, handle_result, handle_remark, handle_admin_id,
                       handle_time, last_detect_time, create_time
                  FROM pay_exception
                """ + where + " ORDER BY handled ASC, create_time DESC, id DESC LIMIT ?, ?", args.toArray());
        return new PageResult<>(rows.stream().map(this::toExceptionResp).toList(), total == null ? 0 : total);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> manualSync(Long adminId, Long payOrderId) {
        if (adminId == null || adminId <= 0) {
            throw new ServerException(401, "管理员身份无效");
        }
        SyncOutcome outcome = syncPayOrder(payOrderId, adminId, true);
        return Map.of(
                "payOrderId", payOrderId,
                "success", outcome.success(),
                "message", outcome.message());
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean handleException(Long adminId, Long exceptionId, String remark) {
        if (adminId == null || adminId <= 0) {
            throw new ServerException(401, "管理员身份无效");
        }
        if (exceptionId == null || exceptionId <= 0) {
            throw new ServerException(400, "异常ID不能为空");
        }
        String finalRemark = remark == null ? "" : remark.trim();
        if (finalRemark.length() < 4 || finalRemark.length() > 200) {
            throw new ServerException(400, "处理备注长度应为 4 至 200 个字符");
        }
        int updated = jdbcTemplate.update("""
                UPDATE pay_exception
                   SET handled = 1, handle_result = 'MANUAL_CONFIRMED',
                       handle_remark = ?, handle_admin_id = ?, handle_time = NOW()
                 WHERE id = ? AND handled = 0 AND deleted = b'0'
                """, finalRemark, adminId, exceptionId);
        if (updated != 1) {
            throw new ServerException(400, "支付异常不存在或已处理");
        }
        return true;
    }

    public int scanPendingWechatPayments(int limit) {
        List<Long> ids = payOrderService.listPendingWechatPayOrderIds(
                Math.min(Math.max(limit, 1), AUTO_SCAN_LIMIT));
        int processed = 0;
        for (Long id : ids) {
            try {
                syncPayOrder(id, 0L, false);
                processed++;
            } catch (Exception exception) {
                log.warn("[PayExceptionWorkbenchService] 支付异常扫描失败, payOrderId={}, message={}",
                        id, exception.getMessage());
            }
        }
        return processed;
    }

    public void recordNotifyVerifyFailure(String paySn, String notificationId, String message) {
        try {
            PayOrderDO payOrder = hasText(paySn) ? getPayOrderByPaySn(paySn.trim()) : null;
            TradeOrderDO order = payOrder == null ? null : tradeOrderMapper.selectById(payOrder.getOrderId());
            upsertException(payOrder, order, REASON_NOTIFY_VERIFY_FAILED,
                    "微信支付回调验签或解析失败" + (hasText(message) ? "：" + trim(message, 120) : ""),
                    "", null, "", false, null, null);
        } catch (Exception exception) {
            log.warn("[PayExceptionWorkbenchService] 支付回调失败异常记录失败, notificationId={}, message={}",
                    notificationId, exception.getMessage());
        }
    }

    private SyncOutcome syncPayOrder(Long payOrderId, Long adminId, boolean manual) {
        PayOrderDO payOrder = payOrderMapper.selectById(payOrderId);
        if (payOrder == null) {
            throw new ServerException(404, "支付单不存在");
        }
        TradeOrderDO order = tradeOrderMapper.selectById(payOrder.getOrderId());
        if (order == null) {
            throw new ServerException(404, "支付单关联订单不存在");
        }
        if (!"wx_lite".equals(payOrder.getChannel())) {
            updateQuerySnapshot(payOrder, "", null, "非微信支付渠道，无需查单");
            return new SyncOutcome(true, "非微信支付渠道，无需查单");
        }
        if (!wechatPayService.isEnabled()) {
            if (manual) {
                throw new ServerException(503, "微信支付尚未完成生产配置，不能查单");
            }
            return new SyncOutcome(false, "微信支付未启用，跳过自动查单");
        }

        LocalDateTime queryTime = LocalDateTime.now();
        payOrderMapper.update(null, new LambdaUpdateWrapper<PayOrderDO>()
                .eq(PayOrderDO::getId, payOrder.getId())
                .set(PayOrderDO::getLastQueryTime, queryTime));

        WechatPayService.PaymentQueryResult queryResult;
        try {
            queryResult = wechatPayService.queryPayment(payOrder.getPaySn());
        } catch (Exception exception) {
            updateQuerySnapshot(payOrder, "QUERY_FAILED", null, trim(exception.getMessage(), 255));
            upsertException(payOrder, order, REASON_QUERY_FAILED, "微信支付查单失败：" + trim(exception.getMessage(), 120),
                    "QUERY_FAILED", null, "", false, adminId, null);
            return new SyncOutcome(false, "微信支付查单失败");
        }

        updateQuerySnapshot(payOrder, queryResult.tradeState(), queryResult.amount(), "微信查单完成");
        if (!payOrder.getPaySn().equals(queryResult.paySn())) {
            upsertException(payOrder, order, REASON_QUERY_FAILED, "微信支付查单返回的支付单号不匹配",
                    queryResult.tradeState(), queryResult.amount(), queryResult.transactionId(), false, adminId, null);
            return new SyncOutcome(false, "微信支付查单返回的支付单号不匹配");
        }
        if (!"SUCCESS".equals(queryResult.tradeState())) {
            if (isPendingTooLong(payOrder)) {
                upsertException(payOrder, order, REASON_PENDING_TIMEOUT, "支付中超过 30 分钟仍未成功",
                        queryResult.tradeState(), queryResult.amount(), queryResult.transactionId(), false, adminId, null);
                return new SyncOutcome(false, "支付中超时，已进入异常列表");
            }
            return new SyncOutcome(true, "微信支付状态：" + queryResult.tradeState());
        }

        if (!amountMatches(payOrder, order, queryResult.amount())) {
            upsertException(payOrder, order, REASON_AMOUNT_MISMATCH, "微信支付金额与本地支付金额或订单金额不一致",
                    queryResult.tradeState(), queryResult.amount(), queryResult.transactionId(), false, adminId, null);
            return new SyncOutcome(false, "支付金额不一致，已进入异常列表");
        }
        if (PayOrderStatus.CLOSED == safeStatus(payOrder) || Integer.valueOf(4).equals(order.getStatus())) {
            upsertException(payOrder, order, REASON_WECHAT_PAID_LOCAL_CLOSED, "本地已关闭但微信已支付",
                    queryResult.tradeState(), queryResult.amount(), queryResult.transactionId(), false, adminId, null);
            return new SyncOutcome(false, "本地已关闭但微信已支付，已进入异常列表");
        }
        if (PayOrderStatus.PENDING == safeStatus(payOrder)
                && !Integer.valueOf(TradeOrderPayStatus.PAID).equals(order.getPayStatus())) {
            payOrderService.handleWechatNotification(new WechatPayService.PaymentNotification(
                    "QUERY-" + queryResult.transactionId(),
                    manual ? "ADMIN.QUERY" : "JOB.QUERY",
                    queryResult.paySn(),
                    queryResult.transactionId(),
                    queryResult.tradeState(),
                    queryResult.amount(),
                    queryResult.successTime()), queryResult.rawBody());
            upsertException(payOrder, order, REASON_WECHAT_PAID_LOCAL_UNPAID, "微信已支付，本地待支付，已同步修复",
                    queryResult.tradeState(), queryResult.amount(), queryResult.transactionId(), true, adminId,
                    manual ? "MANUAL_SYNC_FIXED" : "AUTO_FIXED");
            return new SyncOutcome(true, manual ? "人工查单已同步为已支付" : "自动查单已同步为已支付");
        }
        return new SyncOutcome(true, "支付状态一致，无需修复");
    }

    private void updateQuerySnapshot(PayOrderDO payOrder, String wechatState, Integer wechatAmount, String message) {
        payOrderMapper.update(null, new LambdaUpdateWrapper<PayOrderDO>()
                .eq(PayOrderDO::getId, payOrder.getId())
                .set(PayOrderDO::getWechatTradeState, wechatState == null ? "" : trim(wechatState, 32))
                .set(PayOrderDO::getWechatAmount, wechatAmount)
                .set(PayOrderDO::getSyncMessage, message == null ? "" : trim(message, 255)));
    }

    private void upsertException(PayOrderDO payOrder, TradeOrderDO order, String code, String reason,
                                 String wechatState, Integer wechatAmount, String channelTradeNo,
                                 boolean handled, Long adminId, String handleResult) {
        Long activeId = payOrder == null ? null : jdbcTemplate.query("""
                        SELECT id FROM pay_exception
                         WHERE pay_order_id = ? AND reason_code = ? AND handled = 0 AND deleted = b'0'
                         ORDER BY id DESC LIMIT 1
                        """,
                rs -> rs.next() ? rs.getLong("id") : null, payOrder.getId(), code);
        if (activeId != null) {
            jdbcTemplate.update("""
                    UPDATE pay_exception
                       SET reason = ?, wechat_trade_state = ?, wechat_amount = ?, channel_trade_no = ?,
                           local_status = ?, order_pay_status = ?, handled = ?, handle_result = ?,
                           handle_admin_id = ?, handle_time = ?, last_detect_time = NOW()
                     WHERE id = ?
                    """,
                    trim(reason, 255), safe(wechatState, 32), wechatAmount, safe(channelTradeNo, 64),
                    payOrder == null ? null : payOrder.getStatus(),
                    order == null ? null : order.getPayStatus(),
                    handled ? 1 : 0, handled ? safe(handleResult, 32) : "",
                    handled ? adminId : null, handled ? Timestamp.valueOf(LocalDateTime.now()) : null,
                    activeId);
            return;
        }
        jdbcTemplate.update("""
                INSERT INTO pay_exception
                    (pay_order_id, pay_sn, order_id, order_sn, user_id, reason_code, reason,
                     wechat_trade_state, wechat_amount, channel_trade_no, local_status,
                     order_pay_status, handled, handle_result, handle_admin_id, handle_time)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                payOrder == null ? null : payOrder.getId(),
                payOrder == null ? "" : safe(payOrder.getPaySn(), 32),
                order == null ? (payOrder == null ? null : payOrder.getOrderId()) : order.getId(),
                order == null ? "" : safe(order.getOrderSn(), 32),
                payOrder == null ? null : payOrder.getUserId(),
                code, trim(reason, 255), safe(wechatState, 32), wechatAmount, safe(channelTradeNo, 64),
                payOrder == null ? null : payOrder.getStatus(),
                order == null ? null : order.getPayStatus(),
                handled ? 1 : 0, handled ? safe(handleResult, 32) : "",
                handled ? adminId : null, handled ? Timestamp.valueOf(LocalDateTime.now()) : null);
    }

    private PayOrderDO getPayOrderByPaySn(String paySn) {
        return payOrderMapper.selectOne(new LambdaQueryWrapper<PayOrderDO>()
                .eq(PayOrderDO::getPaySn, paySn)
                .last("LIMIT 1"));
    }

    private boolean amountMatches(PayOrderDO payOrder, TradeOrderDO order, Integer wechatAmount) {
        return wechatAmount != null
                && wechatAmount.equals(payOrder.getAmount())
                && wechatAmount.equals(order.getActualPrice());
    }

    private boolean isPendingTooLong(PayOrderDO payOrder) {
        LocalDateTime createTime = payOrder.getCreateTime();
        return createTime != null && createTime.plusMinutes(PENDING_TIMEOUT_MINUTES).isBefore(LocalDateTime.now());
    }

    private int safeStatus(PayOrderDO payOrder) {
        return payOrder.getStatus() == null ? PayOrderStatus.PENDING : payOrder.getStatus();
    }

    private List<Map<String, Object>> listNotifyLogs(Long payOrderId) {
        return payNotifyLogMapper.selectList(new LambdaQueryWrapper<PayNotifyLogDO>()
                        .eq(PayNotifyLogDO::getPayOrderId, payOrderId)
                        .orderByDesc(PayNotifyLogDO::getCreateTime)
                        .last("LIMIT 20"))
                .stream()
                .map(log -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", log.getId());
                    item.put("notificationId", log.getNotificationId());
                    item.put("paySn", log.getPaySn());
                    item.put("channelTradeNo", log.getChannelTradeNo());
                    item.put("eventType", log.getEventType());
                    item.put("status", log.getStatus());
                    item.put("statusText", Integer.valueOf(1).equals(log.getStatus()) ? "已处理" : "已接收");
                    item.put("message", log.getMessage());
                    item.put("createTime", format(log.getCreateTime()));
                    return item;
                }).toList();
    }

    private List<Map<String, Object>> listExceptions(Long payOrderId) {
        return jdbcTemplate.queryForList("""
                        SELECT id, pay_order_id, pay_sn, order_id, order_sn, user_id, reason_code, reason,
                               wechat_trade_state, wechat_amount, channel_trade_no, local_status,
                               order_pay_status, handled, handle_result, handle_remark, handle_admin_id,
                               handle_time, last_detect_time, create_time
                          FROM pay_exception
                         WHERE deleted = b'0' AND pay_order_id = ?
                         ORDER BY create_time DESC, id DESC
                        """, payOrderId)
                .stream().map(this::toExceptionResp).toList();
    }

    private Map<String, Object> toPayOrderResp(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", longValue(row.get("id")));
        result.put("paySn", text(row.get("pay_sn")));
        result.put("orderId", longValue(row.get("order_id")));
        result.put("orderSn", text(row.get("order_sn")));
        result.put("userId", longValue(row.get("user_id")));
        result.put("amount", TradeMoneyUtils.formatYuan(intValue(row.get("amount"))));
        result.put("amountCent", intValue(row.get("amount")));
        result.put("channel", text(row.get("channel")));
        result.put("channelTradeNo", text(row.get("channel_trade_no")));
        result.put("status", intValue(row.get("status")));
        result.put("statusText", PayOrderStatus.getText(intValue(row.get("status"))));
        result.put("payTime", format(row.get("pay_time")));
        result.put("lastQueryTime", format(row.get("last_query_time")));
        result.put("wechatTradeState", text(row.get("wechat_trade_state")));
        result.put("wechatAmount", row.get("wechat_amount") == null ? "" : TradeMoneyUtils.formatYuan(intValue(row.get("wechat_amount"))));
        result.put("wechatAmountCent", intValue(row.get("wechat_amount")));
        result.put("syncMessage", text(row.get("sync_message")));
        result.put("orderPayStatus", intValue(row.get("order_pay_status")));
        result.put("orderStatus", intValue(row.get("order_status")));
        result.put("createTime", format(row.get("create_time")));
        return result;
    }

    private Map<String, Object> toOrderResp(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderId", longValue(row.get("order_id")));
        result.put("orderSn", text(row.get("order_sn")));
        result.put("orderStatus", intValue(row.get("order_status")));
        result.put("payStatus", intValue(row.get("order_pay_status")));
        result.put("actualPrice", TradeMoneyUtils.formatYuan(intValue(row.get("actual_price"))));
        result.put("goodsPrice", TradeMoneyUtils.formatYuan(intValue(row.get("goods_price"))));
        result.put("freightPrice", TradeMoneyUtils.formatYuan(intValue(row.get("freight_price"))));
        result.put("couponPrice", TradeMoneyUtils.formatYuan(intValue(row.get("coupon_price"))));
        return result;
    }

    private Map<String, Object> toExceptionResp(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", longValue(row.get("id")));
        result.put("payOrderId", longValue(row.get("pay_order_id")));
        result.put("paySn", text(row.get("pay_sn")));
        result.put("orderId", longValue(row.get("order_id")));
        result.put("orderSn", text(row.get("order_sn")));
        result.put("userId", longValue(row.get("user_id")));
        result.put("reasonCode", text(row.get("reason_code")));
        result.put("reason", text(row.get("reason")));
        result.put("wechatTradeState", text(row.get("wechat_trade_state")));
        result.put("wechatAmount", row.get("wechat_amount") == null ? "" : TradeMoneyUtils.formatYuan(intValue(row.get("wechat_amount"))));
        result.put("channelTradeNo", text(row.get("channel_trade_no")));
        result.put("localStatus", intValue(row.get("local_status")));
        result.put("localStatusText", PayOrderStatus.getText(intValue(row.get("local_status"))));
        result.put("orderPayStatus", intValue(row.get("order_pay_status")));
        result.put("handled", intValue(row.get("handled")));
        result.put("handleResult", text(row.get("handle_result")));
        result.put("handleRemark", text(row.get("handle_remark")));
        result.put("handleAdminId", longValue(row.get("handle_admin_id")));
        result.put("handleTime", format(row.get("handle_time")));
        result.put("lastDetectTime", format(row.get("last_detect_time")));
        result.put("createTime", format(row.get("create_time")));
        return result;
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

    private boolean hasText(String value) {
        return value != null && !value.trim().isBlank();
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String safe(String value, int maxLength) {
        return trim(value == null ? "" : value.replaceAll("[\\r\\n\\t]", " "), maxLength);
    }

    private String trim(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
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

    private record SyncOutcome(boolean success, String message) {
    }
}
