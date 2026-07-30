package com.shop.module.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shop.module.trade.dal.dataobject.TradeAfterSaleDO;
import com.shop.module.trade.dal.dataobject.TradeOrderDO;
import com.shop.module.trade.dal.dataobject.TradeOrderItemDO;
import com.shop.module.trade.dal.dataobject.TradeOrderLogisticsDO;
import com.shop.module.trade.dal.mysql.TradeAfterSaleMapper;
import com.shop.module.trade.dal.mysql.TradeOrderItemMapper;
import com.shop.module.trade.dal.mysql.TradeOrderLogisticsMapper;
import com.shop.module.trade.util.TradeMoneyUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 批量读取并装配订单列表当前页的关联数据。
 */
@Component
@RequiredArgsConstructor
public class TradeOrderListAssembler {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Comparator<LocalDateTime> TIME_COMPARATOR =
            Comparator.nullsFirst(Comparator.naturalOrder());
    private static final Comparator<Long> ID_COMPARATOR =
            Comparator.nullsFirst(Comparator.naturalOrder());

    private final TradeOrderItemMapper tradeOrderItemMapper;
    private final TradeOrderLogisticsMapper tradeOrderLogisticsMapper;
    private final TradeAfterSaleMapper tradeAfterSaleMapper;

    public List<Map<String, Object>> assemble(List<TradeOrderDO> orders) {
        if (orders == null || orders.isEmpty()) {
            return List.of();
        }
        Set<Long> orderIds = new LinkedHashSet<>();
        for (TradeOrderDO order : orders) {
            orderIds.add(order.getId());
        }

        Map<Long, List<TradeOrderItemDO>> itemsByOrderId = loadItems(orderIds);
        Map<Long, TradeOrderLogisticsDO> logisticsByOrderId = loadLatestLogistics(orderIds);
        Map<Long, TradeAfterSaleDO> afterSalesByOrderId = loadLatestAfterSales(orderIds);

        List<Map<String, Object>> result = new ArrayList<>(orders.size());
        for (TradeOrderDO order : orders) {
            Map<String, Object> item = toOrderInfo(order);
            item.put("goodsList", itemsByOrderId.getOrDefault(order.getId(), List.of())
                    .stream()
                    .map(this::toOrderGoods)
                    .toList());
            item.put("logistics", toLogisticsInfo(logisticsByOrderId.get(order.getId()), order.getStatus()));
            item.put("afterSale", toAfterSaleInfo(afterSalesByOrderId.get(order.getId())));
            result.add(item);
        }
        return result;
    }

    private Map<Long, List<TradeOrderItemDO>> loadItems(Set<Long> orderIds) {
        List<TradeOrderItemDO> items = tradeOrderItemMapper.selectList(
                new LambdaQueryWrapper<TradeOrderItemDO>()
                        .in(TradeOrderItemDO::getOrderId, orderIds)
                        .orderByAsc(TradeOrderItemDO::getOrderId)
                        .orderByAsc(TradeOrderItemDO::getId));
        Map<Long, List<TradeOrderItemDO>> result = new LinkedHashMap<>();
        for (TradeOrderItemDO item : items) {
            result.computeIfAbsent(item.getOrderId(), ignored -> new ArrayList<>()).add(item);
        }
        return result;
    }

    private Map<Long, TradeOrderLogisticsDO> loadLatestLogistics(Set<Long> orderIds) {
        List<TradeOrderLogisticsDO> logisticsList = tradeOrderLogisticsMapper.selectList(
                new LambdaQueryWrapper<TradeOrderLogisticsDO>()
                        .in(TradeOrderLogisticsDO::getOrderId, orderIds)
                        .orderByDesc(TradeOrderLogisticsDO::getUpdateTime)
                        .orderByDesc(TradeOrderLogisticsDO::getId));
        Map<Long, TradeOrderLogisticsDO> result = new LinkedHashMap<>();
        for (TradeOrderLogisticsDO logistics : logisticsList) {
            result.merge(logistics.getOrderId(), logistics, this::latestLogistics);
        }
        return result;
    }

    private Map<Long, TradeAfterSaleDO> loadLatestAfterSales(Set<Long> orderIds) {
        List<TradeAfterSaleDO> afterSales = tradeAfterSaleMapper.selectList(
                new LambdaQueryWrapper<TradeAfterSaleDO>()
                        .in(TradeAfterSaleDO::getOrderId, orderIds)
                        .orderByDesc(TradeAfterSaleDO::getUpdateTime)
                        .orderByDesc(TradeAfterSaleDO::getId));
        Map<Long, TradeAfterSaleDO> result = new LinkedHashMap<>();
        for (TradeAfterSaleDO afterSale : afterSales) {
            result.merge(afterSale.getOrderId(), afterSale, this::latestAfterSale);
        }
        return result;
    }

    private TradeOrderLogisticsDO latestLogistics(TradeOrderLogisticsDO first,
                                                   TradeOrderLogisticsDO second) {
        int timeCompare = TIME_COMPARATOR.compare(first.getUpdateTime(), second.getUpdateTime());
        if (timeCompare != 0) {
            return timeCompare > 0 ? first : second;
        }
        return ID_COMPARATOR.compare(first.getId(), second.getId()) >= 0 ? first : second;
    }

    private TradeAfterSaleDO latestAfterSale(TradeAfterSaleDO first, TradeAfterSaleDO second) {
        int timeCompare = TIME_COMPARATOR.compare(first.getUpdateTime(), second.getUpdateTime());
        if (timeCompare != 0) {
            return timeCompare > 0 ? first : second;
        }
        return ID_COMPARATOR.compare(first.getId(), second.getId()) >= 0 ? first : second;
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
        item.put("payTime", formatTime(order.getPayTime()));
        item.put("expireTime", formatTime(order.getExpireTime()));
        item.put("closeTime", formatTime(order.getCloseTime()));
        item.put("closeReason", order.getCloseReason());
        item.put("handleOption", buildHandleOption(order));
        item.put("addTime", formatTime(order.getCreateTime()));
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

    private Map<String, Object> toLogisticsInfo(TradeOrderLogisticsDO logistics, Integer orderStatus) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("hasLogistics", logistics != null);
        result.put("orderStatus", orderStatus);
        if (logistics == null) {
            result.put("logisticsCompany", "");
            result.put("logisticsNo", "");
            result.put("deliveryTime", "");
            result.put("traces", List.of());
            return result;
        }

        String deliveryTime = formatTime(logistics.getDeliveryTime());
        result.put("id", logistics.getId());
        result.put("orderId", logistics.getOrderId());
        result.put("logisticsCompany", logistics.getLogisticsCompany());
        result.put("logisticsNo", logistics.getLogisticsNo());
        result.put("deliveryTime", deliveryTime);
        result.put("traces", List.of(
                Map.of("time", deliveryTime, "text", "商家已发货，包裹交由" + logistics.getLogisticsCompany()),
                Map.of("time", deliveryTime, "text", "物流单号：" + logistics.getLogisticsNo())
        ));
        return result;
    }

    private Map<String, Object> toAfterSaleInfo(TradeAfterSaleDO afterSale) {
        if (afterSale == null) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("hasAfterSale", false);
            return result;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("hasAfterSale", true);
        result.put("id", afterSale.getId());
        result.put("orderId", afterSale.getOrderId());
        result.put("afterSaleSn", afterSale.getAfterSaleSn());
        result.put("type", afterSale.getType());
        result.put("typeText", afterSale.getType() != null && afterSale.getType() == 1 ? "仅退款" : "退货退款");
        result.put("status", afterSale.getStatus());
        result.put("statusText", getAfterSaleStatusText(afterSale.getStatus()));
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

    private Map<String, Object> buildHandleOption(TradeOrderDO order) {
        Map<String, Object> option = new LinkedHashMap<>();
        option.put("pay", order.getStatus() == 0);
        option.put("cancel", order.getStatus() == 0);
        option.put("ship", order.getStatus() == 1);
        option.put("logistics", order.getStatus() == 2 || order.getStatus() == 3);
        option.put("confirm", order.getStatus() == 2);
        option.put("refund", order.getPayStatus() != null && order.getPayStatus() == TradeOrderPayStatus.PAID
                && order.getStatus() != null
                && (order.getStatus() == 1 || order.getStatus() == 2 || order.getStatus() == 3));
        option.put("refundApprove",
                order.getPayStatus() != null && order.getPayStatus() == TradeOrderPayStatus.PAID
                        && order.getStatus() != null && order.getStatus() == 5);
        option.put("refundCancel",
                order.getPayStatus() != null && order.getPayStatus() == TradeOrderPayStatus.PAID
                        && order.getStatus() != null && order.getStatus() == 5);
        return option;
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

    private String getAfterSaleStatusText(Integer status) {
        return switch (status == null ? 0 : status) {
            case 0 -> "退款处理中";
            case 1 -> "已退款";
            case 2 -> "退款已拒绝";
            case 3 -> "已撤销";
            default -> "未知";
        };
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? "" : time.format(TIME_FORMATTER);
    }
}
