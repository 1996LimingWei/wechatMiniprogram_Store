package com.shop.module.trade.service;

import com.shop.module.trade.config.TradeOrderProperties;
import com.shop.module.trade.dal.dataobject.MemberAddressDO;
import com.shop.module.trade.dal.dataobject.PayOrderDO;
import com.shop.module.trade.dal.dataobject.TradeCartDO;
import com.shop.module.trade.dal.dataobject.TradeOrderDO;
import com.shop.module.trade.dal.dataobject.TradeOrderItemDO;
import com.shop.module.trade.dal.mysql.PayOrderMapper;
import com.shop.module.trade.dal.mysql.TradeOrderItemMapper;
import com.shop.module.trade.dal.mysql.TradeOrderMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradeOrderServiceTest {

    @BeforeAll
    static void initializeLambdaCache() {
        MybatisLambdaTestUtils.initialize(TradeOrderDO.class, PayOrderDO.class);
    }

    @Mock
    private TradeCartService tradeCartService;
    @Mock
    private TradeCheckoutService tradeCheckoutService;
    @Mock
    private MemberAddressService memberAddressService;
    @Mock
    private TradeProductService tradeProductService;
    @Mock
    private TradeLogisticsService tradeLogisticsService;
    @Mock
    private TradeAfterSaleService tradeAfterSaleService;
    @Mock
    private TradeOrderProperties tradeOrderProperties;
    @Mock
    private TradeOrderLogService tradeOrderLogService;
    @Mock
    private TradeOrderMapper tradeOrderMapper;
    @Mock
    private TradeOrderItemMapper tradeOrderItemMapper;
    @Mock
    private PayOrderMapper payOrderMapper;
    @Mock
    private WechatPayService wechatPayService;
    @InjectMocks
    private TradeOrderService tradeOrderService;

    @Test
    void shouldClosePendingPayOrderWhenUserCancelsOrder() {
        TradeOrderDO pendingOrder = createOrder(0);
        TradeOrderDO closedOrder = createOrder(4);
        when(tradeOrderMapper.selectOne(any())).thenReturn(pendingOrder);
        when(tradeOrderMapper.update(isNull(), any())).thenReturn(1);
        when(tradeOrderMapper.selectById(10L)).thenReturn(closedOrder);
        when(tradeOrderItemMapper.selectList(any())).thenReturn(List.of());

        String result = tradeOrderService.cancelOrder(1L, 10L);

        assertEquals("订单已取消", result);
        verify(payOrderMapper).update(isNull(), any());
        verify(tradeOrderLogService).recordStatusChanged(eq(closedOrder), eq(TradeOrderLogService.OPERATOR_USER),
                eq(1L), eq("USER_CANCEL"), eq(0), eq(4), any());
    }

    @Test
    void shouldUseRealtimeSkuSnapshotWhenSubmittingOrder() {
        TradeCartDO cart = new TradeCartDO();
        cart.setId(5L);
        cart.setSpuId(100L);
        cart.setSkuId(200L);
        cart.setCount(2);
        cart.setPrice(100);
        MemberAddressDO address = new MemberAddressDO();
        address.setId(8L);
        address.setUserName("收货人");
        address.setTelNumber("13800000000");
        address.setFullRegion("浙江省杭州市");
        address.setDetailInfo("测试地址");
        TradeProductSnapshot snapshot = new TradeProductSnapshot();
        snapshot.setSpuId(100L);
        snapshot.setSkuId(200L);
        snapshot.setName("实时商品名");
        snapshot.setPicUrl("https://example.com/goods.png");
        snapshot.setSpecName("标准规格");
        snapshot.setPrice(1299);
        snapshot.setStock(10);

        when(tradeOrderMapper.selectOne(any())).thenReturn(null);
        when(tradeCartService.getCheckedCartList(1L)).thenReturn(List.of(cart));
        when(memberAddressService.getAddress(1L, 8L)).thenReturn(address);
        when(tradeProductService.getSnapshot(100L, 200L)).thenReturn(snapshot);
        when(tradeCheckoutService.calculateFreight(2598)).thenReturn(1000);
        when(tradeOrderMapper.insert(any())).thenAnswer(invocation -> {
            TradeOrderDO order = invocation.getArgument(0);
            order.setId(99L);
            return 1;
        });

        Map<String, Object> result = tradeOrderService.submitOrder(1L, 8L, "MP202607310001");

        assertEquals(99L, ((Map<?, ?>) result.get("orderInfo")).get("id"));
        ArgumentCaptor<TradeOrderDO> orderCaptor = ArgumentCaptor.forClass(TradeOrderDO.class);
        verify(tradeOrderMapper).insert(orderCaptor.capture());
        assertEquals(2598, orderCaptor.getValue().getGoodsPrice());
        assertEquals(3598, orderCaptor.getValue().getActualPrice());
        assertEquals("MP202607310001", orderCaptor.getValue().getRequestId());
        ArgumentCaptor<TradeOrderItemDO> itemCaptor = ArgumentCaptor.forClass(TradeOrderItemDO.class);
        verify(tradeOrderItemMapper).insert(itemCaptor.capture());
        assertEquals(1299, itemCaptor.getValue().getPrice());
        assertEquals(2598, itemCaptor.getValue().getTotalPrice());
        verify(tradeProductService).reduceStock(
                eq(snapshot), eq(2), eq("ORDER"), any(),
                eq(TradeOrderLogService.OPERATOR_USER), eq(1L));
    }

    @Test
    void shouldReturnExistingOrderForRepeatedRequestId() {
        TradeOrderDO existing = new TradeOrderDO();
        existing.setId(88L);
        existing.setOrderSn("202607310000000001");
        when(tradeOrderMapper.selectOne(any())).thenReturn(existing);

        Map<String, Object> result = tradeOrderService.submitOrder(1L, 8L, "MP202607310002");

        assertEquals(88L, ((Map<?, ?>) result.get("orderInfo")).get("id"));
        verify(tradeCartService, never()).getCheckedCartList(any());
        verify(tradeOrderMapper, never()).insert(any());
    }

    @Test
    void shouldClosePendingPayOrderWhenOrderExpires() {
        TradeOrderDO pendingOrder = createOrder(0);
        TradeOrderDO closedOrder = createOrder(4);
        when(tradeOrderProperties.getUnpaidTimeoutMinutes()).thenReturn(30);
        when(tradeOrderProperties.getExpireBatchSize()).thenReturn(100);
        when(tradeOrderMapper.selectList(any())).thenReturn(List.of(pendingOrder));
        when(tradeOrderMapper.update(isNull(), any())).thenReturn(1);
        when(tradeOrderMapper.selectById(10L)).thenReturn(closedOrder);
        when(tradeOrderItemMapper.selectList(any())).thenReturn(List.of());

        assertEquals(1, tradeOrderService.closeExpiredUnpaidOrders());

        verify(payOrderMapper).update(isNull(), any());
        verify(tradeOrderLogService).recordStatusChanged(eq(closedOrder), eq(TradeOrderLogService.OPERATOR_SYSTEM),
                eq(0L), eq("SYSTEM_CLOSE"), eq(0), eq(4), any());
    }

    @Test
    void shouldConfirmReceiptByConditionalUpdate() {
        TradeOrderDO pendingReceiptOrder = createOrder(2);
        pendingReceiptOrder.setPayStatus(TradeOrderPayStatus.PAID);
        when(tradeOrderMapper.selectOne(any())).thenReturn(pendingReceiptOrder);
        when(tradeOrderMapper.update(isNull(), any())).thenReturn(1);

        String result = tradeOrderService.confirmOrder(1L, 10L);

        assertEquals("已确认收货", result);
        assertEquals(3, pendingReceiptOrder.getStatus());
        verify(tradeOrderMapper, never()).updateById(any());
        verify(tradeOrderLogService).recordStatusChanged(eq(pendingReceiptOrder),
                eq(TradeOrderLogService.OPERATOR_USER), eq(1L), eq("CONFIRM_RECEIPT"),
                eq(2), eq(3), any());
    }

    @Test
    void shouldIncreaseProductSalesOnlyWhenPaymentStateChanges() {
        TradeOrderDO pendingOrder = createOrder(0);
        TradeOrderItemDO item = new TradeOrderItemDO();
        item.setSpuId(100L);
        item.setCount(3);
        when(tradeOrderMapper.selectOne(any())).thenReturn(pendingOrder);
        when(tradeOrderMapper.update(isNull(), any())).thenReturn(1);
        when(tradeOrderItemMapper.selectList(any())).thenReturn(List.of(item));

        tradeOrderService.markPaid(1L, 10L);

        verify(tradeProductService).adjustSales(100L, 3);
    }

    private TradeOrderDO createOrder(int status) {
        TradeOrderDO order = new TradeOrderDO();
        order.setId(10L);
        order.setUserId(1L);
        order.setStatus(status);
        order.setPayStatus(TradeOrderPayStatus.UNPAID);
        return order;
    }
}
