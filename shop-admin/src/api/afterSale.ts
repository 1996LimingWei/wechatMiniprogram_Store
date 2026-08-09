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
export const approveAfterSale = (afterSaleId: number) => {
  return http.post<AfterSale, { afterSaleId: number }>(
    "/admin-api/trade/after-sale/approve",
    { data: { afterSaleId } }
  );
};

/** 拒绝售后 */
export const rejectAfterSale = (afterSaleId: number, rejectReason: string) => {
  return http.post<AfterSale, { afterSaleId: number; rejectReason: string }>(
    "/admin-api/trade/after-sale/reject",
    { data: { afterSaleId, rejectReason } }
  );
};

/** 同步退款渠道状态 */
export const syncAfterSale = (afterSaleId: number) => {
  return http.post<AfterSale, { afterSaleId: number }>(
    "/admin-api/trade/after-sale/sync",
    { data: { afterSaleId } }
  );
};
