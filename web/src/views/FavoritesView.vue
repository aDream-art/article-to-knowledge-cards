<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import BaseIcon from '@/components/BaseIcon.vue'
import { useFavoriteStore } from '@/stores/favorite'
import { useTagStore } from '@/stores/tag'
import { PROVIDER_LABELS } from '@/utils/constants'
import { formatDateTime } from '@/utils/format'
import { renderMarkdown } from '@/utils/markdown'
import type { Favorite, Tag } from '@/types'

const favorite = useFavoriteStore()
const tagStore = useTagStore()

const searchInput = ref('')
const tagDialog = ref(false)
const newTagName = ref('')
const tagSaving = ref(false)
const expanded = ref<Set<number>>(new Set())

const emptyDescription = computed(() => {
  if (favorite.keyword.trim()) return `没有匹配「${favorite.keyword.trim()}」的收藏`
  if (favorite.activeTagId) return '这个标签下还没有收藏'
  return '还没有收藏，在聊天页把满意的回答收藏起来吧'
})

const hasFilter = computed(() => Boolean(favorite.keyword.trim() || favorite.activeTagId))

onMounted(async () => {
  await Promise.all([favorite.fetchList(1), tagStore.loadTags(true)])
})

function handleSearch(): void {
  favorite.applyKeyword(searchInput.value)
}

function clearSearch(): void {
  searchInput.value = ''
  favorite.applyKeyword('')
}

function handleTagFilter(tagId?: number): void {
  favorite.applyTag(tagId)
}

function tagCountText(tag: Tag): string {
  return typeof tag.count === 'number' ? String(tag.count) : ''
}

function isLong(answer: string): boolean {
  return answer.length > 280
}

function toggleExpand(id: number): void {
  const next = new Set(expanded.value)
  if (next.has(id)) next.delete(id)
  else next.add(id)
  expanded.value = next
}

function providerName(provider: string | null): string {
  if (!provider) return '未知来源'
  return PROVIDER_LABELS[provider] ?? provider
}

async function copyAnswer(answer: string): Promise<void> {
  try {
    await navigator.clipboard.writeText(answer)
    ElMessage.success('回答已复制')
  } catch {
    ElMessage.warning('复制失败，请手动选择文本')
  }
}

async function removeFavorite(item: Favorite): Promise<void> {
  try {
    await ElMessageBox.confirm('删除后这条收藏将不可恢复（原会话不受影响）。', '删除收藏', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      confirmButtonClass: 'el-button--danger',
    })
  } catch {
    return
  }
  try {
    await favorite.removeFavorite(item.id)
    ElMessage.success('已删除收藏')
    await tagStore.loadTags(true)
  } catch {
    // 拦截器已提示
  }
}

async function createTag(): Promise<void> {
  const name = newTagName.value.trim()
  if (!name || tagSaving.value) return
  tagSaving.value = true
  try {
    await tagStore.createTag(name)
    newTagName.value = ''
    ElMessage.success('标签已创建')
  } catch {
    // 拦截器已提示
  } finally {
    tagSaving.value = false
  }
}

async function removeTag(tag: Tag): Promise<void> {
  try {
    await ElMessageBox.confirm(`删除标签「${tag.name}」？已收藏的内容不受影响。`, '删除标签', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      confirmButtonClass: 'el-button--danger',
    })
  } catch {
    return
  }
  try {
    await tagStore.removeTag(tag.id)
    if (favorite.activeTagId === tag.id) favorite.applyTag(undefined)
    ElMessage.success('标签已删除')
  } catch {
    // 拦截器已提示
  }
}
</script>

<template>
  <div class="page">
    <div class="page-shell">
      <header class="page-header">
        <div>
          <h1 class="page-title">
            收藏夹
            <span class="title-count">{{ favorite.total }}</span>
          </h1>
          <p class="page-subtitle">收藏是问答快照，删除原会话也不会丢失。</p>
        </div>
        <div class="page-actions">
          <el-button size="default" @click="tagDialog = true">
            <BaseIcon name="tag" :size="15" />
            标签管理
          </el-button>
          <el-button size="default" :loading="favorite.loading" @click="favorite.fetchList(favorite.pageNum)">
            <BaseIcon name="refresh" :size="14" />
            刷新
          </el-button>
        </div>
      </header>

      <section class="filter-bar surface-card">
        <div class="search-row">
          <el-input
            v-model="searchInput"
            class="search-input"
            placeholder="按提问或回答关键词搜索…"
            clearable
            @keyup.enter="handleSearch"
            @clear="clearSearch"
          >
            <template #prefix><BaseIcon name="search" :size="15" /></template>
          </el-input>
          <el-button type="primary" @click="handleSearch">
            <BaseIcon name="search" :size="14" />
            搜索
          </el-button>
        </div>

        <div class="tag-filter">
          <button
            type="button"
            class="tag-filter-chip"
            :class="{ active: !favorite.activeTagId }"
            @click="handleTagFilter(undefined)"
          >
            全部
          </button>
          <button
            v-for="tag in tagStore.tags"
            :key="tag.id"
            type="button"
            class="tag-filter-chip"
            :class="{ active: favorite.activeTagId === tag.id }"
            @click="handleTagFilter(tag.id)"
          >
            {{ tag.name }}
            <span v-if="tagCountText(tag)" class="chip-count">{{ tagCountText(tag) }}</span>
          </button>
          <span v-if="!tagStore.tags.length" class="tag-empty-hint">
            还没有标签，可在「标签管理」中创建
          </span>
        </div>
      </section>

      <div v-loading="favorite.loading" class="favorite-body">
        <div v-if="favorite.favorites.length" class="favorite-grid">
          <article v-for="item in favorite.favorites" :key="item.id" class="fav-card surface-card fade-in">
            <header class="fav-head">
              <span class="q-mark">Q</span>
              <h2 class="fav-question">{{ item.question }}</h2>
            </header>

            <div
              class="fav-answer"
              :class="{ collapsed: isLong(item.answer) && !expanded.has(item.id) }"
            >
              <div class="markdown-body" v-html="renderMarkdown(item.answer)" />
            </div>

            <button
              v-if="isLong(item.answer)"
              type="button"
              class="link-btn expand-btn"
              @click="toggleExpand(item.id)"
            >
              {{ expanded.has(item.id) ? '收起回答' : '展开完整回答' }}
            </button>

            <div class="fav-tags">
              <span v-for="tag in item.tags" :key="tag.id" class="tag-chip">{{ tag.name }}</span>
              <span v-if="!item.tags || !item.tags.length" class="tag-none">未打标签</span>
            </div>

            <footer class="fav-foot">
              <div class="fav-origin">
                <span class="origin-provider">{{ providerName(item.provider) }}</span>
                <span v-if="item.model" class="origin-model mono">{{ item.model }}</span>
                <span class="origin-time">{{ formatDateTime(item.createdAt) }}</span>
              </div>
              <div class="fav-actions">
                <button class="icon-btn" type="button" title="复制回答" @click="copyAnswer(item.answer)">
                  <BaseIcon name="copy" :size="14" />
                </button>
                <button class="icon-btn danger" type="button" title="删除收藏" @click="removeFavorite(item)">
                  <BaseIcon name="trash" :size="14" />
                </button>
              </div>
            </footer>
          </article>
        </div>

        <div v-else-if="!favorite.loading" class="favorite-empty surface-card">
          <span class="empty-icon"><BaseIcon name="bookmark" :size="22" /></span>
          <h2>{{ hasFilter ? '没有找到匹配的收藏' : '收藏夹还是空的' }}</h2>
          <p>{{ emptyDescription }}</p>
          <el-button v-if="hasFilter" @click="clearSearch(); handleTagFilter(undefined)">清空筛选</el-button>
        </div>
      </div>

      <div v-if="favorite.total > favorite.pageSize" class="pager-row">
        <el-pagination
          background
          layout="total, prev, pager, next"
          :total="favorite.total"
          :page-size="favorite.pageSize"
          :current-page="favorite.pageNum"
          @current-change="favorite.changePage"
        />
      </div>
    </div>

    <el-dialog v-model="tagDialog" title="标签管理" width="460px">
      <div class="tag-create-row">
        <el-input
          v-model="newTagName"
          maxlength="16"
          show-word-limit
          placeholder="新标签名称，例如：Spring"
          @keyup.enter="createTag"
        />
        <el-button type="primary" :loading="tagSaving" @click="createTag">添加</el-button>
      </div>

      <div v-if="tagStore.tags.length" class="tag-manage-list">
        <div v-for="tag in tagStore.tags" :key="tag.id" class="tag-manage-item">
          <span class="tag-chip">{{ tag.name }}</span>
          <span class="tag-manage-count">
            {{ typeof tag.count === 'number' ? `${tag.count} 条收藏` : '—' }}
          </span>
          <button class="icon-btn danger" type="button" title="删除标签" @click="removeTag(tag)">
            <BaseIcon name="trash" :size="14" />
          </button>
        </div>
      </div>
      <el-empty v-else description="还没有标签，先添加一个吧" :image-size="56" />
    </el-dialog>
  </div>
</template>

<style scoped>
.title-count {
  display: inline-grid;
  place-items: center;
  min-width: 26px;
  height: 22px;
  margin-left: 6px;
  padding: 0 8px;
  border-radius: 999px;
  background: var(--primary-soft);
  color: var(--primary-strong);
  font-size: 12px;
  font-weight: 600;
  vertical-align: 3px;
}

.filter-bar {
  display: flex;
  flex-direction: column;
  gap: 13px;
  padding: 16px 18px;
  margin-bottom: 20px;
}

.search-row {
  display: flex;
  gap: 10px;
  max-width: 560px;
}

.search-input {
  flex: 1;
}

.tag-filter {
  display: flex;
  align-items: center;
  gap: 7px;
  flex-wrap: wrap;
}

.tag-filter-chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 11px;
  border: 1px solid var(--border);
  border-radius: 999px;
  background: #fff;
  color: var(--text-secondary);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.tag-filter-chip:hover {
  border-color: var(--primary-light);
  color: var(--primary-strong);
}

.tag-filter-chip.active {
  border-color: var(--primary);
  background: var(--primary-soft);
  color: var(--primary-strong);
  font-weight: 600;
}

.chip-count {
  font-size: 10.5px;
  color: var(--text-muted);
}

.tag-filter-chip.active .chip-count {
  color: var(--primary);
}

.tag-empty-hint {
  font-size: 11.5px;
  color: var(--text-muted);
}

.favorite-body {
  min-height: 220px;
}

.favorite-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 16px;
}

.fav-card {
  display: flex;
  flex-direction: column;
  gap: 11px;
  padding: 18px 18px 13px;
  transition: border-color 0.18s ease, box-shadow 0.18s ease;
}

.fav-card:hover {
  border-color: var(--border-strong);
  box-shadow: 0 12px 30px rgba(22, 25, 42, 0.07);
}

.fav-head {
  display: flex;
  align-items: flex-start;
  gap: 9px;
}

.q-mark {
  display: grid;
  place-items: center;
  flex: none;
  width: 20px;
  height: 20px;
  margin-top: 1px;
  border-radius: 6px;
  background: var(--primary-soft);
  color: var(--primary-strong);
  font-size: 11px;
  font-weight: 700;
}

.fav-question {
  font-size: 14px;
  font-weight: 600;
  line-height: 1.55;
  color: var(--text-strong);
  word-break: break-word;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.fav-answer {
  position: relative;
  font-size: 13px;
  color: var(--text-secondary);
}

.fav-answer :deep(.markdown-body) {
  font-size: 13px;
  line-height: 1.72;
}

.fav-answer :deep(pre) {
  font-size: 12px;
}

.fav-answer.collapsed {
  max-height: 168px;
  overflow: hidden;
  -webkit-mask-image: linear-gradient(180deg, #000 62%, transparent 100%);
  mask-image: linear-gradient(180deg, #000 62%, transparent 100%);
}

.expand-btn {
  align-self: flex-start;
}

.fav-tags {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  margin-top: auto;
}

.tag-none {
  font-size: 11px;
  color: #a7adbd;
}

.fav-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding-top: 10px;
  border-top: 1px dashed var(--border);
}

.fav-origin {
  display: flex;
  align-items: center;
  gap: 7px;
  flex-wrap: wrap;
  font-size: 10.5px;
  color: var(--text-muted);
  min-width: 0;
}

.origin-provider {
  padding: 1px 7px;
  border-radius: 999px;
  background: var(--bg-subtle);
  border: 1px solid var(--border);
  color: var(--text-secondary);
}

.origin-model {
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.origin-time {
  color: #a7adbd;
}

.fav-actions {
  display: flex;
  gap: 2px;
  flex: none;
}

.icon-btn.danger:hover:not(:disabled) {
  color: var(--danger);
  background: #fef3f2;
}

.favorite-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 54px 24px;
  text-align: center;
}

.favorite-empty .empty-icon {
  display: grid;
  place-items: center;
  width: 48px;
  height: 48px;
  border-radius: 15px;
  background: var(--primary-soft);
  color: var(--primary);
  margin-bottom: 4px;
}

.favorite-empty h2 {
  font-size: 15px;
  font-weight: 650;
  color: var(--text-strong);
}

.favorite-empty p {
  max-width: 420px;
  font-size: 12.5px;
  color: var(--text-muted);
}

.pager-row {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

.tag-create-row {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}

.tag-manage-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 320px;
  overflow-y: auto;
}

.tag-manage-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 9px;
  background: var(--bg-subtle);
}

.tag-manage-count {
  margin-left: auto;
  font-size: 11px;
  color: var(--text-muted);
}

.icon-btn.danger {
  color: var(--text-muted);
}

@media (max-width: 720px) {
  .favorite-grid {
    grid-template-columns: 1fr;
  }

  .search-row {
    max-width: none;
  }
}
</style>
