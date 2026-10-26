/** 会话与消息类型（spec §4 / §5）。 */

/** 模型提供方：deepseek 与 qwen 为真实接入，mock 为无 Key 演示模式。 */
export type Provider = 'deepseek' | 'qwen' | 'mock'

/** 消息角色（后端可能返回大写或小写，读取时统一归一化为大写）。 */
export type MessageRole = 'USER' | 'ASSISTANT'

/**
 * 消息状态：后端仅落库 COMPLETED / ABORTED / FAILED，
 * STREAMING 是前端流式进行中的临时状态。
 */
export type MessageStatus = 'COMPLETED' | 'ABORTED' | 'FAILED' | 'STREAMING'

/** 会话（GET /api/sessions 的 list 项）。 */
export interface ChatSession {
  id: number
  title: string
  provider: Provider
  model: string
  documentId: number | null
  lastMessageAt: string | null
}

/** 服务端返回的消息结构。 */
export interface MessageDto {
  id: number
  role: string
  content: string
  status: string
  provider: Provider | null
  model: string | null
  createdAt: string
}

/** 前端渲染用的消息（含流式过程中的临时字段）。 */
export interface ChatMessage {
  /** 服务端 id；流式 meta 到达前或失败时为负数占位。 */
  id: number
  /** 稳定的本地 key，避免 id 变化导致列表重渲染。 */
  key: string
  role: MessageRole
  content: string
  status: MessageStatus
  provider: Provider | null
  model: string | null
  createdAt: string
  /** 是否正在流式输出（仅前端使用）。 */
  streaming?: boolean
  tokenIn?: number
  tokenOut?: number
  firstTokenMs?: number
  /** 是否发生了跨 provider 降级（meta.fallback）。 */
  fallback?: boolean
  /** 流式失败时的可读错误信息。 */
  errorMessage?: string
}

/** 新建会话请求体。 */
export interface CreateSessionPayload {
  title?: string
  provider?: Provider
  model?: string
}

/* ------------------------------ SSE 事件载荷 ------------------------------ */

/** event: meta */
export interface StreamMetaEvent {
  messageId: number
  provider: Provider
  model: string
  fallback: boolean
}

/** event: delta */
export interface StreamDeltaEvent {
  text: string
}

/** event: usage */
export interface StreamUsageEvent {
  tokenIn: number
  tokenOut: number
  firstTokenMs: number
}

/** event: error */
export interface StreamErrorEvent {
  code: number
  message: string
}
