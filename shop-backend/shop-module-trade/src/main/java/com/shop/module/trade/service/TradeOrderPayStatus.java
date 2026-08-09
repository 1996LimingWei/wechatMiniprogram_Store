package com.shop.module.trade.service;

/**
 * 订单支付状态常量，与支付单状态独立维护。
 */
public final class TradeOrderPayStatus {

    public static final int UNPAID = 0;
    public static final int PAID = 1;
    public static final int REFUNDED = 2;

    private TradeOrderPayStatus() {
    }
}
