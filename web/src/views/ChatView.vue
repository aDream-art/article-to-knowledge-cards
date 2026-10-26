<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import BaseIcon from '@/components/BaseIcon.vue'
import ChatComposer from '@/components/ChatComposer.vue'
import DocumentPanel from '@/components/DocumentPanel.vue'
import FavoriteDialog from '@/components/FavoriteDialog.vue'
import MessageItem from '@/components/MessageItem.vue'
import SessionSidebar from '@/components/SessionSidebar.vue'
import { useChatStore } from '@/stores/chat'
import { PROVIDER_LABELS } from '@/utils/constants'
import type { ChatMessage } from '@/types'

const chat = useChatStore()

const scroller = ref<HTMLElement | null>(null)
const composerRef = ref<{ focus: () => void } | null>(null)
const showDocs = ref(true)
const favoriteOpen = ref(false)
const favoriteTarget = ref<{
  question: string
  answer: string
  provider: string | null
  model: string | null
} | null>(null)

let stickToBottom = true

const draft = computed({
  get: () => chat.getDraft(chat.activeSessionId),
  set: (value: string) => chat.setDraft(chat.activeSessionId, value),
})

const providerLabel = computed(() => {
  const provider = chat.activeSession?.provider
  return provider ? (PROVIDER_LABELS[provider] ?? provider) : ''
})

const suggestions = [
  { tag: '概念辨析', text: '用通俗的话解释 SSE 和 WebSocket 的区别，各适合什么场景？' },
  { tag: '写代码', text: '帮我写一个 MySQL 分页查询，并说明需要哪些索引。' },
  { tag: '拆任务', text: '把「支持 TXT 文档问答」拆成一份可执行的开发任务清单。' },
]

function isNearBottom(): boolean {
  const el = scroller.value
  if (!el) return true
  return el.scrollHeight - el.scrollTop - el.clientHeight < 120
}

function onScroll(): void {
  stickToBottom = isNearBottom()
}

function scrollToBottom(smooth = false): void {
  const el = scroller.value
  if (!el) return
  el.scrollTo({ top: el.scrollHeight, behavior: smooth ? 'smooth' : 'auto' })
}

async function followIfSticky(): Promise<void> {
  if (!stickToBottom) return
  await nextTick()
  scrollToBottom()
}

watch(() => chat.streamVersion, followIfSticky)
watch(() => chat.messages.length, followIfSticky)
watch(
  () => chat.activeSessionId,
  async () => {
    stickToBottom = true
    await nextTick()
    scrollToBottom()
  },
)

onMounted(async () => {
  await chat.loadSessions(true)
  if (!chat.activeSessionId) {
    const first = chat.sessions[0]
    if (first) await chat.selectSession(first.id)
  }
  await nextTick()
  scrollToBottom()
})

async function send(): Promise<void> {
  const content = draft.value.trim()
  const sessionId = chat.activeSessionId
  if (!content || !sessionId || chat.streaming) return
  chat.setDraft(sessionId, '')
  stickToBottom = true
  await chat.sendMessage(content)
}

async function loadOlder(): Promise<void> {
  const el = scroller.value
  const beforeHeight = el?.scrollHeight ?? 0
  const beforeTop = el?.scrollTop ?? 0
  stickToBottom = false
  await chat.loadOlderMessages()
  await nextTick()
  if (!el) return
  el.scrollTop = beforeTop + (el.scrollHeight - beforeHeight)
}

function useSuggestion(text: string): void {
  if (!chat.activeSessionId) return
  chat.setDraft(chat.activeSessionId, text)
  void nextTick(() => composerRef.value?.focus())
}

function openFavorite(message: ChatMessage): void {
  const index = chat.messages.findIndex((item) => item.key === message.key)
  let question = ''
  for (let cursor = index - 1; cursor >= 0; cursor -= 1) {
    const candidate = chat.messages[cursor]
    if (candidate && candidate.role === 'USER') {
      question = candidate.content
      break
    }
  }
  if (!question) {
    ElMessage.warning('没有找到这一轮对应的提问，无法收藏')
    return
  }
  favoriteTarget.value = {
    question,
    answer: message.content,
    provider: message.provider,
    model: message.model,
  }
  favoriteOpen.value = true
}

async function createFirstSession(): Promise<void> {
  try {
    await chat.createSession({ provider: 'deepseek', model: 'deepseek-chat' })
    ElMessage.success('会话已创建')
  } catch {
    // 拦截器已提示
  }
}
</script>

<template>
  <div class="chat-page">
    <SessionSidebar />

    <section class="chat-main">
      <header class="chat-headbar">
        <div class="chat-heading">
          <h1 class="chat-title" :title="chat.activeSession?.title">
            {{ chat.activeSession?.title ?? '未选择会话' }}
          </h1>
          <div class="chat-sub">
            <template v-if="chat.activeSession">
              <span class="meta-chip">
                <i class="provider-dot" :data-provider="chat.activeSession.provider" />
                {{ providerLabel }}
              </span>
              <span v-if="chat.activeSession.model" class="meta-chip mono">
                {{ chat.activeSession.model }}
              </span>
              <span class="meta-chip" :class="{ 'is-empty': !chat.activeDocument }">
                <BaseIcon name="file" :size="12" />
                {{ chat.activeDocument?.filename ?? '未绑定文档' }}
              </span>
            </template>
            <span v-else>从左侧选择或新建一个会话</span>
          </div>
        </div>

        <div class="chat-head-actions">
          <el-button
            size="small"
            :type="showDocs ? 'primary' : 'default'"
            :plain="!showDocs"
            @click="showDocs = !showDocs"
          >
            <BaseIcon name="file" :size="14" />
            文档面板
          </el-button>
        </div>
      </header>

      <div ref="scroller" class="message-scroll" @scroll="onScroll">
        <div v-if="chat.messagesLoading" class="message-loading">
          <span class="loading-dot" />
          正在加载消息…
        </div>

        <template v-else-if="chat.messages.length">
          <div v-if="!chat.messagesFinished" class="load-older">
            <button class="link-btn" type="button" :disabled="chat.olderLoading" @click="loadOlder">
              {{ chat.olderLoading ? '加载中…' : '加载更早的消息' }}
            </button>
          </div>
          <MessageItem
            v-for="message in chat.messages"
            :key="message.key"
            :message="message"
            @favorite="openFavorite"
          />
        </template>

        <div v-else-if="chat.activeSession" class="chat-empty">
          <span class="empty-mark"><BaseIcon name="sparkles" :size="24" /></span>
          <h2>开始第一轮提问</h2>
          <p>当前会话的上下文独立保存，文档绑定后回答会优先引用原文片段。</p>
          <div class="suggestion-list">
            <button
              v-for="item in suggestions"
              :key="item.text"
              type="button"
              class="suggestion"
              @click="useSuggestion(item.text)"
            >
              <span class="suggestion-tag">{{ item.tag }}</span>
              <span class="suggestion-text">{{ item.text }}</span>
              <BaseIcon name="arrow-left" :size="14" class="suggestion-arrow" />
            </button>
          </div>
        </div>

        <div v-else class="chat-empty">
          <span class="empty-mark"><BaseIcon name="chat" :size="24" /></span>
          <h2>还没有可用的会话</h2>
          <p>新建会话时可以选择 DeepSeek 或通义千问，之后可在右上角上传文档。</p>
          <el-button type="primary" @click="createFirstSession">
            <BaseIcon name="plus" :size="14" />
            新建会话
          </el-button>
        </div>
      </div>

      <ChatComposer
        ref="composerRef"
        v-model="draft"
        :streaming="chat.streaming"
        :disabled="!chat.activeSession"
        @send="send"
        @stop="chat.stopStreaming()"
      />
    </section>

    <Transition name="doc-panel-fade">
      <DocumentPanel v-if="showDocs" @close="showDocs = false" />
    </Transition>

    <FavoriteDialog
      v-model:visible="favoriteOpen"
      :session-id="chat.activeSessionId"
      :question="favoriteTarget?.question ?? ''"
      :answer="favoriteTarget?.answer ?? ''"
      :provider="favoriteTarget?.provider ?? null"
      :model="favoriteTarget?.model ?? null"
    />
  </div>
</template>

<style scoped>
.chat-page {
  position: relative;
  display: flex;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  background: var(--bg-app);
}

.chat-main {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
  min-height: 0;
}

.chat-headbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex: none;
  padding: 12px 24px;
  background: rgba(255, 255, 255, 0.72);
  border-bottom: 1px solid var(--border);
  backdrop-filter: blur(8px);
}

.chat-heading {
  min-width: 0;
}

.chat-title {
  font-size: 15px;
  font-weight: 650;
  color: var(--text-strong);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-sub {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  margin-top: 4px;
  font-size: 11.5px;
  color: var(--text-muted);
}

.meta-chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  max-width: 280px;
  padding: 2px 8px;
  border-radius: 999px;
  background: #fff;
  border: 1px solid var(--border);
  color: var(--text-secondary);
  font-size: 11px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.meta-chip.is-empty {
  color: var(--text-muted);
  border-style: dashed;
}

.chat-head-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: none;
}

.message-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 18px 24px 8px;
  scroll-behavior: auto;
}

.message-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 48px 0;
  font-size: 12.5px;
  color: var(--text-muted);
}

.loading-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--primary);
  animation: loading-pulse 1s ease-in-out infinite;
}

@keyframes loading-pulse {
  0%,
  100% {
    opacity: 0.3;
    transform: scale(0.85);
  }
  50% {
    opacity: 1;
    transform: scale(1);
  }
}

.load-older {
  display: flex;
  justify-content: center;
  padding: 4px 0 14px;
}

.chat-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  max-width: 560px;
  margin: 7vh auto 0;
  text-align: center;
}

.empty-mark {
  display: grid;
  place-items: center;
  width: 52px;
  height: 52px;
  margin-bottom: 6px;
  border-radius: 17px;
  background: #fff;
  border: 1px solid var(--border);
  color: var(--primary);
  box-shadow: var(--shadow-sm);
}

.chat-empty h2 {
  font-size: 17px;
  font-weight: 650;
  color: var(--text-strong);
}

.chat-empty p {
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--text-muted);
  max-width: 420px;
}

.suggestion-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
  margin-top: 18px;
}

.suggestion {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 10px 13px;
  border: 1px solid var(--border);
  border-radius: 11px;
  background: #fff;
  text-align: left;
  cursor: pointer;
  transition: all 0.16s ease;
}

.suggestion:hover {
  border-color: var(--primary-light);
  box-shadow: 0 6px 18px rgba(79, 70, 229, 0.09);
  transform: translateY(-1px);
}

.suggestion-tag {
  flex: none;
  padding: 2px 8px;
  border-radius: 999px;
  background: var(--primary-soft);
  color: var(--primary-strong);
  font-size: 10.5px;
  font-weight: 600;
}

.suggestion-text {
  flex: 1;
  min-width: 0;
  font-size: 12.5px;
  color: var(--text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.suggestion-arrow {
  flex: none;
  color: var(--text-muted);
  transform: rotate(180deg);
}

.doc-panel-fade-enter-active,
.doc-panel-fade-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.doc-panel-fade-enter-from,
.doc-panel-fade-leave-to {
  opacity: 0;
  transform: translateX(12px);
}

@media (max-width: 860px) {
  .message-scroll {
    padding: 14px 12px 6px;
  }

  .chat-headbar {
    padding: 10px 12px;
  }
}
</style>
