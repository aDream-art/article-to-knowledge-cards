/** Provider / 状态 等前端常量与展示文案。 */

import type { DocumentStatus, MessageStatus, Provider } from '@/types'

export interface ModelOption {
  label: string
  value: string
}

export interface ProviderOption {
  label: string
  value: Provider
  hint: string
  models: ModelOption[]
}

/** 会话可选的模型提供方（deepseek / qwen 为真实接入）。 */
export const PROVIDER_OPTIONS: ProviderOption[] = [
  {
    label: 'DeepSeek',
    value: 'deepseek',
    hint: '推理与中文长文表现均衡',
    models: [
      { label: 'deepseek-chat', value: 'deepseek-chat' },
      { label: 'deepseek-reasoner', value: 'deepseek-reasoner' },
    ],
  },
  {
    label: '通义千问',
    value: 'qwen',
    hint: '响应快，适合日常问答',
    models: [
      { label: 'qwen-plus', value: 'qwen-plus' },
      { label: 'qwen-turbo', value: 'qwen-turbo' },
      { label: 'qwen-max', value: 'qwen-max' },
    ],
  },
]

/** provider → 展示名；mock 是无 Key 演示模式。 */
export const PROVIDER_LABELS: Record<string, string> = {
  deepseek: 'DeepSeek',
  qwen: '通义千问',
  mock: '模拟模式',
}

/** 新建会话时各 provider 的默认模型。 */
export const DEFAULT_MODEL: Record<Provider, string> = {
  deepseek: 'deepseek-chat',
  qwen: 'qwen-plus',
  mock: 'mock',
}

export const MESSAGE_STATUS_LABELS: Record<MessageStatus, string> = {
  COMPLETED: '已完成',
  STREAMING: '生成中',
  ABORTED: '已中止',
  FAILED: '生成失败',
}

export interface DocumentStatusMeta {
  label: string
  type: 'success' | 'warning' | 'danger' | 'info'
  hint: string
}

export const DOCUMENT_STATUS_META: Record<DocumentStatus, DocumentStatusMeta> = {
  READY: { label: '可用', type: 'success', hint: '已建立索引，可绑定到会话' },
  PARSING: { label: '解析中', type: 'warning', hint: '正在切分与向量化，请稍候' },
  EMBED_FAILED: { label: '向量化失败', type: 'danger', hint: '可删除后重新上传' },
}

/** 单文件上限 1MB（spec §6.4）。 */
export const MAX_DOCUMENT_BYTES = 1024 * 1024

/** 每个用户最多 10 份文档（spec §6.4）。 */
export const MAX_DOCUMENT_COUNT = 10
