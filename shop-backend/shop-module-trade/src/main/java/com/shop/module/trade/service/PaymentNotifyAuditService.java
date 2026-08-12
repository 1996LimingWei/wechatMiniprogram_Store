package com.shop.module.trade.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class PaymentNotifyAuditService {

    private final JdbcTemplate jdbcTemplate;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(
            String timestamp, String serial, String body,
            WechatPayService.PaymentNotification notification, String message) {
        jdbcTemplate.update("""
                INSERT INTO pay_notify_failure_log
                    (notification_id, pay_sn, wechatpay_serial, request_timestamp,
                     body_sha256, error_message)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                notification == null ? "" : safe(notification.notificationId(), 64),
                notification == null ? "" : safe(notification.paySn(), 32),
                safe(serial, 128), safe(timestamp, 32), sha256(body), safe(message, 255));
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("支付通知摘要计算失败", exception);
        }
    }

    private String safe(String value, int maxLength) {
        if (value == null) return "";
        String normalized = value.replaceAll("[\\r\\n\\t]", " ").trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }
}
