import Axios, {
  type AxiosInstance,
  type AxiosRequestConfig,
  type CustomParamsSerializer
} from "axios";
import type {
  PureHttpError,
  RequestMethods,
  PureHttpResponse,
  PureHttpRequestConfig
} from "./types.d";
import { stringify } from "qs";
import { ElMessage } from "element-plus";
import { getToken, formatToken, removeToken } from "@/utils/auth";
import { router } from "@/router";

/** 后端统一响应格式 */
interface BackendResponse<T = any> {
  code: number;
  msg: string;
  data: T;
}

// 相关配置请参考：www.axios-js.com/zh-cn/docs/#axios-request-config-1
const defaultConfig: AxiosRequestConfig = {
  // 请求超时时间
  timeout: 10000,
  headers: {
    Accept: "application/json, text/plain, */*",
    "Content-Type": "application/json",
    "X-Requested-With": "XMLHttpRequest"
  },
  // 数组格式参数序列化（https://github.com/axios/axios/issues/5142）
  paramsSerializer: {
    serialize: stringify as unknown as CustomParamsSerializer
  }
};

class PureHttp {
  constructor() {
    this.httpInterceptorsRequest();
    this.httpInterceptorsResponse();
  }

  /** 初始化配置对象 */
  private static initConfig: PureHttpRequestConfig = {};

  /** 保存当前`Axios`实例对象 */
  private static axiosInstance: AxiosInstance = Axios.create(defaultConfig);

  /** 请求拦截：自动附加 Authorization header */
  private httpInterceptorsRequest(): void {
    PureHttp.axiosInstance.interceptors.request.use(
      async (config: PureHttpRequestConfig): Promise<any> => {
        // 优先判断post/get等方法是否传入回调
        if (typeof config.beforeRequestCallback === "function") {
          config.beforeRequestCallback(config);
          return config;
        }
        if (PureHttp.initConfig.beforeRequestCallback) {
          PureHttp.initConfig.beforeRequestCallback(config);
          return config;
        }
        /** 请求白名单，无需 token 的接口 */
        const whiteList = ["/admin-api/auth/login"];
        const isWhite = whiteList.some(url => config.url.endsWith(url));
        if (!isWhite) {
          const data = getToken();
          if (data?.token) {
            config.headers["Authorization"] = formatToken(data.token);
          }
        }
        return config;
      },
      error => {
        return Promise.reject(error);
      }
    );
  }

  /** 响应拦截：解包 {code, msg, data} 格式 + 错误提示 + 401 跳转 */
  private httpInterceptorsResponse(): void {
    const instance = PureHttp.axiosInstance;
    instance.interceptors.response.use(
      (response: PureHttpResponse) => {
        const $config = response.config;
        // 优先判断回调
        if (typeof $config.beforeResponseCallback === "function") {
          $config.beforeResponseCallback(response);
          return response.data;
        }
        if (PureHttp.initConfig.beforeResponseCallback) {
          PureHttp.initConfig.beforeResponseCallback(response);
          return response.data;
        }

        // 解包后端统一响应格式 {code, msg, data}
        const res = response.data as BackendResponse;
        if (res && typeof res.code === "number") {
          if (res.code !== 0) {
            // 业务错误：弹出提示
            ElMessage.error(res.msg || "请求失败");
            return Promise.reject(new Error(res.msg || "请求失败"));
          }
          // 成功：直接返回 data 字段
          return res.data;
        }
        // 非标准格式（如文件下载等）直接返回
        return response.data;
      },
      (error: PureHttpError) => {
        const $error = error;
        $error.isCancelRequest = Axios.isCancel($error);

        // HTTP 状态码处理
        if (error.response) {
          const { status } = error.response;
          if (status === 401) {
            // 未授权：清除 token 并跳转登录页
            removeToken();
            router.push("/login");
            ElMessage.error("登录已过期，请重新登录");
          } else if (status === 403) {
            ElMessage.error("没有权限访问该资源");
          } else if (status === 500) {
            ElMessage.error("服务器内部错误");
          } else {
            ElMessage.error(`请求失败 (${status})`);
          }
        } else if (!error.isCancelRequest) {
          ElMessage.error("网络异常，请检查网络连接");
        }

        return Promise.reject($error);
      }
    );
  }

  /** 通用请求工具函数 */
  public request<T>(
    method: RequestMethods,
    url: string,
    param?: AxiosRequestConfig,
    axiosConfig?: PureHttpRequestConfig
  ): Promise<T> {
    const config = {
      method,
      url,
      ...param,
      ...axiosConfig
    } as PureHttpRequestConfig;

    return new Promise((resolve, reject) => {
      PureHttp.axiosInstance
        .request(config)
        .then((response: undefined) => {
          resolve(response);
        })
        .catch(error => {
          reject(error);
        });
    });
  }

  /** 单独抽离的`post`工具函数 */
  public post<T, P>(
    url: string,
    params?: AxiosRequestConfig<P>,
    config?: PureHttpRequestConfig
  ): Promise<T> {
    return this.request<T>("post", url, params, config);
  }

  /** 单独抽离的`get`工具函数 */
  public get<T, P>(
    url: string,
    params?: AxiosRequestConfig<P>,
    config?: PureHttpRequestConfig
  ): Promise<T> {
    return this.request<T>("get", url, params, config);
  }
}

export const http = new PureHttp();
