import { http } from "@/utils/http";
import type { PageResult, MemberUser, ProductComment } from "./types";

// ==================== 会员 ====================
export const getMemberPage = (params: {
    page?: number;
    size?: number;
    nickname?: string;
    mobile?: string;
}) => {
    return http.get<PageResult<MemberUser>, typeof params>(
        "/admin-api/member/user/page",
        { params }
    );
};

export const getMemberDetail = (id: number) => {
    return http.get<MemberUser, { id: number }>("/admin-api/member/user/detail", {
        params: { id }
    });
};

// ==================== 评论 ====================
export const getCommentPage = (params: {
    page?: number;
    size?: number;
    status?: number;
}) => {
    return http.get<PageResult<ProductComment>, typeof params>(
        "/admin-api/product/comment/page",
        { params }
    );
};

export const updateCommentStatus = (id: number, status: number) => {
    return http.request<boolean>("put", "/admin-api/product/comment/status", {
        data: { id, status }
    });
};
