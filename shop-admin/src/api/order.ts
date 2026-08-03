import { http } from "@/utils/http";
import type { PageResult, TradeOrder } from "./types";

/** 订单分页列表 */
export const getOrderPage = (params: {
    page?: number;
    size?: number;
    status?: number;
    orderSn?: string;
}) => {
    return http.post<PageResult<TradeOrder>, typeof params>(
        "/admin-api/trade/order/list",
        { data: params }
    );
};

/** 订单详情 */
export const getOrderDetail = (orderId: number) => {
    return http.post<TradeOrder, { orderId: number }>(
        "/admin-api/trade/order/detail",
        { data: { orderId } }
    );
};

/** 管理员发货 */
export const shipOrder = (data: {
    orderId: number;
    logisticsCompany?: string;
    logisticsNo?: string;
}) => {
    return http.post<Record<string, any>, typeof data>(
        "/admin-api/trade/order/ship",
        { data }
    );
};
