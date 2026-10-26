/** 展示层格式化工具。 */

const DAY = 24 * 60 * 60 * 1000

/** 把后端时间（`2026-10-25 20:23:14` 或 ISO 串）解析为 Date。 */
export function parseTime(value?: string | null): Date | null {
  if (!value) return null
  const normalized = value.includes('T') ? value : value.replace(' ', 'T')
  const date = new Date(normalized)
  return Number.isNaN(date.getTime()) ? null : date
}

function pad(value: number): string {
  return value < 10 ? `0${value}` : String(value)
}

/** `YYYY-MM-DD HH:mm`。 */
export function formatDateTime(value?: string | null): string {
  const date = parseTime(value)
  if (!date) return '—'
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

/** `HH:mm`。 */
export function formatTime(value?: string | null): string {
  const date = parseTime(value)
  if (!date) return '—'
  return `${pad(date.getHours())}:${pad(date.getMinutes())}`
}

/** 会话列表用的相对时间：刚刚 / x 分钟前 / x 小时前 / 昨天 / MM-DD。 */
export function formatRelativeTime(value?: string | null): string {
  const date = parseTime(value)
  if (!date) return '暂无消息'
  const diff = Date.now() - date.getTime()
  if (diff < 60 * 1000) return '刚刚'
  if (diff < 60 * 60 * 1000) return `${Math.floor(diff / 60000)} 分钟前`
  if (diff < DAY) return `${Math.floor(diff / 3600000)} 小时前`
  if (diff < 2 * DAY) return '昨天'
  if (diff < 365 * DAY) return `${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

/** 图表横轴日期：`2026-10-25` → `10-25`。 */
export function formatDayLabel(date: string): string {
  const parts = date.split('-')
  return parts.length === 3 ? `${Number(parts[1])}-${Number(parts[2])}` : date
}

/** 字节数 → 1.2 KB / 860 B。 */
export function formatBytes(bytes: number): string {
  if (!Number.isFinite(bytes) || bytes <= 0) return '0 B'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(2)} MB`
}

const numberFormatter = new Intl.NumberFormat('zh-CN')

/** 千分位整数。 */
export function formatNumber(value: number): string {
  if (!Number.isFinite(value)) return '0'
  return numberFormatter.format(Math.round(value))
}

/** 坐标轴/球标用的紧凑数字：1.2 万 / 3.4k。 */
export function formatCompact(value: number): string {
  if (!Number.isFinite(value)) return '0'
  if (value >= 100_000_000) return `${(value / 100_000_000).toFixed(1)} 亿`
  if (value >= 10_000) return `${(value / 10_000).toFixed(1)} 万`
  if (value >= 1_000) return `${(value / 1_000).toFixed(1)}k`
  return String(Math.round(value))
}

/** 429 限流的可读提示：滑动窗口是 1 分钟，拿不到精确剩余时间，给保守估计。 */
export function rateLimitHint(): string {
  return '本分钟请求额度已用尽，最迟 1 分钟后恢复'
}
