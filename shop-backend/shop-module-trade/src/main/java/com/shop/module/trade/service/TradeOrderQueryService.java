package com.shop.module.trade.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shop.common.exception.ServerException;
import com.shop.common.pojo.PageResult;
import com.shop.module.trade.dal.dataobject.TradeOrderDO;
import com.shop.module.trade.dal.mysql.TradeOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单列表只读查询服务。
 */
@Service
@RequiredArgsConstructor
public class TradeOrderQueryService {

    static final int MAX_PAGE_SIZE = 100;
    private static final DateTimeFormatter QUERY_TIME_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("uuuu-MM-dd HH:mm:ss")
            .toFormatter()
            .withResolverStyle(ResolverStyle.STRICT);

    private final TradeOrderMapper tradeOrderMapper;
    private final TradeOrderListAssembler tradeOrderListAssembler;

    public Map<String, Object> getOrderList(Long userId, int showType, int page, int size) {
        int finalPage = normalizePage(page);
        int finalSize = normalizeSize(size);
        LambdaQueryWrapper<TradeOrderDO> wrapper = new LambdaQueryWrapper<TradeOrderDO>()
                .eq(TradeOrderDO::getUserId, userId);
        Integer status = mapShowTypeToStatus(showType);
        if (status != null) {
            if (status < 0) {
                wrapper.and(condition -> condition.eq(TradeOrderDO::getStatus, 5)
                        .or()
                        .eq(TradeOrderDO::getPayStatus, TradeOrderPayStatus.REFUNDED));
            } else {
                wrapper.eq(TradeOrderDO::getStatus, status);
            }
        }
        addStableOrder(wrapper);

        Page<TradeOrderDO> pageResult = tradeOrderMapper.selectPage(
                new Page<>(finalPage, finalSize), wrapper);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", tradeOrderListAssembler.assemble(pageResult.getRecords()));
        result.put("page", finalPage);
        result.put("total", pageResult.getTotal());
        return result;
    }

    public PageResult<Map<String, Object>> getAdminOrderPage(int page, int size,
                                                              Map<String, Object> request) {
        int finalPage = normalizePage(page);
        int finalSize = normalizeSize(size);
        LambdaQueryWrapper<TradeOrderDO> wrapper = new LambdaQueryWrapper<>();

        Long userId = getLong(request, "userId", 0L);
        Long orderId = getLong(request, "orderId", 0L);
        Integer status = getInteger(request, "status");
        Integer payStatus = getInteger(request, "payStatus");
        String orderSn = getString(request, "orderSn");
        String mobile = getString(request, "mobile");
        LocalDateTime createTimeStart = parseTime(request, "createTimeStart");
        LocalDateTime createTimeEnd = parseTime(request, "createTimeEnd");
        validateTimeRange(createTimeStart, createTimeEnd);
        validateMobilePrefix(mobile);

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
            wrapper.eq(TradeOrderDO::getOrderSn, orderSn);
        }
        if (!mobile.isBlank()) {
            wrapper.likeRight(TradeOrderDO::getMobile, mobile);
        }
        if (createTimeStart != null) {
            wrapper.ge(TradeOrderDO::getCreateTime, createTimeStart);
        }
        if (createTimeEnd != null) {
            wrapper.lt(TradeOrderDO::getCreateTime, createTimeEnd);
        }
        addStableOrder(wrapper);

        Page<TradeOrderDO> pageResult = tradeOrderMapper.selectPage(
                new Page<>(finalPage, finalSize), wrapper);
        List<Map<String, Object>> list = tradeOrderListAssembler.assemble(pageResult.getRecords());
        return new PageResult<>(list, pageResult.getTotal());
    }

    private void addStableOrder(LambdaQueryWrapper<TradeOrderDO> wrapper) {
        wrapper.orderByDesc(TradeOrderDO::getCreateTime)
                .orderByDesc(TradeOrderDO::getId);
    }

    private int normalizePage(int page) {
        return Math.max(page, 1);
    }

    private int normalizeSize(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }

    private LocalDateTime parseTime(Map<String, Object> request, String key) {
        String value = getString(request, key);
        if (value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, QUERY_TIME_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new ServerException(400, key + "格式必须为 yyyy-MM-dd HH:mm:ss");
        }
    }

    private void validateTimeRange(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && !start.isBefore(end)) {
            throw new ServerException(400, "createTimeStart必须早于createTimeEnd");
        }
    }

    private void validateMobilePrefix(String mobile) {
        if (!mobile.isBlank() && !mobile.matches("\\d+")) {
            throw new ServerException(400, "mobile必须为纯数字前缀");
        }
    }

    private String getString(Map<String, Object> request, String key) {
        Object value = request == null ? null : request.get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private Long getLong(Map<String, Object> request, String key, Long defaultValue) {
        Object value = request == null ? null : request.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private Integer getInteger(Map<String, Object> request, String key) {
        Object value = request == null ? null : request.get(key);
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
}
