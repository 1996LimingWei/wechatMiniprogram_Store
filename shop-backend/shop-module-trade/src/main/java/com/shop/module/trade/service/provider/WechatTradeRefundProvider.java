package com.shop.module.trade.service.provider;

import com.shop.common.exception.ServerException;
import com.shop.module.trade.service.WechatPayService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/** 微信支付退款实现，使用售后单号作为商户退款单号保证重复请求幂等。 */
@Component
@RequiredArgsConstructor
public class WechatTradeRefundProvider implements TradeRefundProvider {

    private static final String REFUND_PATH = "/v3/refund/domestic/refunds";

    private final WechatPayService wechatPayService;

    @Override
    public String type() {
        return "wechat";
    }

    @Override
    public RefundResult refund(RefundRequest request) {
        if (request.amount() == null || request.amount() <= 0) {
            throw new ServerException(400, "退款金额必须大于 0");
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("out_trade_no", request.paySn());
        payload.put("out_refund_no", request.afterSaleSn());
        payload.put("reason", normalizeReason(request.reason()));
        payload.put("amount", Map.of(
                "refund", request.amount(),
                "total", request.amount(),
                "currency", "CNY"));
        return toResult(wechatPayService.postJson(REFUND_PATH, payload), request);
    }

    @Override
    public RefundResult query(RefundQuery query) {
        Map<String, Object> response = wechatPayService.getJson(
                REFUND_PATH + "/" + query.afterSaleSn());
        return toResult(response, new RefundRequest(
                query.afterSaleSn(), "", query.paySn(), query.amount(), ""));
    }

    private RefundResult toResult(Map<String, Object> response, RefundRequest request) {
        String outRefundNo = requireText(response, "out_refund_no");
        String outTradeNo = requireText(response, "out_trade_no");
        if (!request.afterSaleSn().equals(outRefundNo) || !request.paySn().equals(outTradeNo)) {
            throw new ServerException(502, "微信退款返回的商户单号不匹配");
        }
        Map<String, Object> amount = requireMap(response, "amount");
        if (requireAmount(amount, "refund") != request.amount()
                || !"CNY".equals(requireText(amount, "currency"))) {
            throw new ServerException(502, "微信退款返回的金额或币种不匹配");
        }
        String status = requireText(response, "status");
        RefundStatus refundStatus = switch (status) {
            case "SUCCESS" -> RefundStatus.SUCCESS;
            case "PROCESSING" -> RefundStatus.PROCESSING;
            case "CLOSED", "ABNORMAL" -> RefundStatus.FAILED;
            default -> throw new ServerException(502, "微信退款返回未知状态: " + status);
        };
        String providerRefundNo = String.valueOf(response.getOrDefault("refund_id", ""));
        return new RefundResult(providerRefundNo, refundStatus, getStatusMessage(status));
    }

    private String normalizeReason(String reason) {
        String normalized = reason == null || reason.isBlank() ? "用户申请退款" : reason.trim();
        return normalized.length() <= 80 ? normalized : normalized.substring(0, 80);
    }

    private String getStatusMessage(String status) {
        return switch (status) {
            case "SUCCESS" -> "微信退款成功";
            case "PROCESSING" -> "微信退款处理中";
            case "CLOSED" -> "微信退款已关闭";
            case "ABNORMAL" -> "微信退款异常";
            default -> "微信退款状态未知";
        };
    }

    private String requireText(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            throw new ServerException(502, "微信退款响应字段缺失: " + key);
        }
        return String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> requireMap(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (!(value instanceof Map<?, ?>)) {
            throw new ServerException(502, "微信退款响应字段缺失: " + key);
        }
        return (Map<String, Object>) value;
    }

    private int requireAmount(Map<String, Object> amount, String key) {
        Object value = amount.get(key);
        if (!(value instanceof Number number) || number.intValue() <= 0) {
            throw new ServerException(502, "微信退款响应金额无效");
        }
        return number.intValue();
    }
}
