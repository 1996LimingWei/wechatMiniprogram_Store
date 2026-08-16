import { http } from "@/utils/http";
import type { PageResult } from "./types";

export interface AdminUser {
  id: number;
  username: string;
  nickname: string;
  avatar?: string;
  status: number;
  failedLoginCount: number;
  lockedUntil?: string;
  lastLoginTime?: string;
  lastLoginIp?: string;
  roleIds: number[];
  roleCodes: string[];
  roleNames: string[];
  createTime?: string;
}

export interface SystemRole {
  id: number;
  code: string;
  name: string;
  status: number;
  permissionIds: number[];
  permissionCodes: string[];
  createTime?: string;
}

export interface SystemPermission {
  id: number;
  code: string;
  name: string;
  pathPattern: string;
  httpMethod: string;
  status: number;
}

export interface LoginAuditLog {
  id: number;
  adminUserId?: number;
  username: string;
  nickname: string;
  success: number;
  ip?: string;
  userAgent?: string;
  message?: string;
  createTime?: string;
}

export interface OperationAuditLog {
  id: number;
  adminUserId: number;
  username: string;
  nickname: string;
  adminRoleCodes?: string;
  method: string;
  requestUri: string;
  operationType?: string;
  highRisk?: number;
  businessRef?: string;
  success: number;
  ip?: string;
  userAgent?: string;
  durationMs: number;
  message?: string;
  beforeSnapshot?: string;
  afterSnapshot?: string;
  createTime?: string;
}

export const getAdminUserPage = (params: Record<string, unknown>) =>
  http.get<PageResult<AdminUser>, Record<string, unknown>>(
    "/admin-api/system/admin-user/page",
    { params }
  );

export const saveAdminUser = (data: Record<string, unknown>) =>
  http.post<number, Record<string, unknown>>("/admin-api/system/admin-user/save", { data });

export const setAdminUserStatus = (id: number, status: number) =>
  http.post<boolean, { id: number; status: number }>("/admin-api/system/admin-user/status", { data: { id, status } });

export const unlockAdminUser = (id: number) =>
  http.post<boolean, { id: number }>("/admin-api/system/admin-user/unlock", { data: { id } });

export const resetAdminUserPassword = (id: number, password: string) =>
  http.post<boolean, { id: number; password: string }>("/admin-api/system/admin-user/reset-password", { data: { id, password } });

export const forceLogoutAdminUser = (id: number) =>
  http.post<boolean, { id: number }>("/admin-api/system/admin-user/force-logout", { data: { id } });

export const getSystemRoles = () => http.get<SystemRole[], undefined>("/admin-api/system/role/list");
export const saveSystemRole = (data: Record<string, unknown>) => http.post<number, Record<string, unknown>>("/admin-api/system/role/save", { data });
export const setSystemRoleStatus = (id: number, status: number) => http.post<boolean, { id: number; status: number }>("/admin-api/system/role/status", { data: { id, status } });
export const getSystemPermissions = () => http.get<SystemPermission[], undefined>("/admin-api/system/permission/list");

export const changeOwnPassword = (oldPassword: string, newPassword: string) =>
  http.post<boolean, { oldPassword: string; newPassword: string }>("/admin-api/system/password/change", { data: { oldPassword, newPassword } });

export const getLoginAuditPage = (params: Record<string, unknown>) => http.get<PageResult<LoginAuditLog>, Record<string, unknown>>("/admin-api/system/audit/login-page", { params });
export const getOperationAuditPage = (params: Record<string, unknown>) => http.get<PageResult<OperationAuditLog>, Record<string, unknown>>("/admin-api/system/audit/operation-page", { params });
