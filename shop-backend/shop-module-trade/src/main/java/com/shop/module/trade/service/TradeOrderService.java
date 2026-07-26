package com.shop.module.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.common.exception.ServerException;
import com.shop.module.trade.dal.dataobject.MemberAddressDO;
import com.shop.module.trade.dal.dataobject.TradeCartDO;
import com.shop.module.trade.dal.dataobject.TradeOrderDO;
import com.shop.module.trade.dal.dataobject.TradeOrderItemDO;
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
    private final TradeOrderMapper tradeOrderMapper;
    private final TradeOrderItemMapper tradeOrderItemMapper;

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
        order.setPayStatus(0);
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
        tradeOrderMapper.insert(order);

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
                        .eq(TradeOrderDO::getPayStatus, 2));
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
        List<TradeOrderItemDO> items = getOrderItems(order.getId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderInfo", toOrderInfo(order));
        result.put("orderGoods", items.stream().map(this::toOrderGoods).toList());
        result.put("handleOption", buildHandleOption(order));
        result.put("logistics", tradeLogisticsService.getOrderLogisticsInfo(order.getId(), order.getStatus()));
        result.put("afterSale", tradeAfterSaleService.getOrderAfterSaleInfo(order.getId()));
        return result;
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
        List<TradeOrderItemDO> items = getOrderItems(order.getId());
        for (TradeOrderItemDO item : items) {
            tradeProductService.recoverStock(item.getSpuId(), item.getCount());
        }
        order.setStatus(4);
        tradeOrderMapper.updateById(order);
        return "订单已取消";
    }

    public String confirmOrder(Long userId, Long orderId) {
        TradeOrderDO order = getUserOrder(userId, orderId);
        if (order.getStatus() != 2) {
            throw new ServerException(400, "当前订单不能确认收货");
        }
        order.setStatus(3);
        tradeOrderMapper.updateById(order);
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
        if (order.getPayStatus() == 1) {
            return;
        }
        if (order.getStatus() != 0) {
            throw new ServerException(400, "当前订单不能支付");
        }
        order.setPayStatus(1);
        order.setStatus(1);
        order.setPayTime(LocalDateTime.now());
        tradeOrderMapper.updateById(order);
    }

    public TradeOrderDO getOrder(Long orderId) {
        TradeOrderDO order = tradeOrderMapper.selectById(orderId);
        if (order == null) {
            throw new ServerException(1404, "订单不存在");
        }
        return order;
    }

    private Map<String, Object> toOrderListItem(TradeOrderDO order) {
        Map<String, Object> item = toOrderInfo(order);
        item.put("goodsList", getOrderItems(order.getId()).stream().map(this::toOrderGoods).toList());
        item.put("logistics", tradeLogisticsService.getOrderLogisticsInfo(order.getId(), order.getStatus()));
        item.put("afterSale", tradeAfterSaleService.getOrderAfterSaleInfo(order.getId()));
        return item;
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
        option.put("refund", order.getPayStatus() != null && order.getPayStatus() == 1
                && order.getStatus() != null && (order.getStatus() == 1 || order.getStatus() == 2 || order.getStatus() == 3));
        option.put("refundApprove", order.getPayStatus() != null && order.getPayStatus() == 1
                && order.getStatus() != null && order.getStatus() == 5);
        return option;
    }

    private List<TradeOrderItemDO> getOrderItems(Long orderId) {
        return tradeOrderItemMapper.selectList(new LambdaQueryWrapper<TradeOrderItemDO>()
                .eq(TradeOrderItemDO::getOrderId, orderId)
                .orderByAsc(TradeOrderItemDO::getId));
    }

    private String getOrderStatusText(TradeOrderDO order) {
        if (order.getPayStatus() != null && order.getPayStatus() == 2) {
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
