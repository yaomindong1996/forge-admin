import { describe, expect, it } from 'vitest'
import {
  DEFAULT_PAGE_TITLE_CONTENT,
  mergeBlockTypographyStyle,
  resolvePageTitleRichContent,
} from '../gridBlockRendererUtils.js'

describe('resolvePageTitleRichContent', () => {
  it('用 title/subtitle 覆盖仍为出厂默认的 content', () => {
    const html = resolvePageTitleRichContent({
      content: DEFAULT_PAGE_TITLE_CONTENT,
      title: 'E2E_page-title',
      subtitle: 'E2E_subtitle',
    })
    expect(html).toContain('E2E_page-title')
    expect(html).toContain('E2E_subtitle')
    expect(html).not.toContain('>页面标题<')
  })

  it('自定义富文本 content 优先于 title', () => {
    const html = resolvePageTitleRichContent({
      content: '<h1>自定义标题</h1>',
      title: 'E2E_page-title',
      subtitle: 'E2E_subtitle',
    })
    expect(html).toBe('<h1>自定义标题</h1>')
  })

  it('content 为空时按 title 拼装', () => {
    const html = resolvePageTitleRichContent({ title: '仅标题' })
    expect(html).toBe('<h1>仅标题</h1>')
  })
})

describe('mergeBlockTypographyStyle', () => {
  it('合并 style.css 与顶层 color/fontSize', () => {
    const merged = mergeBlockTypographyStyle(
      { width: '100%' },
      {
        css: { color: 'rgb(80, 60, 136)', fontSize: '14px', borderRadius: '8px' },
        color: '#0b7285',
        fontSize: 16,
      },
    )
    expect(merged.color).toBe('#0b7285')
    expect(merged.fontSize).toBe('16px')
    expect(merged.borderRadius).toBe('8px')
  })
})
