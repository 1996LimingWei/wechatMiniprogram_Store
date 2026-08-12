package com.shop.module.trade.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradeRefundRequestedListener {

    private final TradeRefundExecutionService tradeRefundExecutionService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRefundRequested(TradeRefundRequestedEvent event) {
        try {
            tradeRefundExecutionService.execute(
                    event.afterSaleId(), event.operatorType(), event.operatorId(), false);
        } catch (Exception exception) {
            log.warn("[TradeRefundRequestedListener] 退款任务首次执行失败, afterSaleId={}, message={}",
                    event.afterSaleId(), exception.getMessage());
        }
    }
}
