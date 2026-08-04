import { http } from "@/utils/http";
import type { ContentBanner, ContentChannel, ContentBrand, ContentTopic } from "./types";

// ==================== Banner ====================
export const getBannerList = () => {
    return http.get<ContentBanner[], undefined>("/admin-api/content/banner/list");
};
export const createBanner = (data: ContentBanner) => {
    return http.post<boolean, ContentBanner>("/admin-api/content/banner/create", { data });
};
export const updateBanner = (data: ContentBanner) => {
    return http.request<boolean>("put", "/admin-api/content/banner/update", { data });
};
export const deleteBanner = (id: number) => {
    return http.request<boolean>("delete", "/admin-api/content/banner/delete", { params: { id } });
};

// ==================== 频道 ====================
export const getChannelList = () => {
    return http.get<ContentChannel[], undefined>("/admin-api/content/channel/list");
};
export const createChannel = (data: ContentChannel) => {
    return http.post<boolean, ContentChannel>("/admin-api/content/channel/create", { data });
};
export const updateChannel = (data: ContentChannel) => {
    return http.request<boolean>("put", "/admin-api/content/channel/update", { data });
};
export const deleteChannel = (id: number) => {
    return http.request<boolean>("delete", "/admin-api/content/channel/delete", { params: { id } });
};

// ==================== 品牌 ====================
export const getBrandList = () => {
    return http.get<ContentBrand[], undefined>("/admin-api/content/brand/list");
};
export const createBrand = (data: ContentBrand) => {
    return http.post<boolean, ContentBrand>("/admin-api/content/brand/create", { data });
};
export const updateBrand = (data: ContentBrand) => {
    return http.request<boolean>("put", "/admin-api/content/brand/update", { data });
};
export const deleteBrand = (id: number) => {
    return http.request<boolean>("delete", "/admin-api/content/brand/delete", { params: { id } });
};

// ==================== 专题 ====================
export const getTopicList = () => {
    return http.get<ContentTopic[], undefined>("/admin-api/content/topic/list");
};
export const createTopic = (data: ContentTopic) => {
    return http.post<boolean, ContentTopic>("/admin-api/content/topic/create", { data });
};
export const updateTopic = (data: ContentTopic) => {
    return http.request<boolean>("put", "/admin-api/content/topic/update", { data });
};
export const deleteTopic = (id: number) => {
    return http.request<boolean>("delete", "/admin-api/content/topic/delete", { params: { id } });
};

// ==================== 专题关联商品 ====================
export const getTopicProducts = (topicId: number) => {
    return http.get<number[], undefined>("/admin-api/content/topic/products", { params: { topicId } });
};
export const setTopicProducts = (data: { topicId: number; spuIds: number[] }) => {
    return http.post<boolean, typeof data>("/admin-api/content/topic/products", { data });
};
