import { http } from "@/utils/http";
import type { CouponTemplate, PromotionRule, ShippingRule, CouponInstance, MarketingShippingAuditLog } from "./types";

// ==================== 优惠券模板 ====================
export const getCouponTemplateList = (params: Record<string, any>) => {
    return http.get<{ list: CouponTemplate[]; total: number }, undefined>(
        "/admin-api/marketing/coupon-template/list",
        { params }
    );
};
export const createCouponTemplate = (data: Record<string, any>) => {
    return http.post<CouponTemplate, Record<string, any>>(
        "/admin-api/marketing/coupon-template/create",
        { data }
    );
};
export const updateCouponTemplate = (data: Record<string, any>) => {
    return http.post<CouponTemplate, Record<string, any>>(
        "/admin-api/marketing/coupon-template/update",
        { data }
    );
};
export const deleteCouponTemplate = (data: { id: number }) => {
    return http.post<boolean, { id: number }>(
        "/admin-api/marketing/coupon-template/delete",
        { data }
    );
};
export const updateCouponTemplateStatus = (data: { id: number; status: number }) => {
    return http.request<boolean>("put", "/admin-api/marketing/coupon-template/update-status", { data });
};

// ==================== 满减规则 ====================
export const getPromotionList = (params: Record<string, any>) => {
    return http.get<{ list: PromotionRule[]; total: number }, undefined>(
        "/admin-api/marketing/promotion/list",
        { params }
    );
};
export const createPromotion = (data: Record<string, any>) => {
    return http.post<PromotionRule, Record<string, any>>(
        "/admin-api/marketing/promotion/create",
        { data }
    );
};
export const updatePromotion = (data: Record<string, any>) => {
    return http.post<PromotionRule, Record<string, any>>(
        "/admin-api/marketing/promotion/update",
        { data }
    );
};
export const deletePromotion = (data: { id: number }) => {
    return http.post<boolean, { id: number }>(
        "/admin-api/marketing/promotion/delete",
        { data }
    );
};
export const updatePromotionStatus = (data: { id: number; status: number }) => {
    return http.request<boolean>("put", "/admin-api/marketing/promotion/update-status", { data });
};

// ==================== 包邮规则 ====================
export const getShippingList = (params: Record<string, any>) => {
    return http.get<{ list: ShippingRule[]; total: number }, undefined>(
        "/admin-api/marketing/shipping/list",
        { params }
    );
};
export const getCurrentShipping = () => {
    return http.get<ShippingRule | null, undefined>(
        "/admin-api/marketing/shipping/current"
    );
};
export const createShipping = (data: Record<string, any>) => {
    return http.post<ShippingRule, Record<string, any>>(
        "/admin-api/marketing/shipping/create",
        { data }
    );
};
export const updateShipping = (data: Record<string, any>) => {
    return http.post<ShippingRule, Record<string, any>>(
        "/admin-api/marketing/shipping/update",
        { data }
    );
};
export const updateShippingStatus = (data: { id: number; status: number }) => {
    return http.request<boolean>("put", "/admin-api/marketing/shipping/update-status", { data });
};
export const getShippingAuditList = (params: Record<string, any>) => {
    return http.get<{ list: MarketingShippingAuditLog[]; total: number }, undefined>(
        "/admin-api/marketing/shipping/audit-page",
        { params }
    );
};

// ==================== 优惠券实例 ====================
export const getCouponInstanceList = (params: Record<string, any>) => {
    return http.get<{ list: CouponInstance[]; total: number }, undefined>(
        "/admin-api/marketing/coupon/instance/list",
        { params }
    );
};
