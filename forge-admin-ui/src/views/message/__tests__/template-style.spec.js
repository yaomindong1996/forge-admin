import { describe, expect, it } from 'vitest'
import {
  buildJavaTemplateExample,
  getStyleGuide,
  getStyleSnippets,
  insertAtCursor,
  renderPreviewHtml,
  resolveContentStyleMode,
} from '../template-style'

describe('message template style helpers', () => {
  it('按渠道给出可插入的样式，而不是让用户写 CSS', () => {
    expect(resolveContentStyleMode({ channel: 'SMS' })).toBe('plain')
    expect(resolveContentStyleMode({ channel: 'COLLABORATION', templateCode: 'FLOW_TODO_CARD' })).toBe('card')
    expect(resolveContentStyleMode({ templateCode: 'FLOW_TODO_CARD_DINGTALK' })).toBe('markdown')
    expect(getStyleSnippets('card').map(item => item.key)).toEqual(['gray', 'normal', 'highlight'])
    expect(getStyleSnippets('plain')).toEqual([])
    expect(getStyleGuide('card')).toContain('三种行样式')
  })

  it('插入样式行并预览出业务人员能看懂的卡片', () => {
    const inserted = insertAtCursor('你好', '<div class="highlight">请尽快处理</div>\n', 2)
    expect(inserted.value).toContain('你好<div class="highlight">请尽快处理</div>')
    const keySlot = '$' + '{taskTitle}'
    expect(renderPreviewHtml(`<div class="normal">任务：${keySlot}</div>`, 'card')).toContain('采购单审批')
    expect(buildJavaTemplateExample({ templateCode: 'FLOW_TODO_CARD', variables: ['taskTitle'] }))
      .toContain('req.setTemplateCode("FLOW_TODO_CARD")')
  })
})
