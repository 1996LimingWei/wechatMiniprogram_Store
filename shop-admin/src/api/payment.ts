import { http } from "@/utils/http";
import type { PageResult } from "./types";

export interface AdminPayOrder {
  id: number;
  paySn: string;
  orderId: number;
  orderSn: string;
  userId: number;
  amount: string;
  amountCent: number;
  channel: string;
  channelTradeNo: string;
  status: number;
  statusText: string;
  payTime: string;
  lastQueryTime: string;
  wechatTradeState: string;
  wechatAmount: string;
  wechatAmountCent?: number;
  syncMessage: string;
  orderPayStatus?: number;
  orderStatus?: number;
  createTime: string;
}

export interface PayNotifyLog {
  id: number;
  notificationId: string;
  paySn: string;
  channelTradeNo: string;
  eventType: string;
  status: number;
  statusText: string;
  message: string;
  createTime: string;
}

export interface PayException {
  id: number;
  payOrderId?: number;
  paySn: string;
  orderId?: number;
  orderSn: string;
  userId?: number;
  reasonCode: string;
  reason: string;
  wechatTradeState: string;
  wechatAmount: string;
  channelTradeNo: string;
  localStatus?: number;
  localStatusText: string;
  orderPayStatus?: number;
  handled: number;
  handleResult: string;
  handleRemark: string;
  handleAdminId?: number;
  handleTime: string;
  lastDetectTime: string;
  createTime: string;
}

export interface PayOrderDetail {
  payOrder: AdminPayOrder;
  order: {
    orderId: number;
    orderSn: string;
    orderStatus?: number;
    payStatus?: number;
    actualPrice: string;
    goodsPrice: string;
    freightPrice: string;
    couponPrice: string;
  };
  notifyLogs: PayNotifyLog[];
  exceptions: PayException[];
}

export interface PaySyncResult {
  payOrderId: number;
  success: boolean;
  message: string;
}

export const getPayOrderPage = (params: Record<string, unknown>) =>
  http.get<PageResult<AdminPayOrder>, Record<string, unknown>>(
    "/admin-api/trade/pay/order/list",
    { params }
  );

export const getPayOrderDetail = (payOrderId: number) =>
  http.get<PayOrderDetail, { payOrderId: number }>(
    "/admin-api/trade/pay/order/detail",
    { params: { payOrderId } }
  );

export const syncPayOrder = (payOrderId: number) =>
  http.post<PaySyncResult, { payOrderId: number }>(
    "/admin-api/trade/pay/order/sync",
    { data: { payOrderId } }
  );

export const getPayExceptionPage = (params: Record<string, unknown>) =>
  http.get<PageResult<PayException>, Record<string, unknown>>(
    "/admin-api/trade/pay/exception/list",
    { params }
  );

export const handlePayException = (exceptionId: number, remark: string) =>
  http.post<boolean, { exceptionId: number; remark: string }>(
    "/admin-api/trade/pay/exception/handle",
    { data: { exceptionId, remark } }
  );
