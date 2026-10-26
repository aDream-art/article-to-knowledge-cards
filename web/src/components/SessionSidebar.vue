<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import BaseIcon from './BaseIcon.vue'
import { useChatStore } from '@/stores/chat'
import { DEFAULT_MODEL, PROVIDER_LABELS, PROVIDER_OPTIONS } from '@/utils/constants'
import { formatRelativeTime } from '@/utils/format'
import type { ChatSession, Provider } from '@/types'

const chat = useChatStore()

const dialogVisible = ref(false)
const form = reactive<{ title: string; provider: Provider; model: string }>({
  title: '',
  provider: 'deepseek',
  model: DEFAULT_MODEL.deepseek,
})

const modelOptions = computed(
  () => PROVIDER_OPTIONS.find((item) => item.value === form.provider)?.models ?? [],
)

function openCreate(): void {
  form.title = ''
  form.provider = 'deepseek'
  form.model = DEFAULT_MODEL.deepseek
  dialogVisible.value = true
}

function selectProvider(provider: Provider): void {
  form.provider = provider
  form.model = DEFAULT_MODEL[provider] ?? modelOptions.value[0]?.value ?? ''
}

async function submitCreate(): Promise<void> {
  if (chat.creatingSession) return
  try {
    await chat.createSession({
      title: form.title.trim() || undefined,
      provider: form.provider,
      model: form.model,
    })
    dialogVisible.value = false
    ElMessage.success('会话已创建')
  } catch {
    // 拦截器已提示
  }
}

async function handleRename(session: ChatSession): Promise<void> {
  try {
    const result = await ElMessageBox.prompt('请输入新的会话标题', '重命名会话', {
      confirmButtonText: '保存',
      cancelButtonText: '取消',
      inputValue: session.title,
      inputValidator: (value: string) => (value && value.trim().length > 0 ? true : '标题不能为空'),
      inputErrorMessage: '标题不能为空',
    })
    const title = String(result.value ?? '').trim()
    if (!title || title === session.title) return
    await chat.renameSession(session.id, title)
    ElMessage.success('已重命名')
  } catch {
    // 用户取消或请求失败
  }
}

async function handleDelete(session: ChatSession): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `删除「${session.title}」后，会话与消息将无法恢复。`,
      '删除会话',
      {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        confirmButtonClass: 'el-button--danger',
      },
    )
  } catch {
    return
  }
  try {
    await chat.removeSession(session.id)
    ElMessage.success('会话已删除')
  } catch {
    // 拦截器已提示
  }
}
</script>

<template>
  <aside class="session-sidebar">
    <div class="sidebar-head">
      <span class="sidebar-title">会话</span>
      <el-button class="new-session-btn" type="primary" size="small" @click="openCreate">
        <BaseIcon name="plus" :size="14" />
        新建
      </el-button>
    </div>

    <div v-loading="chat.sessionsLoading && !chat.sessions.length" class="session-list">
      <div v-if="!chat.sessions.length && !chat.sessionsLoading" class="sidebar-empty">
        <span class="empty-icon"><BaseIcon name="chat" :size="20" /></span>
        <p>还没有会话</p>
        <span>新建一个会话开始提问</span>
      </div>

      <div
        v-for="session in chat.sessions"
        :key="session.id"
        class="session-item"
        :class="{ active: session.id === chat.activeSessionId }"
        role="button"
        tabindex="0"
        @click="chat.selectSession(session.id)"
        @keydown.enter="chat.selectSession(session.id)"
      >
        <div class="session-main">
          <span class="session-title" :title="session.title">{{ session.title }}</span>
          <span class="session-meta">
            <i class="provider-dot" :data-provider="session.provider" />
            {{ PROVIDER_LABELS[session.provider] ?? session.provider }} · {{ formatRelativeTime(session.lastMessageAt) }}
          </span>
        </div>
        <div class="session-actions">
          <button class="icon-btn" type="button" title="重命名" @click.stop="handleRename(session)">
            <BaseIcon name="edit" :size="14" />
          </button>
          <button class="icon-btn" type="button" title="删除" @click.stop="handleDelete(session)">
            <BaseIcon name="trash" :size="14" />
          </button>
        </div>
      </div>

      <button
        v-if="chat.hasMoreSessions"
        class="load-more"
        type="button"
        :disabled="chat.sessionsLoading"
        @click="chat.loadSessions(false)"
      >
        {{ chat.sessionsLoading ? '加载中…' : '加载更多' }}
      </button>
    </div>

    <div class="sidebar-foot">
      <span>共 {{ chat.sessionTotal }} 个会话</span>
    </div>

    <el-dialog v-model="dialogVisible" title="新建会话" width="480px" :close-on-click-modal="false">
      <el-form label-position="top" @submit.prevent>
        <el-form-item label="会话标题（可选）">
          <el-input
            v-model="form.title"
            maxlength="50"
            show-word-limit
            placeholder="例如：Spring Boot 排障"
            @keyup.enter="submitCreate"
          />
        </el-form-item>
        <el-form-item label="模型提供方">
          <div class="provider-grid">
            <button
              v-for="option in PROVIDER_OPTIONS"
              :key="option.value"
              type="button"
              class="provider-card"
              :class="{ selected: form.provider === option.value }"
              @click="selectProvider(option.value)"
            >
              <strong>{{ option.label }}</strong>
              <span>{{ option.hint }}</span>
            </button>
          </div>
        </el-form-item>
        <el-form-item label="模型">
          <el-select v-model="form.model" style="width: 100%">
            <el-option
              v-for="model in modelOptions"
              :key="model.value"
              :label="model.label"
              :value="model.value"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="chat.creatingSession" @click="submitCreate">
          创建并开始
        </el-button>
      </template>
    </el-dialog>
  </aside>
</template>

<style scoped>
.session-sidebar {
  display: flex;
  flex-direction: column;
  width: 262px;
  flex: none;
  min-height: 0;
  background: #fbfbfe;
  border-right: 1px solid var(--border);
}

.sidebar-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 14px 12px 18px;
}

.sidebar-title {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
  color: var(--text-muted);
}

.new-session-btn {
  gap: 4px;
}

.session-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 0 10px 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.session-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 10px 10px 12px;
  border: 1px solid transparent;
  border-radius: 11px;
  cursor: pointer;
  transition: background 0.15s ease, border-color 0.15s ease;
}

.session-item:hover {
  background: #f2f3fa;
}

.session-item.active {
  background: #fff;
  border-color: var(--border);
  box-shadow: var(--shadow-sm);
}

.session-item.active::before {
  content: '';
  position: absolute;
  left: 3px;
  top: 50%;
  width: 3px;
  height: 20px;
  transform: translateY(-50%);
  border-radius: 999px;
  background: var(--primary);
}

.session-main {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
  flex: 1;
}

.session-title {
  font-size: 13.5px;
  font-weight: 500;
  color: var(--text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-item.active .session-title {
  color: var(--text-strong);
  font-weight: 600;
}

.session-meta {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  color: var(--text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.provider-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex: none;
  background: #6366f1;
}

.provider-dot[data-provider='qwen'] {
  background: #0ea5e9;
}

.provider-dot[data-provider='mock'] {
  background: #94a3b8;
}

.session-actions {
  display: flex;
  gap: 2px;
  opacity: 0;
  transition: opacity 0.15s ease;
}

.session-item:hover .session-actions,
.session-item.active .session-actions {
  opacity: 1;
}

.session-actions .icon-btn {
  width: 24px;
  height: 24px;
}

.sidebar-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 44px 12px;
  text-align: center;
  color: var(--text-muted);
  font-size: 12px;
}

.sidebar-empty .empty-icon {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  margin-bottom: 6px;
  border-radius: 14px;
  background: #fff;
  border: 1px solid var(--border);
  color: var(--primary);
}

.sidebar-empty p {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
}

.load-more {
  margin: 8px 4px 0;
  padding: 7px 0;
  border: 1px dashed var(--border-strong);
  border-radius: 9px;
  background: transparent;
  color: var(--text-muted);
  font-size: 12px;
  cursor: pointer;
}

.load-more:hover:not(:disabled) {
  color: var(--primary);
  border-color: var(--primary-light);
  background: #fff;
}

.sidebar-foot {
  flex: none;
  padding: 10px 18px 14px;
  border-top: 1px solid var(--border);
  font-size: 11.5px;
  color: var(--text-muted);
}

.provider-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  width: 100%;
}

.provider-card {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 3px;
  padding: 11px 13px;
  border: 1px solid var(--border);
  border-radius: 11px;
  background: #fff;
  text-align: left;
  cursor: pointer;
  transition: all 0.16s ease;
}

.provider-card:hover {
  border-color: var(--primary-light);
}

.provider-card strong {
  font-size: 13.5px;
  color: var(--text-strong);
}

.provider-card span {
  font-size: 11.5px;
  color: var(--text-muted);
}

.provider-card.selected {
  border-color: var(--primary);
  background: var(--primary-soft);
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.08);
}

.provider-card.selected strong {
  color: var(--primary-strong);
}

@media (max-width: 860px) {
  .session-sidebar {
    width: 208px;
  }
}
</style>
