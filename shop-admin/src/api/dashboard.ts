import { http } from "@/utils/http";
import type { DashboardSummary, OrderTrend } from "./types";

/** 核心指标汇总 */
export const getDashboardSummary = () => {
    return http.get<DashboardSummary, undefined>(
        "/admin-api/dashboard/summary"
    );
};

/** 订单趋势（近 N 天） */
export const getOrderTrend = (days: number = 7) => {
    return http.get<OrderTrend[], { days: number }>(
        "/admin-api/dashboard/order-trend",
        { params: { days } }
    );
};

/** 订单状态分布 */
export const getOrderStatusDistribution = () => {
    return http.get<Record<string, number>[], undefined>(
        "/admin-api/dashboard/order-status"
    );
};

/** 热销商品 TOP N */
export const getTopProducts = (limit: number = 10) => {
    return http.get<Record<string, any>[], { limit: number }>(
        "/admin-api/dashboard/top-products",
        { params: { limit } }
    );
};
