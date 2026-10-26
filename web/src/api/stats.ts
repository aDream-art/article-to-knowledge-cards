/** 管理统计接口。 */

import type { AdminStats } from '@/types'
import { request } from './http'

export function fetchStatsApi(): Promise<AdminStats> {
  return request<AdminStats>({ url: '/admin/stats', method: 'get' })
}
