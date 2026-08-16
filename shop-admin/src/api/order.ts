import { http } from "@/utils/http";
import type {
  BatchShipResult,
  DeliveryNote,
  PageResult,
  PickingList,
  TradeLogistics,
  TradeOrder,
  TradeOrderDetail
} from "./types";

/** 订单分页列表 */
export const getOrderPage = (params: {
  page?: number;
  size?: number;
  userId?: number;
  orderId?: number;
  status?: number;
  payStatus?: number;
  orderSn?: string;
  mobile?: string;
  createTimeStart?: string;
  createTimeEnd?: string;
}) => {
  return http.get<PageResult<TradeOrder>, typeof params>(
    "/admin-api/trade/order/list",
    { params }
  );
};

export const exportOrders = (params: Parameters<typeof getOrderPage>[0]) => {
  return http.get<Blob, typeof params>("/admin-api/trade/order/export", {
    params,
    responseType: "blob"
  } as any);
};

/** 订单详情 */
export const getOrderDetail = (orderId: number) => {
  return http.get<TradeOrderDetail, { orderId: number }>(
    "/admin-api/trade/order/detail",
    { params: { orderId } }
  );
};

/** 管理员发货 */
export const shipOrder = (data: {
  orderId: number;
  logisticsCompany?: string;
  logisticsCode?: string;
  logisticsNo?: string;
}) => {
  return http.post<TradeLogistics, typeof data>("/admin-api/trade/order/ship", {
    data
  });
};

export const downloadBatchShipTemplate = () => {
  return http.get<Blob, undefined>("/admin-api/trade/order/batch-ship/template", {
    responseType: "blob"
  } as any);
};

export const importBatchShip = (data: { content: string; dryRun?: boolean }) => {
  return http.post<BatchShipResult, typeof data>(
    "/admin-api/trade/order/batch-ship/import",
    { data }
  );
};

export const updateOrderRemark = (data: { orderId: number; remark: string }) => {
  return http.post<boolean, typeof data>("/admin-api/trade/order/remark", {
    data
  });
};

export const getDeliveryNote = (orderId: number) => {
  return http.get<DeliveryNote, { orderId: number }>(
    "/admin-api/trade/order/delivery-note",
    { params: { orderId } }
  );
};

export const getPickingList = (orderIds: number[]) => {
  return http.get<PickingList, { orderIds: string }>(
    "/admin-api/trade/order/picking-list",
    { params: { orderIds: orderIds.join(",") } }
  );
};

/** 查询订单物流详情 */
export const getOrderLogistics = (orderId: number) => {
  return http.get<TradeLogistics, { orderId: number }>(
    "/admin-api/trade/logistics/detail",
    { params: { orderId } }
  );
};
