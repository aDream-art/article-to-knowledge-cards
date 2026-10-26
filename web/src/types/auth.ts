/** 认证相关类型。 */

export interface LoginPayload {
  username: string
  password: string
}

export interface RegisterPayload {
  username: string
  password: string
  nickname: string
}

/** POST /api/auth/login 的 data。 */
export interface LoginResult {
  token: string
  userId: number
  username: string
  nickname: string
}

/** 本地持久化的用户信息。 */
export interface AuthUser {
  userId: number
  username: string
  nickname: string
}
