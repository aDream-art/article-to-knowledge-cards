/**
 * axios 实例与统一错误处理。
 *
 * - 请求：自动附加 `Authorization: Bearer <token>`
 * - 响应：校验 `{ code, message, data }`，code !== 200 走业务错误
 * - 401：清空登录态并跳转登录页（回调在 main.ts 中注册，避免循环依赖）
 */

import axios from 'axios'
import type { AxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import { getToken } from '@/utils/storage'
import { rateLimitHint } from '@/utils/format'

export class ApiError extends Error {
  readonly code: number

  constructor(code: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.code = code
  }
}

const CODE_MESSAGES: Record<number, string> = {
  40001: '请求参数有误',
  40101: '登录已过期，请重新登录',
  40301: '没有权限访问该资源',
  40401: '资源不存在或已被删除',
  42901: '请求过于频繁，请稍后再试',
  42902: '今日 Token 配额已用尽，请明天再来',
  50001: '上游模型服务异常，请稍后重试',
}

/** 业务错误码 → 可读文案；后端 message 优先。 */
export function messageForCode(code: number, fallback?: string): string {
  const text = (fallback ?? '').trim()
  if (text) return text
  if (code === 42901) return `请求过于频繁，${rateLimitHint()}`
  return CODE_MESSAGES[code] ?? `请求失败（${code}）`
}

/** HTTP 状态码兜底映射到项目错误码。 */
export function codeFromHttpStatus(status: number): number {
  if (status === 401) return 40101
  if (status === 403) return 40301
  if (status === 404) return 40401
  if (status === 429) return 42901
  if (status >= 500) return 50001
  return 40001
}

let unauthorizedHandler: (() => void) | null = null

/** 注册 401 处理逻辑（main.ts 在 pinia/router 就绪后调用）。 */
export function onUnauthorized(handler: () => void): void {
  unauthorizedHandler = handler
}

/** 供 SSE 等非 axios 通道复用。 */
export function triggerUnauthorized(): void {
  unauthorizedHandler?.()
}

let lastToast = { message: '', at: 0 }

/** 错误提示去重：并发请求同时失败时只弹一次。 */
export function notifyError(message: string): void {
  const now = Date.now()
  if (lastToast.message === message && now - lastToast.at < 1500) return
  lastToast = { message, at: now }
  ElMessage.error(message)
}

const http = axios.create({
  baseURL: '/api',
  timeout: 20_000,
})

http.interceptors.request.use((config) => {
  const token = getToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use(
  (response: AxiosResponse) => {
    const body = response.data as { code?: unknown; message?: unknown; data?: unknown } | undefined
    // 非包裹结构（如文件流）直接放行
    if (!body || typeof body.code !== 'number') return response

    const code = body.code
    if (code === 200) return response

    const message = messageForCode(code, typeof body.message === 'string' ? body.message : undefined)
    if (code === 40101) triggerUnauthorized()
    notifyError(message)
    return Promise.reject(new ApiError(code, message))
  },
  (error: unknown) => {
    if (!axios.isAxiosError(error)) {
      const message = error instanceof Error ? error.message : '未知错误'
      notifyError(message)
      return Promise.reject(new ApiError(50001, message))
    }

    if (error.code === 'ECONNABORTED' || error.code === 'ETIMEDOUT') {
      const message = '请求超时，请检查后端服务'
      notifyError(message)
      return Promise.reject(new ApiError(50001, message))
    }

    const response = error.response
    if (!response) {
      const message = '网络异常，请确认后端已在 http://localhost:8080 启动'
      notifyError(message)
      return Promise.reject(new ApiError(50001, message))
    }

    const body = response.data as { code?: unknown; message?: unknown } | undefined
    const hasEnvelope = typeof body?.code === 'number'

    // 网关/代理层错误（后端没起、端口不通）不会有业务信封，不要伪装成"上游模型异常"
    if (response.status >= 500 && !hasEnvelope) {
      const message = '后端服务不可达（localhost:8080）：请确认后端已启动，且 MySQL / Redis 正常'
      notifyError(message)
      return Promise.reject(new ApiError(50001, message))
    }

    const businessCode = hasEnvelope ? (body as { code: number }).code : codeFromHttpStatus(response.status)
    const message = messageForCode(businessCode, typeof body?.message === 'string' ? body.message : undefined)

    if (response.status === 401 || businessCode === 40101) {
      triggerUnauthorized()
      notifyError('登录已过期，请重新登录')
      return Promise.reject(new ApiError(40101, '登录已过期，请重新登录'))
    }

    notifyError(message)
    return Promise.reject(new ApiError(businessCode, message))
  },
)

/** 发起请求并解包 `data`。 */
export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await http.request<{ code: number; message: string; data: T }>(config)
  return response.data.data
}

export default http
