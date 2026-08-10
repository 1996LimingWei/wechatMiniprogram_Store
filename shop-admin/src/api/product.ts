import { http } from "@/utils/http";
import type { PageParam, PageResult, ProductSpu } from "./types";
import type { ProductSku } from "./types";

/** 商品分页列表（支持筛选） */
export const getProductPage = (
    params: PageParam & {
        name?: string;
        categoryId?: number;
        status?: number;
    }
) => {
    return http.get<PageResult<ProductSpu>, PageParam>(
        "/admin-api/product/spu/page",
        { params }
    );
};

/** 商品详情 */
export const getProductDetail = (id: number) => {
    return http.get<ProductSpu, undefined>("/admin-api/product/spu/detail", {
        params: { id }
    });
};

/** 新增商品 */
export const createProduct = (data: ProductSpu) => {
    return http.post<boolean, ProductSpu>("/admin-api/product/spu/create", {
        data
    });
};

/** 原子保存商品基础信息与 SKU，避免出现半成品商品。 */
export const saveProduct = (spu: ProductSpu, skus: ProductSku[]) => {
    return http.post<number, { spu: ProductSpu; skus: ProductSku[] }>(
        "/admin-api/product/spu/save",
        { data: { spu, skus } }
    );
};

/** 更新商品 */
export const updateProduct = (data: ProductSpu) => {
    return http.request<boolean>("put", "/admin-api/product/spu/update", {
        data
    });
};

/** 删除商品 */
export const deleteProduct = (id: number) => {
    return http.request<boolean>("delete", "/admin-api/product/spu/delete", {
        params: { id }
    });
};
