/** 认证接口。 */

import type { AuthUser, LoginPayload, LoginResult, RegisterPayload } from '@/types'
import { request } from './http'

export function loginApi(payload: LoginPayload): Promise<LoginResult> {
  return request<LoginResult>({ url: '/auth/login', method: 'post', data: payload })
}

export function registerApi(payload: RegisterPayload): Promise<AuthUser | null> {
  return request<AuthUser | null>({ url: '/auth/register', method: 'post', data: payload })
}

export function logoutApi(): Promise<null> {
  return request<null>({ url: '/auth/logout', method: 'post' })
}
