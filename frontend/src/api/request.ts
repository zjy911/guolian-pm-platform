import axios, { AxiosError, type AxiosInstance, type AxiosRequestConfig } from 'axios'
import type { ApiResponse } from './types'
import { DEMO, demoRequest } from './demo'

/**
 * 统一请求封装：
 * - Base URL 走环境变量，绝不硬编码
 * - 令牌只存在内存 + sessionStorage（禁止 localStorage，禁止放进 URL）
 * - 4xx 不重试，5xx / 网络异常最多重试 3 次
 * - 后端错误码映射为可读文案
 */
const BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1'
const TOKEN_KEY = 'pm_token'
const MAX_RETRY = 3

let memoryToken: string | null = sessionStorage.getItem(TOKEN_KEY)

export const tokenStore = {
  get(): string | null {
    return memoryToken
  },
  set(token: string) {
    memoryToken = token
    sessionStorage.setItem(TOKEN_KEY, token)
  },
  clear() {
    memoryToken = null
    sessionStorage.removeItem(TOKEN_KEY)
  }
}

export class ApiError extends Error {
  code: number
  constructor(code: number, message: string) {
    super(message)
    this.code = code
  }
}

let unauthorizedHandler: (() => void) | null = null
export function onUnauthorized(handler: () => void) {
  unauthorizedHandler = handler
}

const http: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' }
})

http.interceptors.request.use((config) => {
  const token = tokenStore.get()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

function isRetryable(status: number) {
  return status >= 500
}

async function sleep(ms: number) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

http.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResponse<unknown>
    if (body && typeof body.code === 'number' && body.code !== 0) {
      throw new ApiError(body.code, body.msg || '请求失败')
    }
    return response
  },
  async (error: AxiosError<ApiResponse<unknown>>) => {
    const status = error.response?.status
    const config = error.config as (AxiosRequestConfig & { __retry?: number }) | undefined

    if (status === 401) {
      tokenStore.clear()
      unauthorizedHandler?.()
      throw new ApiError(401, error.response?.data?.msg || '登录已失效，请重新登录')
    }
    if (status === 403) {
      throw new ApiError(403, error.response?.data?.msg || '没有权限执行该操作')
    }
    if (status && status >= 400 && status < 500) {
      throw new ApiError(status, error.response?.data?.msg || '请求参数不合法')
    }

    // 5xx 与网络异常：仅对幂等请求重试，最多 3 次
    const method = (config?.method || 'get').toUpperCase()
    const retryCount = config?.__retry || 0
    const idempotent = method === 'GET'
    if (config && idempotent && (isRetryable(status || 0) || !error.response) && retryCount < MAX_RETRY) {
      config.__retry = retryCount + 1
      await sleep(300 * (retryCount + 1))
      return http.request(config)
    }
    if (!error.response) {
      throw new ApiError(-1, '网络连接失败，请检查网络后重试')
    }
    throw new ApiError(status || 500, error.response?.data?.msg || '服务器开小差了，请稍后重试')
  }
)

export async function get<T>(url: string, params?: Record<string, unknown>): Promise<T> {
  if (DEMO) return demoRequest<T>('GET', url, params)
  const res = await http.get<ApiResponse<T>>(url, { params })
  return res.data.data
}

export async function post<T>(url: string, data?: unknown): Promise<T> {
  if (DEMO) return demoRequest<T>('POST', url, undefined, data)
  const res = await http.post<ApiResponse<T>>(url, data ?? {})
  return res.data.data
}

export async function put<T>(url: string, data?: unknown): Promise<T> {
  if (DEMO) return demoRequest<T>('PUT', url, undefined, data)
  const res = await http.put<ApiResponse<T>>(url, data ?? {})
  return res.data.data
}

export async function del<T>(url: string): Promise<T> {
  if (DEMO) return demoRequest<T>('DELETE', url)
  const res = await http.delete<ApiResponse<T>>(url)
  return res.data.data
}

/** 文件下载（导出 / 附件） */
export async function download(url: string, filename: string, params?: Record<string, unknown>) {
  if (DEMO) {
    // 演示模式没有真实文件，弹出提示即可
    window.alert(`演示模式：${filename} 下载不可用`)
    return
  }
  const res = await http.get(url, { params, responseType: 'blob' })
  const blob = new Blob([res.data as BlobPart])
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = filename
  link.click()
  URL.revokeObjectURL(link.href)
}

export default http
