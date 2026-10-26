/**
 * Markdown 渲染：marked + highlight.js。
 *
 * 流式过程中不渲染 Markdown（见 ChatView / MessageItem），
 * 只在回答完成后整体渲染，避免半截代码块闪烁。
 */

import { marked } from 'marked'
import hljs from 'highlight.js/lib/common'

const renderer = new marked.Renderer()

renderer.code = ({ text, lang }) => {
  const rawLang = (lang ?? '').trim().split(/\s+/)[0] ?? ''
  const language = rawLang && hljs.getLanguage(rawLang) ? rawLang : 'plaintext'
  let highlighted: string
  try {
    highlighted = hljs.highlight(text, { language, ignoreIllegals: true }).value
  } catch {
    highlighted = escapeHtml(text)
  }
  return `<pre class="hljs" data-lang="${escapeHtml(language)}"><code class="hljs language-${escapeHtml(language)}">${highlighted}</code></pre>`
}

const BLOCKED_TAGS = new Set([
  'SCRIPT',
  'STYLE',
  'IFRAME',
  'OBJECT',
  'EMBED',
  'LINK',
  'META',
  'FORM',
  'INPUT',
  'BUTTON',
  'TEXTAREA',
])

function escapeHtml(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

/**
 * 轻量清理：模型输出属于不可信内容，去掉脚本与事件属性。
 * 用 DOMParser 解析后按标签/属性白名单式裁剪，避免引入额外依赖。
 */
export function sanitizeHtml(html: string): string {
  const doc = new DOMParser().parseFromString(html, 'text/html')
  doc.body.querySelectorAll('*').forEach((element) => {
    if (BLOCKED_TAGS.has(element.tagName)) {
      element.remove()
      return
    }
    for (const attribute of Array.from(element.attributes)) {
      const name = attribute.name.toLowerCase()
      if (name.startsWith('on')) {
        element.removeAttribute(attribute.name)
        continue
      }
      if ((name === 'href' || name === 'src' || name === 'xlink:href') && /^\s*(javascript|data):/i.test(attribute.value)) {
        element.removeAttribute(attribute.name)
      }
    }
  })
  doc.body.querySelectorAll('a').forEach((anchor) => {
    anchor.setAttribute('target', '_blank')
    anchor.setAttribute('rel', 'noopener noreferrer')
  })
  return doc.body.innerHTML
}

/** 把 Markdown 文本渲染为安全的 HTML 字符串。 */
export function renderMarkdown(source: string): string {
  if (!source.trim()) return ''
  try {
    const html = marked.parse(source, {
      gfm: true,
      breaks: true,
      renderer,
      async: false,
    })
    return sanitizeHtml(typeof html === 'string' ? html : '')
  } catch {
    return `<p>${escapeHtml(source)}</p>`
  }
}
