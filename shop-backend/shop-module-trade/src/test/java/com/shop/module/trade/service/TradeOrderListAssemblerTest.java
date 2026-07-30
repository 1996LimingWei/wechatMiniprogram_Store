package com.shop.module.trade.service;

import com.shop.module.trade.dal.dataobject.TradeAfterSaleDO;
import com.shop.module.trade.dal.dataobject.TradeOrderDO;
import com.shop.module.trade.dal.dataobject.TradeOrderItemDO;
import com.shop.module.trade.dal.dataobject.TradeOrderLogisticsDO;
import com.shop.module.trade.dal.mysql.TradeAfterSaleMapper;
import com.shop.module.trade.dal.mysql.TradeOrderItemMapper;
import com.shop.module.trade.dal.mysql.TradeOrderLogisticsMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradeOrderListAssemblerTest {

    @BeforeAll
    static void initializeLambdaCache() {
        MybatisLambdaTestUtils.initialize(
                TradeOrderDO.class,
                TradeOrderItemDO.class,
                TradeOrderLogisticsDO.class,
                TradeAfterSaleDO.class);
    }

    @Mock
    private TradeOrderItemMapper tradeOrderItemMapper;
    @Mock
    private TradeOrderLogisticsMapper tradeOrderLogisticsMapper;
    @Mock
    private TradeAfterSaleMapper tradeAfterSaleMapper;

    @Test
    void shouldBatchLoadAssociationsKeepOrderAndSelectLatestRecords() {
        TradeOrderListAssembler assembler = new TradeOrderListAssembler(
                tradeOrderItemMapper, tradeOrderLogisticsMapper, tradeAfterSaleMapper);
        TradeOrderDO firstOrder = createOrder(20L, 0);
        TradeOrderDO secondOrder = createOrder(10L, 2);
        TradeOrderItemDO firstGoods = createItem(201L, 20L, "首单商品");
        TradeOrderItemDO secondGoods = createItem(101L, 10L, "次单商品");
        LocalDateTime oldTime = LocalDateTime.of(2026, 7, 30, 10, 0);
        LocalDateTime latestTime = oldTime.plusHours(1);
        TradeOrderLogisticsDO oldLogistics = createLogistics(1L, 10L, oldTime, "OLD");
        TradeOrderLogisticsDO latestLogistics = createLogistics(2L, 10L, latestTime, "NEW");
        TradeAfterSaleDO nullTimeAfterSale = createAfterSale(99L, 10L, null, 2);
        TradeAfterSaleDO latestAfterSale = createAfterSale(1L, 10L, latestTime, 1);
        when(tradeOrderItemMapper.selectList(any())).thenReturn(List.of(secondGoods, firstGoods));
        when(tradeOrderLogisticsMapper.selectList(any()))
                .thenReturn(List.of(oldLogistics, latestLogistics));
        when(tradeAfterSaleMapper.selectList(any()))
                .thenReturn(List.of(nullTimeAfterSale, latestAfterSale));

        List<Map<String, Object>> result = assembler.assemble(List.of(firstOrder, secondOrder));

        assertEquals(List.of(20L, 10L), result.stream().map(item -> item.get("id")).toList());
        assertEquals(EXPECTED_LIST_KEYS, result.getFirst().keySet());
        assertEquals("首单商品", firstGoods(result.getFirst()).get("goodsName"));
        assertFalse((Boolean) childMap(result.getFirst(), "logistics").get("hasLogistics"));
        assertFalse((Boolean) childMap(result.getFirst(), "afterSale").get("hasAfterSale"));

        Map<String, Object> secondLogistics = childMap(result.get(1), "logistics");
        Map<String, Object> secondAfterSale = childMap(result.get(1), "afterSale");
        assertEquals("NEW", secondLogistics.get("logisticsNo"));
        assertEquals(1, secondAfterSale.get("status"));
        assertTrue((Boolean) secondAfterSale.get("hasAfterSale"));
        assertEquals("20.00", result.get(1).get("actualPrice"));
        assertEquals("20.00", firstGoods(result.get(1)).get("retailPrice"));

        verify(tradeOrderItemMapper, times(1)).selectList(any());
        verify(tradeOrderLogisticsMapper, times(1)).selectList(any());
        verify(tradeAfterSaleMapper, times(1)).selectList(any());
    }

    @Test
    void shouldUseIdAsLatestRecordTieBreaker() {
        TradeOrderListAssembler assembler = new TradeOrderListAssembler(
                tradeOrderItemMapper, tradeOrderLogisticsMapper, tradeAfterSaleMapper);
        TradeOrderDO order = createOrder(10L, 2);
        LocalDateTime sameTime = LocalDateTime.of(2026, 7, 31, 9, 0);
        when(tradeOrderItemMapper.selectList(any())).thenReturn(List.of());
        when(tradeOrderLogisticsMapper.selectList(any())).thenReturn(List.of(
                createLogistics(2L, 10L, sameTime, "LOW"),
                createLogistics(3L, 10L, sameTime, "HIGH")));
        when(tradeAfterSaleMapper.selectList(any())).thenReturn(List.of(
                createAfterSale(2L, 10L, sameTime, 0),
                createAfterSale(3L, 10L, sameTime, 2)));

        Map<String, Object> result = assembler.assemble(List.of(order)).getFirst();

        assertEquals("HIGH", childMap(result, "logistics").get("logisticsNo"));
        assertEquals(2, childMap(result, "afterSale").get("status"));
    }

    @Test
    void shouldReturnEmptyPageWithoutAssociationQueries() {
        TradeOrderListAssembler assembler = new TradeOrderListAssembler(
                tradeOrderItemMapper, tradeOrderLogisticsMapper, tradeAfterSaleMapper);

        assertEquals(List.of(), assembler.assemble(List.of()));

        verifyNoInteractions(tradeOrderItemMapper, tradeOrderLogisticsMapper, tradeAfterSaleMapper);
    }

    private TradeOrderDO createOrder(Long id, int status) {
        TradeOrderDO order = new TradeOrderDO();
        order.setId(id);
        order.setOrderSn("ORDER-" + id);
        order.setStatus(status);
        order.setPayStatus(TradeOrderPayStatus.PAID);
        order.setActualPrice(2000);
        order.setGoodsPrice(1800);
        order.setFreightPrice(200);
        order.setCouponPrice(0);
        order.setOrderPrice(2000);
        order.setCreateTime(LocalDateTime.of(2026, 7, 31, 8, 0));
        return order;
    }

    private TradeOrderItemDO createItem(Long id, Long orderId, String goodsName) {
        TradeOrderItemDO item = new TradeOrderItemDO();
        item.setId(id);
        item.setOrderId(orderId);
        item.setSpuId(id + 1000);
        item.setSkuId(id + 2000);
        item.setGoodsName(goodsName);
        item.setSpecName("默认规格");
        item.setCount(1);
        item.setPrice(2000);
        item.setGoodsPicUrl("/goods.png");
        return item;
    }

    private TradeOrderLogisticsDO createLogistics(Long id, Long orderId,
                                                   LocalDateTime updateTime, String logisticsNo) {
        TradeOrderLogisticsDO logistics = new TradeOrderLogisticsDO();
        logistics.setId(id);
        logistics.setOrderId(orderId);
        logistics.setUpdateTime(updateTime);
        logistics.setLogisticsCompany("顺丰速运");
        logistics.setLogisticsNo(logisticsNo);
        logistics.setDeliveryTime(LocalDateTime.of(2026, 7, 31, 10, 0));
        return logistics;
    }

    private TradeAfterSaleDO createAfterSale(Long id, Long orderId,
                                             LocalDateTime updateTime, int status) {
        TradeAfterSaleDO afterSale = new TradeAfterSaleDO();
        afterSale.setId(id);
        afterSale.setOrderId(orderId);
        afterSale.setUpdateTime(updateTime);
        afterSale.setAfterSaleSn("AFTER-" + id);
        afterSale.setType(1);
        afterSale.setStatus(status);
        afterSale.setRefundAmount(2000);
        return afterSale;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> childMap(Map<String, Object> parent, String key) {
        return (Map<String, Object>) parent.get(key);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> firstGoods(Map<String, Object> order) {
        return ((List<Map<String, Object>>) order.get("goodsList")).getFirst();
    }

    private static final Set<String> EXPECTED_LIST_KEYS = Set.of(
            "id", "orderSn", "orderStatusText", "actualPrice", "goodsPrice", "freightPrice",
            "couponPrice", "consignee", "mobile", "fullRegion", "address", "status", "payStatus",
            "orderPrice", "payTime", "expireTime", "closeTime", "closeReason", "handleOption",
            "addTime", "goodsList", "logistics", "afterSale");
}
