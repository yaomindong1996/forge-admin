import { renderMarkdown, renderMarkdownStreaming } from '../components/agent-chat/chat-markdown'
import { applyAgentPagePart1 } from './useAgentPage.part1.js'
import { applyAgentPagePart2 } from './useAgentPage.part2.js'
import 'highlight.js/styles/atom-one-dark.css'

function renderPreviewMarkdown(message) {
  if (!message?.content)
    return ''
  return message.streaming
    ? renderMarkdownStreaming(message.content)
    : renderMarkdown(message.content)
}

function handlePreviewContentClick(event) {
  const btn = event?.target?.closest?.('.code-copy-btn')
  if (!btn)
    return
  const raw = decodeURIComponent(btn.dataset.code || '')
  navigator.clipboard?.writeText(raw).then(() => {
    btn.textContent = '已复制'
    setTimeout(() => {
      btn.textContent = '复制'
    }, 1500)
  }).catch(() => {})
}

export function useAgentPage() {
  let api = applyAgentPagePart1()
  api = applyAgentPagePart2(api)
  const { __impl, mut, ...publicApi } = api
  return {
    ...publicApi,
    renderPreviewMarkdown,
    handlePreviewContentClick,
  }
}
