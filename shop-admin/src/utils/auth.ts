import Cookies from "js-cookie";
import { useUserStoreHook } from "@/store/modules/user";
import { storageLocal } from "@pureadmin/utils";

export interface DataInfo<T> {
  /** 后端签发的 JWT token */
  token: string;
  /** 管理员用户ID */
  userId: T;
  /** 用户名 */
  username?: string;
  /** 昵称 */
  nickname?: string;
  /** 头像 */
  avatar?: string;
  /** 角色列表 */
  roles?: Array<string>;
  /** 按钮级别权限 */
  permissions?: Array<string>;
}

export const userKey = "user-info";
export const TokenKey = "authorized-token";
/**
 * 通过`multiple-tabs`是否在`cookie`中，判断用户是否已经登录系统，
 * 浏览器完全关闭后`multiple-tabs`将自动从`cookie`中销毁，
 * 再次打开浏览器需要重新登录系统
 */
export const multipleTabsKey = "multiple-tabs";

/** 获取`token` */
export function getToken(): DataInfo<number> {
  return Cookies.get(TokenKey)
    ? JSON.parse(Cookies.get(TokenKey))
    : storageLocal().getItem(userKey);
}

/** 设置`token`及用户信息 */
export function setToken(data: DataInfo<number>) {
  const { token, userId } = data;
  const { isRemembered, loginDay } = useUserStoreHook();
  const cookieString = JSON.stringify({ token, userId });

  // Cookie 默认 7 天过期
  Cookies.set(TokenKey, cookieString, {
    expires: isRemembered ? loginDay : 7
  });

  Cookies.set(
    multipleTabsKey,
    "true",
    isRemembered ? { expires: loginDay } : {}
  );

  function setUserKey({ avatar, username, nickname, roles, permissions }) {
    useUserStoreHook().SET_AVATAR(avatar);
    useUserStoreHook().SET_USERNAME(username);
    useUserStoreHook().SET_NICKNAME(nickname);
    useUserStoreHook().SET_ROLES(roles);
    useUserStoreHook().SET_PERMS(permissions);
    storageLocal().setItem(userKey, {
      token,
      userId,
      avatar,
      username,
      nickname,
      roles,
      permissions
    });
  }

  if (data.username && data.roles) {
    setUserKey({
      avatar: data?.avatar ?? "",
      username: data.username,
      nickname: data?.nickname ?? "",
      roles: data.roles,
      permissions: data?.permissions ?? []
    });
  } else {
    const stored = storageLocal().getItem<DataInfo<number>>(userKey);
    setUserKey({
      avatar: stored?.avatar ?? "",
      username: stored?.username ?? "",
      nickname: stored?.nickname ?? "",
      roles: stored?.roles ?? [],
      permissions: stored?.permissions ?? []
    });
  }
}

/** 删除`token`以及用户信息 */
export function removeToken() {
  Cookies.remove(TokenKey);
  Cookies.remove(multipleTabsKey);
  storageLocal().removeItem(userKey);
}

/** 格式化 token（Bearer 格式） */
export const formatToken = (token: string): string => {
  return "Bearer " + token;
};

/** 是否有按钮级别的权限 */
export const hasPerms = (value: string | Array<string>): boolean => {
  if (!value) return false;
  const allPerms = "*:*:*";
  const { permissions } = useUserStoreHook();
  if (!permissions) return false;
  if (permissions.length === 1 && permissions[0] === allPerms) return true;
  const isString = typeof value === "string";
  return isString
    ? permissions.includes(value as string)
    : (value as string[]).every(p => permissions.includes(p));
};
