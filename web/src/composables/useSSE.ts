/**
 * `useSSE`：基于 fetch + ReadableStream 的 SSE 客户端。
 *
 * 之所以不用 EventSource：流式端点需要 POST 且必须带 `Authorization` 头。
 *
 * 解析规则（spec §6.1）：
 * - 按空行（`\n\n` / `\r\n\r\n`）切帧，残缺的尾巴留在 buffer 等下一次 chunk
 * - `data:` 可以跨多行，按规范用 `\n` 拼接后再 JSON.parse
 * - 以 `:` 开头的是心跳注释行，直接忽略
 * - `AbortController` 用于“停止生成”，中断后通过 `aborted` 通知调用方
 */

import { ref } from 'vue'
import type { Ref } from 'vue'
import { messageForCode, codeFromHttpStatus, triggerUnauthorized } from '@/api/http'
import { getToken } from '@/utils/storage'
import type { StreamDeltaEvent, StreamErrorEvent, StreamMetaEvent, StreamUsageEvent } from '@/types'

export interface SSECallbacks {
  onMeta?: (payload: StreamMetaEvent) => void
  onDelta?: (payload: StreamDeltaEvent) => void
  onUsage?: (payload: StreamUsageEvent) => void
  onDone?: () => void
  onError?: (payload: StreamErrorEvent) => void
}

export interface ParsedSSEFrame {
  event: string
  data: string
}

export interface UseSSEReturn {
  /** 是否有进行中的流。 */
  streaming: Ref<boolean>
  /** 本次流是否被主动中断（停止生成）。 */
  aborted: Ref<boolean>
  start: (url: string, body: unknown, callbacks: SSECallbacks) => Promise<void>
  abort: () => void
}

const FRAME_SEPARATOR = /\r?\n\r?\n/

/** 从缓冲区取出一个完整帧；没有完整帧时返回 null。 */
export function takeFrame(buffer: string): { frame: string; rest: string } | null {
  const match = FRAME_SEPARATOR.exec(buffer)
  if (!match || match.index === undefined) return null
  return {
    frame: buffer.slice(0, match.index),
    rest: buffer.slice(match.index + match[0].length),
  }
}

/**
 * 解析单个 SSE 帧。
 * 纯注释帧（如心跳 `: ping`）返回 null；`event: done` 这类无 data 帧仍然保留。
 */
export function parseFrame(rawFrame: string): ParsedSSEFrame | null {
  let event = 'message'
  const dataLines: string[] = []
  let hasField = false

  for (const line of rawFrame.split(/\r?\n/)) {
    if (line === '' || line.startsWith(':')) continue
    const colonIndex = line.indexOf(':')
    const field = colonIndex === -1 ? line : line.slice(0, colonIndex)
    let value = colonIndex === -1 ? '' : line.slice(colonIndex + 1)
    if (value.startsWith(' ')) value = value.slice(1)

    if (field === 'event') {
      event = value.trim()
      hasField = true
    } else if (field === 'data') {
      dataLines.push(value)
      hasField = true
    } else if (field === 'id' || field === 'retry') {
      hasField = true
    }
  }

  if (!hasField) return null
  return { event, data: dataLines.join('\n') }
}

function parseJson<T>(text: string): T | null {
  if (!text.trim()) return null
  try {
    return JSON.parse(text) as T
  } catch {
    return null
  }
}

function isAbortError(error: unknown): boolean {
  return error instanceof Error && error.name === 'AbortError'
}

function readErrorPayload(payload: unknown): { code?: number; message?: string } | null {
  if (!payload || typeof payload !== 'object') return null
  const record = payload as { code?: unknown; message?: unknown }
  return {
    code: typeof record.code === 'number' ? record.code : undefined,
    message: typeof record.message === 'string' ? record.message : undefined,
  }
}

export function useSSE(): UseSSEReturn {
  const streaming = ref(false)
  const aborted = ref(false)
  let controller: AbortController | null = null

  /** 停止生成：中断 fetch，后端会把已生成内容按 ABORTED 落库。 */
  function abort(): void {
    if (!controller) return
    aborted.value = true
    controller.abort()
  }

  async function start(url: string, body: unknown, callbacks: SSECallbacks): Promise<void> {
    if (streaming.value) return

    streaming.value = true
    aborted.value = false

    const current = new AbortController()
    controller = current

    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      // 带上 application/json：限流/配额等"还没开流"的失败，后端返回的是 JSON 错误体
      Accept: 'text/event-stream, application/json',
    }
    const token = getToken()
    if (token) headers.Authorization = `Bearer ${token}`

    let doneCalled = false
    let errorCalled = false

    const dispatch = (rawFrame: string): void => {
      const frame = parseFrame(rawFrame)
      if (!frame) return

      switch (frame.event) {
        case 'meta': {
          const data = parseJson<StreamMetaEvent>(frame.data)
          if (data) callbacks.onMeta?.(data)
          break
        }
        case 'delta': {
          const data = parseJson<{ text?: unknown }>(frame.data)
          const text = data && typeof data.text === 'string' ? data.text : frame.data
          if (text) callbacks.onDelta?.({ text })
          break
        }
        case 'usage': {
          const data = parseJson<StreamUsageEvent>(frame.data)
          if (data) callbacks.onUsage?.(data)
          break
        }
        case 'done': {
          doneCalled = true
          callbacks.onDone?.()
          break
        }
        case 'error': {
          errorCalled = true
          const data = parseJson<StreamErrorEvent>(frame.data)
          const code = typeof data?.code === 'number' ? data.code : 50001
          callbacks.onError?.({
            code,
            message: data?.message?.trim() || messageForCode(code),
          })
          break
        }
        default:
          // 未知事件：忽略，保持协议向后兼容
          break
      }
    }

    try {
      const response = await fetch(url, {
        method: 'POST',
        headers,
        body: JSON.stringify(body),
        signal: current.signal,
      })

      const contentType = response.headers.get('content-type') ?? ''
      const isEventStream = contentType.includes('text/event-stream')

      // 限流 / 配额 / 未登录等场景后端直接返回 JSON（spec §6.3）
      if (!response.ok || !isEventStream) {
        let payload: { code?: number; message?: string } | null = null
        try {
          const text = await response.text()
          payload = readErrorPayload(text ? JSON.parse(text) : null)
        } catch {
          payload = null
        }

        if (response.status === 401 || payload?.code === 40101) {
          triggerUnauthorized()
          callbacks.onError?.({ code: 40101, message: '登录已过期，请重新登录' })
          return
        }

        const code = payload?.code ?? codeFromHttpStatus(response.status)
        callbacks.onError?.({ code, message: messageForCode(code, payload?.message) })
        return
      }

      const reader = response.body?.getReader()
      if (!reader) {
        callbacks.onError?.({ code: 50001, message: '当前浏览器不支持流式响应' })
        return
      }

      const decoder = new TextDecoder('utf-8')
      let buffer = ''

      while (!doneCalled && !errorCalled) {
        const { value, done } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })

        let extracted = takeFrame(buffer)
        while (extracted) {
          buffer = extracted.rest
          dispatch(extracted.frame)
          if (doneCalled || errorCalled) break
          extracted = takeFrame(buffer)
        }
      }

      if (doneCalled || errorCalled) {
        try {
          await reader.cancel()
        } catch {
          /* 流可能已被服务端关闭 */
        }
      }

      // flush 尾部：服务端最后一帧可能没有以空行结尾
      buffer += decoder.decode()
      if (!doneCalled && !errorCalled && buffer.trim()) dispatch(buffer)

      // 流正常关闭但没有显式 done 事件时，按完成处理
      if (!doneCalled && !errorCalled && !aborted.value) callbacks.onDone?.()
    } catch (error) {
      if (aborted.value || isAbortError(error)) return
      callbacks.onError?.({ code: 50001, message: '网络连接中断，请稍后重试' })
    } finally {
      if (controller === current) controller = null
      streaming.value = false
    }
  }

  return { streaming, aborted, start, abort }
}
