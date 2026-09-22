import { describe, expect, it } from 'vitest'
import {
  applyPrintPageWatermark,
  composePrintPageWatermarkText,
  normalizePrintPageWatermark,
  readPagePrintWatermark,
} from '../pagePrintWatermark'

describe('page print watermark', () => {
  it('composes the same fields as application settings watermark', () => {
    expect(composePrintPageWatermarkText({
      enabled: true,
      text: '内部资料',
      showUsername: true,
      showTime: true,
    }, { userName: '张三', now: new Date(2026, 8, 21, 20, 7) })).toBe('内部资料 · 张三 · 2026-09-21 20:07')
    expect(composePrintPageWatermarkText({ enabled: false, text: '内部资料' }, { userName: '张三' })).toBe('')
  })

  it('overrides the template watermark when the page watermark is on', () => {
    const next = applyPrintPageWatermark(
      { watermark: { text: '模板水印', expression: 'main.company', opacity: 0.2 } },
      { enabled: true, text: '页面水印', showUsername: false, showTime: false },
    )
    expect(next.watermark).toMatchObject({ text: '页面水印', opacity: 0.2 })
    expect(next.watermark).not.toHaveProperty('expression')
  })

  it('uses the page color, size and density when the page watermark is on', () => {
    const next = applyPrintPageWatermark(
      { watermark: { text: '模板水印', color: '#ff0000', fontSizePt: 20, gapXMm: 40, gapYMm: 28, opacity: 0.2 } },
      { enabled: true, text: '页面水印', showUsername: false, showTime: false, density: 'sparse', fontSizePt: 18, color: '#112233' },
    )
    expect(next.watermark).toMatchObject({
      text: '页面水印',
      opacity: 0.2,
      color: '#112233',
      fontSizePt: 18,
      gapXMm: 96,
      gapYMm: 72,
    })
  })

  it('leaves a template alone when it turned watermark off', () => {
    const document = { watermark: { enabled: false, text: '模板水印' } }
    expect(applyPrintPageWatermark(document, { enabled: true, text: '页面水印' })).toBe(document)
  })

  it('reads the current page watermark from application options', () => {
    expect(readPagePrintWatermark({
      options: {
        inAppBuilder: {
          nodes: [{
            id: 'page_purchase',
            printWatermark: { enabled: true, text: '采购页', showUsername: false },
          }],
        },
      },
    }, 'page_purchase')).toEqual(normalizePrintPageWatermark({
      enabled: true,
      text: '采购页',
      showUsername: false,
    }))
  })
})
