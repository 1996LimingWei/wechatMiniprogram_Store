import { http } from "@/utils/http";
import type { PageResult, UserFeedback } from "./types";

export const getFeedbackPage = (params: { pageNo?: number; pageSize?: number; status?: number; type?: number }) =>
    http.get<PageResult<UserFeedback>, typeof params>("/admin-api/feedback/page", { params });

export const getFeedbackDetail = (id: number) =>
    http.get<UserFeedback, { id: number }>("/admin-api/feedback/detail", { params: { id } });

export const handleFeedback = (data: { id: number; status: number; handleRemark?: string }) =>
    http.post<boolean, typeof data>("/admin-api/feedback/handle", { data });
