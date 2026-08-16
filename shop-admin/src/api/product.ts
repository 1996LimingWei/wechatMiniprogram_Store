import { http } from "@/utils/http";
import type { PageParam, PageResult, ProductSpu } from "./types";
import type { ProductSku } from "./types";
import type { ProductComment } from "./types";

export interface ProductImportRow {
    rowNo: number;
    valid: boolean;
    productName?: string;
    categoryName?: string;
    skuCode?: string;
    specName?: string;
    specValue?: string;
    price?: string;
    stock?: number;
    errorColumns?: string[];
    errors?: string[];
}

export interface ProductImportPreview {
    totalRows: number;
    validRows: number;
    errorRows: number;
    createdProductCount: number;
    createdSkuCount: number;
    dryRun: boolean;
    rows: ProductImportRow[];
}

export interface ProductBatchItemResult {
    id: number;
    name?: string;
    success: boolean;
    message: string;
    beforePrice?: number;
    afterPrice?: number;
    beforeStock?: number;
    afterStock?: number;
}

export interface ProductBatchOperationResult {
    totalCount: number;
    successCount: number;
    failureCount: number;
    dryRun: boolean;
    rows: ProductBatchItemResult[];
}

export interface ProductBatchOperationRequest {
    ids: number[];
    confirmCount?: number;
    status?: number;
    categoryId?: number;
    sort?: number;
    priceAdjustType?: "FIXED_AMOUNT" | "PERCENT";
    priceAdjustValue?: number;
    stockDelta?: number;
    reason?: string;
}

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
export const saveProduct = (
    spu: ProductSpu,
    skus: ProductSku[],
    stockAdjustReason = ""
) => {
    return http.post<number, { spu: ProductSpu; skus: ProductSku[]; stockAdjustReason: string }>(
        "/admin-api/product/spu/save",
        { data: { spu, skus, stockAdjustReason } }
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

/** 下载商品导入模板 */
export const downloadProductImportTemplate = () => {
    return http.request<Blob>("get", "/admin-api/product/spu/import-template", {
        responseType: "blob"
    });
};

/** 商品导入预校验，不写入数据库 */
export const previewProductImport = (file: File) => {
    const formData = new FormData();
    formData.append("file", file);
    return http.request<ProductImportPreview>("post", "/admin-api/product/spu/import-preview", {
        data: formData,
        headers: { "Content-Type": "multipart/form-data" }
    });
};

/** 确认导入商品，成功后写入商品、SKU 和库存流水 */
export const confirmProductImport = (file: File) => {
    const formData = new FormData();
    formData.append("file", file);
    return http.request<ProductImportPreview>("post", "/admin-api/product/spu/import-confirm", {
        data: formData,
        headers: { "Content-Type": "multipart/form-data" }
    });
};

/** 按当前筛选导出商品 CSV */
export const exportProducts = (params: {
    name?: string;
    categoryId?: number;
    status?: number;
    startTime?: string;
    endTime?: string;
}) => {
    return http.request<Blob>("get", "/admin-api/product/spu/export", {
        params,
        responseType: "blob"
    });
};

export const batchUpdateProductStatus = (data: ProductBatchOperationRequest) =>
    http.post<ProductBatchOperationResult, ProductBatchOperationRequest>(
        "/admin-api/product/spu/batch/status",
        { data }
    );

export const batchUpdateProductCategory = (data: ProductBatchOperationRequest) =>
    http.post<ProductBatchOperationResult, ProductBatchOperationRequest>(
        "/admin-api/product/spu/batch/category",
        { data }
    );

export const batchUpdateProductSort = (data: ProductBatchOperationRequest) =>
    http.post<ProductBatchOperationResult, ProductBatchOperationRequest>(
        "/admin-api/product/spu/batch/sort",
        { data }
    );

export const previewProductPriceBatch = (data: ProductBatchOperationRequest) =>
    http.post<ProductBatchOperationResult, ProductBatchOperationRequest>(
        "/admin-api/product/spu/batch/price-preview",
        { data }
    );

export const batchUpdateProductPrice = (data: ProductBatchOperationRequest) =>
    http.post<ProductBatchOperationResult, ProductBatchOperationRequest>(
        "/admin-api/product/spu/batch/price",
        { data }
    );

export const batchUpdateProductStock = (data: ProductBatchOperationRequest) =>
    http.post<ProductBatchOperationResult, ProductBatchOperationRequest>(
        "/admin-api/product/spu/batch/stock",
        { data }
    );

export const getCommentPage = (params: {
    pageNo?: number;
    pageSize?: number;
    status?: number;
}) => http.get<PageResult<ProductComment>, typeof params>("/admin-api/product/comment/page", { params });

export const updateCommentStatus = (id: number, status: number) =>
    http.request<boolean>("put", "/admin-api/product/comment/status", { data: { id, status } });
