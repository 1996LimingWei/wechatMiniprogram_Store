package com.shop.module.trade.service;

/**
 * 支付单状态常量。
 */
public final class PayOrderStatus {

    public static final int PENDING = 0;
    public static final int PAID = 1;
    public static final int CLOSED = 2;
    public static final int REFUNDED = 3;

    private PayOrderStatus() {
    }

    public static String getText(Integer status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case PENDING -> "待支付";
            case PAID -> "已支付";
            case CLOSED -> "已关闭";
            case REFUNDED -> "已退款";
            default -> "未知";
        };
    }
}
