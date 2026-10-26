/** 标签接口。 */

import type { Tag } from '@/types'
import { request } from './http'

/** data 是纯数组，不是分页结构。 */
export function listTagsApi(): Promise<Tag[]> {
  return request<Tag[]>({ url: '/tags', method: 'get' })
}

export function createTagApi(name: string): Promise<Tag | null> {
  return request<Tag | null>({ url: '/tags', method: 'post', data: { name } })
}

export function deleteTagApi(tagId: number): Promise<null> {
  return request<null>({ url: `/tags/${tagId}`, method: 'delete' })
}
