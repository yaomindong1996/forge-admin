import { describe, expect, it } from 'vitest'
import {
  designerActionTypeToListType,
  listActionTypeToDesignerType,
  normalizeDesignerActionType,
  normalizeListActionType,
  resolveListActionTypeFromDesigner,
} from '../listActionTypeBridge'

describe('listActionTypeBridge', () => {
  it('preserves refresh through list ↔ designer round-trip', () => {
    expect(normalizeListActionType('refresh')).toBe('refresh')
    expect(listActionTypeToDesignerType('refresh')).toBe('REFRESH')
    expect(normalizeDesignerActionType('REFRESH')).toBe('REFRESH')
    expect(designerActionTypeToListType('REFRESH')).toBe('refresh')
    expect(resolveListActionTypeFromDesigner('REFRESH', {}, {})).toBe('refresh')
  })

  it('keeps page when navigationMode is page even without targetPageKey', () => {
    expect(listActionTypeToDesignerType('page')).toBe('OPEN_PAGE')
    expect(resolveListActionTypeFromDesigner('OPEN_PAGE', {}, { navigationMode: 'page' })).toBe('page')
  })

  it('maps external and api types both ways', () => {
    expect(listActionTypeToDesignerType('external')).toBe('OPEN_EXTERNAL')
    expect(designerActionTypeToListType('OPEN_EXTERNAL')).toBe('external')
    expect(listActionTypeToDesignerType('CALL_API')).toBe('CALL_API')
    expect(designerActionTypeToListType('CALL_API')).toBe('CALL_API')
  })
})

describe('listActionTypeBridge source wiring', () => {
  it('BusinessListDesigner part2 imports bridge mappers', async () => {
    const fs = await import('node:fs')
    const path = await import('node:path')
    const source = fs.readFileSync(
      path.resolve(__dirname, '../useBusinessListDesigner.part2.js'),
      'utf8',
    )
    expect(source).toContain('listActionTypeBridge')
    expect(source).toContain('bridgeListActionTypeToDesignerType')
    // 按钮样式与显示条件必须写入 designer action，避免回写丢字段
    expect(source).toMatch(/type:\s*normalized\.type/)
    expect(source).toMatch(/displayCondition:\s*normalized\.displayCondition/)
  })
})
