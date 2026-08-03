import { http } from "@/utils/http";

type Result = {
  success: boolean;
  data: Array<any>;
};

/**
 * 获取异步路由（当前项目使用纯静态路由，返回空数组即可）
 * 保留此接口以兼容 vue-pure-admin 路由守卫的 initRouter 调用
 */
export const getAsyncRoutes = (): Promise<Result> => {
  return Promise.resolve({ success: true, data: [] });
};
