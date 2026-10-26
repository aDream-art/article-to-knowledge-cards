<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import BaseIcon from './BaseIcon.vue'
import { MESSAGE_STATUS_LABELS, PROVIDER_LABELS } from '@/utils/constants'
import { formatTime } from '@/utils/format'
import { renderMarkdown } from '@/utils/markdown'
import type { ChatMessage } from '@/types'

const props = defineProps<{ message: ChatMessage }>()
const emit = defineEmits<{ favorite: [message: ChatMessage] }>()

const copied = ref(false)

const isUser = computed(() => props.message.role === 'USER')

/** 流式过程中只追加纯文本，结束后才整体渲染 Markdown（避免半截代码块闪烁）。 */
const html = computed(() => (props.message.streaming ? '' : renderMarkdown(props.message.content)))
const favoriteDisabled = computed(() => props.message.streaming || !props.message.content.trim())
const showStatus = computed(
  () =>
    !props.message.streaming &&
    props.message.status !== 'COMPLETED' &&
    props.message.status !== 'STREAMING',
)
const statusLabel = computed(() => MESSAGE_STATUS_LABELS[props.message.status])
const providerLabel = computed(() =>
  props.message.provider ? (PROVIDER_LABELS[props.message.provider] ?? props.message.provider) : '',
)
const usageText = computed(() => {
  if (!props.message.tokenOut) return ''
  const parts = [`${props.message.tokenOut} tokens`]
  if (props.message.tokenIn) parts.push(`入 ${props.message.tokenIn}`)
  if (props.message.firstTokenMs) parts.push(`首字 ${props.message.firstTokenMs}ms`)
  return parts.join(' · ')
})

async function copy(): Promise<void> {
  if (!props.message.content) return
  try {
    await navigator.clipboard.writeText(props.message.content)
    copied.value = true
    ElMessage.success('已复制到剪贴板')
    window.setTimeout(() => {
      copied.value = false
    }, 1600)
  } catch {
    ElMessage.warning('复制失败，请手动选择文本')
  }
}
</script>

<template>
  <div class="message-row" :class="isUser ? 'is-user' : 'is-assistant'">
    <div class="avatar" :class="isUser ? 'avatar-user' : 'avatar-assistant'">
      <span v-if="isUser">我</span>
      <BaseIcon v-else name="sparkles" :size="17" />
    </div>

    <div class="message-body">
      <div class="message-head">
        <span class="role-name">{{ isUser ? '我' : '助手' }}</span>
        <span v-if="!isUser && providerLabel" class="head-meta">
          {{ providerLabel }}<template v-if="message.model"> · {{ message.model }}</template>
        </span>
        <span class="head-time">{{ formatTime(message.createdAt) }}</span>
        <el-tag v-if="message.fallback" size="small" type="warning" effect="plain">已降级</el-tag>
        <el-tag
          v-if="showStatus"
          size="small"
          :type="message.status === 'FAILED' ? 'danger' : 'warning'"
          effect="plain"
        >
          {{ statusLabel }}
        </el-tag>
      </div>

      <div v-if="isUser" class="user-bubble">{{ message.content }}</div>

      <div v-else class="assistant-content">
        <div v-if="message.streaming" class="markdown-body plain">
          {{ message.content }}<span class="stream-cursor" />
        </div>
        <div v-else-if="html" class="markdown-body" v-html="html" />
        <div v-else-if="message.status === 'FAILED'" class="failed-box">
          <BaseIcon name="alert" :size="15" />
          <span>{{ message.errorMessage || '回答生成失败，请稍后重试' }}</span>
        </div>
        <p v-else class="muted empty-answer">（空回答）</p>
      </div>

      <div v-if="!isUser" class="message-tools">
        <button class="tool-btn" type="button" :disabled="!message.content" @click="copy">
          <BaseIcon :name="copied ? 'check' : 'copy'" :size="13" />
          {{ copied ? '已复制' : '复制' }}
        </button>
        <button
          class="tool-btn"
          type="button"
          :disabled="favoriteDisabled"
          title="把这一轮问答存为收藏"
          @click="emit('favorite', message)"
        >
          <BaseIcon name="bookmark" :size="13" />
          收藏
        </button>
        <span v-if="usageText" class="usage-text">{{ usageText }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.message-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 8px 4px;
}

.message-row.is-user {
  flex-direction: row-reverse;
}

.avatar {
  display: grid;
  place-items: center;
  flex: none;
  width: 32px;
  height: 32px;
  margin-top: 2px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 600;
}

.avatar-user {
  background: linear-gradient(135deg, #4f46e5, #7c5cf5);
  color: #fff;
  box-shadow: 0 4px 10px rgba(79, 70, 229, 0.24);
}

.avatar-assistant {
  background: #fff;
  border: 1px solid var(--border);
  color: var(--primary);
}

.message-body {
  min-width: 0;
  max-width: min(820px, 82%);
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.is-user .message-body {
  align-items: flex-end;
}

.is-assistant .message-body {
  flex: 1 1 auto;
}

.message-head {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11.5px;
  color: var(--text-muted);
  flex-wrap: wrap;
}

.role-name {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--text-secondary);
}

.head-meta {
  color: var(--text-muted);
}

.head-time {
  color: #a7adbd;
}

.user-bubble {
  padding: 11px 15px;
  border-radius: 14px 4px 14px 14px;
  background: linear-gradient(135deg, #4f46e5, #6355e8);
  color: #fff;
  font-size: 14.5px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  box-shadow: 0 6px 18px rgba(79, 70, 229, 0.18);
}

.assistant-content {
  width: 100%;
  padding: 14px 17px;
  border: 1px solid var(--border);
  border-radius: 4px 14px 14px 14px;
  background: #fff;
  box-shadow: var(--shadow-sm);
}

.markdown-body.plain {
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 14.5px;
  line-height: 1.78;
  min-height: 22px;
}

.stream-cursor {
  display: inline-block;
  width: 7px;
  height: 1.02em;
  margin-left: 2px;
  vertical-align: -2px;
  border-radius: 2px;
  background: var(--primary);
  animation: cursor-blink 1s steps(2, start) infinite;
}

@keyframes cursor-blink {
  50% {
    opacity: 0;
  }
}

.empty-answer {
  font-size: 13px;
}

.failed-box {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 9px 11px;
  border-radius: 9px;
  background: #fef3f2;
  color: #b42318;
  font-size: 13px;
  line-height: 1.6;
}

.message-tools {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}

.tool-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 8px;
  border: 1px solid transparent;
  border-radius: 7px;
  background: transparent;
  color: var(--text-muted);
  font-size: 11.5px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.tool-btn:hover:not(:disabled) {
  background: #fff;
  border-color: var(--border);
  color: var(--text-secondary);
}

.tool-btn:disabled {
  opacity: 0.42;
  cursor: not-allowed;
}

.usage-text {
  margin-left: 4px;
  font-size: 11px;
  color: #a7adbd;
}

@media (max-width: 720px) {
  .message-body {
    max-width: 88%;
  }
}
</style>
