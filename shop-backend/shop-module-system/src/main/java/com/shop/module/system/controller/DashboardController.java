package com.shop.module.system.controller;

import com.shop.common.pojo.CommonResult;
import com.shop.module.system.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理后台 — 数据看板
 */
@RestController
@RequestMapping("/admin-api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * 核心指标汇总
     */
    @RequestMapping("/summary")
    public CommonResult<Map<String, Object>> summary() {
        return CommonResult.success(dashboardService.getSummary());
    }

    /**
     * 订单趋势（近 N 天）
     */
    @RequestMapping("/order-trend")
    public CommonResult<List<Map<String, Object>>> orderTrend(
            @RequestParam(defaultValue = "7") int days) {
        return CommonResult.success(dashboardService.getOrderTrend(days));
    }

    /**
     * 订单状态分布
     */
    @RequestMapping("/order-status")
    public CommonResult<List<Map<String, Object>>> orderStatus() {
        return CommonResult.success(dashboardService.getOrderStatusDistribution());
    }

    /**
     * 热销商品 TOP N
     */
    @RequestMapping("/top-products")
    public CommonResult<List<Map<String, Object>>> topProducts(
            @RequestParam(defaultValue = "10") int limit) {
        return CommonResult.success(dashboardService.getTopProducts(limit));
    }

    /**
     * 最近订单列表
     */
    @RequestMapping("/recent-orders")
    public CommonResult<List<Map<String, Object>>> recentOrders() {
        return CommonResult.success(dashboardService.getRecentOrders());
    }
}
