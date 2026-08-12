import { defineStore } from "pinia";
import {
  type userType,
  store,
  router,
  resetRouter,
  routerArrays,
  storageLocal
} from "../utils";
import { type LoginParams, type LoginResult, loginApi } from "@/api/user";
import { useMultiTagsStoreHook } from "./multiTags";
import {
  type DataInfo,
  setToken,
  removeToken,
  userKey
} from "@/utils/auth";
import { logoutApi } from "@/api/auth";

export const useUserStore = defineStore("pure-user", {
  state: (): userType => ({
    avatar: storageLocal().getItem<DataInfo<number>>(userKey)?.avatar ?? "",
    username: storageLocal().getItem<DataInfo<number>>(userKey)?.username ?? "",
    nickname: storageLocal().getItem<DataInfo<number>>(userKey)?.nickname ?? "",
    roles: storageLocal().getItem<DataInfo<number>>(userKey)?.roles ?? [],
    permissions:
      storageLocal().getItem<DataInfo<number>>(userKey)?.permissions ?? [],
    isRemembered: false,
    loginDay: 7
  }),
  actions: {
    SET_AVATAR(avatar: string) {
      this.avatar = avatar;
    },
    SET_USERNAME(username: string) {
      this.username = username;
    },
    SET_NICKNAME(nickname: string) {
      this.nickname = nickname;
    },
    SET_ROLES(roles: Array<string>) {
      this.roles = roles;
    },
    SET_PERMS(permissions: Array<string>) {
      this.permissions = permissions;
    },
    SET_ISREMEMBERED(bool: boolean) {
      this.isRemembered = bool;
    },
    SET_LOGINDAY(value: number) {
      this.loginDay = Number(value);
    },
    /** 管理员登录 */
    async loginByUsername(data: LoginParams): Promise<LoginResult> {
      const result = await loginApi(data);
      // 登录成功，设置 token 和用户信息
      setToken({
        token: result.token,
        userId: result.userId,
        username: result.username,
        nickname: result.nickname,
        avatar: result.avatar ?? "",
        roles: result.roles,
        permissions: result.permissions
      });
      return result;
    },
    /** 退出登录 */
    async logOut() {
      // 调用退出接口（当前为前端操作，后续可替换为后端接口）
      await logoutApi();
      // 清除所有用户状态
      this.avatar = "";
      this.username = "";
      this.nickname = "";
      this.roles = [];
      this.permissions = [];
      removeToken();
      useMultiTagsStoreHook().handleTags("equal", [...routerArrays]);
      resetRouter();
      router.push("/login");
    }
  }
});

export function useUserStoreHook() {
  return useUserStore(store);
}
