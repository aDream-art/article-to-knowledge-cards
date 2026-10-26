/** 标签：用户自有分类词，只挂在收藏上。 */

import { ref } from 'vue'
import { defineStore } from 'pinia'
import { createTagApi, deleteTagApi, listTagsApi } from '@/api/tag'
import type { Tag } from '@/types'

export const useTagStore = defineStore('tag', () => {
  const tags = ref<Tag[]>([])
  const loading = ref(false)
  const saving = ref(false)
  let loaded = false

  async function loadTags(force = false): Promise<void> {
    if (loading.value) return
    if (loaded && !force) return
    loading.value = true
    try {
      const data = await listTagsApi()
      tags.value = Array.isArray(data) ? data : []
      loaded = true
    } catch {
      // 拦截器已提示
    } finally {
      loading.value = false
    }
  }

  /** 按名称创建；已存在则直接复用，返回可用于收藏的标签。 */
  async function createTag(name: string): Promise<Tag | null> {
    const trimmed = name.trim()
    if (!trimmed) return null
    const existing = tags.value.find((tag) => tag.name === trimmed)
    if (existing) return existing

    saving.value = true
    try {
      const created = await createTagApi(trimmed)
      const fallback = created ?? { id: -1, name: trimmed }
      await loadTags(true)
      return tags.value.find((tag) => tag.name === trimmed) ?? fallback
    } finally {
      saving.value = false
    }
  }

  async function removeTag(tagId: number): Promise<void> {
    await deleteTagApi(tagId)
    tags.value = tags.value.filter((tag) => tag.id !== tagId)
  }

  function reset(): void {
    tags.value = []
    loaded = false
  }

  return { tags, loading, saving, loadTags, createTag, removeTag, reset }
})
