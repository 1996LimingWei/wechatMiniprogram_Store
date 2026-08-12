package com.shop.module.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shop.common.exception.ServerException;
import com.shop.module.trade.dal.dataobject.PayOrderDO;
import com.shop.module.trade.dal.dataobject.TradeAfterSaleDO;
import com.shop.module.trade.dal.dataobject.TradeAfterSaleItemDO;
import com.shop.module.trade.dal.dataobject.TradeOrderDO;
import com.shop.module.trade.dal.dataobject.TradeOrderItemDO;
import com.shop.module.trade.dal.mysql.PayOrderMapper;
import com.shop.module.trade.dal.mysql.TradeAfterSaleMapper;
import com.shop.module.trade.dal.mysql.TradeAfterSaleItemMapper;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TradeAfterSaleService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int STATUS_PENDING = 0;
    private static final int STATUS_REFUNDED = 1;
    private static final int STATUS_REJECTED = 2;
    private static final int STATUS_CANCELLED = 3;
    private static final int STATUS_REFUNDING = 4;
    private static final int STATUS_REFUND_FAILED = 5;
    private static final int STATUS_WAIT_RETURN = 6;
    private static final int STATUS_WAIT_RECEIVE = 7;
    private static final int AFTER_SALE_DAYS = 7;
    private static final int RETURN_DAYS = 7;

    private final TradeAfterSaleMapper tradeAfterSaleMapper;
    private final TradeAfterSaleItemMapper tradeAfterSaleItemMapper;
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
        if (Integer.valueOf(3).equals(order.getStatus()) && order.getUpdateTime() != null
                && order.getUpdateTime().plusDays(AFTER_SALE_DAYS).isBefore(LocalDateTime.now())) {
            throw new ServerException(400, "订单已超过七天售后申请期限");
        }
        TradeAfterSaleDO existed = getAfterSale(orderId);
        if (existed != null && existed.getStatus() != null
                && Set.of(STATUS_PENDING, STATUS_REFUNDING, STATUS_WAIT_RETURN, STATUS_WAIT_RECEIVE)
                .contains(existed.getStatus())) {
            return toResp(existed);
        }

        String reason = TradeRequestUtils.getString(request, "reason", "用户申请退款").trim();
        String remark = TradeRequestUtils.getString(request, "remark", "").trim();
        if (reason.length() < 2 || reason.length() > 128) {
            throw new ServerException(400, "售后原因长度应为 2 至 128 个字符");
        }
        if (remark.length() > 255) {
            throw new ServerException(400, "售后说明不能超过 255 个字符");
        }
        List<TradeOrderItemDO> orderItems = tradeOrderItemMapper.selectList(
                new LambdaQueryWrapper<TradeOrderItemDO>().eq(TradeOrderItemDO::getOrderId, orderId));
        List<TradeAfterSaleItemDO> afterSaleItems = buildAfterSaleItems(orderItems, request);
        int goodsRefundAmount = afterSaleItems.stream()
                .mapToInt(TradeAfterSaleItemDO::getRefundAmount)
                .reduce(0, Math::addExact);
        int alreadyRefunded = order.getRefundedAmount() == null ? 0 : order.getRefundedAmount();
        int availableAmount = Math.subtractExact(order.getActualPrice(), alreadyRefunded);
        boolean allRemainingItems = isAllRemainingItems(orderItems, afterSaleItems);
        int refundAmount = allRemainingItems ? availableAmount : goodsRefundAmount;
        if (refundAmount <= 0 || refundAmount > availableAmount) {
            throw new ServerException(400, "售后退款金额超出订单可退金额");
        }
        TradeAfterSaleDO afterSale = new TradeAfterSaleDO();
        afterSale.setOrderId(order.getId());
        afterSale.setUserId(userId);
        afterSale.setAfterSaleSn(generateAfterSaleSn());
        afterSale.setType(order.getStatus() == 1 ? 1 : 2);
        afterSale.setStatus(STATUS_PENDING);
        afterSale.setRefundAmount(refundAmount);
        afterSale.setReason(reason);
        afterSale.setApplyRemark(remark);
        afterSale.setBeforeOrderStatus(order.getStatus());
        afterSale.setApplyTime(LocalDateTime.now());
        afterSale.setStockRecovered(0);
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
            for (TradeAfterSaleItemDO item : afterSaleItems) {
                item.setAfterSaleId(afterSale.getId());
                tradeAfterSaleItemMapper.insert(item);
            }
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
    public Map<String, Object> submitReturn(Long userId, Long orderId, String company, String returnNo) {
        TradeOrderDO order = getUserOrder(userId, orderId);
        TradeAfterSaleDO afterSale = getAfterSale(orderId);
        if (afterSale == null || !Integer.valueOf(STATUS_WAIT_RETURN).equals(afterSale.getStatus())) {
            throw new ServerException(400, "当前售后单不需要填写退货物流");
        }
        if (afterSale.getReturnDeadline() != null && afterSale.getReturnDeadline().isBefore(LocalDateTime.now())) {
            throw new ServerException(400, "已超过退货寄回期限，请联系商家处理");
        }
        String normalizedCompany = company == null ? "" : company.trim();
        String normalizedNo = returnNo == null ? "" : returnNo.trim();
        if (normalizedCompany.length() < 2 || normalizedCompany.length() > 64) {
            throw new ServerException(400, "物流公司长度应为 2 至 64 个字符");
        }
        if (!normalizedNo.matches("[A-Za-z0-9-]{6,32}")) {
            throw new ServerException(400, "物流单号仅支持 6 至 32 位字母、数字或连字符");
        }
        LocalDateTime returnTime = LocalDateTime.now();
        int updated = tradeAfterSaleMapper.update(null, new LambdaUpdateWrapper<TradeAfterSaleDO>()
                .eq(TradeAfterSaleDO::getId, afterSale.getId())
                .eq(TradeAfterSaleDO::getStatus, STATUS_WAIT_RETURN)
                .set(TradeAfterSaleDO::getStatus, STATUS_WAIT_RECEIVE)
                .set(TradeAfterSaleDO::getReturnCompany, normalizedCompany)
                .set(TradeAfterSaleDO::getReturnNo, normalizedNo)
                .set(TradeAfterSaleDO::getReturnTime, returnTime));
        if (updated != 1) {
            throw new ServerException(409, "售后状态已变化，请刷新后重试");
        }
        afterSale.setStatus(STATUS_WAIT_RECEIVE);
        afterSale.setReturnCompany(normalizedCompany);
        afterSale.setReturnNo(normalizedNo);
        afterSale.setReturnTime(returnTime);
        tradeOrderLogService.recordStatusChanged(order, TradeOrderLogService.OPERATOR_USER, userId,
                "SUBMIT_RETURN_LOGISTICS", order.getStatus(), order.getStatus(),
                normalizedCompany + " " + normalizedNo);
        return toResp(afterSale);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> adminReceive(Long adminId, Long afterSaleId, String receiveRemark) {
        TradeAfterSaleDO afterSale = getAfterSaleById(afterSaleId);
        if (!Integer.valueOf(STATUS_WAIT_RECEIVE).equals(afterSale.getStatus())) {
            throw new ServerException(400, "当前售后单不在待收货状态");
        }
        String normalizedRemark = receiveRemark == null ? "" : receiveRemark.trim();
        if (normalizedRemark.length() > 255) {
            throw new ServerException(400, "收货质检说明不能超过 255 个字符");
        }
        LocalDateTime receiveTime = LocalDateTime.now();
        int updated = tradeAfterSaleMapper.update(null, new LambdaUpdateWrapper<TradeAfterSaleDO>()
                .eq(TradeAfterSaleDO::getId, afterSaleId)
                .eq(TradeAfterSaleDO::getStatus, STATUS_WAIT_RECEIVE)
                .set(TradeAfterSaleDO::getStatus, STATUS_PENDING)
                .set(TradeAfterSaleDO::getReceiveTime, receiveTime)
                .set(TradeAfterSaleDO::getReceiveRemark, normalizedRemark));
        if (updated != 1) {
            throw new ServerException(409, "售后状态已变化，请刷新后重试");
        }
        afterSale.setStatus(STATUS_PENDING);
        afterSale.setReceiveTime(receiveTime);
        afterSale.setReceiveRemark(normalizedRemark);
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
        if (afterSale == null || afterSale.getStatus() == null
                || !Set.of(STATUS_PENDING, STATUS_WAIT_RETURN).contains(afterSale.getStatus())) {
            throw new ServerException(400, "当前售后单不能撤销");
        }
        if (order.getStatus() == null || order.getStatus() != 5) {
            throw new ServerException(400, "当前订单不在售后处理中");
        }

        Integer fromStatus = order.getStatus();
        Integer restoreStatus = getRestoreOrderStatus(afterSale);
        LocalDateTime cancelTime = LocalDateTime.now();
        int afterSaleUpdated = tradeAfterSaleMapper.update(null, new LambdaUpdateWrapper<TradeAfterSaleDO>()
                .eq(TradeAfterSaleDO::getId, afterSale.getId())
                .in(TradeAfterSaleDO::getStatus, STATUS_PENDING, STATUS_WAIT_RETURN)
                .set(TradeAfterSaleDO::getStatus, 3)
                .set(TradeAfterSaleDO::getCancelTime, cancelTime));
        if (afterSaleUpdated != 1) {
            throw new ServerException(400, "售后单状态已变更，不能撤销");
        }

        int orderUpdated = tradeOrderMapper.update(null, new LambdaUpdateWrapper<TradeOrderDO>()
                .eq(TradeOrderDO::getId, order.getId())
                .eq(TradeOrderDO::getUserId, userId)
                .eq(TradeOrderDO::getStatus, 5)
                .eq(TradeOrderDO::getPayStatus, TradeOrderPayStatus.PAID)
                .set(TradeOrderDO::getStatus, restoreStatus));
        if (orderUpdated != 1) {
            throw new ServerException(400, "订单状态已变更，不能撤销售后");
        }
        afterSale.setStatus(3);
        afterSale.setCancelTime(cancelTime);
        order.setStatus(restoreStatus);
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

        if (Integer.valueOf(2).equals(afterSale.getType()) && afterSale.getReceiveTime() == null) {
            LocalDateTime auditTime = LocalDateTime.now();
            LocalDateTime returnDeadline = auditTime.plusDays(RETURN_DAYS);
            int updated = tradeAfterSaleMapper.update(null, new LambdaUpdateWrapper<TradeAfterSaleDO>()
                    .eq(TradeAfterSaleDO::getId, afterSale.getId())
                    .eq(TradeAfterSaleDO::getStatus, STATUS_PENDING)
                    .set(TradeAfterSaleDO::getStatus, STATUS_WAIT_RETURN)
                    .set(TradeAfterSaleDO::getAuditTime, auditTime)
                    .set(TradeAfterSaleDO::getReturnDeadline, returnDeadline)
                    .set(TradeAfterSaleDO::getRefundMessage, "审核通过，等待买家寄回商品"));
            if (updated != 1) {
                return toResp(getAfterSaleById(afterSale.getId()));
            }
            afterSale.setStatus(STATUS_WAIT_RETURN);
            afterSale.setAuditTime(auditTime);
            afterSale.setReturnDeadline(returnDeadline);
            afterSale.setRefundMessage("审核通过，等待买家寄回商品");
            tradeOrderLogService.recordStatusChanged(order, operatorType, operatorId,
                    "APPROVE_RETURN", order.getStatus(), order.getStatus(), "等待买家寄回商品");
            return toResp(afterSale);
        }

        PayOrderDO payOrder = getPaidPayOrder(order);
        if (payOrder == null || payOrder.getStatus() == null || payOrder.getStatus() != PayOrderStatus.PAID) {
            throw new ServerException(400, "支付单当前不能退款");
        }
        int payRefunded = payOrder.getRefundedAmount() == null ? 0 : payOrder.getRefundedAmount();
        if (payOrder.getAmount() == null || afterSale.getRefundAmount() == null
                || afterSale.getRefundAmount() <= 0
                || Math.addExact(payRefunded, afterSale.getRefundAmount()) > payOrder.getAmount()) {
            throw new ServerException(400, "退款金额超过支付单可退金额");
        }

        String refundProvider = tradeRefundProviderService.currentType();
        LocalDateTime auditTime = LocalDateTime.now();
        int locked = tradeAfterSaleMapper.update(null, new LambdaUpdateWrapper<TradeAfterSaleDO>()
                .eq(TradeAfterSaleDO::getId, afterSale.getId())
                .eq(TradeAfterSaleDO::getStatus, 0)
                .set(TradeAfterSaleDO::getStatus, 4)
                .set(TradeAfterSaleDO::getAuditTime, auditTime)
                .set(TradeAfterSaleDO::getRefundProvider, refundProvider)
                .set(TradeAfterSaleDO::getRefundMessage, "退款请求提交中"));
        if (locked != 1) {
            return toResp(getAfterSaleById(afterSale.getId()));
        }
        afterSale.setStatus(4);
        afterSale.setAuditTime(auditTime);
        afterSale.setRefundProvider(refundProvider);
        afterSale.setRefundMessage("退款请求提交中");

        TradeRefundProvider.RefundResult refundResult = tradeRefundProviderService.refund(
                new TradeRefundProvider.RefundRequest(
                        afterSale.getAfterSaleSn(),
                        order.getOrderSn(),
                        payOrder.getPaySn(),
                        afterSale.getRefundAmount(),
                        payOrder.getAmount(),
                        afterSale.getReason()));
        if (refundResult.status() == TradeRefundProvider.RefundStatus.FAILED) {
            return failRefund(afterSale, order, refundProvider, refundResult,
                    4, operatorType, operatorId);
        }
        if (refundResult.status() == TradeRefundProvider.RefundStatus.PROCESSING) {
            int afterSaleUpdated = tradeAfterSaleMapper.update(null, new LambdaUpdateWrapper<TradeAfterSaleDO>()
                    .eq(TradeAfterSaleDO::getId, afterSale.getId())
                    .eq(TradeAfterSaleDO::getStatus, 4)
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
                4, operatorType, operatorId);
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

        int previousPayRefunded = payOrder.getRefundedAmount() == null ? 0 : payOrder.getRefundedAmount();
        int newPayRefunded = Math.addExact(previousPayRefunded, afterSale.getRefundAmount());
        boolean fullyRefunded = newPayRefunded == payOrder.getAmount();
        int payOrderUpdated = payOrderMapper.update(null, new LambdaUpdateWrapper<PayOrderDO>()
                .eq(PayOrderDO::getId, payOrder.getId())
                .eq(PayOrderDO::getStatus, PayOrderStatus.PAID)
                .eq(PayOrderDO::getRefundedAmount, previousPayRefunded)
                .set(PayOrderDO::getRefundedAmount, newPayRefunded)
                .set(PayOrderDO::getStatus, fullyRefunded ? PayOrderStatus.REFUNDED : PayOrderStatus.PAID));
        if (payOrderUpdated != 1) {
            throw new ServerException(400, "支付单状态已变更，不能退款");
        }

        Integer fromStatus = order.getStatus();
        Integer fromPayStatus = order.getPayStatus();
        int previousOrderRefunded = order.getRefundedAmount() == null ? 0 : order.getRefundedAmount();
        int newOrderRefunded = Math.addExact(previousOrderRefunded, afterSale.getRefundAmount());
        int finalOrderStatus = fullyRefunded ? 5 : getRestoreOrderStatus(afterSale);
        int finalOrderPayStatus = fullyRefunded ? TradeOrderPayStatus.REFUNDED : TradeOrderPayStatus.PAID;
        int orderUpdated = tradeOrderMapper.update(null, new LambdaUpdateWrapper<TradeOrderDO>()
                .eq(TradeOrderDO::getId, order.getId())
                .eq(TradeOrderDO::getPayStatus, TradeOrderPayStatus.PAID)
                .eq(TradeOrderDO::getRefundedAmount, previousOrderRefunded)
                .set(TradeOrderDO::getRefundedAmount, newOrderRefunded)
                .set(TradeOrderDO::getPayStatus, finalOrderPayStatus)
                .set(TradeOrderDO::getStatus, finalOrderStatus));
        if (orderUpdated != 1) {
            throw new ServerException(400, "订单状态已变更，不能退款");
        }

        afterSale.setStatus(1);
        afterSale.setAuditTime(refundTime);
        afterSale.setRefundTime(refundTime);
        afterSale.setRefundProvider(refundProvider);
        afterSale.setProviderRefundNo(refundResult.providerRefundNo());
        afterSale.setRefundMessage(refundResult.message());
        payOrder.setRefundedAmount(newPayRefunded);
        payOrder.setStatus(fullyRefunded ? PayOrderStatus.REFUNDED : PayOrderStatus.PAID);
        order.setRefundedAmount(newOrderRefunded);
        order.setPayStatus(finalOrderPayStatus);
        order.setStatus(finalOrderStatus);
        List<TradeAfterSaleItemDO> afterSaleItems = getAfterSaleItems(afterSale.getId());
        for (TradeAfterSaleItemDO item : afterSaleItems) {
            tradeProductService.adjustSales(item.getSpuId(), -item.getApplyCount());
        }
        if (Integer.valueOf(1).equals(afterSale.getType())
                || (Integer.valueOf(2).equals(afterSale.getType()) && afterSale.getReceiveTime() != null)) {
            for (TradeAfterSaleItemDO item : afterSaleItems) {
                tradeProductService.recoverStock(item.getSkuId(), item.getApplyCount(), "AFTER_SALE",
                        afterSale.getAfterSaleSn(), operatorType, operatorId);
            }
            tradeAfterSaleMapper.update(null, new LambdaUpdateWrapper<TradeAfterSaleDO>()
                    .eq(TradeAfterSaleDO::getId, afterSale.getId())
                    .eq(TradeAfterSaleDO::getStockRecovered, 0)
                    .set(TradeAfterSaleDO::getStockRecovered, 1));
            afterSale.setStockRecovered(1);
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
        result.put("returnCompany", afterSale.getReturnCompany() == null ? "" : afterSale.getReturnCompany());
        result.put("returnNo", afterSale.getReturnNo() == null ? "" : afterSale.getReturnNo());
        result.put("returnDeadline", formatTime(afterSale.getReturnDeadline()));
        result.put("returnTime", formatTime(afterSale.getReturnTime()));
        result.put("receiveTime", formatTime(afterSale.getReceiveTime()));
        result.put("receiveRemark", afterSale.getReceiveRemark() == null ? "" : afterSale.getReceiveRemark());
        result.put("items", getAfterSaleItems(afterSale.getId()).stream().map(item -> {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("orderItemId", item.getOrderItemId());
            value.put("spuId", item.getSpuId());
            value.put("skuId", item.getSkuId());
            value.put("goodsName", item.getGoodsName());
            value.put("specName", item.getSpecName());
            value.put("price", TradeMoneyUtils.formatYuan(item.getPrice()));
            value.put("applyCount", item.getApplyCount());
            value.put("refundAmount", TradeMoneyUtils.formatYuan(item.getRefundAmount()));
            return value;
        }).toList());
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
            case 6 -> "待买家寄回";
            case 7 -> "待商家收货";
            default -> "未知";
        };
    }

    @Transactional(rollbackFor = Exception.class)
    public int expireOverdueReturns(int limit) {
        LocalDateTime now = LocalDateTime.now();
        List<TradeAfterSaleDO> overdueList = tradeAfterSaleMapper.selectList(
                new LambdaQueryWrapper<TradeAfterSaleDO>()
                        .eq(TradeAfterSaleDO::getStatus, STATUS_WAIT_RETURN)
                        .isNotNull(TradeAfterSaleDO::getReturnDeadline)
                        .lt(TradeAfterSaleDO::getReturnDeadline, now)
                        .orderByAsc(TradeAfterSaleDO::getReturnDeadline)
                        .last("LIMIT " + Math.min(Math.max(limit, 1), 200)));
        int expired = 0;
        for (TradeAfterSaleDO afterSale : overdueList) {
            int updated = tradeAfterSaleMapper.update(null, new LambdaUpdateWrapper<TradeAfterSaleDO>()
                    .eq(TradeAfterSaleDO::getId, afterSale.getId())
                    .eq(TradeAfterSaleDO::getStatus, STATUS_WAIT_RETURN)
                    .lt(TradeAfterSaleDO::getReturnDeadline, now)
                    .set(TradeAfterSaleDO::getStatus, STATUS_CANCELLED)
                    .set(TradeAfterSaleDO::getCancelTime, now)
                    .set(TradeAfterSaleDO::getRejectReason, "超过退货寄回期限，售后自动关闭"));
            if (updated != 1) continue;
            TradeOrderDO order = getUserOrder(null, afterSale.getOrderId());
            int restoreStatus = getRestoreOrderStatus(afterSale);
            int orderUpdated = tradeOrderMapper.update(null, new LambdaUpdateWrapper<TradeOrderDO>()
                    .eq(TradeOrderDO::getId, order.getId())
                    .eq(TradeOrderDO::getStatus, 5)
                    .eq(TradeOrderDO::getPayStatus, TradeOrderPayStatus.PAID)
                    .set(TradeOrderDO::getStatus, restoreStatus));
            if (orderUpdated != 1) {
                throw new ServerException(409, "退货超期关闭时订单状态已变化");
            }
            order.setStatus(restoreStatus);
            tradeOrderLogService.recordStatusChanged(order, TradeOrderLogService.OPERATOR_SYSTEM, 0L,
                    "RETURN_DEADLINE_EXPIRED", 5, restoreStatus, "买家未在期限内寄回商品");
            expired++;
        }
        return expired;
    }

    private List<TradeAfterSaleItemDO> buildAfterSaleItems(
            List<TradeOrderItemDO> orderItems, Map<String, Object> request) {
        if (orderItems.isEmpty()) {
            throw new ServerException(400, "订单没有可售后商品");
        }
        Map<Long, TradeOrderItemDO> orderItemById = new HashMap<>();
        for (TradeOrderItemDO item : orderItems) {
            orderItemById.put(item.getId(), item);
        }
        Map<Long, Integer> remainingCounts = getRemainingCounts(orderItems);
        Map<Long, Integer> requestedCounts = new LinkedHashMap<>();
        Object rawItems = request.get("items");
        if (rawItems instanceof List<?> list && !list.isEmpty()) {
            for (Object rawItem : list) {
                if (!(rawItem instanceof Map<?, ?> map)) {
                    throw new ServerException(400, "售后商品明细格式不正确");
                }
                Long orderItemId = parsePositiveLong(map.get("orderItemId"), "订单商品 ID 不正确");
                int count = parsePositiveInt(map.get("count"), "售后数量不正确");
                if (requestedCounts.putIfAbsent(orderItemId, count) != null) {
                    throw new ServerException(400, "售后商品不能重复");
                }
            }
        } else {
            remainingCounts.forEach((id, count) -> {
                if (count > 0) requestedCounts.put(id, count);
            });
        }
        List<TradeAfterSaleItemDO> result = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : requestedCounts.entrySet()) {
            TradeOrderItemDO orderItem = orderItemById.get(entry.getKey());
            int remaining = remainingCounts.getOrDefault(entry.getKey(), 0);
            if (orderItem == null || entry.getValue() > remaining) {
                throw new ServerException(400, "售后数量超过订单商品可退数量");
            }
            TradeAfterSaleItemDO item = new TradeAfterSaleItemDO();
            item.setOrderItemId(orderItem.getId());
            item.setSpuId(orderItem.getSpuId());
            item.setSkuId(orderItem.getSkuId());
            item.setGoodsName(orderItem.getGoodsName());
            item.setSpecName(orderItem.getSpecName() == null ? "" : orderItem.getSpecName());
            item.setPrice(orderItem.getPrice());
            item.setApplyCount(entry.getValue());
            item.setRefundAmount(Math.multiplyExact(orderItem.getPrice(), entry.getValue()));
            result.add(item);
        }
        if (result.isEmpty()) {
            throw new ServerException(400, "订单商品已全部完成售后");
        }
        return result;
    }

    private boolean isAllRemainingItems(
            List<TradeOrderItemDO> orderItems, List<TradeAfterSaleItemDO> selectedItems) {
        Map<Long, Integer> selectedCounts = new HashMap<>();
        selectedItems.forEach(item -> selectedCounts.put(item.getOrderItemId(), item.getApplyCount()));
        Map<Long, Integer> remainingCounts = getRemainingCounts(orderItems);
        return remainingCounts.entrySet().stream()
                .allMatch(entry -> entry.getValue() == selectedCounts.getOrDefault(entry.getKey(), 0));
    }

    private Map<Long, Integer> getRemainingCounts(List<TradeOrderItemDO> orderItems) {
        Map<Long, Integer> appliedCounts = new HashMap<>();
        if (!orderItems.isEmpty()) {
            List<Long> orderItemIds = orderItems.stream().map(TradeOrderItemDO::getId).toList();
            List<TradeAfterSaleDO> countedSales = tradeAfterSaleMapper.selectList(
                    new LambdaQueryWrapper<TradeAfterSaleDO>()
                            .eq(TradeAfterSaleDO::getOrderId, orderItems.get(0).getOrderId())
                            .in(TradeAfterSaleDO::getStatus,
                                    STATUS_PENDING, STATUS_REFUNDED, STATUS_REFUNDING,
                                    STATUS_WAIT_RETURN, STATUS_WAIT_RECEIVE));
            if (!countedSales.isEmpty()) {
                List<Long> afterSaleIds = countedSales.stream().map(TradeAfterSaleDO::getId).toList();
                for (TradeAfterSaleItemDO item : tradeAfterSaleItemMapper.selectList(
                        new LambdaQueryWrapper<TradeAfterSaleItemDO>()
                                .in(TradeAfterSaleItemDO::getAfterSaleId, afterSaleIds)
                                .in(TradeAfterSaleItemDO::getOrderItemId, orderItemIds))) {
                    appliedCounts.merge(item.getOrderItemId(), item.getApplyCount(), Math::addExact);
                }
            }
        }
        Map<Long, Integer> remaining = new LinkedHashMap<>();
        for (TradeOrderItemDO item : orderItems) {
            remaining.put(item.getId(), Math.max(0,
                    item.getCount() - appliedCounts.getOrDefault(item.getId(), 0)));
        }
        return remaining;
    }

    private List<TradeAfterSaleItemDO> getAfterSaleItems(Long afterSaleId) {
        if (afterSaleId == null) return List.of();
        return tradeAfterSaleItemMapper.selectList(new LambdaQueryWrapper<TradeAfterSaleItemDO>()
                .eq(TradeAfterSaleItemDO::getAfterSaleId, afterSaleId)
                .orderByAsc(TradeAfterSaleItemDO::getId));
    }

    private Long parsePositiveLong(Object value, String message) {
        try {
            long parsed = value instanceof Number number ? number.longValue() : Long.parseLong(String.valueOf(value));
            if (parsed <= 0) throw new NumberFormatException();
            return parsed;
        } catch (Exception exception) {
            throw new ServerException(400, message);
        }
    }

    private int parsePositiveInt(Object value, String message) {
        try {
            int parsed = value instanceof Number number ? number.intValue() : Integer.parseInt(String.valueOf(value));
            if (parsed <= 0 || parsed > 99) throw new NumberFormatException();
            return parsed;
        } catch (Exception exception) {
            throw new ServerException(400, message);
        }
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
        return "R" + System.currentTimeMillis()
                + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
