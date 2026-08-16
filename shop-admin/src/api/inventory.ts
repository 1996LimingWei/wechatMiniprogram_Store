import { http } from "@/utils/http";
import type { PageResult } from "./types";

export interface InventorySku {
  skuId: number;
  spuId: number;
  skuCode?: string;
  productName: string;
  specName: string;
  categoryId?: number;
  categoryName?: string;
  picUrl?: string;
  price: number;
  stock: number;
  availableStock: number;
  lockedStock: number;
  warningStock: number;
  stockStatus: "OUT_OF_STOCK" | "LOW_STOCK" | "NORMAL";
  stockStatusName: string;
  createTime?: string;
}

export interface InventoryStockLog {
  id: number;
  skuId: number;
  spuId: number;
  skuCode?: string;
  productName?: string;
  bizType: string;
  bizNo: string;
  changeQuantity: number;
  beforeStock: number;
  afterStock: number;
  operatorType: string;
  operatorId: number;
  remark?: string;
  createTime?: string;
}

export interface InventoryReconcileRow {
  skuId: number;
  spuId: number;
  skuCode?: string;
  productName?: string;
  currentStock: number;
  ledgerStock: number;
  difference: number;
}

export interface InventoryReconcileResult {
  totalSkuCount: number;
  mismatchCount: number;
  rows: InventoryReconcileRow[];
}

export const getInventoryPage = (params: {
  pageNo?: number;
  pageSize?: number;
  productName?: string;
  skuCode?: string;
  stockStatus?: string;
  lowStockOnly?: boolean;
}) => http.get<PageResult<InventorySku>, typeof params>("/admin-api/product/inventory/page", { params });

export const updateInventoryWarningStock = (data: {
  skuId: number;
  warningStock: number;
}) => http.request<boolean>("put", "/admin-api/product/inventory/warning-stock", { data });

export const adjustInventoryStock = (data: {
  skuId: number;
  changeQuantity: number;
  reason: string;
}) => http.post<InventorySku, typeof data>("/admin-api/product/inventory/adjust", { data });

export const getInventoryLogPage = (params: {
  pageNo?: number;
  pageSize?: number;
  skuId?: number;
  spuId?: number;
  bizNo?: string;
}) => http.get<PageResult<InventoryStockLog>, typeof params>("/admin-api/product/inventory/log-page", { params });

export const reconcileInventory = () =>
  http.get<InventoryReconcileResult, undefined>("/admin-api/product/inventory/reconcile");
