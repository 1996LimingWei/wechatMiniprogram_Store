package com.shop.module.trade.service;

public record TradeRefundRequestedEvent(Long afterSaleId, String operatorType, Long operatorId) {
}
