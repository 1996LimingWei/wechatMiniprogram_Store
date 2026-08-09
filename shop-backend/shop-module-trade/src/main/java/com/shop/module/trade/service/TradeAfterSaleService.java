package com.shop.module.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shop.common.exception.ServerException;
import com.shop.module.trade.dal.dataobject.PayOrderDO;
import com.shop.module.trade.dal.dataobject.TradeAfterSaleDO;
import com.shop.module.trade.dal.dataobject.TradeOrderDO;
import com.shop.module.trade.dal.dataobject.TradeOrderItemDO;
import com.shop.module.trade.dal.mysql.PayOrderMapper;
import com.shop.module.trade.dal.mysql.TradeAfterSaleMapper;
import com.shop.module.trade.dal.mysql.TradeOrderItemMapper;
import com.shop.module.trade.dal.mysql.TradeOrderMapper;
import com.shop.module.trade.service.provider.TradeRefundProvider;
import com.shop.module.trade.service.provider.TradeRefundProviderService;
import com.shop.module.trade.util.TradeMoneyUtils;
import com.shop.module.trade.util.TradeRequestUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class TradeAfterSaleService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TradeAfterSaleMapper tradeAfterSaleMapper;
    private final TradeOrderMapper tradeOrderMapper;
    private final PayOrderMapper payOrderMapper;
    private final TradeOrderItemMapper tradeOrderItemMapper;
    private final TradeProductService tradeProductService;
    private final TradeOrderLogService tradeOrderLogService;
    private final TradeRefundProviderService tradeRefundProviderService;

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> apply(Long userId, Long orderId, Map<String, Object> request) {
        TradeOrderDO order = getUserOrder(userId, orderId);
        if (order.getPayStatus() == null || order.getPayStatus() != TradeOrderPayStatus.PAID) {
            throw new ServerException(400, "未支付订单不能申请退款");
        }
        if (order.getStatus() == null || order.getStatus() == 0 || order.getStatus() == 4) {
            throw new ServerException(400, "当前订单不能申请售后");
        }
        if (order.getPayStatus() == TradeOrderPayStatus.REFUNDED) {
            throw new ServerException(400, "订单已退款");
        }
        TradeAfterSaleDO existed = getAfterSale(orderId);
        if (existed != null && existed.getStatus() != null
                && (existed.getStatus() == 0 || existed.getStatus() == 1 || existed.getStatus() == 4)) {
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
        Integer fromStatus = order.getStatus();
        int orderUpdated = tradeOrderMapper.update(null, new LambdaUpdateWrapper<TradeOrderDO>()
                .eq(TradeOrderDO::getId, order.getId())
                .eq(TradeOrderDO::getUserId, userId)
                .eq(TradeOrderDO::getStatus, fromStatus)
                .eq(TradeOrderDO::getPayStatus, TradeOrderPayStatus.PAID)
                .set(TradeOrderDO::getStatus, 5));
        if (orderUpdated != 1) {
            TradeAfterSaleDO concurrent = getAfterSale(orderId);
            if (concurrent != null) {
                return toResp(concurrent);
            }
            throw new ServerException(400, "订单状态已变更，不能申请售后");
        }
        try {
            tradeAfterSaleMapper.insert(afterSale);
        } catch (org.springframework.dao.DuplicateKeyException exception) {
            TradeAfterSaleDO concurrent = getAfterSale(orderId);
            if (concurrent != null) {
                return toResp(concurrent);
            }
            throw exception;
        }
        order.setStatus(5);
        tradeOrderLogService.recordStatusChanged(order, TradeOrderLogService.OPERATOR_USER, userId,
                "APPLY_AFTER_SALE", fromStatus, order.getStatus(), reason);
        return toResp(afterSale);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> mockApprove(Long userId, Long orderId) {
        return approve(userId, orderId, null, TradeOrderLogService.OPERATOR_USER, userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> adminApprove(Long adminId, Long afterSaleId) {
        TradeAfterSaleDO afterSale = getAfterSaleById(afterSaleId);
        return approve(null, afterSale.getOrderId(), afterSale,
                TradeOrderLogService.OPERATOR_ADMIN, adminId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> adminReject(Long adminId, Long afterSaleId, String rejectReason) {
        TradeAfterSaleDO afterSale = getAfterSaleById(afterSaleId);
        TradeOrderDO order = getUserOrder(null, afterSale.getOrderId());
        if (afterSale == null || afterSale.getStatus() == null || afterSale.getStatus() != 0) {
            throw new ServerException(400, "当前售后单不能拒绝");
        }
        if (order.getStatus() == null || order.getStatus() != 5) {
            throw new ServerException(400, "当前订单不在售后处理中");
        }

        String normalizedReason = rejectReason == null ? "" : rejectReason.trim();
        if (normalizedReason.length() < 4 || normalizedReason.length() > 200) {
            throw new ServerException(400, "拒绝原因长度应为 4 至 200 个字符");
        }

        Integer fromStatus = order.getStatus();
        Integer restoreStatus = getRestoreOrderStatus(afterSale);
        LocalDateTime rejectTime = LocalDateTime.now();
        int afterSaleUpdated = tradeAfterSaleMapper.update(null, new LambdaUpdateWrapper<TradeAfterSaleDO>()
                .eq(TradeAfterSaleDO::getId, afterSale.getId())
                .eq(TradeAfterSaleDO::getStatus, 0)
                .set(TradeAfterSaleDO::getStatus, 2)
                .set(TradeAfterSaleDO::getRejectReason, normalizedReason)
                .set(TradeAfterSaleDO::getRejectTime, rejectTime));
        if (afterSaleUpdated != 1) {
            throw new ServerException(400, "售后单状态已变更，不能拒绝");
        }

        int orderUpdated = tradeOrderMapper.update(null, new LambdaUpdateWrapper<TradeOrderDO>()
                .eq(TradeOrderDO::getId, order.getId())
                .eq(TradeOrderDO::getStatus, 5)
                .set(TradeOrderDO::getStatus, restoreStatus));
        if (orderUpdated != 1) {
            throw new ServerException(400, "订单状态已变更，不能拒绝售后");
        }
        afterSale.setStatus(2);
        afterSale.setRejectReason(normalizedReason);
        afterSale.setRejectTime(rejectTime);
        order.setStatus(restoreStatus);
        tradeOrderLogService.recordStatusChanged(order, TradeOrderLogService.OPERATOR_ADMIN, adminId,
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
                .orderByDesc(TradeAfterSaleDO::getCreateTime)
                .orderByDesc(TradeAfterSaleDO::getId);
        if (status != null) {
            wrapper.eq(TradeAfterSaleDO::getStatus, status);
        }
        if (userId != null && userId > 0) {
            wrapper.eq(TradeAfterSaleDO::getUserId, userId);
        }
        if (orderId != null && orderId > 0) {
            wrapper.eq(TradeAfterSaleDO::getOrderId, orderId);
        }
        int finalPage = Math.max(page, 1);
        int finalSize = Math.min(Math.max(size, 1), 100);
        Page<TradeAfterSaleDO> pageResult = tradeAfterSaleMapper.selectPage(
                new Page<>(finalPage, finalSize), wrapper);
        List<TradeAfterSaleDO> records = pageResult.getRecords();
        Set<Long> orderIds = new LinkedHashSet<>();
        for (TradeAfterSaleDO afterSale : records) {
            orderIds.add(afterSale.getOrderId());
        }
        Map<Long, String> orderSnById = new LinkedHashMap<>();
        if (!orderIds.isEmpty()) {
            for (TradeOrderDO order : tradeOrderMapper.selectBatchIds(orderIds)) {
                orderSnById.put(order.getId(), order.getOrderSn());
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", records.stream()
                .map(afterSale -> toAdminResp(afterSale, orderSnById.get(afterSale.getOrderId())))
                .toList());
        result.put("page", finalPage);
        result.put("total", pageResult.getTotal());
        return result;
    }

    private Map<String, Object> approve(Long userId, Long orderId, TradeAfterSaleDO selectedAfterSale,
                                        String operatorType, Long operatorId) {
        TradeOrderDO order = getUserOrder(userId, orderId);
        TradeAfterSaleDO afterSale = selectedAfterSale == null ? getAfterSale(orderId) : selectedAfterSale;
        if (afterSale == null || afterSale.getStatus() == null) {
            throw new ServerException(400, "请先提交有效的售后申请");
        }
        if (afterSale.getStatus() == 1) {
            return toResp(afterSale);
        }
        if (afterSale.getStatus() == 4) {
            return syncProcessingInternal(afterSale, operatorType, operatorId);
        }
        if (afterSale.getStatus() != 0) {
            throw new ServerException(400, "当前售后单不能退款");
        }
        if (order.getPayStatus() == null || order.getPayStatus() != TradeOrderPayStatus.PAID) {
            throw new ServerException(400, "当前订单不能退款");
        }
        if (order.getStatus() == null || order.getStatus() != 5) {
            throw new ServerException(400, "当前订单不在售后处理中");
        }

        PayOrderDO payOrder = getPaidPayOrder(order);
        if (payOrder == null || payOrder.getStatus() == null || payOrder.getStatus() != PayOrderStatus.PAID) {
            throw new ServerException(400, "支付单当前不能退款");
        }
        if (payOrder.getAmount() == null || !payOrder.getAmount().equals(afterSale.getRefundAmount())) {
            throw new ServerException(400, "退款金额与支付单金额不一致");
        }

        String refundProvider = tradeRefundProviderService.currentType();
        TradeRefundProvider.RefundResult refundResult = tradeRefundProviderService.refund(
                new TradeRefundProvider.RefundRequest(
                        afterSale.getAfterSaleSn(),
                        order.getOrderSn(),
                        payOrder.getPaySn(),
                        afterSale.getRefundAmount(),
                        afterSale.getReason()));
        if (refundResult.status() == TradeRefundProvider.RefundStatus.FAILED) {
            return failRefund(afterSale, order, refundProvider, refundResult,
                    0, operatorType, operatorId);
        }
        if (refundResult.status() == TradeRefundProvider.RefundStatus.PROCESSING) {
            LocalDateTime auditTime = LocalDateTime.now();
            int afterSaleUpdated = tradeAfterSaleMapper.update(null, new LambdaUpdateWrapper<TradeAfterSaleDO>()
                    .eq(TradeAfterSaleDO::getId, afterSale.getId())
                    .eq(TradeAfterSaleDO::getStatus, 0)
                    .set(TradeAfterSaleDO::getStatus, 4)
                    .set(TradeAfterSaleDO::getAuditTime, auditTime)
                    .set(TradeAfterSaleDO::getRefundProvider, refundProvider)
                    .set(TradeAfterSaleDO::getProviderRefundNo, refundResult.providerRefundNo())
                    .set(TradeAfterSaleDO::getRefundMessage, refundResult.message()));
            if (afterSaleUpdated != 1) {
                return toResp(getAfterSaleById(afterSale.getId()));
            }
            afterSale.setStatus(4);
            afterSale.setAuditTime(auditTime);
            afterSale.setRefundProvider(refundProvider);
            afterSale.setProviderRefundNo(refundResult.providerRefundNo());
            afterSale.setRefundMessage(refundResult.message());
            tradeOrderLogService.recordStatusChanged(order, operatorType, operatorId,
                    "REFUND_PROCESSING", order.getStatus(), order.getStatus(), refundResult.message());
            return toResp(afterSale);
        }
        return completeRefund(afterSale, order, payOrder, refundProvider, refundResult,
                0, operatorType, operatorId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> syncProcessing(Long adminId, Long afterSaleId) {
        return syncProcessingInternal(getAfterSaleById(afterSaleId),
                TradeOrderLogService.OPERATOR_ADMIN, adminId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> syncProcessingBySystem(Long afterSaleId) {
        return syncProcessingInternal(getAfterSaleById(afterSaleId),
                TradeOrderLogService.OPERATOR_SYSTEM, 0L);
    }

    public List<Long> listProcessingIds(int limit) {
        return tradeAfterSaleMapper.selectList(new LambdaQueryWrapper<TradeAfterSaleDO>()
                        .eq(TradeAfterSaleDO::getStatus, 4)
                        .orderByAsc(TradeAfterSaleDO::getUpdateTime)
                        .last("LIMIT " + Math.min(Math.max(limit, 1), 100)))
                .stream()
                .map(TradeAfterSaleDO::getId)
                .toList();
    }

    private Map<String, Object> syncProcessingInternal(TradeAfterSaleDO afterSale,
                                                        String operatorType, Long operatorId) {
        if (afterSale.getStatus() == null || afterSale.getStatus() != 4) {
            return toResp(afterSale);
        }
        if (!tradeRefundProviderService.currentType().equals(afterSale.getRefundProvider())) {
            throw new ServerException(503, "退款渠道配置与售后单不一致");
        }
        TradeOrderDO order = getUserOrder(null, afterSale.getOrderId());
        PayOrderDO payOrder = getPaidPayOrder(order);
        if (payOrder == null || payOrder.getStatus() == null || payOrder.getStatus() != PayOrderStatus.PAID) {
            throw new ServerException(400, "支付单当前不能同步退款");
        }
        TradeRefundProvider.RefundResult result = tradeRefundProviderService.query(
                new TradeRefundProvider.RefundQuery(
                        afterSale.getAfterSaleSn(),
                        afterSale.getProviderRefundNo(),
                        payOrder.getPaySn(),
                        afterSale.getRefundAmount()));
        if (result.status() == TradeRefundProvider.RefundStatus.PROCESSING) {
            tradeAfterSaleMapper.update(null, new LambdaUpdateWrapper<TradeAfterSaleDO>()
                    .eq(TradeAfterSaleDO::getId, afterSale.getId())
                    .eq(TradeAfterSaleDO::getStatus, 4)
                    .set(TradeAfterSaleDO::getRefundMessage, result.message()));
            afterSale.setRefundMessage(result.message());
            return toResp(afterSale);
        }
        if (result.status() == TradeRefundProvider.RefundStatus.FAILED) {
            return failRefund(afterSale, order, afterSale.getRefundProvider(), result,
                    4, operatorType, operatorId);
        }
        return completeRefund(afterSale, order, payOrder, afterSale.getRefundProvider(), result,
                4, operatorType, operatorId);
    }

    private Map<String, Object> failRefund(
            TradeAfterSaleDO afterSale, TradeOrderDO order, String refundProvider,
            TradeRefundProvider.RefundResult refundResult, int expectedAfterSaleStatus,
            String operatorType, Long operatorId) {
        LocalDateTime auditTime = LocalDateTime.now();
        int afterSaleUpdated = tradeAfterSaleMapper.update(null, new LambdaUpdateWrapper<TradeAfterSaleDO>()
                .eq(TradeAfterSaleDO::getId, afterSale.getId())
                .eq(TradeAfterSaleDO::getStatus, expectedAfterSaleStatus)
                .set(TradeAfterSaleDO::getStatus, 5)
                .set(TradeAfterSaleDO::getAuditTime, auditTime)
                .set(TradeAfterSaleDO::getRefundProvider, refundProvider)
                .set(TradeAfterSaleDO::getProviderRefundNo, refundResult.providerRefundNo())
                .set(TradeAfterSaleDO::getRefundMessage, refundResult.message()));
        if (afterSaleUpdated != 1) {
            return toResp(getAfterSaleById(afterSale.getId()));
        }

        Integer fromStatus = order.getStatus();
        Integer restoreStatus = getRestoreOrderStatus(afterSale);
        int orderUpdated = tradeOrderMapper.update(null, new LambdaUpdateWrapper<TradeOrderDO>()
                .eq(TradeOrderDO::getId, order.getId())
                .eq(TradeOrderDO::getStatus, 5)
                .eq(TradeOrderDO::getPayStatus, TradeOrderPayStatus.PAID)
                .set(TradeOrderDO::getStatus, restoreStatus));
        if (orderUpdated != 1) {
            throw new ServerException(400, "订单状态已变更，不能结束失败退款");
        }

        afterSale.setStatus(5);
        afterSale.setAuditTime(auditTime);
        afterSale.setRefundProvider(refundProvider);
        afterSale.setProviderRefundNo(refundResult.providerRefundNo());
        afterSale.setRefundMessage(refundResult.message());
        order.setStatus(restoreStatus);
        tradeOrderLogService.recordStatusChanged(order, operatorType, operatorId,
                "REFUND_FAILED", fromStatus, restoreStatus, refundResult.message());
        return toResp(afterSale);
    }

    private Map<String, Object> completeRefund(
            TradeAfterSaleDO afterSale, TradeOrderDO order, PayOrderDO payOrder,
            String refundProvider, TradeRefundProvider.RefundResult refundResult,
            int expectedAfterSaleStatus, String operatorType, Long operatorId) {
        LocalDateTime refundTime = LocalDateTime.now();
        int afterSaleUpdated = tradeAfterSaleMapper.update(null, new LambdaUpdateWrapper<TradeAfterSaleDO>()
                .eq(TradeAfterSaleDO::getId, afterSale.getId())
                .eq(TradeAfterSaleDO::getStatus, expectedAfterSaleStatus)
                .set(TradeAfterSaleDO::getStatus, 1)
                .set(TradeAfterSaleDO::getAuditTime, refundTime)
                .set(TradeAfterSaleDO::getRefundTime, refundTime)
                .set(TradeAfterSaleDO::getRefundProvider, refundProvider)
                .set(TradeAfterSaleDO::getProviderRefundNo, refundResult.providerRefundNo())
                .set(TradeAfterSaleDO::getRefundMessage, refundResult.message()));
        if (afterSaleUpdated != 1) {
            TradeAfterSaleDO latest = getAfterSaleById(afterSale.getId());
            if (latest.getStatus() != null && latest.getStatus() == 1) {
                return toResp(latest);
            }
            throw new ServerException(400, "售后单状态已变更，不能完成退款");
        }

        int payOrderUpdated = payOrderMapper.update(null, new LambdaUpdateWrapper<PayOrderDO>()
                .eq(PayOrderDO::getId, payOrder.getId())
                .eq(PayOrderDO::getStatus, PayOrderStatus.PAID)
                .set(PayOrderDO::getStatus, PayOrderStatus.REFUNDED));
        if (payOrderUpdated != 1) {
            throw new ServerException(400, "支付单状态已变更，不能退款");
        }

        Integer fromStatus = order.getStatus();
        Integer fromPayStatus = order.getPayStatus();
        int orderUpdated = tradeOrderMapper.update(null, new LambdaUpdateWrapper<TradeOrderDO>()
                .eq(TradeOrderDO::getId, order.getId())
                .eq(TradeOrderDO::getPayStatus, TradeOrderPayStatus.PAID)
                .set(TradeOrderDO::getPayStatus, TradeOrderPayStatus.REFUNDED)
                .set(TradeOrderDO::getStatus, 5));
        if (orderUpdated != 1) {
            throw new ServerException(400, "订单状态已变更，不能退款");
        }

        afterSale.setStatus(1);
        afterSale.setAuditTime(refundTime);
        afterSale.setRefundTime(refundTime);
        afterSale.setRefundProvider(refundProvider);
        afterSale.setProviderRefundNo(refundResult.providerRefundNo());
        afterSale.setRefundMessage(refundResult.message());
        payOrder.setStatus(PayOrderStatus.REFUNDED);
        order.setPayStatus(TradeOrderPayStatus.REFUNDED);
        order.setStatus(5);
        if (Integer.valueOf(1).equals(afterSale.getBeforeOrderStatus())) {
            List<TradeOrderItemDO> orderItems = tradeOrderItemMapper.selectList(
                    new LambdaQueryWrapper<TradeOrderItemDO>()
                            .eq(TradeOrderItemDO::getOrderId, order.getId()));
            for (TradeOrderItemDO orderItem : orderItems) {
                tradeProductService.recoverStock(orderItem.getSkuId(), orderItem.getCount());
            }
        }
        tradeOrderLogService.recordPayChanged(order, operatorType, operatorId,
                "REFUND_SUCCESS", fromStatus, order.getStatus(), fromPayStatus, order.getPayStatus(),
                refundResult.message());
        return toResp(afterSale);
    }

    private PayOrderDO getPaidPayOrder(TradeOrderDO order) {
        return payOrderMapper.selectOne(new LambdaQueryWrapper<PayOrderDO>()
                .eq(PayOrderDO::getOrderId, order.getId())
                .eq(PayOrderDO::getUserId, order.getUserId())
                .orderByDesc(PayOrderDO::getUpdateTime)
                .last("LIMIT 1"));
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

    private TradeAfterSaleDO getAfterSaleById(Long afterSaleId) {
        if (afterSaleId == null || afterSaleId <= 0) {
            throw new ServerException(400, "售后单 ID 不能为空");
        }
        TradeAfterSaleDO afterSale = tradeAfterSaleMapper.selectById(afterSaleId);
        if (afterSale == null) {
            throw new ServerException(1404, "售后单不存在");
        }
        return afterSale;
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
        result.put("userId", afterSale.getUserId());
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
        result.put("refundProvider", afterSale.getRefundProvider());
        result.put("providerRefundNo", afterSale.getProviderRefundNo());
        result.put("refundMessage", afterSale.getRefundMessage());
        result.put("applyTime", formatTime(afterSale.getApplyTime()));
        result.put("auditTime", formatTime(afterSale.getAuditTime()));
        result.put("refundTime", formatTime(afterSale.getRefundTime()));
        result.put("rejectTime", formatTime(afterSale.getRejectTime()));
        result.put("cancelTime", formatTime(afterSale.getCancelTime()));
        return result;
    }

    private Map<String, Object> toAdminResp(TradeAfterSaleDO afterSale, String orderSn) {
        Map<String, Object> result = toResp(afterSale);
        result.put("orderSn", orderSn == null ? "" : orderSn);
        return result;
    }

    private String getStatusText(Integer status) {
        return switch (status == null ? 0 : status) {
            case 0 -> "待审核";
            case 1 -> "已退款";
            case 2 -> "退款已拒绝";
            case 3 -> "已撤销";
            case 4 -> "退款处理中";
            case 5 -> "退款失败";
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
