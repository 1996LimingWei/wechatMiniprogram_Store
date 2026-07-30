package com.shop.module.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shop.common.exception.ServerException;
import com.shop.common.pojo.PageResult;
import com.shop.module.trade.dal.dataobject.TradeOrderDO;
import com.shop.module.trade.dal.mysql.TradeOrderMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TradeOrderQueryServiceTest {

    @BeforeAll
    static void initializeLambdaCache() {
        MybatisLambdaTestUtils.initialize(TradeOrderDO.class);
    }

    @Mock
    private TradeOrderMapper tradeOrderMapper;
    @Mock
    private TradeOrderListAssembler tradeOrderListAssembler;

    @Test
    void shouldBuildIndexedAdminQueryAndNormalizePagination() {
        TradeOrderQueryService service = new TradeOrderQueryService(
                tradeOrderMapper, tradeOrderListAssembler);
        TradeOrderDO order = new TradeOrderDO();
        order.setId(9L);
        List<Map<String, Object>> assembled = List.of(Map.of("id", 9L));
        AtomicReference<Page<TradeOrderDO>> capturedPage = new AtomicReference<>();
        AtomicReference<LambdaQueryWrapper<TradeOrderDO>> capturedWrapper = new AtomicReference<>();
        mockPageQuery(List.of(order), 23L, capturedPage, capturedWrapper);
        when(tradeOrderListAssembler.assemble(List.of(order))).thenReturn(assembled);

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("userId", 7L);
        request.put("orderId", 9L);
        request.put("status", 2);
        request.put("payStatus", 1);
        request.put("orderSn", "202607310001");
        request.put("mobile", "138");
        request.put("createTimeStart", "2026-07-01 00:00:00");
        request.put("createTimeEnd", "2026-08-01 00:00:00");

        PageResult<Map<String, Object>> result = service.getAdminOrderPage(0, 999, request);

        assertEquals(assembled, result.getList());
        assertEquals(23L, result.getTotal());
        assertEquals(1L, capturedPage.get().getCurrent());
        assertEquals(100L, capturedPage.get().getSize());
        String sql = normalizeSql(capturedWrapper.get().getSqlSegment());
        assertTrue(sql.contains("USER_ID ="));
        assertTrue(sql.contains("ID ="));
        assertTrue(sql.contains("STATUS ="));
        assertTrue(sql.contains("PAY_STATUS ="));
        assertTrue(sql.contains("ORDER_SN ="));
        assertTrue(sql.contains("MOBILE LIKE"));
        assertTrue(sql.contains("CREATE_TIME >="));
        assertTrue(sql.contains("CREATE_TIME <"));
        assertTrue(sql.endsWith("ORDER BY CREATE_TIME DESC,ID DESC"));

        List<Object> parameterValues = capturedWrapper.get().getParamNameValuePairs().values().stream().toList();
        assertTrue(parameterValues.contains("202607310001"));
        assertTrue(parameterValues.contains("138%"));
        assertTrue(parameterValues.contains(LocalDateTime.of(2026, 7, 1, 0, 0)));
        assertTrue(parameterValues.contains(LocalDateTime.of(2026, 8, 1, 0, 0)));
    }

    @Test
    void shouldUseDatabasePaginationForUserRefundList() {
        TradeOrderQueryService service = new TradeOrderQueryService(
                tradeOrderMapper, tradeOrderListAssembler);
        TradeOrderDO order = new TradeOrderDO();
        order.setId(12L);
        AtomicReference<Page<TradeOrderDO>> capturedPage = new AtomicReference<>();
        AtomicReference<LambdaQueryWrapper<TradeOrderDO>> capturedWrapper = new AtomicReference<>();
        mockPageQuery(List.of(order), 8L, capturedPage, capturedWrapper);
        when(tradeOrderListAssembler.assemble(List.of(order))).thenReturn(List.of(Map.of("id", 12L)));

        Map<String, Object> result = service.getOrderList(3L, 5, -2, 101);

        assertEquals(1, result.get("page"));
        assertEquals(8L, result.get("total"));
        assertEquals(1L, capturedPage.get().getCurrent());
        assertEquals(100L, capturedPage.get().getSize());
        String sql = normalizeSql(capturedWrapper.get().getSqlSegment());
        assertTrue(sql.contains("USER_ID ="));
        assertTrue(sql.contains("STATUS ="));
        assertTrue(sql.contains("OR PAY_STATUS ="));
        assertTrue(sql.endsWith("ORDER BY CREATE_TIME DESC,ID DESC"));
    }

    @Test
    void shouldRejectInvalidCalendarDate() {
        TradeOrderQueryService service = new TradeOrderQueryService(
                tradeOrderMapper, tradeOrderListAssembler);
        Map<String, Object> request = Map.of("createTimeStart", "2026-02-30 00:00:00");

        ServerException exception = assertThrows(ServerException.class,
                () -> service.getAdminOrderPage(1, 10, request));

        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("yyyy-MM-dd HH:mm:ss"));
        verifyNoInteractions(tradeOrderMapper, tradeOrderListAssembler);
    }

    @Test
    void shouldRejectIncompleteTimeAndInvalidRange() {
        TradeOrderQueryService service = new TradeOrderQueryService(
                tradeOrderMapper, tradeOrderListAssembler);

        ServerException incompleteTime = assertThrows(ServerException.class,
                () -> service.getAdminOrderPage(1, 10,
                        Map.of("createTimeEnd", "2026-08-01 00:00")));
        ServerException invalidRange = assertThrows(ServerException.class,
                () -> service.getAdminOrderPage(1, 10, Map.of(
                        "createTimeStart", "2026-08-01 00:00:00",
                        "createTimeEnd", "2026-08-01 00:00:00")));

        assertEquals(400, incompleteTime.getCode());
        assertEquals(400, invalidRange.getCode());
        verifyNoInteractions(tradeOrderMapper, tradeOrderListAssembler);
    }

    @Test
    void shouldAcceptTrimmedLeapDayAndClampMinimumPageSize() {
        TradeOrderQueryService service = new TradeOrderQueryService(
                tradeOrderMapper, tradeOrderListAssembler);
        AtomicReference<Page<TradeOrderDO>> capturedPage = new AtomicReference<>();
        AtomicReference<LambdaQueryWrapper<TradeOrderDO>> capturedWrapper = new AtomicReference<>();
        mockPageQuery(List.of(), 0L, capturedPage, capturedWrapper);
        when(tradeOrderListAssembler.assemble(List.of())).thenReturn(List.of());

        service.getAdminOrderPage(3, 0,
                Map.of("createTimeStart", " 2028-02-29 12:30:00 "));

        assertEquals(3L, capturedPage.get().getCurrent());
        assertEquals(1L, capturedPage.get().getSize());
        String sql = normalizeSql(capturedWrapper.get().getSqlSegment());
        assertTrue(capturedWrapper.get().getParamNameValuePairs().containsValue(
                LocalDateTime.of(2028, 2, 29, 12, 30)));
        assertTrue(sql.contains("CREATE_TIME >="));
    }

    @Test
    void shouldRejectDescendingTimeRange() {
        TradeOrderQueryService service = new TradeOrderQueryService(
                tradeOrderMapper, tradeOrderListAssembler);

        ServerException exception = assertThrows(ServerException.class,
                () -> service.getAdminOrderPage(1, 10, Map.of(
                        "createTimeStart", "2026-08-02 00:00:00",
                        "createTimeEnd", "2026-08-01 00:00:00")));

        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("必须早于"));
        verifyNoInteractions(tradeOrderMapper, tradeOrderListAssembler);
    }

    @Test
    void shouldRejectNonNumericMobilePrefix() {
        TradeOrderQueryService service = new TradeOrderQueryService(
                tradeOrderMapper, tradeOrderListAssembler);

        for (String mobile : List.of("138%", "138_", "138abc")) {
            ServerException exception = assertThrows(ServerException.class,
                    () -> service.getAdminOrderPage(1, 10, Map.of("mobile", mobile)));
            assertEquals(400, exception.getCode());
            assertTrue(exception.getMessage().contains("纯数字"));
        }
        verifyNoInteractions(tradeOrderMapper, tradeOrderListAssembler);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void mockPageQuery(List<TradeOrderDO> records, long total,
                               AtomicReference<Page<TradeOrderDO>> capturedPage,
                               AtomicReference<LambdaQueryWrapper<TradeOrderDO>> capturedWrapper) {
        when(tradeOrderMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenAnswer(invocation -> {
                    Page<TradeOrderDO> page = invocation.getArgument(0);
                    LambdaQueryWrapper<TradeOrderDO> wrapper = invocation.getArgument(1);
                    capturedPage.set(page);
                    capturedWrapper.set(wrapper);
                    page.setRecords(records);
                    page.setTotal(total);
                    return page;
                });
    }

    private String normalizeSql(String sql) {
        return sql.replaceAll("\\s+", " ").trim().toUpperCase(Locale.ROOT);
    }
}
