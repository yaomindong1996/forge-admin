import { describe, expect, it } from 'vitest'
import { buildPrintExportFileName, isExportFileNamePattern } from '../exportFileName'

describe('print export file name', () => {
  const now = new Date(2026, 8, 21, 11, 30, 5)

  it('accepts field and token placeholders and rejects path separators', () => {
    expect(isExportFileNamePattern('{template}-{{main.code}}-{timestamp}')).toBe(true)
    expect(isExportFileNamePattern('单据')).toBe(true)
    expect(isExportFileNamePattern('../secret')).toBe(false)
    expect(isExportFileNamePattern('{{constructor.prototype}}')).toBe(false)
    expect(isExportFileNamePattern('{eval}')).toBe(false)
  })

  it('defaults to template name, a business field and a timestamp', () => {
    expect(buildPrintExportFileName({
      templateName: '供应商打印',
      context: { main: { name: '华能材料', code: 'GYS-009' } },
      now,
    })).toBe('供应商打印-华能材料-20260921113005.pdf')
  })

  it('uses the designer pattern and still stamps the file when timestamp is omitted', () => {
    expect(buildPrintExportFileName({
      pattern: '{template}-{{main.code}}',
      templateName: '供应商打印',
      context: { main: { code: 'GYS-009' } },
      now,
    })).toBe('供应商打印-GYS-009-20260921113005.pdf')
  })
})
