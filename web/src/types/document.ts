/** 文档类型（spec §4 chat_document）。 */

/** 文档处理状态：解析中 / 可用 / 向量化失败（可重传）。 */
export type DocumentStatus = 'READY' | 'EMBED_FAILED' | 'PARSING'

export interface ChatDocument {
  id: number
  filename: string
  byteSize: number
  charCount: number
  chunkCount: number
  status: DocumentStatus
  createdAt: string
}
