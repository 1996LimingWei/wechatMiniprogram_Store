import { http } from "@/utils/http";
import { type LoginParams, type LoginResult, loginApi } from "./user";

/** 用户信息 */
export type UserInfo = {
    userId: number;
    username: string;
    nickname: string;
    avatar?: string;
    roles: string[];
    permissions: string[];
};

/** 管理员登录 */
export { loginApi as login, type LoginParams, type LoginResult };

/** 获取当前管理员及实时角色权限。 */
export const getUserInfo = (): Promise<UserInfo> => {
    return http.get<UserInfo, undefined>("/admin-api/auth/profile");
};

/** 退出登录并立即注销服务端 Token。 */
export const logoutApi = (): Promise<boolean> => {
    return http.post<boolean, undefined>("/admin-api/auth/logout");
};
