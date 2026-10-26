/** 文档接口。 */

import type { ChatDocument, PageResult } from '@/types'
import { request } from './http'

/** 流式端点：fetch 不走 axios，需要带 /api 前缀。 */
export const CHAT_STREAM_URL = '/api/chat/stream'

/**
 * 文档列表。
 * 契约是 `data: ChatDocument[]`，这里同时兼容后端包一层分页结构的情况。
 */
export async function listDocumentsApi(): Promise<ChatDocument[]> {
  const data = await request<ChatDocument[] | PageResult<ChatDocument>>({
    url: '/documents',
    method: 'get',
  })
  if (Array.isArray(data)) return data
  return Array.isArray(data?.list) ? data.list : []
}

export function uploadDocumentApi(file: File): Promise<ChatDocument | null> {
  const form = new FormData()
  form.append('file', file)
  return request<ChatDocument | null>({
    url: '/documents',
    method: 'post',
    data: form,
    timeout: 60_000,
  })
}

export function deleteDocumentApi(documentId: number): Promise<null> {
  return request<null>({ url: `/documents/${documentId}`, method: 'delete' })
}
