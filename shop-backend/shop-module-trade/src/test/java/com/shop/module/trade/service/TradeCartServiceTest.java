package com.shop.module.trade.service;

import com.shop.module.trade.dal.dataobject.TradeCartDO;
import com.shop.module.trade.dal.mysql.TradeCartMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradeCartServiceTest {

    @BeforeAll
    static void initializeLambdaCache() {
        MybatisLambdaTestUtils.initialize(TradeCartDO.class);
    }

    @Mock
    private TradeCartMapper tradeCartMapper;
    @Mock
    private TradeProductService tradeProductService;
    @InjectMocks
    private TradeCartService tradeCartService;

    @Test
    void shouldIncrementExistingCartByConditionalSqlUpdate() {
        TradeProductSnapshot snapshot = createSnapshot();
        TradeCartDO cart = createCart();
        when(tradeProductService.getSnapshot(100L, 200L)).thenReturn(snapshot);
        when(tradeCartMapper.selectOne(any())).thenReturn(cart);
        when(tradeCartMapper.update(isNull(), any())).thenReturn(1);
        when(tradeCartMapper.selectList(any())).thenReturn(List.of(cart));

        tradeCartService.addCart(1L, 100L, 200L, 2);

        verify(tradeCartMapper, never()).updateById(any());
        verify(tradeCartMapper).update(isNull(), any());
    }

    @Test
    void shouldReloadAndUpdateWhenConcurrentInsertHitsUniqueKey() {
        TradeProductSnapshot snapshot = createSnapshot();
        TradeCartDO cart = createCart();
        when(tradeProductService.getSnapshot(100L, 200L)).thenReturn(snapshot);
        when(tradeCartMapper.selectOne(any())).thenReturn(null, cart);
        when(tradeCartMapper.insert(any())).thenThrow(new DuplicateKeyException("uk_user_sku"));
        when(tradeCartMapper.update(isNull(), any())).thenReturn(1);
        when(tradeCartMapper.selectList(any())).thenReturn(List.of(cart));

        tradeCartService.addCart(1L, 100L, 200L, 2);

        verify(tradeCartMapper).insert(any(TradeCartDO.class));
        verify(tradeCartMapper).update(isNull(), any());
    }

    private TradeProductSnapshot createSnapshot() {
        TradeProductSnapshot snapshot = new TradeProductSnapshot();
        snapshot.setSpuId(100L);
        snapshot.setSkuId(200L);
        snapshot.setName("测试商品");
        snapshot.setPicUrl("/goods.png");
        snapshot.setSpecName("默认规格");
        snapshot.setPrice(1000);
        snapshot.setStock(20);
        return snapshot;
    }

    private TradeCartDO createCart() {
        TradeCartDO cart = new TradeCartDO();
        cart.setId(10L);
        cart.setUserId(1L);
        cart.setSpuId(100L);
        cart.setSkuId(200L);
        cart.setCount(1);
        cart.setPrice(1000);
        cart.setChecked(1);
        return cart;
    }
}
