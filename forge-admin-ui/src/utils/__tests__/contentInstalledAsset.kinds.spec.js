import { describe, expect, it } from 'vitest'
import { CONTENT_ASSET_KIND_LABEL } from '../contentInstalledAsset'

describe('content asset kind labels', () => {
  it('covers print and form for library UI', () => {
    expect(CONTENT_ASSET_KIND_LABEL.print).toContain('打印')
    expect(CONTENT_ASSET_KIND_LABEL.form).toContain('表单')
    expect(CONTENT_ASSET_KIND_LABEL['print-component']).toContain('组件')
  })
})
