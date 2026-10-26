/** 统计页类型（GET /api/admin/stats）。 */

export interface TopUserStat {
  userId: number
  username: string
  calls: number
  tokens: number
}

export interface DailyStat {
  date: string
  calls: number
  tokens: number
}

export interface AdminStats {
  totalCalls: number
  totalTokens: number
  topUsers: TopUserStat[]
  daily: DailyStat[]
}
