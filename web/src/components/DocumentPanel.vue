<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import BaseIcon from './BaseIcon.vue'
import { useChatStore } from '@/stores/chat'
import { DOCUMENT_STATUS_META, MAX_DOCUMENT_BYTES, MAX_DOCUMENT_COUNT } from '@/utils/constants'
import { formatBytes, formatDateTime } from '@/utils/format'
import type { ChatDocument } from '@/types'

const emit = defineEmits<{ close: [] }>()

const chat = useChatStore()
const dragging = ref(false)
const fileInput = ref<HTMLInputElement>()

const boundId = computed<number | null>(() => chat.activeSession?.documentId ?? null)
const canUpload = computed(() => chat.documents.length < MAX_DOCUMENT_COUNT && !chat.uploadingDocument)

onMounted(() => {
  void chat.loadDocuments()
})

function openPicker(): void {
  if (!canUpload.value) return
  fileInput.value?.click()
}

function validate(file: File): boolean {
  if (!file.name.toLowerCase().endsWith('.txt')) {
    ElMessage.error('仅支持 .txt 文件')
    return false
  }
  if (file.size > MAX_DOCUMENT_BYTES) {
    ElMessage.error('文件大小不能超过 1MB')
    return false
  }
  if (file.size === 0) {
    ElMessage.error('文件内容为空')
    return false
  }
  return true
}

async function upload(file: File): Promise<void> {
  if (!validate(file)) return
  if (chat.documents.length >= MAX_DOCUMENT_COUNT) {
    ElMessage.warning(`最多上传 ${MAX_DOCUMENT_COUNT} 份文档`)
    return
  }
  try {
    await chat.uploadDocument(file)
  } catch {
    // 拦截器已提示
  }
}

function handleFileChange(event: Event): void {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (file) void upload(file)
}

function handleDrop(event: DragEvent): void {
  dragging.value = false
  const file = event.dataTransfer?.files?.[0]
  if (file) void upload(file)
}

async function toggleBind(doc: ChatDocument): Promise<void> {
  if (!chat.activeSession) {
    ElMessage.warning('请先选择会话')
    return
  }
  try {
    await chat.bindDocument(boundId.value === doc.id ? null : doc.id)
  } catch {
    // 拦截器已提示
  }
}

async function removeDocument(doc: ChatDocument): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定删除「${doc.filename}」吗？绑定它的会话会自动解绑。`,
      '删除文档',
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
    await chat.removeDocument(doc.id)
    ElMessage.success('文档已删除')
  } catch {
    // 拦截器已提示
  }
}
</script>

<template>
  <aside class="doc-panel">
    <header class="doc-head">
      <div class="doc-title">
        <BaseIcon name="file" :size="16" />
        <span>文档</span>
        <span class="doc-count">{{ chat.documents.length }}/{{ MAX_DOCUMENT_COUNT }}</span>
      </div>
      <div class="doc-head-actions">
        <button
          class="icon-btn"
          type="button"
          title="刷新"
          :disabled="chat.documentsLoading"
          @click="chat.loadDocuments(true)"
        >
          <BaseIcon name="refresh" :size="14" />
        </button>
        <button class="icon-btn" type="button" title="收起面板" @click="emit('close')">
          <BaseIcon name="close" :size="15" />
        </button>
      </div>
    </header>

    <p class="doc-hint">
      上传 TXT 后可绑定到当前会话，提问时优先引用文档片段（≤1MB，单人最多 10 份）。
    </p>

    <div
      class="upload-zone"
      :class="{ dragging, busy: chat.uploadingDocument, disabled: !canUpload }"
      role="button"
      tabindex="0"
      @click="openPicker"
      @keydown.enter="openPicker"
      @dragover.prevent="dragging = true"
      @dragleave.prevent="dragging = false"
      @drop.prevent="handleDrop"
    >
      <input
        ref="fileInput"
        class="hidden-input"
        type="file"
        accept=".txt,text/plain"
        @change="handleFileChange"
      />
      <span class="upload-icon"><BaseIcon name="upload" :size="18" /></span>
      <div class="upload-text">
        <strong>{{ chat.uploadingDocument ? '上传中…' : '点击或拖拽 TXT 到此处' }}</strong>
        <span>仅支持 .txt，不超过 1MB</span>
      </div>
    </div>

    <div v-if="!chat.activeSession" class="bind-tip">
      <BaseIcon name="alert" :size="13" />
      未选择会话，绑定按钮不可用
    </div>
    <div v-else class="bind-tip is-bound">
      <BaseIcon name="check" :size="13" />
      当前会话：{{ chat.activeDocument?.filename ?? '未绑定文档' }}
    </div>

    <div v-loading="chat.documentsLoading" class="doc-list">
      <div v-if="!chat.documents.length && !chat.documentsLoading" class="doc-empty">
        <span class="doc-empty-icon"><BaseIcon name="file" :size="18" /></span>
        <p>还没有文档</p>
        <span>上传一份 TXT 后即可基于内容提问</span>
      </div>

      <div
        v-for="doc in chat.documents"
        :key="doc.id"
        class="doc-item"
        :class="{ bound: doc.id === boundId }"
      >
        <div class="doc-item-head">
          <span class="doc-name" :title="doc.filename">{{ doc.filename }}</span>
          <el-tag size="small" :type="DOCUMENT_STATUS_META[doc.status].type" effect="plain">
            {{ DOCUMENT_STATUS_META[doc.status].label }}
          </el-tag>
        </div>
        <div class="doc-meta">
          <span>{{ formatBytes(doc.byteSize) }}</span>
          <span>{{ doc.charCount }} 字</span>
          <span>{{ doc.chunkCount }} 片段</span>
        </div>
        <div class="doc-item-foot">
          <span class="doc-time">{{ formatDateTime(doc.createdAt) }}</span>
          <div class="doc-item-actions">
            <el-button
              size="small"
              :type="doc.id === boundId ? 'primary' : 'default'"
              :plain="doc.id !== boundId"
              :disabled="!chat.activeSession"
              @click="toggleBind(doc)"
            >
              {{ doc.id === boundId ? '已绑定' : '绑定' }}
            </el-button>
            <button class="icon-btn danger" type="button" title="删除" @click="removeDocument(doc)">
              <BaseIcon name="trash" :size="14" />
            </button>
          </div>
        </div>
      </div>
    </div>
  </aside>
</template>

<style scoped>
.doc-panel {
  display: flex;
  flex-direction: column;
  width: 318px;
  flex: none;
  min-height: 0;
  padding: 16px 16px 14px;
  background: #fbfbfe;
  border-left: 1px solid var(--border);
}

.doc-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.doc-title {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 13px;
  font-weight: 650;
  color: var(--text-strong);
}

.doc-count {
  padding: 1px 7px;
  border-radius: 999px;
  background: var(--bg-hover);
  color: var(--text-muted);
  font-size: 10.5px;
  font-weight: 500;
}

.doc-head-actions {
  display: flex;
  gap: 2px;
}

.doc-hint {
  margin: 9px 0 12px;
  font-size: 11.5px;
  line-height: 1.65;
  color: var(--text-muted);
}

.upload-zone {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 13px 12px;
  border: 1.5px dashed var(--border-strong);
  border-radius: 12px;
  background: #fff;
  cursor: pointer;
  transition: all 0.16s ease;
}

.upload-zone:hover:not(.disabled) {
  border-color: var(--primary-light);
  background: #fcfcff;
}

.upload-zone.dragging {
  border-color: var(--primary);
  background: var(--primary-soft);
}

.upload-zone.busy {
  opacity: 0.7;
  cursor: progress;
}

.upload-zone.disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.upload-icon {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  flex: none;
  border-radius: 10px;
  background: var(--primary-soft);
  color: var(--primary);
}

.upload-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.upload-text strong {
  font-size: 12.5px;
  font-weight: 550;
  color: var(--text);
}

.upload-text span {
  font-size: 11px;
  color: var(--text-muted);
}

.hidden-input {
  display: none;
}

.bind-tip {
  display: flex;
  align-items: center;
  gap: 5px;
  margin: 10px 2px 4px;
  font-size: 11px;
  color: var(--text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bind-tip.is-bound {
  color: var(--success);
}

.doc-list {
  flex: 1;
  min-height: 0;
  margin-top: 8px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-right: 2px;
}

.doc-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  padding: 34px 10px;
  text-align: center;
  color: var(--text-muted);
  font-size: 11.5px;
}

.doc-empty-icon {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  margin-bottom: 6px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid var(--border);
  color: var(--primary);
}

.doc-empty p {
  font-size: 12.5px;
  color: var(--text-secondary);
  font-weight: 500;
}

.doc-item {
  padding: 11px 12px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: #fff;
  transition: border-color 0.16s ease, box-shadow 0.16s ease;
}

.doc-item:hover {
  border-color: var(--border-strong);
  box-shadow: var(--shadow-sm);
}

.doc-item.bound {
  border-color: var(--primary-light);
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.06);
}

.doc-item-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.doc-name {
  font-size: 12.5px;
  font-weight: 550;
  color: var(--text-strong);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.doc-meta {
  display: flex;
  gap: 10px;
  margin-top: 4px;
  font-size: 10.5px;
  color: var(--text-muted);
}

.doc-item-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  margin-top: 9px;
}

.doc-time {
  font-size: 10px;
  color: #a7adbd;
}

.doc-item-actions {
  display: flex;
  align-items: center;
  gap: 2px;
}

.icon-btn.danger:hover:not(:disabled) {
  color: var(--danger);
  background: #fef3f2;
}

@media (max-width: 1180px) {
  .doc-panel {
    position: absolute;
    top: 0;
    right: 0;
    bottom: 0;
    z-index: 12;
    width: 320px;
    box-shadow: -16px 0 40px rgba(22, 25, 42, 0.12);
  }
}
</style>
