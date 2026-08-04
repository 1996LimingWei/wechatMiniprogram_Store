import { http } from "@/utils/http";
import type { ProductSku } from "./types";

/** 查询某 SPU 的所有 SKU */
export const getSkuList = (spuId: number) => {
    return http.get<ProductSku[], undefined>(
        "/admin-api/product/sku/list",
        { params: { spuId } }
    );
};

/** 批量保存 SKU（覆盖写入） */
export const saveSkuBatch = (spuId: number, data: ProductSku[]) => {
    return http.post<boolean, ProductSku[]>(
        "/admin-api/product/sku/save-batch",
        { data, params: { spuId } }
    );
};
