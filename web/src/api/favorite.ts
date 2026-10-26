/** 收藏接口。 */

import type { CreateFavoritePayload, Favorite, FavoriteQuery, PageResult } from '@/types'
import { request } from './http'

export function listFavoritesApi(query: FavoriteQuery = {}): Promise<PageResult<Favorite>> {
  return request<PageResult<Favorite>>({
    url: '/favorites',
    method: 'get',
    params: {
      pageNum: query.pageNum ?? 1,
      pageSize: query.pageSize ?? 10,
      ...(query.tagId ? { tagId: query.tagId } : {}),
      ...(query.keyword ? { keyword: query.keyword } : {}),
    },
  })
}

export function createFavoriteApi(payload: CreateFavoritePayload): Promise<Favorite | null> {
  return request<Favorite | null>({ url: '/favorites', method: 'post', data: payload })
}

export function deleteFavoriteApi(favoriteId: number): Promise<null> {
  return request<null>({ url: `/favorites/${favoriteId}`, method: 'delete' })
}
