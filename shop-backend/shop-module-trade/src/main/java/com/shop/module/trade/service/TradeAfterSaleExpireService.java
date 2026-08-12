package com.shop.module.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.shop.common.exception.ServerException;
import com.shop.module.trade.dal.dataobject.TradeAfterSaleDO;
import com.shop.module.trade.dal.dataobject.TradeOrderDO;
import com.shop.module.trade.dal.mysql.TradeAfterSaleMapper;
import com.shop.module.trade.dal.mysql.TradeOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TradeAfterSaleExpireService {

    private static final int STATUS_CANCELLED = 3;
    private static final int STATUS_WAIT_RETURN = 6;

    private final TradeAfterSaleMapper tradeAfterSaleMapper;
    private final TradeOrderMapper tradeOrderMapper;
    private final TradeOrderLogService tradeOrderLogService;

    public List<Long> listOverdueIds(int limit) {
        return tradeAfterSaleMapper.selectList(new LambdaQueryWrapper<TradeAfterSaleDO>()
                        .eq(TradeAfterSaleDO::getStatus, STATUS_WAIT_RETURN)
                        .isNotNull(TradeAfterSaleDO::getReturnDeadline)
                        .lt(TradeAfterSaleDO::getReturnDeadline, LocalDateTime.now())
                        .orderByAsc(TradeAfterSaleDO::getReturnDeadline)
                        .last("LIMIT " + Math.min(Math.max(limit, 1), 200)))
                .stream()
                .map(TradeAfterSaleDO::getId)
                .toList();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean expireOne(Long afterSaleId) {
        LocalDateTime now = LocalDateTime.now();
        TradeAfterSaleDO afterSale = tradeAfterSaleMapper.selectById(afterSaleId);
        if (afterSale == null || !Integer.valueOf(STATUS_WAIT_RETURN).equals(afterSale.getStatus())
                || afterSale.getReturnDeadline() == null || !afterSale.getReturnDeadline().isBefore(now)) {
            return false;
        }
        int updated = tradeAfterSaleMapper.update(null, new LambdaUpdateWrapper<TradeAfterSaleDO>()
                .eq(TradeAfterSaleDO::getId, afterSaleId)
                .eq(TradeAfterSaleDO::getStatus, STATUS_WAIT_RETURN)
                .lt(TradeAfterSaleDO::getReturnDeadline, now)
                .set(TradeAfterSaleDO::getStatus, STATUS_CANCELLED)
                .set(TradeAfterSaleDO::getCancelTime, now)
                .set(TradeAfterSaleDO::getRejectReason, "超过退货寄回期限，售后自动关闭"));
        if (updated != 1) return false;

        TradeOrderDO order = tradeOrderMapper.selectById(afterSale.getOrderId());
        if (order == null) throw new ServerException(1404, "退货超期关联订单不存在");
        int restoreStatus = normalizeRestoreStatus(afterSale.getBeforeOrderStatus());
        int orderUpdated = tradeOrderMapper.update(null, new LambdaUpdateWrapper<TradeOrderDO>()
                .eq(TradeOrderDO::getId, order.getId())
                .eq(TradeOrderDO::getStatus, 5)
                .eq(TradeOrderDO::getPayStatus, TradeOrderPayStatus.PAID)
                .set(TradeOrderDO::getStatus, restoreStatus));
        if (orderUpdated != 1) throw new ServerException(409, "退货超期关闭时订单状态已变化");

        order.setStatus(restoreStatus);
        tradeOrderLogService.recordStatusChanged(order, TradeOrderLogService.OPERATOR_SYSTEM, 0L,
                "RETURN_DEADLINE_EXPIRED", 5, restoreStatus, "买家未在期限内寄回商品");
        return true;
    }

    private int normalizeRestoreStatus(Integer status) {
        return status != null && (status == 1 || status == 2 || status == 3) ? status : 1;
    }
}
