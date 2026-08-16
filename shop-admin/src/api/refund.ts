import { http } from "@/utils/http";
import type { PageResult } from "./types";

export interface RefundWorkbenchItem {
  id: number;
  afterSaleSn: string;
  refundSn: string;
  orderId: number;
  orderSn: string;
  userId: number;
  type: number;
  status: number;
  statusText: string;
  refundAmount: string;
  payAmount: string;
  payRefundedAmount: string;
  reason: string;
  refundProvider: string;
  providerRefundNo: string;
  refundMessage: string;
  refundAttemptCount: number;
  refundLastAttemptTime: string;
  refundNextAttemptTime: string;
  refundClaimUntil: string;
  refundLastError: string;
  refundChannelState: string;
  refundExceptionCode: string;
  refundExceptionMessage: string;
  refundHandled: number;
  refundHandleRemark: string;
  refundHandleAdminId?: number;
  refundHandleTime: string;
  applyTime: string;
  auditTime: string;
  refundTime: string;
  createTime: string;
  canRetry: boolean;
}

export interface RefundWorkbenchDetail {
  refund: RefundWorkbenchItem;
  taskRecords: Array<Record<string, unknown>>;
  callbackRecords: Array<Record<string, unknown>>;
}

export const getRefundPage = (params: Record<string, unknown>) =>
  http.get<PageResult<RefundWorkbenchItem>, Record<string, unknown>>(
    "/admin-api/trade/refund/list",
    { params }
  );

export const getRefundDetail = (afterSaleId: number) =>
  http.get<RefundWorkbenchDetail, { afterSaleId: number }>(
    "/admin-api/trade/refund/detail",
    { params: { afterSaleId } }
  );

export const syncRefund = (afterSaleId: number) =>
  http.post<RefundWorkbenchDetail, { afterSaleId: number }>(
    "/admin-api/trade/refund/sync",
    { data: { afterSaleId } }
  );

export const retryRefund = (afterSaleId: number) =>
  http.post<RefundWorkbenchDetail, { afterSaleId: number }>(
    "/admin-api/trade/refund/retry",
    { data: { afterSaleId } }
  );

export const handleRefundException = (afterSaleId: number, remark: string) =>
  http.post<boolean, { afterSaleId: number; remark: string }>(
    "/admin-api/trade/refund/handle",
    { data: { afterSaleId, remark } }
  );
