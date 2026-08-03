import { http } from "@/utils/http";
import type { PageResult, AfterSale } from "./types";

/** 售后列表 */
export const getAfterSalePage = (params: {
    page?: number;
    size?: number;
    status?: number;
}) => {
    return http.post<PageResult<AfterSale>, typeof params>(
        "/admin-api/trade/after-sale/list",
        { data: params }
    );
};

/** 同意售后 */
export const approveAfterSale = (orderId: number) => {
    return http.post<Record<string, any>, { orderId: number }>(
        "/admin-api/trade/after-sale/approve",
        { data: { orderId } }
    );
};

/** 拒绝售后 */
export const rejectAfterSale = (orderId: number, rejectReason?: string) => {
    return http.post<Record<string, any>, { orderId: number; rejectReason?: string }>(
        "/admin-api/trade/after-sale/reject",
        { data: { orderId, rejectReason } }
    );
};
