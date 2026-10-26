/**
 * 聊天主状态：会话列表、消息流、流式生成、文档绑定。
 *
 * 流式约定（spec §6.1）：
 * - `meta` 到达后把本地占位消息绑定到服务端 messageId
 * - `delta` 增量追加纯文本，结束后再整体渲染 Markdown（见 MessageItem）
 * - `stopStreaming()` 走 AbortController，后端把半截回答按 ABORTED 落库
 */

import { computed, reactive, ref } from 'vue'
import { defineStore } from 'pinia'
import { ElMessage } from 'element-plus'
import {
  CHAT_STREAM_URL,
  deleteDocumentApi,
  listDocumentsApi,
  uploadDocumentApi,
} from '@/api/document'
import {
  bindSessionDocumentApi,
  createSessionApi,
  deleteSessionApi,
  listMessagesApi,
  listSessionsApi,
  renameSessionApi,
} from '@/api/session'
import { notifyError } from '@/api/http'
import { useSSE } from '@/composables/useSSE'
import type {
  ChatDocument,
  ChatMessage,
  ChatSession,
  CreateSessionPayload,
  DocumentStatus,
  MessageDto,
  MessageRole,
  MessageStatus,
  Provider,
} from '@/types'

const PROVIDERS: Provider[] = ['deepseek', 'qwen', 'mock']
const SESSION_PAGE_SIZE = 20
const MESSAGE_PAGE_SIZE = 30

function normalizeProvider(value?: string | null): Provider | null {
  const key = (value ?? '').toLowerCase()
  return (PROVIDERS as string[]).includes(key) ? (key as Provider) : null
}

/** 后端 role 可能大写也可能小写，统一成前端枚举。 */
function normalizeRole(value?: string | null): MessageRole {
  return (value ?? '').toUpperCase() === 'USER' ? 'USER' : 'ASSISTANT'
}

function normalizeStatus(value?: string | null): MessageStatus {
  const key = (value ?? '').toUpperCase()
  if (key === 'ABORTED' || key === 'FAILED' || key === 'STREAMING') return key
  return 'COMPLETED'
}

function normalizeDocumentStatus(value?: string | null): DocumentStatus {
  const key = (value ?? '').toUpperCase()
  if (key === 'EMBED_FAILED' || key === 'PARSING') return key
  return 'READY'
}

function normalizeMessage(dto: MessageDto): ChatMessage {
  return {
    id: dto.id,
    key: `msg-${dto.id}`,
    role: normalizeRole(dto.role),
    content: dto.content ?? '',
    status: normalizeStatus(dto.status),
    provider: normalizeProvider(dto.provider),
    model: dto.model ?? null,
    createdAt: dto.createdAt,
  }
}

function normalizeSession(session: ChatSession): ChatSession {
  return {
    ...session,
    title: session.title || '未命名会话',
    provider: normalizeProvider(session.provider) ?? 'deepseek',
    model: session.model || '',
    documentId: session.documentId ?? null,
    lastMessageAt: session.lastMessageAt ?? null,
  }
}

function normalizeDocument(document: ChatDocument): ChatDocument {
  return { ...document, status: normalizeDocumentStatus(document.status) }
}

let keySeed = 0
function nextLocalKey(prefix: string): string {
  keySeed += 1
  return `${prefix}-${Date.now()}-${keySeed}`
}

export const useChatStore = defineStore('chat', () => {
  /* ------------------------------ 会话 ------------------------------ */
  const sessions = ref<ChatSession[]>([])
  const sessionsLoading = ref(false)
  const sessionsFinished = ref(false)
  const sessionTotal = ref(0)
  const sessionPageNum = ref(1)
  const creatingSession = ref(false)
  const activeSessionId = ref<number | null>(null)

  /* ------------------------------ 消息 ------------------------------ */
  const messages = ref<ChatMessage[]>([])
  const messagesLoading = ref(false)
  const olderLoading = ref(false)
  const messagesFinished = ref(true)
  const messagePageNum = ref(1)
  const messageTotal = ref(0)

  /* ---------------------------- 流式状态 ---------------------------- */
  const streaming = ref(false)
  /** 每次 delta 自增，供视图做自动滚动。 */
  const streamVersion = ref(0)
  const drafts = reactive<Record<number, string>>({})
  const sse = useSSE()

  /* ------------------------------ 文档 ------------------------------ */
  const documents = ref<ChatDocument[]>([])
  const documentsLoading = ref(false)
  const documentsLoaded = ref(false)
  const uploadingDocument = ref(false)

  const activeSession = computed<ChatSession | null>(
    () => sessions.value.find((session) => session.id === activeSessionId.value) ?? null,
  )
  const activeDocument = computed<ChatDocument | null>(
    () => documents.value.find((doc) => doc.id === activeSession.value?.documentId) ?? null,
  )
  const canSend = computed(() => Boolean(activeSessionId.value) && !streaming.value)
  const hasMoreSessions = computed(() => !sessionsFinished.value && sessions.value.length > 0)

  /* ----------------------------- 会话操作 ---------------------------- */

  async function fetchSessionPage(page: number): Promise<void> {
    if (sessionsLoading.value) return
    sessionsLoading.value = true
    const previousActive = activeSessionId.value
      ? (sessions.value.find((session) => session.id === activeSessionId.value) ?? null)
      : null
    try {
      const data = await listSessionsApi({ pageNum: page, pageSize: SESSION_PAGE_SIZE })
      const list = (Array.isArray(data?.list) ? data.list : []).map(normalizeSession)
      if (page === 1) {
        const merged = [...list]
        // 当前会话可能因分页被挤出首页，保留它以免头部信息闪空
        if (previousActive && !merged.some((session) => session.id === previousActive.id)) {
          merged.unshift(previousActive)
        }
        sessions.value = merged
      } else {
        const existing = new Set(sessions.value.map((session) => session.id))
        sessions.value = [...sessions.value, ...list.filter((session) => !existing.has(session.id))]
      }
      sessionPageNum.value = page
      sessionTotal.value = typeof data?.total === 'number' ? data.total : sessions.value.length
      const totalPage = typeof data?.totalPage === 'number' ? data.totalPage : page
      sessionsFinished.value =
        list.length < SESSION_PAGE_SIZE ||
        page >= totalPage ||
        sessions.value.length >= sessionTotal.value
    } catch {
      sessionsFinished.value = true
    } finally {
      sessionsLoading.value = false
    }
  }

  function loadSessions(reset = true): Promise<void> {
    return fetchSessionPage(reset ? 1 : sessionPageNum.value + 1)
  }

  async function createSession(payload: CreateSessionPayload): Promise<ChatSession | null> {
    creatingSession.value = true
    try {
      const created = await createSessionApi(payload)
      if (created && typeof created.id === 'number') {
        const session = normalizeSession(created)
        sessions.value = [session, ...sessions.value.filter((item) => item.id !== session.id)]
        sessionTotal.value += 1
        sessionPageNum.value = 1
        await selectSession(session.id)
        return session
      }
      // 后端未返回会话体时回退到列表首条
      await fetchSessionPage(1)
      const first = sessions.value[0] ?? null
      if (first) await selectSession(first.id)
      return first
    } finally {
      creatingSession.value = false
    }
  }

  async function renameSession(sessionId: number, title: string): Promise<void> {
    const trimmed = title.trim()
    if (!trimmed) return
    await renameSessionApi(sessionId, trimmed)
    const target = sessions.value.find((session) => session.id === sessionId)
    if (target) target.title = trimmed
  }

  async function removeSession(sessionId: number): Promise<void> {
    await deleteSessionApi(sessionId)
    sessions.value = sessions.value.filter((session) => session.id !== sessionId)
    sessionTotal.value = Math.max(0, sessionTotal.value - 1)
    delete drafts[sessionId]

    if (activeSessionId.value !== sessionId) return

    if (streaming.value) stopStreaming()
    activeSessionId.value = null
    messages.value = []
    messagesFinished.value = true
    messageTotal.value = 0
    messagePageNum.value = 1

    const next = sessions.value[0]
    if (next) await selectSession(next.id)
  }

  async function selectSession(sessionId: number): Promise<void> {
    if (activeSessionId.value === sessionId) return
    if (streaming.value) stopStreaming()
    activeSessionId.value = sessionId
    messages.value = []
    messagesFinished.value = true
    messageTotal.value = 0
    messagePageNum.value = 1
    await loadMessages(1, true)
  }

  /* ----------------------------- 消息操作 ---------------------------- */

  async function loadMessages(page: number, replace: boolean): Promise<void> {
    const sessionId = activeSessionId.value
    if (!sessionId) return

    if (replace) messagesLoading.value = true
    else olderLoading.value = true

    try {
      const data = await listMessagesApi(sessionId, { pageNum: page, pageSize: MESSAGE_PAGE_SIZE })
      if (activeSessionId.value !== sessionId) return

      // 后端倒序返回，展示需要 旧 → 新
      const chronological = (Array.isArray(data?.list) ? data.list : []).map(normalizeMessage).reverse()

      if (replace) {
        messages.value = chronological
      } else {
        const existing = new Set(messages.value.map((message) => message.key))
        messages.value = [...chronological.filter((message) => !existing.has(message.key)), ...messages.value]
      }

      messagePageNum.value = page
      messageTotal.value = typeof data?.total === 'number' ? data.total : messages.value.length
      const totalPage = typeof data?.totalPage === 'number' ? data.totalPage : page
      messagesFinished.value =
        chronological.length === 0 ||
        page >= totalPage ||
        messages.value.length >= messageTotal.value
    } catch {
      messagesFinished.value = true
    } finally {
      messagesLoading.value = false
      olderLoading.value = false
    }
  }

  function loadOlderMessages(): Promise<void> {
    if (messagesFinished.value || messagesLoading.value || olderLoading.value) return Promise.resolve()
    return loadMessages(messagePageNum.value + 1, false)
  }

  function refreshSessionsQuietly(): void {
    void fetchSessionPage(1)
  }

  /* ------------------------------ 发送 ------------------------------ */

  async function sendMessage(rawContent: string): Promise<void> {
    const content = rawContent.trim()
    const sessionId = activeSessionId.value
    if (!content || !sessionId || streaming.value) return

    const session = activeSession.value
    const createdAt = new Date().toISOString()

    const userMessage = reactive<ChatMessage>({
      id: -Date.now(),
      key: nextLocalKey('user'),
      role: 'USER',
      content,
      status: 'COMPLETED',
      provider: session?.provider ?? null,
      model: session?.model ?? null,
      createdAt,
    })

    const assistantMessage = reactive<ChatMessage>({
      id: -Date.now() - 1,
      key: nextLocalKey('assistant'),
      role: 'ASSISTANT',
      content: '',
      status: 'STREAMING',
      provider: session?.provider ?? null,
      model: session?.model ?? null,
      createdAt,
      streaming: true,
    })

    messages.value.push(userMessage, assistantMessage)
    drafts[sessionId] = ''
    streaming.value = true
    streamVersion.value += 1
    let receivedMeta = false

    await sse.start(CHAT_STREAM_URL, { sessionId, content }, {
      onMeta(meta) {
        receivedMeta = true
        assistantMessage.id = meta.messageId
        assistantMessage.provider = normalizeProvider(meta.provider) ?? assistantMessage.provider
        assistantMessage.model = meta.model
        assistantMessage.fallback = Boolean(meta.fallback)
      },
      onDelta(delta) {
        assistantMessage.content += delta.text
        streamVersion.value += 1
      },
      onUsage(usage) {
        assistantMessage.tokenIn = usage.tokenIn
        assistantMessage.tokenOut = usage.tokenOut
        assistantMessage.firstTokenMs = usage.firstTokenMs
      },
      onDone() {
        assistantMessage.status = 'COMPLETED'
      },
      onError(error) {
        assistantMessage.status = 'FAILED'
        assistantMessage.errorMessage = error.message
        notifyError(error.message)
      },
    })

    assistantMessage.streaming = false
    if (assistantMessage.status === 'STREAMING') {
      assistantMessage.status = sse.aborted.value ? 'ABORTED' : 'COMPLETED'
    }
    streaming.value = false
    streamVersion.value += 1

    if (session) session.lastMessageAt = new Date().toISOString()

    if (sse.aborted.value) {
      ElMessage.info('已停止生成')
    }

    // 首字节都没拿到就失败：撤掉空气泡，错误已通过 ElMessage 提示
    if (!receivedMeta && assistantMessage.status === 'FAILED' && !assistantMessage.content) {
      messages.value = messages.value.filter((message) => message.key !== assistantMessage.key)
    }

    refreshSessionsQuietly()
  }

  function stopStreaming(): void {
    if (!streaming.value) return
    sse.abort()
  }

  /* ------------------------------ 草稿 ------------------------------ */

  function getDraft(sessionId: number | null): string {
    return sessionId ? (drafts[sessionId] ?? '') : ''
  }

  function setDraft(sessionId: number | null, value: string): void {
    if (sessionId) drafts[sessionId] = value
  }

  /* ------------------------------ 文档 ------------------------------ */

  async function loadDocuments(force = false): Promise<void> {
    if (documentsLoading.value) return
    if (documentsLoaded.value && !force) return
    documentsLoading.value = true
    try {
      const list = await listDocumentsApi()
      documents.value = list.map(normalizeDocument)
      documentsLoaded.value = true
    } catch {
      // 拦截器已提示
    } finally {
      documentsLoading.value = false
    }
  }

  async function uploadDocument(file: File): Promise<ChatDocument | null> {
    uploadingDocument.value = true
    try {
      const created = await uploadDocumentApi(file)
      if (created && typeof created.id === 'number') {
        const document = normalizeDocument(created)
        documents.value = [document, ...documents.value.filter((item) => item.id !== document.id)]
      } else {
        await loadDocuments(true)
      }
      ElMessage.success('上传成功，正在解析与向量化')
      return created
    } finally {
      uploadingDocument.value = false
    }
  }

  async function removeDocument(documentId: number): Promise<void> {
    await deleteDocumentApi(documentId)
    documents.value = documents.value.filter((document) => document.id !== documentId)
    for (const session of sessions.value) {
      if (session.documentId === documentId) session.documentId = null
    }
  }

  async function bindDocument(documentId: number | null): Promise<void> {
    const session = activeSession.value
    if (!session) {
      ElMessage.warning('请先选择会话')
      return
    }
    await bindSessionDocumentApi(session.id, documentId)
    session.documentId = documentId
    ElMessage.success(documentId ? '已绑定文档' : '已解绑文档')
  }

  function reset(): void {
    stopStreaming()
    sessions.value = []
    sessionsLoading.value = false
    sessionsFinished.value = false
    sessionTotal.value = 0
    sessionPageNum.value = 1
    activeSessionId.value = null
    messages.value = []
    messagesFinished.value = true
    messageTotal.value = 0
    messagePageNum.value = 1
    streaming.value = false
    documents.value = []
    documentsLoaded.value = false
    for (const key of Object.keys(drafts)) delete drafts[Number(key)]
  }

  return {
    // 会话
    sessions,
    sessionsLoading,
    sessionsFinished,
    sessionTotal,
    creatingSession,
    activeSessionId,
    activeSession,
    hasMoreSessions,
    // 消息
    messages,
    messagesLoading,
    olderLoading,
    messagesFinished,
    messageTotal,
    // 流式
    streaming,
    streamVersion,
    canSend,
    // 文档
    documents,
    documentsLoading,
    uploadingDocument,
    activeDocument,
    // 动作
    loadSessions,
    createSession,
    renameSession,
    removeSession,
    selectSession,
    loadMessages,
    loadOlderMessages,
    sendMessage,
    stopStreaming,
    getDraft,
    setDraft,
    loadDocuments,
    uploadDocument,
    removeDocument,
    bindDocument,
    reset,
  }
})
