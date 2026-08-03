import { storageLocal } from "@pureadmin/utils";
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

/** 获取当前用户信息（前端本地存储读取，暂无后端接口） */
export const getUserInfo = (): Promise<UserInfo> => {
    return new Promise((resolve, reject) => {
        const info = storageLocal().getItem<{
            token?: string;
            userId?: number;
            username?: string;
            nickname?: string;
            avatar?: string;
            roles?: string[];
            permissions?: string[];
        }>("user-info");
        if (info?.token) {
            resolve({
                userId: info.userId ?? 0,
                username: info.username ?? "",
                nickname: info.nickname ?? "管理员",
                avatar: info.avatar ?? "",
                roles: info.roles ?? ["admin"],
                permissions: info.permissions ?? ["*:*:*"]
            });
        } else {
            reject(new Error("未登录"));
        }
    });
};

/** 退出登录（纯前端操作，暂无后端接口） */
export const logoutApi = (): Promise<boolean> => {
    return Promise.resolve(true);
};
