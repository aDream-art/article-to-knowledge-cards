/** localStorage 持久化（token 与用户信息）。 */

import type { AuthUser } from '@/types'

const TOKEN_KEY = 'qa_token'
const USER_KEY = 'qa_user'

function safeGet(key: string): string | null {
  try {
    return window.localStorage.getItem(key)
  } catch {
    return null
  }
}

function safeSet(key: string, value: string): void {
  try {
    window.localStorage.setItem(key, value)
  } catch {
    /* 隐私模式等场景写入失败时忽略 */
  }
}

function safeRemove(key: string): void {
  try {
    window.localStorage.removeItem(key)
  } catch {
    /* ignore */
  }
}

export function getToken(): string {
  return safeGet(TOKEN_KEY) ?? ''
}

export function setToken(token: string): void {
  safeSet(TOKEN_KEY, token)
}

export function getStoredUser(): AuthUser | null {
  const raw = safeGet(USER_KEY)
  if (!raw) return null
  try {
    const parsed = JSON.parse(raw) as Partial<AuthUser>
    if (typeof parsed.userId === 'number' && typeof parsed.username === 'string') {
      return {
        userId: parsed.userId,
        username: parsed.username,
        nickname: typeof parsed.nickname === 'string' ? parsed.nickname : parsed.username,
      }
    }
  } catch {
    /* 损坏的数据直接丢弃 */
  }
  return null
}

export function setStoredUser(user: AuthUser | null): void {
  if (user) safeSet(USER_KEY, JSON.stringify(user))
  else safeRemove(USER_KEY)
}

export function clearStoredAuth(): void {
  safeRemove(TOKEN_KEY)
  safeRemove(USER_KEY)
}
