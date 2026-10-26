/** 会话与消息接口。 */

import type { ChatSession, CreateSessionPayload, MessageDto, PageQuery, PageResult } from '@/types'
import { request } from './http'

export function listSessionsApi(query: PageQuery = {}): Promise<PageResult<ChatSession>> {
  return request<PageResult<ChatSession>>({
    url: '/sessions',
    method: 'get',
    params: { pageNum: query.pageNum ?? 1, pageSize: query.pageSize ?? 20 },
  })
}

export function createSessionApi(payload: CreateSessionPayload): Promise<ChatSession | null> {
  return request<ChatSession | null>({ url: '/sessions', method: 'post', data: payload })
}

export function renameSessionApi(sessionId: number, title: string): Promise<null> {
  return request<null>({ url: `/sessions/${sessionId}`, method: 'patch', data: { title } })
}

export function deleteSessionApi(sessionId: number): Promise<null> {
  return request<null>({ url: `/sessions/${sessionId}`, method: 'delete' })
}

/** 历史消息：后端按倒序返回（最新在前），展示前需反转。 */
export function listMessagesApi(sessionId: number, query: PageQuery = {}): Promise<PageResult<MessageDto>> {
  return request<PageResult<MessageDto>>({
    url: `/sessions/${sessionId}/messages`,
    method: 'get',
    params: { pageNum: query.pageNum ?? 1, pageSize: query.pageSize ?? 30 },
  })
}

/** 绑定 / 解绑文档（documentId 传 null 表示解绑）。 */
export function bindSessionDocumentApi(sessionId: number, documentId: number | null): Promise<null> {
  return request<null>({
    url: `/sessions/${sessionId}/document`,
    method: 'post',
    data: { documentId },
  })
}
