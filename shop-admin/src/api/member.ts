import { http } from "@/utils/http";
import type { PageResult, MemberUser, MemberUserDetail, ProductComment } from "./types";

// ==================== 会员 ====================
export const getMemberPage = (params: {
    pageNo?: number;
    pageSize?: number;
    nickname?: string;
    mobile?: string;
}) => {
    return http.get<PageResult<MemberUser>, typeof params>(
        "/admin-api/member/user/page",
        { params }
    );
};

export const getMemberDetail = (id: number) => {
    return http.get<MemberUserDetail, { id: number }>("/admin-api/member/user/detail", {
        params: { id }
    });
};

export const updateMember = (data: {
    id: number;
    nickname?: string;
    mobile?: string;
    avatar?: string;
    status?: number;
}) => {
    return http.request<boolean>("put", "/admin-api/member/user/update", {
        data
    });
};

// ==================== 评论 ====================
export const getCommentPage = (params: {
    pageNo?: number;
    pageSize?: number;
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
