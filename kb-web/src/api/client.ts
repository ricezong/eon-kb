import axios, {
  type AxiosInstance,
  type AxiosError,
  type InternalAxiosRequestConfig
} from 'axios';
import type { ApiErrorBody } from './types';

/**
 * 统一的 axios 实例。
 * - baseURL 默认 /api，通过 vite 代理转发到后端 8080；
 * - 生产同源部署时无需改动；跨域部署可用 VITE_API_BASE 覆盖为绝对地址。
 */
const baseURL = (import.meta.env.VITE_API_BASE as string | undefined) || '/api';

export const http: AxiosInstance = axios.create({
  baseURL,
  timeout: 60_000,
  headers: { 'Content-Type': 'application/json' }
});

http.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  // 预留鉴权扩展点：如果后续后端加了 token，可在此从 store 里注入 Authorization 头
  return config;
});

http.interceptors.response.use(
  (resp) => resp,
  (error: AxiosError<ApiErrorBody>) => {
    // 后端所有异常都返回 { error: "..." }，此处统一抛出 Error 便于上层 message.error 显示
    const msg =
      error.response?.data?.error ||
      (error.code === 'ECONNABORTED' ? '请求超时' : null) ||
      error.message ||
      '网络异常';
    return Promise.reject(new Error(msg));
  }
);

/** 便捷方法：把 axios 响应体拆出来，减少样板代码 */
export async function unwrap<T>(promise: Promise<{ data: T }>): Promise<T> {
  const r = await promise;
  return r.data;
}
