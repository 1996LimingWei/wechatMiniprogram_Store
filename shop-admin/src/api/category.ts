import { http } from "@/utils/http";
import type { Category } from "./types";

/** 获取全部分类列表 */
export const getCategoryList = () => {
    return http.get<Category[], undefined>("/admin-api/product/category/list");
};

/** 新增分类 */
export const createCategory = (data: Category) => {
    return http.post<boolean, Category>("/admin-api/product/category/create", {
        data
    });
};

/** 更新分类 */
export const updateCategory = (data: Category) => {
    return http.request<boolean>("put", "/admin-api/product/category/update", {
        data
    });
};

/** 删除分类 */
export const deleteCategory = (id: number) => {
    return http.request<boolean>("delete", "/admin-api/product/category/delete", {
        params: { id }
    });
};
