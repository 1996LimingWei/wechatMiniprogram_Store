import { http } from "@/utils/http";

/** 登录请求参数 */
export type LoginParams = {
  username: string;
  password: string;
};

/** 登录响应数据 */
export type LoginResult = {
  token: string;
  userId: number;
  username: string;
  nickname: string;
  avatar?: string;
  roles: string[];
  permissions: string[];
};

/** 管理员登录 */
export const loginApi = (data: LoginParams): Promise<LoginResult> => {
  return http.request<LoginResult>("post", "/admin-api/auth/login", {
    data
  });
};
