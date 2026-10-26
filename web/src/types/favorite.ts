/** 收藏与标签类型（spec §4 chat_favorite / chat_tag）。 */

export interface Tag {
  id: number
  name: string
  count?: number
}

/** 收藏是问答快照，脱离原会话依然可读。 */
export interface Favorite {
  id: number
  sessionId: number | null
  question: string
  answer: string
  provider: string | null
  model: string | null
  createdAt: string
  tags?: Tag[]
}

/** 创建收藏的请求体。 */
export interface CreateFavoritePayload {
  sessionId: number | null
  question: string
  answer: string
  provider: string | null
  model: string | null
  tagIds: number[]
}

export interface FavoriteQuery {
  pageNum?: number
  pageSize?: number
  tagId?: number
  keyword?: string
}
