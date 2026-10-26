/** 登录态与用户信息。 */

import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { loginApi, logoutApi, registerApi } from '@/api/auth'
import { clearStoredAuth, getStoredUser, getToken, setStoredUser, setToken } from '@/utils/storage'
import type { AuthUser, LoginPayload, RegisterPayload } from '@/types'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(getToken())
  const user = ref<AuthUser | null>(getStoredUser())

  const isAuthenticated = computed(() => Boolean(token.value))
  const displayName = computed(() => user.value?.nickname || user.value?.username || '访客')
  const initial = computed(() => displayName.value.slice(0, 1).toUpperCase())

  async function login(payload: LoginPayload): Promise<AuthUser> {
    const result = await loginApi(payload)
    const profile: AuthUser = {
      userId: result.userId,
      username: result.username,
      nickname: result.nickname || result.username,
    }
    token.value = result.token
    user.value = profile
    setToken(result.token)
    setStoredUser(profile)
    return profile
  }

  async function register(payload: RegisterPayload): Promise<void> {
    await registerApi(payload)
  }

  async function logout(): Promise<void> {
    try {
      await logoutApi()
    } catch {
      // 后端不可用时也允许本地退出
    }
    clear()
  }

  function clear(): void {
    token.value = ''
    user.value = null
    clearStoredAuth()
  }

  return { token, user, isAuthenticated, displayName, initial, login, register, logout, clear }
})
