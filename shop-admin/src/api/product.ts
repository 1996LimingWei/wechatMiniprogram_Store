import { http } from "@/utils/http";
import type { PageParam, PageResult, ProductSpu } from "./types";

/** 商品分页列表 */
export const getProductPage = (params: PageParam) => {
    return http.get<PageResult<ProductSpu>, PageParam>(
        "/admin-api/product/spu/page",
        { params }
    );
};

/** 新增商品 */
export const createProduct = (data: ProductSpu) => {
    return http.post<boolean, ProductSpu>("/admin-api/product/spu/create", {
        data
    });
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
