package com.shop.module.system.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 管理后台 — 数据看板服务
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 核心指标汇总：今日订单数、今日销售额、商品总数、会员总数
     */
    public Map<String, Object> getSummary() {
        Map<String, Object> result = new LinkedHashMap<>();

        String today = LocalDate.now().toString();

        // 今日订单数
        Integer todayOrderCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM trade_order WHERE deleted = 0 AND DATE(create_time) = ?",
                Integer.class, today);
        result.put("todayOrderCount", todayOrderCount != null ? todayOrderCount : 0);

        // 今日销售额（分）
        Long todaySalesAmount = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(actual_price), 0) FROM trade_order " +
                        "WHERE deleted = 0 AND DATE(pay_time) = ? AND pay_status = 1",
                Long.class, today);
        result.put("todaySalesAmount", todaySalesAmount != null ? todaySalesAmount : 0);

        // 商品总数（上架）
        Integer productCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM product_spu WHERE deleted = 0 AND status = 1",
                Integer.class);
        result.put("productCount", productCount != null ? productCount : 0);

        // 会员总数
        Integer memberCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM member_user WHERE deleted = 0",
                Integer.class);
        result.put("memberCount", memberCount != null ? memberCount : 0);

        return result;
    }

    /**
     * 订单趋势数据：近 N 天的每日订单量和销售额
     */
    public List<Map<String, Object>> getOrderTrend(int days) {
        if (days <= 0) days = 7;
        if (days > 90) days = 90;

        LocalDate startDate = LocalDate.now().minusDays(days - 1);
        String startStr = startDate.toString();

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT DATE(pay_time) AS date, COUNT(*) AS order_count, " +
                "COALESCE(SUM(actual_price), 0) AS sales_amount " +
                "FROM trade_order WHERE deleted = 0 AND pay_status = 1 AND DATE(pay_time) >= ? " +
                "GROUP BY DATE(pay_time) ORDER BY date ASC", startStr);

        // 构建完整日期序列（补齐无数据的日期为 0）
        Map<String, Map<String, Object>> dateMap = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            dateMap.put(row.get("date").toString(), row);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (int i = 0; i < days; i++) {
            LocalDate d = startDate.plusDays(i);
            String dateStr = d.format(fmt);
            Map<String, Object> dayData = dateMap.get(dateStr);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", dateStr);
            if (dayData != null) {
                item.put("orderCount", ((Number) dayData.get("order_count")).intValue());
                item.put("salesAmount", ((Number) dayData.get("sales_amount")).longValue());
            } else {
                item.put("orderCount", 0);
                item.put("salesAmount", 0);
            }
            result.add(item);
        }
        return result;
    }

    /**
     * 订单状态分布
     */
    public List<Map<String, Object>> getOrderStatusDistribution() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT status, COUNT(*) AS count FROM trade_order WHERE deleted = 0 GROUP BY status ORDER BY status");

        // 状态映射
        Map<Integer, String> statusNames = Map.of(
                0, "待付款", 1, "待发货", 2, "待收货",
                3, "已完成", 4, "已取消", 5, "退款中");

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            int status = ((Number) row.get("status")).intValue();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", statusNames.getOrDefault(status, "未知"));
            item.put("value", ((Number) row.get("count")).intValue());
            result.add(item);
        }
        return result;
    }

    /**
     * 热销商品 TOP N（按订单商品销量汇总排序）
     */
    public List<Map<String, Object>> getTopProducts(int limit) {
        if (limit <= 0) limit = 10;
        if (limit > 50) limit = 50;

        return jdbcTemplate.queryForList(
                "SELECT COALESCE(MAX(p.name), MAX(oi.goods_name)) AS name, SUM(oi.count) AS sales_count, " +
                "SUM(oi.total_price) AS sales_amount, MAX(oi.goods_pic_url) AS pic_url " +
                "FROM trade_order_item oi " +
                "INNER JOIN trade_order o ON oi.order_id = o.id AND o.deleted = 0 " +
                "LEFT JOIN product_spu p ON p.id = oi.spu_id AND p.deleted = 0 " +
                "WHERE oi.deleted = 0 AND o.pay_status = 1 " +
                "GROUP BY oi.spu_id ORDER BY sales_count DESC LIMIT ?", limit);
    }

    /**
     * 最近订单列表
     */
    public List<Map<String, Object>> getRecentOrders() {
        return jdbcTemplate.queryForList(
                "SELECT o.id, o.order_sn, o.status, o.pay_status, o.actual_price, " +
                "o.consignee, o.create_time, " +
                "(SELECT COALESCE(SUM(oi.count), 0) FROM trade_order_item oi WHERE oi.order_id = o.id AND oi.deleted = 0) AS item_count " +
                "FROM trade_order o WHERE o.deleted = 0 ORDER BY o.create_time DESC LIMIT 10");
    }
}
