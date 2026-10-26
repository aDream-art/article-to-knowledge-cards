/** 收藏：问答快照，脱离原会话依然可检索。 */

import { ref } from 'vue'
import { defineStore } from 'pinia'
import { createFavoriteApi, deleteFavoriteApi, listFavoritesApi } from '@/api/favorite'
import type { CreateFavoritePayload, Favorite } from '@/types'

export const useFavoriteStore = defineStore('favorite', () => {
  const favorites = ref<Favorite[]>([])
  const loading = ref(false)
  const saving = ref(false)
  const total = ref(0)
  const totalPage = ref(0)
  const pageNum = ref(1)
  const pageSize = ref(10)
  const keyword = ref('')
  const activeTagId = ref<number | undefined>(undefined)

  async function fetchList(page = pageNum.value): Promise<void> {
    pageNum.value = Math.max(1, page)
    loading.value = true
    try {
      const data = await listFavoritesApi({
        pageNum: pageNum.value,
        pageSize: pageSize.value,
        keyword: keyword.value.trim() || undefined,
        tagId: activeTagId.value,
      })
      const list = Array.isArray(data?.list) ? data.list : []
      favorites.value = list.map((item) => ({ ...item, tags: item.tags ?? [] }))
      total.value = typeof data?.total === 'number' ? data.total : favorites.value.length
      totalPage.value = typeof data?.totalPage === 'number' ? data.totalPage : 1
    } catch {
      // 拦截器已提示
    } finally {
      loading.value = false
    }
  }

  function applyKeyword(value: string): void {
    keyword.value = value
    void fetchList(1)
  }

  function applyTag(tagId?: number): void {
    activeTagId.value = tagId
    void fetchList(1)
  }

  function changePage(page: number): void {
    void fetchList(page)
  }

  async function createFavorite(payload: CreateFavoritePayload): Promise<void> {
    saving.value = true
    try {
      await createFavoriteApi(payload)
      // 新收藏排在最前，回到第一页展示
      keyword.value = ''
      activeTagId.value = undefined
      await fetchList(1)
    } finally {
      saving.value = false
    }
  }

  async function removeFavorite(favoriteId: number): Promise<void> {
    await deleteFavoriteApi(favoriteId)
    favorites.value = favorites.value.filter((item) => item.id !== favoriteId)
    total.value = Math.max(0, total.value - 1)
    if (favorites.value.length === 0 && pageNum.value > 1) {
      await fetchList(pageNum.value - 1)
    } else if (favorites.value.length === 0) {
      await fetchList(1)
    }
  }

  function reset(): void {
    favorites.value = []
    total.value = 0
    totalPage.value = 0
    pageNum.value = 1
    keyword.value = ''
    activeTagId.value = undefined
  }

  return {
    favorites,
    loading,
    saving,
    total,
    totalPage,
    pageNum,
    pageSize,
    keyword,
    activeTagId,
    fetchList,
    applyKeyword,
    applyTag,
    changePage,
    createFavorite,
    removeFavorite,
    reset,
  }
})
