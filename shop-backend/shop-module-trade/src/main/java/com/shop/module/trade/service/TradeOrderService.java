package com.shop.module.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shop.common.pojo.PageResult;
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
public class TradeOrderService {

    private static final DateTimeFormatter ORDER_SN_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
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

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> submitOrder(Long userId, Long addressId) {
        List<TradeCartDO> checkedList = tradeCartService.getCheckedCartList(userId);
        if (checkedList.isEmpty()) {
            throw new ServerException(400, "请选择要结算的商品");
        }
        MemberAddressDO address = memberAddressService.getAddress(userId, addressId);
        if (address == null) {
            throw new ServerException(400, "请选择收货地址");
        }

        Map<String, Object> checkout = tradeCheckoutService.checkout(userId, address.getId());
        int goodsTotalPrice = (Integer) checkout.get("goodsTotalPriceCent");
        int freightPrice = (Integer) checkout.get("freightPriceCent");
        int couponPrice = 0;
        int actualPrice = (Integer) checkout.get("actualPriceCent");

        for (TradeCartDO cart : checkedList) {
            TradeProductSnapshot snapshot = tradeProductService.getSnapshot(cart.getSpuId(), cart.getSkuId());
            tradeProductService.reduceStock(snapshot, cart.getCount());
        }

        TradeOrderDO order = new TradeOrderDO();
        order.setOrderSn(generateOrderSn());
        order.setUserId(userId);
        order.setStatus(0);
        order.setPayStatus(TradeOrderPayStatus.UNPAID);
        order.setGoodsPrice(goodsTotalPrice);
        order.setFreightPrice(freightPrice);
        order.setCouponPrice(couponPrice);
        order.setOrderPrice(goodsTotalPrice + freightPrice);
        order.setActualPrice(actualPrice);
        order.setAddressId(address.getId());
        order.setConsignee(address.getUserName());
        order.setMobile(address.getTelNumber());
        order.setFullRegion(address.getFullRegion());
        order.setAddress(address.getDetailInfo());
        order.setExpireTime(LocalDateTime.now().plusMinutes(tradeOrderProperties.getUnpaidTimeoutMinutes()));
        tradeOrderMapper.insert(order);
        tradeOrderLogService.recordCreated(order);

        for (TradeCartDO cart : checkedList) {
            TradeOrderItemDO item = new TradeOrderItemDO();
            item.setOrderId(order.getId());
            item.setUserId(userId);
            item.setSpuId(cart.getSpuId());
            item.setSkuId(cart.getSkuId());
            item.setGoodsName(cart.getGoodsName());
            item.setGoodsPicUrl(cart.getGoodsPicUrl());
            item.setSpecName(cart.getSpecName());
            item.setPrice(cart.getPrice());
            item.setCount(cart.getCount());
            item.setTotalPrice(cart.getPrice() * cart.getCount());
            tradeOrderItemMapper.insert(item);
        }
        tradeCartService.clearCheckedCart(userId);

        return Map.of("orderInfo", Map.of("id", order.getId(), "orderSn", order.getOrderSn()));
    }

    public Map<String, Object> getOrderList(Long userId, int showType, int page, int size) {
        LambdaQueryWrapper<TradeOrderDO> wrapper = new LambdaQueryWrapper<TradeOrderDO>()
                .eq(TradeOrderDO::getUserId, userId)
                .orderByDesc(TradeOrderDO::getCreateTime);
        Integer status = mapShowTypeToStatus(showType);
        if (status != null) {
            if (status < 0) {
                wrapper.and(w -> w.eq(TradeOrderDO::getStatus, 5)
                        .or()
                        .eq(TradeOrderDO::getPayStatus, TradeOrderPayStatus.REFUNDED));
            } else {
                wrapper.eq(TradeOrderDO::getStatus, status);
            }
        }
        List<TradeOrderDO> all = tradeOrderMapper.selectList(wrapper);
        int fromIndex = Math.min(Math.max(page - 1, 0) * size, all.size());
        int toIndex = Math.min(fromIndex + size, all.size());
        List<Map<String, Object>> list = all.subList(fromIndex, toIndex)
                .stream()
                .map(this::toOrderListItem)
                .toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", list);
        result.put("page", page);
        result.put("total", all.size());
        return result;
    }

    public Map<String, Object> getOrderDetail(Long userId, Long orderId) {
        TradeOrderDO order = getUserOrder(userId, orderId);
        return buildOrderDetail(order);
    }

    public PageResult<Map<String, Object>> getAdminOrderPage(int page, int size, Map<String, Object> request) {
        LambdaQueryWrapper<TradeOrderDO> wrapper = new LambdaQueryWrapper<TradeOrderDO>()
                .orderByDesc(TradeOrderDO::getCreateTime);
        Long userId = getLong(request, "userId", 0L);
        Long orderId = getLong(request, "orderId", 0L);
        Integer status = getInteger(request, "status");
        Integer payStatus = getInteger(request, "payStatus");
        String orderSn = getString(request, "orderSn");
        String mobile = getString(request, "mobile");
        if (userId > 0) {
            wrapper.eq(TradeOrderDO::getUserId, userId);
        }
        if (orderId > 0) {
            wrapper.eq(TradeOrderDO::getId, orderId);
        }
        if (status != null) {
            wrapper.eq(TradeOrderDO::getStatus, status);
        }
        if (payStatus != null) {
            wrapper.eq(TradeOrderDO::getPayStatus, payStatus);
        }
        if (!orderSn.isBlank()) {
            wrapper.like(TradeOrderDO::getOrderSn, orderSn);
        }
        if (!mobile.isBlank()) {
            wrapper.like(TradeOrderDO::getMobile, mobile);
        }
        Page<TradeOrderDO> pageResult = tradeOrderMapper.selectPage(new Page<>(Math.max(page, 1), Math.max(size, 1)), wrapper);
        return new PageResult<>(pageResult.getRecords().stream().map(this::toOrderListItem).toList(),
                pageResult.getTotal());
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
        order.setStatus(3);
        tradeOrderMapper.updateById(order);
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
            tradeOrderLogService.recordPayChanged(order, TradeOrderLogService.OPERATOR_USER, userId,
                    "PAY_SUCCESS", 0, 1, 0, 1, "Mock 支付成功");
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

    private Map<String, Object> toOrderListItem(TradeOrderDO order) {
        Map<String, Object> item = toOrderInfo(order);
        item.put("goodsList", getOrderItems(order.getId()).stream().map(this::toOrderGoods).toList());
        item.put("logistics", tradeLogisticsService.getOrderLogisticsInfo(order.getId(), order.getStatus()));
        item.put("afterSale", tradeAfterSaleService.getOrderAfterSaleInfo(order.getId()));
        return item;
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
        goods.put("goodsSpecifitionNameValue", item.getSpecName());
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
            tradeProductService.recoverStock(item.getSpuId(), item.getCount());
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

    private String getString(Map<String, Object> request, String key) {
        Object value = request.get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private Long getLong(Map<String, Object> request, String key, Long defaultValue) {
        Object value = request.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private Integer getInteger(Map<String, Object> request, String key) {
        Object value = request.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private Integer mapShowTypeToStatus(int showType) {
        return switch (showType) {
            case 1 -> 0;
            case 2 -> 1;
            case 3 -> 2;
            case 4 -> 3;
            case 5 -> -1;
            default -> null;
        };
    }

    private String generateOrderSn() {
        return LocalDateTime.now().format(ORDER_SN_FORMATTER)
                + ThreadLocalRandom.current().nextInt(1000, 9999);
    }
}
