package com.shop.module.trade.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class RefundNotifyAuditService {

    private static final String REASON_REFUND_NOTIFY_FAILED = "REFUND_NOTIFY_FAILED";

    private final JdbcTemplate jdbcTemplate;

    public boolean recordReceived(WechatPayService.RefundNotification notification, String rawBody) {
        try {
            jdbcTemplate.update("""
                    INSERT INTO refund_notify_log
                        (notification_id, after_sale_sn, provider_refund_no, pay_sn,
                         event_type, refund_status, refund_amount, status, message, raw_body)
                    VALUES (?, ?, ?, ?, ?, ?, ?, 0, '已接收', ?)
                    """,
                    safe(notification.notificationId(), 64),
                    safe(notification.afterSaleSn(), 32),
                    safe(notification.providerRefundNo(), 64),
                    safe(notification.paySn(), 32),
                    safe(notification.eventType(), 64),
                    safe(notification.refundStatus(), 32),
                    notification.amount(),
                    rawBody);
            return true;
        } catch (DuplicateKeyException exception) {
            return false;
        }
    }

    public void markHandled(String notificationId, String message) {
        jdbcTemplate.update("""
                UPDATE refund_notify_log
                   SET status = 1, message = ?
                 WHERE notification_id = ? AND status = 0 AND deleted = b'0'
                """, safe(message, 255), safe(notificationId, 64));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(
            String timestamp, String serial, String body,
            WechatPayService.RefundNotification notification, String message) {
        jdbcTemplate.update("""
                INSERT INTO refund_notify_failure_log
                    (notification_id, after_sale_sn, provider_refund_no, pay_sn, wechatpay_serial,
                     request_timestamp, body_sha256, error_message)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                notification == null ? "" : safe(notification.notificationId(), 64),
                notification == null ? "" : safe(notification.afterSaleSn(), 32),
                notification == null ? "" : safe(notification.providerRefundNo(), 64),
                notification == null ? "" : safe(notification.paySn(), 32),
                safe(serial, 128), safe(timestamp, 32), sha256(body), safe(message, 255));
        if (notification != null && hasText(notification.afterSaleSn())) {
            jdbcTemplate.update("""
                    UPDATE trade_after_sale
                       SET refund_channel_state = ?,
                           refund_exception_code = ?,
                           refund_exception_message = ?,
                           refund_handled = 0
                     WHERE after_sale_sn = ? AND deleted = b'0'
                    """,
                    safe(notification.refundStatus(), 32),
                    REASON_REFUND_NOTIFY_FAILED,
                    safe(message, 255),
                    safe(notification.afterSaleSn(), 32));
        }
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("退款通知摘要计算失败", exception);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isBlank();
    }

    private String safe(String value, int maxLength) {
        if (value == null) return "";
        String normalized = value.replaceAll("[\\r\\n\\t]", " ").trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }
}
