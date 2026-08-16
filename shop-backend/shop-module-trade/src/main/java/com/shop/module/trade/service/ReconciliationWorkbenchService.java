package com.shop.module.trade.service;

import com.shop.common.exception.ServerException;
import com.shop.common.pojo.PageResult;
import com.shop.module.trade.util.TradeMoneyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReconciliationWorkbenchService {

    public static final String DIFF_BALANCED = "BALANCED";
    public static final String DIFF_LOCAL_MORE = "LOCAL_MORE";
    public static final String DIFF_WECHAT_MORE = "WECHAT_MORE";
    public static final String DIFF_AMOUNT_MISMATCH = "AMOUNT_MISMATCH";
    public static final String DIFF_STATUS_MISMATCH = "STATUS_MISMATCH";
    public static final String DIFF_MISSING_ORDER = "MISSING_ORDER";

    private static final int MAX_PAGE_SIZE = 100;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;
    private final WechatPayService wechatPayService;

    public PageResult<Map<String, Object>> getBatchPage(int page, int size, String dateStart, String dateEnd,
                                                        Integer status) {
        int finalPage = normalizePage(page);
        int finalSize = normalizeSize(size);
        LocalDate start = parseDateOptional(dateStart, "dateStart");
        LocalDate end = parseDateOptional(dateEnd, "dateEnd");
        if (start != null && end != null && start.isAfter(end)) {
            throw new ServerException(400, "dateStart不能晚于dateEnd");
        }

        StringBuilder where = new StringBuilder(" WHERE deleted = b'0'");
        List<Object> args = new ArrayList<>();
        if (start != null) {
            where.append(" AND reconcile_date >= ?");
            args.add(Date.valueOf(start));
        }
        if (end != null) {
            where.append(" AND reconcile_date <= ?");
            args.add(Date.valueOf(end));
        }
        if (status != null) {
            where.append(" AND status = ?");
            args.add(status);
        }

        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM trade_reconcile_batch" + where,
                Long.class, args.toArray());
        args.add((finalPage - 1) * finalSize);
        args.add(finalSize);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, reconcile_date, status, source, local_pay_count, local_pay_amount,
                       local_refund_count, local_refund_amount, local_net_amount,
                       wechat_pay_count, wechat_pay_amount, wechat_refund_count,
                       wechat_refund_amount, wechat_net_amount, fee_amount, difference_count,
                       trade_bill_url, fund_bill_url, trigger_type, trigger_admin_id,
                       message, start_time, finish_time, create_time
                  FROM trade_reconcile_batch
                """ + where + " ORDER BY reconcile_date DESC, id DESC LIMIT ?, ?", args.toArray());
        return new PageResult<>(rows.stream().map(this::toBatchResp).toList(), total == null ? 0 : total);
    }

    public Map<String, Object> getBatchDetail(Long batchId) {
        Map<String, Object> batch = getBatch(batchId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("batch", toBatchResp(batch));
        result.put("differences", listDifferences(batchId, null, null, 1, 200).getList());
        return result;
    }

    public PageResult<Map<String, Object>> getDifferencePage(int page, int size, Long batchId,
                                                             String diffType, Integer handled) {
        if (batchId == null || batchId <= 0) {
            throw new ServerException(400, "对账批次ID不能为空");
        }
        return listDifferences(batchId, diffType, handled, page, size);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> run(Long adminId, String reconcileDate, String triggerType) {
        LocalDate date = parseDateRequired(reconcileDate, "reconcileDate");
        if (date.isAfter(LocalDate.now())) {
            throw new ServerException(400, "不能对未来日期执行对账");
        }
        Long batchId = prepareBatch(date, adminId, triggerType);
        jdbcTemplate.update("DELETE FROM trade_reconcile_difference WHERE batch_id = ?", batchId);

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        ReconcileSummary summary = new ReconcileSummary();
        List<DifferenceRow> differences = new ArrayList<>();

        reconcilePayments(batchId, date, start, end, summary, differences);
        reconcileRefunds(batchId, date, start, end, summary, differences);
        reconcileAggregate(batchId, date, summary, differences);
        if (differences.isEmpty()) {
            differences.add(new DifferenceRow(DIFF_BALANCED, "SUMMARY", "", "",
                    summary.localNetAmount(), summary.wechatNetAmount(), "BALANCED", "BALANCED",
                    "本地与渠道快照汇总一致", 1));
        }
        for (DifferenceRow difference : differences) {
            insertDifference(batchId, date, difference);
        }

        BillUrls billUrls = fetchBillUrls(date);
        int unresolvedCount = (int) differences.stream()
                .filter(row -> !DIFF_BALANCED.equals(row.diffType()))
                .count();
        jdbcTemplate.update("""
                UPDATE trade_reconcile_batch
                   SET status = 1, source = ?, local_pay_count = ?, local_pay_amount = ?,
                       local_refund_count = ?, local_refund_amount = ?, local_net_amount = ?,
                       wechat_pay_count = ?, wechat_pay_amount = ?, wechat_refund_count = ?,
                       wechat_refund_amount = ?, wechat_net_amount = ?, fee_amount = ?,
                       difference_count = ?, trade_bill_url = ?, fund_bill_url = ?,
                       message = ?, finish_time = NOW()
                 WHERE id = ?
                """,
                billUrls.source(), summary.localPayCount(), summary.localPayAmount(),
                summary.localRefundCount(), summary.localRefundAmount(), summary.localNetAmount(),
                summary.wechatPayCount(), summary.wechatPayAmount(), summary.wechatRefundCount(),
                summary.wechatRefundAmount(), summary.wechatNetAmount(), summary.feeAmount(),
                unresolvedCount, billUrls.tradeBillUrl(), billUrls.fundBillUrl(),
                billUrls.message(), batchId);
        return getBatchDetail(batchId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean handleDifference(Long adminId, Long differenceId, String remark) {
        if (adminId == null || adminId <= 0) {
            throw new ServerException(401, "管理员身份无效");
        }
        if (differenceId == null || differenceId <= 0) {
            throw new ServerException(400, "差异ID不能为空");
        }
        String finalRemark = remark == null ? "" : remark.trim();
        if (finalRemark.length() < 4 || finalRemark.length() > 200) {
            throw new ServerException(400, "处理备注长度应为 4 至 200 个字符");
        }
        int updated = jdbcTemplate.update("""
                UPDATE trade_reconcile_difference
                   SET handled = 1, handle_remark = ?, handle_admin_id = ?, handle_time = NOW()
                 WHERE id = ? AND handled = 0 AND deleted = b'0'
                """, finalRemark, adminId, differenceId);
        if (updated != 1) {
            throw new ServerException(400, "对账差异不存在或已处理");
        }
        return true;
    }

    public byte[] export(Long batchId) {
        Map<String, Object> batch = getBatch(batchId);
        List<Map<String, Object>> differences = jdbcTemplate.queryForList("""
                SELECT id, diff_type, business_type, business_sn, order_sn, local_amount, channel_amount,
                       local_status, channel_status, reason, handled, handle_remark,
                       handle_admin_id, handle_time, create_time
                  FROM trade_reconcile_difference
                 WHERE batch_id = ? AND deleted = b'0'
                 ORDER BY diff_type, business_type, id
                """, batchId);
        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("对账日期,状态,本地支付笔数,本地支付金额,本地退款笔数,本地退款金额,本地净收入,");
        csv.append("微信支付笔数,微信支付金额,微信退款笔数,微信退款金额,微信净收入,手续费,差异数量,账单来源,说明\n");
        csv.append(csv(toBatchResp(batch).get("reconcileDate"))).append(',')
                .append(csv(toBatchResp(batch).get("statusText"))).append(',')
                .append(csv(batch.get("local_pay_count"))).append(',')
                .append(csv(money(batch.get("local_pay_amount")))).append(',')
                .append(csv(batch.get("local_refund_count"))).append(',')
                .append(csv(money(batch.get("local_refund_amount")))).append(',')
                .append(csv(money(batch.get("local_net_amount")))).append(',')
                .append(csv(batch.get("wechat_pay_count"))).append(',')
                .append(csv(money(batch.get("wechat_pay_amount")))).append(',')
                .append(csv(batch.get("wechat_refund_count"))).append(',')
                .append(csv(money(batch.get("wechat_refund_amount")))).append(',')
                .append(csv(money(batch.get("wechat_net_amount")))).append(',')
                .append(csv(money(batch.get("fee_amount")))).append(',')
                .append(csv(batch.get("difference_count"))).append(',')
                .append(csv(batch.get("source"))).append(',')
                .append(csv(batch.get("message"))).append('\n');
        csv.append('\n');
        csv.append("差异ID,差异类型,业务类型,业务单号,订单号,本地金额,渠道金额,本地状态,渠道状态,原因,处理状态,处理备注,处理人,处理时间,发现时间\n");
        for (Map<String, Object> row : differences) {
            csv.append(csv(row.get("id"))).append(',')
                    .append(csv(diffTypeText(text(row.get("diff_type"))))).append(',')
                    .append(csv(businessTypeText(text(row.get("business_type"))))).append(',')
                    .append(csv(row.get("business_sn"))).append(',')
                    .append(csv(row.get("order_sn"))).append(',')
                    .append(csv(money(row.get("local_amount")))).append(',')
                    .append(csv(money(row.get("channel_amount")))).append(',')
                    .append(csv(row.get("local_status"))).append(',')
                    .append(csv(row.get("channel_status"))).append(',')
                    .append(csv(row.get("reason"))).append(',')
                    .append(csv(intValue(row.get("handled")) == 1 ? "已处理" : "待处理")).append(',')
                    .append(csv(row.get("handle_remark"))).append(',')
                    .append(csv(row.get("handle_admin_id"))).append(',')
                    .append(csv(format(row.get("handle_time")))).append(',')
                    .append(csv(format(row.get("create_time")))).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void reconcilePayments(Long batchId, LocalDate date, LocalDateTime start, LocalDateTime end,
                                   ReconcileSummary summary, List<DifferenceRow> differences) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT p.id, p.pay_sn, p.order_id, p.amount, p.status, p.channel, p.wechat_trade_state,
                       p.wechat_amount, p.channel_trade_no, o.order_sn, o.pay_status AS order_pay_status
                  FROM pay_order p
                  LEFT JOIN trade_order o ON o.id = p.order_id AND o.deleted = b'0'
                 WHERE p.deleted = b'0' AND p.pay_time >= ? AND p.pay_time < ? AND p.status IN (1, 3)
                """, Timestamp.valueOf(start), Timestamp.valueOf(end));
        for (Map<String, Object> row : rows) {
            int localAmount = intValue(row.get("amount"));
            String channelState = paymentChannelState(row);
            Integer channelAmount = paymentChannelAmount(row, localAmount, channelState);
            summary.addLocalPay(localAmount);
            if (isChannelSuccess(channelState)) {
                summary.addWechatPay(channelAmount == null ? localAmount : channelAmount);
            }
            String orderSn = text(row.get("order_sn"));
            if (!hasText(orderSn)) {
                differences.add(new DifferenceRow(DIFF_MISSING_ORDER, "PAY", text(row.get("pay_sn")), "",
                        localAmount, channelAmount, String.valueOf(row.get("status")), channelState,
                        "支付单缺少关联订单", 0));
            }
            if (channelAmount != null && channelAmount != localAmount) {
                differences.add(new DifferenceRow(DIFF_AMOUNT_MISMATCH, "PAY", text(row.get("pay_sn")), orderSn,
                        localAmount, channelAmount, String.valueOf(row.get("status")), channelState,
                        "支付金额与渠道快照不一致", 0));
            }
            if (!isChannelSuccess(channelState)) {
                differences.add(new DifferenceRow(DIFF_STATUS_MISMATCH, "PAY", text(row.get("pay_sn")), orderSn,
                        localAmount, channelAmount, String.valueOf(row.get("status")), channelState,
                        "本地已支付但渠道状态未成功", 0));
            }
        }
    }

    private void reconcileRefunds(Long batchId, LocalDate date, LocalDateTime start, LocalDateTime end,
                                  ReconcileSummary summary, List<DifferenceRow> differences) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT a.id, a.after_sale_sn, a.order_id, a.refund_amount, a.status,
                       a.refund_channel_state, a.provider_refund_no, o.order_sn
                  FROM trade_after_sale a
                  LEFT JOIN trade_order o ON o.id = a.order_id AND o.deleted = b'0'
                 WHERE a.deleted = b'0' AND a.refund_time >= ? AND a.refund_time < ? AND a.status = 1
                """, Timestamp.valueOf(start), Timestamp.valueOf(end));
        for (Map<String, Object> row : rows) {
            int localAmount = intValue(row.get("refund_amount"));
            String channelState = refundChannelState(row);
            Integer channelAmount = isChannelSuccess(channelState) ? localAmount : 0;
            summary.addLocalRefund(localAmount);
            if (isChannelSuccess(channelState)) {
                summary.addWechatRefund(channelAmount);
            }
            String orderSn = text(row.get("order_sn"));
            if (!hasText(orderSn)) {
                differences.add(new DifferenceRow(DIFF_MISSING_ORDER, "REFUND", text(row.get("after_sale_sn")), "",
                        localAmount, channelAmount, String.valueOf(row.get("status")), channelState,
                        "退款单缺少关联订单", 0));
            }
            if (!isChannelSuccess(channelState)) {
                differences.add(new DifferenceRow(DIFF_STATUS_MISMATCH, "REFUND", text(row.get("after_sale_sn")), orderSn,
                        localAmount, channelAmount, String.valueOf(row.get("status")), channelState,
                        "本地已退款但渠道状态未成功", 0));
            }
        }
    }

    private void reconcileAggregate(Long batchId, LocalDate date, ReconcileSummary summary,
                                    List<DifferenceRow> differences) {
        if (summary.localPayAmount() > summary.wechatPayAmount()) {
            differences.add(new DifferenceRow(DIFF_LOCAL_MORE, "SUMMARY", "PAY-" + date, "",
                    summary.localPayAmount(), summary.wechatPayAmount(), "LOCAL_PAID", "CHANNEL_PAID",
                    "本地支付汇总金额大于渠道快照", 0));
        } else if (summary.localPayAmount() < summary.wechatPayAmount()) {
            differences.add(new DifferenceRow(DIFF_WECHAT_MORE, "SUMMARY", "PAY-" + date, "",
                    summary.localPayAmount(), summary.wechatPayAmount(), "LOCAL_PAID", "CHANNEL_PAID",
                    "渠道支付汇总金额大于本地", 0));
        }
        if (summary.localRefundAmount() > summary.wechatRefundAmount()) {
            differences.add(new DifferenceRow(DIFF_LOCAL_MORE, "SUMMARY", "REFUND-" + date, "",
                    summary.localRefundAmount(), summary.wechatRefundAmount(), "LOCAL_REFUNDED", "CHANNEL_REFUNDED",
                    "本地退款汇总金额大于渠道快照", 0));
        } else if (summary.localRefundAmount() < summary.wechatRefundAmount()) {
            differences.add(new DifferenceRow(DIFF_WECHAT_MORE, "SUMMARY", "REFUND-" + date, "",
                    summary.localRefundAmount(), summary.wechatRefundAmount(), "LOCAL_REFUNDED", "CHANNEL_REFUNDED",
                    "渠道退款汇总金额大于本地", 0));
        }
    }

    private BillUrls fetchBillUrls(LocalDate date) {
        if (!wechatPayService.isEnabled()) {
            return new BillUrls("LOCAL_SNAPSHOT", "", "", "微信支付未启用，使用本地与已同步渠道状态生成对账结果");
        }
        String tradeBillUrl = "";
        String fundBillUrl = "";
        StringBuilder message = new StringBuilder("已请求微信账单元数据");
        try {
            tradeBillUrl = wechatPayService.getTradeBillDownloadUrl(date).downloadUrl();
        } catch (Exception exception) {
            message.append("；交易账单获取失败：").append(trim(exception.getMessage(), 80));
        }
        try {
            fundBillUrl = wechatPayService.getFundFlowBillDownloadUrl(date).downloadUrl();
        } catch (Exception exception) {
            message.append("；资金账单获取失败：").append(trim(exception.getMessage(), 80));
        }
        return new BillUrls("WECHAT_BILL_URL", tradeBillUrl, fundBillUrl, trim(message.toString(), 255));
    }

    private Long prepareBatch(LocalDate date, Long adminId, String triggerType) {
        Long existingId = jdbcTemplate.query("""
                        SELECT id FROM trade_reconcile_batch
                         WHERE reconcile_date = ? AND deleted = b'0'
                         LIMIT 1
                        """,
                rs -> rs.next() ? rs.getLong("id") : null, Date.valueOf(date));
        String finalTriggerType = hasText(triggerType) ? triggerType.trim() : "MANUAL";
        if (existingId != null) {
            jdbcTemplate.update("""
                    UPDATE trade_reconcile_batch
                       SET status = 0, difference_count = 0, trigger_type = ?, trigger_admin_id = ?,
                           message = '对账执行中', start_time = NOW(), finish_time = NULL
                     WHERE id = ?
                    """, finalTriggerType, adminId, existingId);
            return existingId;
        }
        jdbcTemplate.update("""
                INSERT INTO trade_reconcile_batch
                    (reconcile_date, status, trigger_type, trigger_admin_id, message, start_time)
                VALUES (?, 0, ?, ?, '对账执行中', NOW())
                """, Date.valueOf(date), finalTriggerType, adminId);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private void insertDifference(Long batchId, LocalDate date, DifferenceRow row) {
        jdbcTemplate.update("""
                INSERT INTO trade_reconcile_difference
                    (batch_id, reconcile_date, diff_type, business_type, business_sn, order_sn,
                     local_amount, channel_amount, local_status, channel_status, reason, handled)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, batchId, Date.valueOf(date), row.diffType(), row.businessType(),
                trim(row.businessSn(), 64), trim(row.orderSn(), 32),
                row.localAmount(), row.channelAmount(), trim(row.localStatus(), 32),
                trim(row.channelStatus(), 32), trim(row.reason(), 255), row.handled());
    }

    private PageResult<Map<String, Object>> listDifferences(Long batchId, String diffType, Integer handled,
                                                            int page, int size) {
        int finalPage = normalizePage(page);
        int finalSize = normalizeSize(size);
        StringBuilder where = new StringBuilder(" WHERE batch_id = ? AND deleted = b'0'");
        List<Object> args = new ArrayList<>();
        args.add(batchId);
        if (hasText(diffType)) {
            where.append(" AND diff_type = ?");
            args.add(diffType.trim());
        }
        if (handled != null) {
            where.append(" AND handled = ?");
            args.add(handled);
        }
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM trade_reconcile_difference" + where,
                Long.class, args.toArray());
        args.add((finalPage - 1) * finalSize);
        args.add(finalSize);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, batch_id, reconcile_date, diff_type, business_type, business_sn, order_sn,
                       local_amount, channel_amount, local_status, channel_status, reason,
                       handled, handle_remark, handle_admin_id, handle_time, create_time
                  FROM trade_reconcile_difference
                """ + where + " ORDER BY handled ASC, diff_type, id LIMIT ?, ?", args.toArray());
        return new PageResult<>(rows.stream().map(this::toDifferenceResp).toList(), total == null ? 0 : total);
    }

    private Map<String, Object> getBatch(Long batchId) {
        if (batchId == null || batchId <= 0) {
            throw new ServerException(400, "对账批次ID不能为空");
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, reconcile_date, status, source, local_pay_count, local_pay_amount,
                       local_refund_count, local_refund_amount, local_net_amount,
                       wechat_pay_count, wechat_pay_amount, wechat_refund_count,
                       wechat_refund_amount, wechat_net_amount, fee_amount, difference_count,
                       trade_bill_url, fund_bill_url, trigger_type, trigger_admin_id,
                       message, start_time, finish_time, create_time
                  FROM trade_reconcile_batch
                 WHERE id = ? AND deleted = b'0'
                 LIMIT 1
                """, batchId);
        if (rows.isEmpty()) {
            throw new ServerException(404, "对账批次不存在");
        }
        return rows.get(0);
    }

    private String paymentChannelState(Map<String, Object> row) {
        String state = text(row.get("wechat_trade_state"));
        if (hasText(state)) {
            return state;
        }
        String channel = text(row.get("channel"));
        if ("mock".equals(channel)) {
            return "SUCCESS";
        }
        return "SUCCESS";
    }

    private Integer paymentChannelAmount(Map<String, Object> row, int localAmount, String channelState) {
        if (!isChannelSuccess(channelState)) {
            return 0;
        }
        Object amount = row.get("wechat_amount");
        return amount == null ? localAmount : intValue(amount);
    }

    private String refundChannelState(Map<String, Object> row) {
        String state = text(row.get("refund_channel_state"));
        return hasText(state) ? state : "SUCCESS";
    }

    private boolean isChannelSuccess(String state) {
        return "SUCCESS".equalsIgnoreCase(state)
                || "REFUND_SUCCESS".equalsIgnoreCase(state)
                || "LOCAL_SUCCESS".equalsIgnoreCase(state);
    }

    private Map<String, Object> toBatchResp(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", longValue(row.get("id")));
        result.put("reconcileDate", formatDate(row.get("reconcile_date")));
        result.put("status", intValue(row.get("status")));
        result.put("statusText", batchStatusText(intValue(row.get("status"))));
        result.put("source", text(row.get("source")));
        result.put("localPayCount", intValue(row.get("local_pay_count")));
        result.put("localPayAmount", money(row.get("local_pay_amount")));
        result.put("localRefundCount", intValue(row.get("local_refund_count")));
        result.put("localRefundAmount", money(row.get("local_refund_amount")));
        result.put("localNetAmount", money(row.get("local_net_amount")));
        result.put("wechatPayCount", intValue(row.get("wechat_pay_count")));
        result.put("wechatPayAmount", money(row.get("wechat_pay_amount")));
        result.put("wechatRefundCount", intValue(row.get("wechat_refund_count")));
        result.put("wechatRefundAmount", money(row.get("wechat_refund_amount")));
        result.put("wechatNetAmount", money(row.get("wechat_net_amount")));
        result.put("feeAmount", row.get("fee_amount") == null ? "" : money(row.get("fee_amount")));
        result.put("differenceCount", intValue(row.get("difference_count")));
        result.put("tradeBillUrl", text(row.get("trade_bill_url")));
        result.put("fundBillUrl", text(row.get("fund_bill_url")));
        result.put("triggerType", text(row.get("trigger_type")));
        result.put("triggerAdminId", longValue(row.get("trigger_admin_id")));
        result.put("message", text(row.get("message")));
        result.put("startTime", format(row.get("start_time")));
        result.put("finishTime", format(row.get("finish_time")));
        result.put("createTime", format(row.get("create_time")));
        return result;
    }

    private Map<String, Object> toDifferenceResp(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", longValue(row.get("id")));
        result.put("batchId", longValue(row.get("batch_id")));
        result.put("reconcileDate", formatDate(row.get("reconcile_date")));
        result.put("diffType", text(row.get("diff_type")));
        result.put("diffTypeText", diffTypeText(text(row.get("diff_type"))));
        result.put("businessType", text(row.get("business_type")));
        result.put("businessTypeText", businessTypeText(text(row.get("business_type"))));
        result.put("businessSn", text(row.get("business_sn")));
        result.put("orderSn", text(row.get("order_sn")));
        result.put("localAmount", money(row.get("local_amount")));
        result.put("channelAmount", money(row.get("channel_amount")));
        result.put("localStatus", text(row.get("local_status")));
        result.put("channelStatus", text(row.get("channel_status")));
        result.put("reason", text(row.get("reason")));
        result.put("handled", intValue(row.get("handled")));
        result.put("handleRemark", text(row.get("handle_remark")));
        result.put("handleAdminId", longValue(row.get("handle_admin_id")));
        result.put("handleTime", format(row.get("handle_time")));
        result.put("createTime", format(row.get("create_time")));
        return result;
    }

    private String batchStatusText(Integer status) {
        return switch (status == null ? -1 : status) {
            case 0 -> "处理中";
            case 1 -> "完成";
            case 2 -> "失败";
            default -> "未知";
        };
    }

    private String diffTypeText(String diffType) {
        return switch (diffType) {
            case DIFF_BALANCED -> "平账";
            case DIFF_LOCAL_MORE -> "本地多";
            case DIFF_WECHAT_MORE -> "微信多";
            case DIFF_AMOUNT_MISMATCH -> "金额不一致";
            case DIFF_STATUS_MISMATCH -> "状态不一致";
            case DIFF_MISSING_ORDER -> "缺少关联订单";
            default -> diffType;
        };
    }

    private String businessTypeText(String businessType) {
        return switch (businessType) {
            case "PAY" -> "支付";
            case "REFUND" -> "退款";
            case "SUMMARY" -> "汇总";
            default -> businessType;
        };
    }

    private int normalizePage(int page) {
        return Math.max(page, 1);
    }

    private int normalizeSize(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }

    private LocalDate parseDateRequired(String value, String field) {
        if (!hasText(value)) {
            throw new ServerException(400, field + "不能为空");
        }
        return parseDate(value, field);
    }

    private LocalDate parseDateOptional(String value, String field) {
        return hasText(value) ? parseDate(value, field) : null;
    }

    private LocalDate parseDate(String value, String field) {
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException exception) {
            throw new ServerException(400, field + "格式必须为 yyyy-MM-dd");
        }
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

    private String csv(Object value) {
        String text = text(value).replace("\"", "\"\"");
        return "\"" + text + "\"";
    }

    private String money(Object value) {
        return TradeMoneyUtils.formatYuan(intValue(value));
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

    private String formatDate(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Date date) {
            return date.toLocalDate().toString();
        }
        if (value instanceof java.util.Date date) {
            return new Date(date.getTime()).toLocalDate().toString();
        }
        return String.valueOf(value);
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

    private record DifferenceRow(String diffType, String businessType, String businessSn, String orderSn,
                                 Integer localAmount, Integer channelAmount, String localStatus,
                                 String channelStatus, String reason, Integer handled) {
    }

    private record BillUrls(String source, String tradeBillUrl, String fundBillUrl, String message) {
    }

    private static final class ReconcileSummary {
        private int localPayCount;
        private int localPayAmount;
        private int localRefundCount;
        private int localRefundAmount;
        private int wechatPayCount;
        private int wechatPayAmount;
        private int wechatRefundCount;
        private int wechatRefundAmount;
        private Integer feeAmount;

        void addLocalPay(int amount) {
            localPayCount++;
            localPayAmount += amount;
        }

        void addWechatPay(int amount) {
            wechatPayCount++;
            wechatPayAmount += amount;
        }

        void addLocalRefund(int amount) {
            localRefundCount++;
            localRefundAmount += amount;
        }

        void addWechatRefund(int amount) {
            wechatRefundCount++;
            wechatRefundAmount += amount;
        }

        int localPayCount() {
            return localPayCount;
        }

        int localPayAmount() {
            return localPayAmount;
        }

        int localRefundCount() {
            return localRefundCount;
        }

        int localRefundAmount() {
            return localRefundAmount;
        }

        int localNetAmount() {
            return localPayAmount - localRefundAmount;
        }

        int wechatPayCount() {
            return wechatPayCount;
        }

        int wechatPayAmount() {
            return wechatPayAmount;
        }

        int wechatRefundCount() {
            return wechatRefundCount;
        }

        int wechatRefundAmount() {
            return wechatRefundAmount;
        }

        int wechatNetAmount() {
            return wechatPayAmount - wechatRefundAmount;
        }

        Integer feeAmount() {
            return feeAmount;
        }
    }
}
