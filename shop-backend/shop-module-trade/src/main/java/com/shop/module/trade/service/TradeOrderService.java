package com.shop.module.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.shop.common.exception.ServerException;
import com.shop.module.trade.config.TradeOrderProperties;
import com.shop.module.trade.dal.dataobject.MemberAddressDO;
import com.shop.module.trade.dal.dataobject.PayOrderDO;
import com.shop.module.trade.dal.dataobject.TradeCartDO;
import com.shop.module.trade.dal.dataobject.TradeOrderDO;
import com.shop.module.trade.dal.dataobject.TradeOrderItemDO;
import com.shop.module.trade.dal.mysql.PayOrderMapper;
import com.shop.module.trade.dal.mysql.TradeOrderItemMapper;
import com.shop.module.trade.dal.mysql.TradeOrderMapper;
import com.shop.module.trade.util.TradeMoneyUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TradeOrderService {

    private static final DateTimeFormatter ORDER_SN_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TradeCartService tradeCartService;
    private final TradeCheckoutService tradeCheckoutService;
    private final MemberAddressService memberAddressService;
    private final TradeProductService tradeProductService;
    private final TradeLogisticsService tradeLogisticsService;
    private final TradeAfterSaleService tradeAfterSaleService;
    private final TradeOrderProperties tradeOrderProperties;
    private final TradeOrderLogService tradeOrderLogService;
    private final TradeOrderMapper tradeOrderMapper;
    private final TradeOrderItemMapper tradeOrderItemMapper;
    private final PayOrderMapper payOrderMapper;
    private final WechatPayService wechatPayService;

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> submitOrder(Long userId, Long addressId, String requestId) {
        validateRequestId(requestId);
        TradeOrderDO existingOrder = findByRequestId(userId, requestId);
        if (existingOrder != null) {
            return buildSubmitResult(existingOrder);
        }

        List<TradeCartDO> checkedList = tradeCartService.getCheckedCartList(userId);
        if (checkedList.isEmpty()) {
            throw new ServerException(400, "请选择要结算的商品");
        }
        MemberAddressDO address = memberAddressService.getAddress(userId, addressId);
        if (address == null) {
            throw new ServerException(400, "请选择收货地址");
        }

        List<OrderLine> orderLines = new ArrayList<>(checkedList.size());
        int goodsTotalPrice = 0;
        for (TradeCartDO cart : checkedList) {
            if (cart.getCount() == null || cart.getCount() < 1 || cart.getCount() > 99) {
                throw new ServerException(400, "商品数量必须在 1 到 99 之间");
            }
            TradeProductSnapshot snapshot = tradeProductService.getSnapshot(cart.getSpuId(), cart.getSkuId());
            if (snapshot.getStock() == null || snapshot.getStock() < cart.getCount()) {
                throw new ServerException(1201, "商品库存不足");
            }
            goodsTotalPrice = Math.addExact(goodsTotalPrice,
                    Math.multiplyExact(snapshot.getPrice(), cart.getCount()));
            orderLines.add(new OrderLine(cart, snapshot));
        }
        int freightPrice = tradeCheckoutService.calculateFreight(goodsTotalPrice);
        int couponPrice = 0;
        int actualPrice = Math.addExact(goodsTotalPrice, freightPrice);

        TradeOrderDO order = new TradeOrderDO();
        order.setOrderSn(generateOrderSn());
        order.setRequestId(requestId);
        order.setUserId(userId);
        order.setStatus(0);
        order.setPayStatus(TradeOrderPayStatus.UNPAID);
        order.setGoodsPrice(goodsTotalPrice);
        order.setFreightPrice(freightPrice);
        order.setCouponPrice(couponPrice);
        order.setOrderPrice(actualPrice);
        order.setActualPrice(actualPrice);
        order.setAddressId(address.getId());
        order.setConsignee(address.getUserName());
        order.setMobile(address.getTelNumber());
        order.setFullRegion(address.getFullRegion());
        order.setAddress(address.getDetailInfo());
        order.setExpireTime(LocalDateTime.now().plusMinutes(tradeOrderProperties.getUnpaidTimeoutMinutes()));
        try {
            tradeOrderMapper.insert(order);
        } catch (DuplicateKeyException exception) {
            TradeOrderDO duplicatedOrder = findByRequestId(userId, requestId);
            if (duplicatedOrder != null) {
                return buildSubmitResult(duplicatedOrder);
            }
            throw exception;
        }
        tradeOrderLogService.recordCreated(order);

        for (OrderLine orderLine : orderLines) {
            TradeCartDO cart = orderLine.cart();
            TradeProductSnapshot snapshot = orderLine.snapshot();
            tradeProductService.reduceStock(snapshot, cart.getCount());
            TradeOrderItemDO item = new TradeOrderItemDO();
            item.setOrderId(order.getId());
            item.setUserId(userId);
            item.setSpuId(snapshot.getSpuId());
            item.setSkuId(snapshot.getSkuId());
            item.setGoodsName(snapshot.getName());
            item.setGoodsPicUrl(snapshot.getPicUrl());
            item.setSpecName(snapshot.getSpecName());
            item.setPrice(snapshot.getPrice());
            item.setCount(cart.getCount());
            item.setTotalPrice(Math.multiplyExact(snapshot.getPrice(), cart.getCount()));
            tradeOrderItemMapper.insert(item);
        }
        tradeCartService.clearCheckedCart(userId);

        return buildSubmitResult(order);
    }

    public Map<String, Object> getOrderDetail(Long userId, Long orderId) {
        TradeOrderDO order = getUserOrder(userId, orderId);
        return buildOrderDetail(order);
    }

    public Map<String, Object> getAdminOrderDetail(Long orderId) {
        return buildOrderDetail(getOrder(orderId));
    }

    @Transactional(rollbackFor = Exception.class)
    public String cancelOrder(Long userId, Long orderId) {
        TradeOrderDO order = getUserOrder(userId, orderId);
        if (order.getStatus() == 4) {
            return "订单已取消";
        }
        if (order.getStatus() != 0) {
            throw new ServerException(400, "当前订单不能取消");
        }
        if (closeUnpaidOrder(order.getId(), userId, TradeOrderLogService.OPERATOR_USER, userId,
                "USER_CANCEL", "用户主动取消")) {
            return "订单已取消";
        }

        TradeOrderDO latest = tradeOrderMapper.selectById(order.getId());
        if (latest != null && latest.getStatus() != null && latest.getStatus() == 4) {
            return "订单已取消";
        }
        throw new ServerException(400, "当前订单不能取消");
    }

    @Transactional(rollbackFor = Exception.class)
    public String confirmOrder(Long userId, Long orderId) {
        TradeOrderDO order = getUserOrder(userId, orderId);
        if (order.getStatus() != 2) {
            throw new ServerException(400, "当前订单不能确认收货");
        }
        Integer fromStatus = order.getStatus();
        int updated = tradeOrderMapper.update(null, new LambdaUpdateWrapper<TradeOrderDO>()
                .eq(TradeOrderDO::getId, orderId)
                .eq(TradeOrderDO::getUserId, userId)
                .eq(TradeOrderDO::getStatus, 2)
                .eq(TradeOrderDO::getPayStatus, TradeOrderPayStatus.PAID)
                .set(TradeOrderDO::getStatus, 3));
        if (updated != 1) {
            TradeOrderDO latest = tradeOrderMapper.selectById(orderId);
            if (latest != null && latest.getStatus() != null && latest.getStatus() == 3) {
                return "已确认收货";
            }
            throw new ServerException(400, "订单状态已变更，不能确认收货");
        }
        order.setStatus(3);
        tradeOrderLogService.recordStatusChanged(order, TradeOrderLogService.OPERATOR_USER, userId,
                "CONFIRM_RECEIPT", fromStatus, order.getStatus(), "用户确认收货");
        return "已确认收货";
    }

    public TradeOrderDO getUserOrder(Long userId, Long orderId) {
        TradeOrderDO order = tradeOrderMapper.selectOne(new LambdaQueryWrapper<TradeOrderDO>()
                .eq(TradeOrderDO::getUserId, userId)
                .eq(TradeOrderDO::getId, orderId));
        if (order == null) {
            throw new ServerException(1404, "订单不存在");
        }
        return order;
    }

    @Transactional(rollbackFor = Exception.class)
    public void markPaid(Long userId, Long orderId) {
        markPaid(userId, orderId, TradeOrderLogService.OPERATOR_USER, userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void markPaidBySystem(Long userId, Long orderId) {
        markPaid(userId, orderId, TradeOrderLogService.OPERATOR_SYSTEM, 0L);
    }

    private void markPaid(Long userId, Long orderId, String operatorType, Long operatorId) {
        TradeOrderDO order = getUserOrder(userId, orderId);
        if (order.getPayStatus() == TradeOrderPayStatus.PAID) {
            return;
        }
        if (order.getStatus() != 0) {
            throw new ServerException(400, "当前订单不能支付");
        }
        int updated = tradeOrderMapper.update(null, new LambdaUpdateWrapper<TradeOrderDO>()
                .eq(TradeOrderDO::getId, orderId)
                .eq(TradeOrderDO::getUserId, userId)
                .eq(TradeOrderDO::getStatus, 0)
                .eq(TradeOrderDO::getPayStatus, TradeOrderPayStatus.UNPAID)
                .set(TradeOrderDO::getPayStatus, TradeOrderPayStatus.PAID)
                .set(TradeOrderDO::getStatus, 1)
                .set(TradeOrderDO::getPayTime, LocalDateTime.now()));
        if (updated == 1) {
            order.setStatus(1);
            order.setPayStatus(TradeOrderPayStatus.PAID);
            for (TradeOrderItemDO item : getOrderItems(orderId)) {
                tradeProductService.adjustSales(item.getSpuId(), item.getCount());
            }
            tradeOrderLogService.recordPayChanged(order, operatorType, operatorId,
                    "PAY_SUCCESS", 0, 1, 0, 1, "支付成功");
            return;
        }
        TradeOrderDO latest = tradeOrderMapper.selectById(orderId);
        if (latest != null && latest.getPayStatus() != null && latest.getPayStatus() == TradeOrderPayStatus.PAID) {
            return;
        }
        if (latest != null && latest.getStatus() != null && latest.getStatus() == 4) {
            throw new ServerException(400, "订单已取消，不能支付");
        }
        throw new ServerException(400, "当前订单不能支付");
    }

    public TradeOrderDO getOrder(Long orderId) {
        TradeOrderDO order = tradeOrderMapper.selectById(orderId);
        if (order == null) {
            throw new ServerException(1404, "订单不存在");
        }
        return order;
    }

    @Transactional(rollbackFor = Exception.class)
    public int closeExpiredUnpaidOrders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fallbackExpireTime = now.minusMinutes(tradeOrderProperties.getUnpaidTimeoutMinutes());
        int batchSize = Math.max(1, tradeOrderProperties.getExpireBatchSize());
        List<TradeOrderDO> expiredOrders = tradeOrderMapper.selectList(new LambdaQueryWrapper<TradeOrderDO>()
                .eq(TradeOrderDO::getStatus, 0)
                .eq(TradeOrderDO::getPayStatus, TradeOrderPayStatus.UNPAID)
                .and(wrapper -> wrapper.le(TradeOrderDO::getExpireTime, now)
                        .or()
                        .isNull(TradeOrderDO::getExpireTime)
                        .le(TradeOrderDO::getCreateTime, fallbackExpireTime))
                .orderByAsc(TradeOrderDO::getCreateTime)
                .last("LIMIT " + batchSize));
        int closedCount = 0;
        for (TradeOrderDO expiredOrder : expiredOrders) {
            if (closeUnpaidOrder(expiredOrder.getId(), expiredOrder.getUserId(),
                    TradeOrderLogService.OPERATOR_SYSTEM, 0L, "SYSTEM_CLOSE", "超时未支付自动关闭")) {
                closedCount++;
            }
        }
        return closedCount;
    }

    private Map<String, Object> buildOrderDetail(TradeOrderDO order) {
        List<TradeOrderItemDO> items = getOrderItems(order.getId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderInfo", toOrderInfo(order));
        result.put("orderGoods", items.stream().map(this::toOrderGoods).toList());
        result.put("handleOption", buildHandleOption(order));
        result.put("logistics", tradeLogisticsService.getOrderLogisticsInfo(order.getId(), order.getStatus()));
        result.put("afterSale", tradeAfterSaleService.getOrderAfterSaleInfo(order.getId()));
        result.put("payOrder", getPayOrderInfo(order));
        result.put("orderLogs", tradeOrderLogService.listByOrderId(order.getId()));
        return result;
    }

    private Map<String, Object> toOrderInfo(TradeOrderDO order) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", order.getId());
        item.put("orderSn", order.getOrderSn());
        item.put("userId", order.getUserId());
        item.put("orderStatusText", getOrderStatusText(order));
        item.put("actualPrice", TradeMoneyUtils.formatYuan(order.getActualPrice()));
        item.put("goodsPrice", TradeMoneyUtils.formatYuan(order.getGoodsPrice()));
        item.put("freightPrice", TradeMoneyUtils.formatYuan(order.getFreightPrice()));
        item.put("couponPrice", TradeMoneyUtils.formatYuan(order.getCouponPrice()));
        item.put("consignee", order.getConsignee());
        item.put("mobile", order.getMobile());
        item.put("fullRegion", order.getFullRegion());
        item.put("address", order.getAddress());
        item.put("status", order.getStatus());
        item.put("payStatus", order.getPayStatus());
        item.put("orderPrice", TradeMoneyUtils.formatYuan(order.getOrderPrice()));
        item.put("payTime", order.getPayTime() == null ? "" : order.getPayTime().format(TIME_FORMATTER));
        item.put("expireTime", order.getExpireTime() == null ? "" : order.getExpireTime().format(TIME_FORMATTER));
        item.put("closeTime", order.getCloseTime() == null ? "" : order.getCloseTime().format(TIME_FORMATTER));
        item.put("closeReason", order.getCloseReason());
        item.put("handleOption", buildHandleOption(order));
        item.put("addTime", order.getCreateTime() == null ? "" : order.getCreateTime().format(TIME_FORMATTER));
        return item;
    }

    private Map<String, Object> toOrderGoods(TradeOrderItemDO item) {
        Map<String, Object> goods = new LinkedHashMap<>();
        goods.put("id", item.getId());
        goods.put("goodsId", item.getSpuId());
        goods.put("productId", item.getSkuId());
        goods.put("goodsName", item.getGoodsName());
        goods.put("goodsSpecifitionNameValue", TradeCartService.formatSpecName(item.getSpecName()));
        goods.put("number", item.getCount());
        goods.put("retailPrice", TradeMoneyUtils.formatYuan(item.getPrice()));
        goods.put("listPicUrl", item.getGoodsPicUrl());
        return goods;
    }

    private Map<String, Object> buildHandleOption(TradeOrderDO order) {
        Map<String, Object> option = new LinkedHashMap<>();
        option.put("pay", order.getStatus() == 0);
        option.put("cancel", order.getStatus() == 0);
        option.put("ship", order.getStatus() == 1);
        option.put("logistics", order.getStatus() == 2 || order.getStatus() == 3);
        option.put("confirm", order.getStatus() == 2);
        option.put("refund", order.getPayStatus() != null && order.getPayStatus() == TradeOrderPayStatus.PAID
                && order.getStatus() != null && (order.getStatus() == 1 || order.getStatus() == 2 || order.getStatus() == 3));
        option.put("refundApprove", order.getPayStatus() != null && order.getPayStatus() == TradeOrderPayStatus.PAID
                && order.getStatus() != null && order.getStatus() == 5);
        option.put("refundCancel", order.getPayStatus() != null && order.getPayStatus() == TradeOrderPayStatus.PAID
                && order.getStatus() != null && order.getStatus() == 5);
        return option;
    }

    private List<TradeOrderItemDO> getOrderItems(Long orderId) {
        return tradeOrderItemMapper.selectList(new LambdaQueryWrapper<TradeOrderItemDO>()
                .eq(TradeOrderItemDO::getOrderId, orderId)
                .orderByAsc(TradeOrderItemDO::getId));
    }

    private boolean closeUnpaidOrder(Long orderId, Long userId, String operatorType, Long operatorId,
                                     String action, String closeReason) {
        PayOrderDO pendingPayOrder = payOrderMapper.selectOne(new LambdaQueryWrapper<PayOrderDO>()
                .eq(PayOrderDO::getOrderId, orderId)
                .eq(PayOrderDO::getUserId, userId)
                .eq(PayOrderDO::getStatus, PayOrderStatus.PENDING)
                .orderByDesc(PayOrderDO::getUpdateTime)
                .last("LIMIT 1"));
        if (pendingPayOrder != null && "wx_lite".equals(pendingPayOrder.getChannel())
                && wechatPayService.isEnabled()) {
            wechatPayService.closePayment(pendingPayOrder.getPaySn());
        }
        int updated = tradeOrderMapper.update(null, new LambdaUpdateWrapper<TradeOrderDO>()
                .eq(TradeOrderDO::getId, orderId)
                .eq(TradeOrderDO::getUserId, userId)
                .eq(TradeOrderDO::getStatus, 0)
                .eq(TradeOrderDO::getPayStatus, TradeOrderPayStatus.UNPAID)
                .set(TradeOrderDO::getStatus, 4)
                .set(TradeOrderDO::getCloseTime, LocalDateTime.now())
                .set(TradeOrderDO::getCloseReason, closeReason));
        if (updated != 1) {
            return false;
        }
        TradeOrderDO closedOrder = tradeOrderMapper.selectById(orderId);
        tradeOrderLogService.recordStatusChanged(closedOrder, operatorType, operatorId, action,
                0, 4, closeReason);
        for (TradeOrderItemDO item : getOrderItems(orderId)) {
            tradeProductService.recoverStock(item.getSkuId(), item.getCount());
        }
        payOrderMapper.update(null, new LambdaUpdateWrapper<PayOrderDO>()
                .eq(PayOrderDO::getOrderId, orderId)
                .eq(PayOrderDO::getUserId, userId)
                .eq(PayOrderDO::getStatus, PayOrderStatus.PENDING)
                .set(PayOrderDO::getStatus, PayOrderStatus.CLOSED));
        return true;
    }

    private String getOrderStatusText(TradeOrderDO order) {
        if (order.getPayStatus() != null && order.getPayStatus() == TradeOrderPayStatus.REFUNDED) {
            return "已退款";
        }
        Integer status = order.getStatus();
        return switch (status == null ? 0 : status) {
            case 0 -> "待付款";
            case 1 -> "待发货";
            case 2 -> "待收货";
            case 3 -> "已完成";
            case 4 -> "已取消";
            case 5 -> "退款中";
            default -> "未知";
        };
    }

    private Map<String, Object> getPayOrderInfo(TradeOrderDO order) {
        PayOrderDO payOrder = payOrderMapper.selectOne(new LambdaQueryWrapper<PayOrderDO>()
                .eq(PayOrderDO::getOrderId, order.getId())
                .eq(PayOrderDO::getUserId, order.getUserId())
                .orderByDesc(PayOrderDO::getUpdateTime)
                .last("LIMIT 1"));
        if (payOrder == null) {
            return Map.of("hasPayOrder", false);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("hasPayOrder", true);
        result.put("id", payOrder.getId());
        result.put("paySn", payOrder.getPaySn());
        result.put("amount", TradeMoneyUtils.formatYuan(payOrder.getAmount()));
        result.put("channel", payOrder.getChannel());
        result.put("status", payOrder.getStatus());
        result.put("statusText", PayOrderStatus.getText(payOrder.getStatus()));
        result.put("payTime", payOrder.getPayTime() == null ? "" : payOrder.getPayTime().format(TIME_FORMATTER));
        return result;
    }

    private String generateOrderSn() {
        return LocalDateTime.now().format(ORDER_SN_FORMATTER)
                + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private void validateRequestId(String requestId) {
        if (requestId == null || !requestId.matches("[A-Za-z0-9_-]{8,64}")) {
            throw new ServerException(400, "订单请求标识格式不正确");
        }
    }

    private TradeOrderDO findByRequestId(Long userId, String requestId) {
        return tradeOrderMapper.selectOne(new LambdaQueryWrapper<TradeOrderDO>()
                .eq(TradeOrderDO::getUserId, userId)
                .eq(TradeOrderDO::getRequestId, requestId)
                .last("LIMIT 1"));
    }

    private Map<String, Object> buildSubmitResult(TradeOrderDO order) {
        return Map.of("orderInfo", Map.of("id", order.getId(), "orderSn", order.getOrderSn()));
    }

    private record OrderLine(TradeCartDO cart, TradeProductSnapshot snapshot) {
    }
}
