package com.shop.module.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.shop.common.exception.ServerException;
import com.shop.module.trade.dal.dataobject.PayOrderDO;
import com.shop.module.trade.dal.dataobject.TradeAfterSaleDO;
import com.shop.module.trade.dal.dataobject.TradeOrderDO;
import com.shop.module.trade.dal.mysql.PayOrderMapper;
import com.shop.module.trade.dal.mysql.TradeAfterSaleMapper;
import com.shop.module.trade.dal.mysql.TradeOrderMapper;
import com.shop.module.trade.service.provider.TradeRefundProvider;
import com.shop.module.trade.service.provider.TradeRefundProviderService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TradeRefundExecutionService {

    private static final int STATUS_REFUNDING = 4;

    private final TradeAfterSaleMapper tradeAfterSaleMapper;
    private final TradeOrderMapper tradeOrderMapper;
    private final PayOrderMapper payOrderMapper;
    private final TradeRefundProviderService tradeRefundProviderService;
    private final TradeAfterSaleService tradeAfterSaleService;

    @Value("${trade.refund.max-auto-attempts:12}")
    private int maxAutoAttempts = 12;

    @Value("${trade.refund.claim-seconds:120}")
    private int claimSeconds = 120;

    public List<Long> listExecutableIds(int limit) {
        LocalDateTime now = LocalDateTime.now();
        return tradeAfterSaleMapper.selectList(new LambdaQueryWrapper<TradeAfterSaleDO>()
                        .eq(TradeAfterSaleDO::getStatus, STATUS_REFUNDING)
                        .lt(TradeAfterSaleDO::getRefundAttemptCount, Math.max(maxAutoAttempts, 1))
                        .and(wrapper -> wrapper.isNull(TradeAfterSaleDO::getRefundNextAttemptTime)
                                .or().le(TradeAfterSaleDO::getRefundNextAttemptTime, now))
                        .and(wrapper -> wrapper.isNull(TradeAfterSaleDO::getRefundClaimUntil)
                                .or().le(TradeAfterSaleDO::getRefundClaimUntil, now))
                        .orderByAsc(TradeAfterSaleDO::getRefundNextAttemptTime)
                        .orderByAsc(TradeAfterSaleDO::getId)
                        .last("LIMIT " + Math.min(Math.max(limit, 1), 100)))
                .stream()
                .map(TradeAfterSaleDO::getId)
                .toList();
    }

    public boolean execute(Long afterSaleId, String operatorType, Long operatorId, boolean force) {
        TradeAfterSaleDO afterSale = tradeAfterSaleMapper.selectById(afterSaleId);
        if (afterSale == null || !Integer.valueOf(STATUS_REFUNDING).equals(afterSale.getStatus())) return false;

        int previousAttempts = afterSale.getRefundAttemptCount() == null ? 0 : afterSale.getRefundAttemptCount();
        if (!force && previousAttempts >= Math.max(maxAutoAttempts, 1)) return false;
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<TradeAfterSaleDO> claim = new LambdaUpdateWrapper<TradeAfterSaleDO>()
                .eq(TradeAfterSaleDO::getId, afterSaleId)
                .eq(TradeAfterSaleDO::getStatus, STATUS_REFUNDING)
                .eq(TradeAfterSaleDO::getRefundAttemptCount, previousAttempts)
                .and(wrapper -> wrapper.isNull(TradeAfterSaleDO::getRefundClaimUntil)
                        .or().le(TradeAfterSaleDO::getRefundClaimUntil, now))
                .set(TradeAfterSaleDO::getRefundAttemptCount, previousAttempts + 1)
                .set(TradeAfterSaleDO::getRefundLastAttemptTime, now)
                .set(TradeAfterSaleDO::getRefundClaimUntil, now.plusSeconds(Math.max(claimSeconds, 30)))
                .set(TradeAfterSaleDO::getRefundLastError, "");
        if (!force) {
            claim.and(wrapper -> wrapper.isNull(TradeAfterSaleDO::getRefundNextAttemptTime)
                    .or().le(TradeAfterSaleDO::getRefundNextAttemptTime, now));
        }
        if (tradeAfterSaleMapper.update(null, claim) != 1) return false;

        afterSale.setRefundAttemptCount(previousAttempts + 1);
        try {
            if (!tradeRefundProviderService.currentType().equals(afterSale.getRefundProvider())) {
                throw new ServerException(503, "退款渠道配置与售后单不一致");
            }
            TradeOrderDO order = tradeOrderMapper.selectById(afterSale.getOrderId());
            if (order == null) throw new ServerException(1404, "退款关联订单不存在");
            PayOrderDO payOrder = payOrderMapper.selectOne(new LambdaQueryWrapper<PayOrderDO>()
                    .eq(PayOrderDO::getOrderId, order.getId())
                    .eq(PayOrderDO::getUserId, order.getUserId())
                    .orderByDesc(PayOrderDO::getUpdateTime)
                    .last("LIMIT 1"));
            if (payOrder == null || !Integer.valueOf(PayOrderStatus.PAID).equals(payOrder.getStatus())) {
                throw new ServerException(409, "支付单当前不能退款");
            }
            TradeRefundProvider.RefundResult result = hasText(afterSale.getProviderRefundNo())
                    ? tradeRefundProviderService.query(new TradeRefundProvider.RefundQuery(
                            afterSale.getAfterSaleSn(), afterSale.getProviderRefundNo(),
                            payOrder.getPaySn(), afterSale.getRefundAmount()))
                    : tradeRefundProviderService.refund(new TradeRefundProvider.RefundRequest(
                            afterSale.getAfterSaleSn(), order.getOrderSn(), payOrder.getPaySn(),
                            afterSale.getRefundAmount(), payOrder.getAmount(), afterSale.getReason()));
            tradeAfterSaleService.applyRefundResult(
                    afterSaleId, result, operatorType, operatorId, previousAttempts + 1);
            return true;
        } catch (Exception exception) {
            tradeAfterSaleService.recordRefundExecutionFailure(
                    afterSaleId, previousAttempts + 1, safeMessage(exception));
            return false;
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) message = exception.getClass().getSimpleName();
        return message.length() <= 255 ? message : message.substring(0, 255);
    }
}
