import { http } from "@/utils/http";
import type { PageResult } from "./types";

export interface ReconcileBatch {
  id: number;
  reconcileDate: string;
  status: number;
  statusText: string;
  source: string;
  localPayCount: number;
  localPayAmount: string;
  localRefundCount: number;
  localRefundAmount: string;
  localNetAmount: string;
  wechatPayCount: number;
  wechatPayAmount: string;
  wechatRefundCount: number;
  wechatRefundAmount: string;
  wechatNetAmount: string;
  feeAmount: string;
  differenceCount: number;
  tradeBillUrl: string;
  fundBillUrl: string;
  triggerType: string;
  triggerAdminId?: number;
  message: string;
  startTime: string;
  finishTime: string;
  createTime: string;
}

export interface ReconcileDifference {
  id: number;
  batchId: number;
  reconcileDate: string;
  diffType: string;
  diffTypeText: string;
  businessType: string;
  businessTypeText: string;
  businessSn: string;
  orderSn: string;
  localAmount: string;
  channelAmount: string;
  localStatus: string;
  channelStatus: string;
  reason: string;
  handled: number;
  handleRemark: string;
  handleAdminId?: number;
  handleTime: string;
  createTime: string;
}

export interface ReconcileDetail {
  batch: ReconcileBatch;
  differences: ReconcileDifference[];
}

export const getReconcileBatchPage = (params: Record<string, unknown>) =>
  http.get<PageResult<ReconcileBatch>, Record<string, unknown>>(
    "/admin-api/trade/reconcile/batch/list",
    { params }
  );

export const getReconcileBatchDetail = (batchId: number) =>
  http.get<ReconcileDetail, { batchId: number }>(
    "/admin-api/trade/reconcile/batch/detail",
    { params: { batchId } }
  );

export const getReconcileDifferencePage = (params: Record<string, unknown>) =>
  http.get<PageResult<ReconcileDifference>, Record<string, unknown>>(
    "/admin-api/trade/reconcile/difference/list",
    { params }
  );

export const runReconcile = (reconcileDate: string) =>
  http.post<ReconcileDetail, { reconcileDate: string }>(
    "/admin-api/trade/reconcile/run",
    { data: { reconcileDate } }
  );

export const handleReconcileDifference = (differenceId: number, remark: string) =>
  http.post<boolean, { differenceId: number; remark: string }>(
    "/admin-api/trade/reconcile/difference/handle",
    { data: { differenceId, remark } }
  );

export const exportReconcile = (batchId: number) =>
  http.get<Blob, { batchId: number }>("/admin-api/trade/reconcile/export", {
    params: { batchId },
    responseType: "blob"
  } as any);
