package com.shop.module.trade.controller;

import com.shop.module.trade.service.PayOrderService;
import com.shop.module.trade.service.PaymentNotifyAuditService;
import com.shop.module.trade.service.WechatPayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/app-api/pay/wechat")
@RequiredArgsConstructor
@Slf4j
public class WechatPayNotifyController {

    private final WechatPayService wechatPayService;
    private final PayOrderService payOrderService;
    private final PaymentNotifyAuditService paymentNotifyAuditService;

    @PostMapping("/notify")
    public ResponseEntity<Map<String, String>> notifyPayment(
            @RequestHeader("Wechatpay-Timestamp") String timestamp,
            @RequestHeader("Wechatpay-Nonce") String nonce,
            @RequestHeader("Wechatpay-Signature") String signature,
            @RequestHeader("Wechatpay-Serial") String serial,
            @RequestBody String body) {
        WechatPayService.PaymentNotification notification = null;
        try {
            notification = wechatPayService.parseNotification(
                    timestamp, nonce, signature, serial, body);
            payOrderService.handleWechatNotification(notification, body);
            return ResponseEntity.ok(Map.of("code", "SUCCESS", "message", "成功"));
        } catch (Exception exception) {
            log.warn("[notifyPayment] 微信支付通知处理失败: {}", exception.getMessage());
            try {
                paymentNotifyAuditService.recordFailure(
                        timestamp, serial, body, notification, exception.getMessage());
            } catch (Exception auditException) {
                log.error("[notifyPayment] 微信支付失败审计写入失败", auditException);
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("code", "FAIL", "message", "支付通知处理失败"));
        }
    }
}
