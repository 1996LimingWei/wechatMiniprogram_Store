package com.shop.module.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.common.exception.ServerException;
import com.shop.module.trade.dal.dataobject.PayOrderDO;
import com.shop.module.trade.dal.dataobject.TradeAfterSaleDO;
import com.shop.module.trade.dal.dataobject.TradeOrderDO;
import com.shop.module.trade.dal.mysql.PayOrderMapper;
import com.shop.module.trade.dal.mysql.TradeAfterSaleMapper;
import com.shop.module.trade.dal.mysql.TradeOrderMapper;
import com.shop.module.trade.util.TradeMoneyUtils;
import com.shop.module.trade.util.TradeRequestUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class TradeAfterSaleService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TradeAfterSaleMapper tradeAfterSaleMapper;
    private final TradeOrderMapper tradeOrderMapper;
    private final PayOrderMapper payOrderMapper;
    private final TradeOrderLogService tradeOrderLogService;

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> apply(Long userId, Long orderId, Map<String, Object> request) {
        TradeOrderDO order = getUserOrder(userId, orderId);
        if (order.getPayStatus() == null || order.getPayStatus() != 1) {
            throw new ServerException(400, "未支付订单不能申请退款");
        }
        if (order.getStatus() == null || order.getStatus() == 0 || order.getStatus() == 4) {
            throw new ServerException(400, "当前订单不能申请售后");
        }
        if (order.getPayStatus() == 2) {
            throw new ServerException(400, "订单已退款");
        }
        TradeAfterSaleDO existed = getAfterSale(orderId);
        if (existed != null) {
            return toResp(existed);
        }

        String reason = TradeRequestUtils.getString(request, "reason", "用户申请退款");
        String remark = TradeRequestUtils.getString(request, "remark", "");
        TradeAfterSaleDO afterSale = new TradeAfterSaleDO();
        afterSale.setOrderId(order.getId());
        afterSale.setUserId(userId);
        afterSale.setAfterSaleSn(generateAfterSaleSn());
        afterSale.setType(order.getStatus() == 1 ? 1 : 2);
        afterSale.setStatus(0);
        afterSale.setRefundAmount(order.getActualPrice());
        afterSale.setReason(reason);
        afterSale.setApplyRemark(remark);
        afterSale.setBeforeOrderStatus(order.getStatus());
        afterSale.setApplyTime(LocalDateTime.now());
        tradeAfterSaleMapper.insert(afterSale);
        Integer fromStatus = order.getStatus();
        order.setStatus(5);
        tradeOrderMapper.updateById(order);
        tradeOrderLogService.recordStatusChanged(order, TradeOrderLogService.OPERATOR_USER, userId,
                "APPLY_AFTER_SALE", fromStatus, order.getStatus(), reason);
        return toResp(afterSale);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> mockApprove(Long userId, Long orderId) {
        return approve(userId, orderId, TradeOrderLogService.OPERATOR_USER, userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> adminApprove(Long orderId) {
        return approve(null, orderId, TradeOrderLogService.OPERATOR_ADMIN, 0L);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> adminReject(Long orderId, String rejectReason) {
        TradeOrderDO order = getUserOrder(null, orderId);
        TradeAfterSaleDO afterSale = getAfterSale(orderId);
        if (afterSale == null || afterSale.getStatus() == null || afterSale.getStatus() != 0) {
            throw new ServerException(400, "当前售后单不能拒绝");
        }
        if (order.getStatus() == null || order.getStatus() != 5) {
            throw new ServerException(400, "当前订单不在售后处理中");
        }

        Integer fromStatus = order.getStatus();
        Integer restoreStatus = getRestoreOrderStatus(afterSale);
        afterSale.setStatus(2);
        afterSale.setRejectReason(rejectReason == null || rejectReason.isBlank() ? "商家拒绝售后申请" : rejectReason);
        afterSale.setRejectTime(LocalDateTime.now());
        tradeAfterSaleMapper.updateById(afterSale);

        order.setStatus(restoreStatus);
        tradeOrderMapper.updateById(order);
        tradeOrderLogService.recordStatusChanged(order, TradeOrderLogService.OPERATOR_ADMIN, 0L,
                "REJECT_AFTER_SALE", fromStatus, order.getStatus(), afterSale.getRejectReason());
        return toResp(afterSale);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> cancel(Long userId, Long orderId) {
        TradeOrderDO order = getUserOrder(userId, orderId);
        TradeAfterSaleDO afterSale = getAfterSale(orderId);
        if (afterSale == null || afterSale.getStatus() == null || afterSale.getStatus() != 0) {
            throw new ServerException(400, "当前售后单不能撤销");
        }
        if (order.getStatus() == null || order.getStatus() != 5) {
            throw new ServerException(400, "当前订单不在售后处理中");
        }

        Integer fromStatus = order.getStatus();
        Integer restoreStatus = getRestoreOrderStatus(afterSale);
        afterSale.setStatus(3);
        afterSale.setCancelTime(LocalDateTime.now());
        tradeAfterSaleMapper.updateById(afterSale);

        order.setStatus(restoreStatus);
        tradeOrderMapper.updateById(order);
        tradeOrderLogService.recordStatusChanged(order, TradeOrderLogService.OPERATOR_USER, userId,
                "CANCEL_AFTER_SALE", fromStatus, order.getStatus(), "用户撤销售后申请");
        return toResp(afterSale);
    }

    public Map<String, Object> adminList(int page, int size, Integer status, Long userId, Long orderId) {
        LambdaQueryWrapper<TradeAfterSaleDO> wrapper = new LambdaQueryWrapper<TradeAfterSaleDO>()
                .orderByDesc(TradeAfterSaleDO::getCreateTime);
        if (status != null) {
            wrapper.eq(TradeAfterSaleDO::getStatus, status);
        }
        if (userId != null && userId > 0) {
            wrapper.eq(TradeAfterSaleDO::getUserId, userId);
        }
        if (orderId != null && orderId > 0) {
            wrapper.eq(TradeAfterSaleDO::getOrderId, orderId);
        }
        List<TradeAfterSaleDO> all = tradeAfterSaleMapper.selectList(wrapper);
        int fromIndex = Math.min(Math.max(page - 1, 0) * size, all.size());
        int toIndex = Math.min(fromIndex + size, all.size());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", all.subList(fromIndex, toIndex).stream().map(this::toResp).toList());
        result.put("page", page);
        result.put("total", all.size());
        return result;
    }

    private Map<String, Object> approve(Long userId, Long orderId, String operatorType, Long operatorId) {
        TradeOrderDO order = getUserOrder(userId, orderId);
        TradeAfterSaleDO afterSale = getAfterSale(orderId);
        if (afterSale == null) {
            afterSale = new TradeAfterSaleDO();
            afterSale.setOrderId(order.getId());
            afterSale.setUserId(userId);
            afterSale.setAfterSaleSn(generateAfterSaleSn());
            afterSale.setType(order.getStatus() == 1 ? 1 : 2);
            afterSale.setStatus(0);
            afterSale.setRefundAmount(order.getActualPrice());
            afterSale.setReason("模拟退款");
            afterSale.setApplyRemark("");
            afterSale.setBeforeOrderStatus(order.getStatus());
            afterSale.setApplyTime(LocalDateTime.now());
            tradeAfterSaleMapper.insert(afterSale);
        }
        if (afterSale.getStatus() == 1) {
            return toResp(afterSale);
        }
        if (order.getPayStatus() == null || order.getPayStatus() != 1) {
            throw new ServerException(400, "当前订单不能退款");
        }

        afterSale.setStatus(1);
        afterSale.setAuditTime(LocalDateTime.now());
        tradeAfterSaleMapper.updateById(afterSale);

        Integer fromStatus = order.getStatus();
        Integer fromPayStatus = order.getPayStatus();
        order.setPayStatus(2);
        order.setStatus(5);
        tradeOrderMapper.updateById(order);
        tradeOrderLogService.recordPayChanged(order, operatorType, operatorId,
                "REFUND_SUCCESS", fromStatus, order.getStatus(), fromPayStatus, order.getPayStatus(),
                "Mock 退款审核通过");

        PayOrderDO payOrder = payOrderMapper.selectOne(new LambdaQueryWrapper<PayOrderDO>()
                .eq(PayOrderDO::getOrderId, orderId)
                .eq(PayOrderDO::getUserId, userId));
        if (payOrder != null) {
            payOrder.setStatus(3);
            payOrderMapper.updateById(payOrder);
        }
        return toResp(afterSale);
    }

    public Map<String, Object> detail(Long userId, Long orderId) {
        getUserOrder(userId, orderId);
        TradeAfterSaleDO afterSale = getAfterSale(orderId);
        return afterSale == null ? emptyResp() : toResp(afterSale);
    }

    public Map<String, Object> getOrderAfterSaleInfo(Long orderId) {
        TradeAfterSaleDO afterSale = getAfterSale(orderId);
        return afterSale == null ? emptyResp() : toResp(afterSale);
    }

    private TradeOrderDO getUserOrder(Long userId, Long orderId) {
        LambdaQueryWrapper<TradeOrderDO> wrapper = new LambdaQueryWrapper<TradeOrderDO>()
                .eq(TradeOrderDO::getId, orderId);
        if (userId != null) {
            wrapper.eq(TradeOrderDO::getUserId, userId);
        }
        TradeOrderDO order = tradeOrderMapper.selectOne(wrapper);
        if (order == null) {
            throw new ServerException(1404, "订单不存在");
        }
        return order;
    }

    private TradeAfterSaleDO getAfterSale(Long orderId) {
        return tradeAfterSaleMapper.selectOne(new LambdaQueryWrapper<TradeAfterSaleDO>()
                .eq(TradeAfterSaleDO::getOrderId, orderId)
                .orderByDesc(TradeAfterSaleDO::getUpdateTime)
                .last("LIMIT 1"));
    }

    private Map<String, Object> emptyResp() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("hasAfterSale", false);
        return result;
    }

    private Map<String, Object> toResp(TradeAfterSaleDO afterSale) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("hasAfterSale", true);
        result.put("id", afterSale.getId());
        result.put("orderId", afterSale.getOrderId());
        result.put("afterSaleSn", afterSale.getAfterSaleSn());
        result.put("type", afterSale.getType());
        result.put("typeText", afterSale.getType() != null && afterSale.getType() == 1 ? "仅退款" : "退货退款");
        result.put("status", afterSale.getStatus());
        result.put("statusText", getStatusText(afterSale.getStatus()));
        result.put("refundAmount", TradeMoneyUtils.formatYuan(afterSale.getRefundAmount()));
        result.put("reason", afterSale.getReason());
        result.put("applyRemark", afterSale.getApplyRemark());
        result.put("beforeOrderStatus", afterSale.getBeforeOrderStatus());
        result.put("rejectReason", afterSale.getRejectReason());
        result.put("applyTime", formatTime(afterSale.getApplyTime()));
        result.put("auditTime", formatTime(afterSale.getAuditTime()));
        result.put("rejectTime", formatTime(afterSale.getRejectTime()));
        result.put("cancelTime", formatTime(afterSale.getCancelTime()));
        return result;
    }

    private String getStatusText(Integer status) {
        return switch (status == null ? 0 : status) {
            case 0 -> "退款处理中";
            case 1 -> "已退款";
            case 2 -> "退款已拒绝";
            case 3 -> "已撤销";
            default -> "未知";
        };
    }

    private Integer getRestoreOrderStatus(TradeAfterSaleDO afterSale) {
        if (afterSale.getBeforeOrderStatus() != null && afterSale.getBeforeOrderStatus() >= 1
                && afterSale.getBeforeOrderStatus() <= 3) {
            return afterSale.getBeforeOrderStatus();
        }
        return 1;
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? "" : time.format(TIME_FORMATTER);
    }

    private String generateAfterSaleSn() {
        return "R" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(1000, 9999);
    }
}
