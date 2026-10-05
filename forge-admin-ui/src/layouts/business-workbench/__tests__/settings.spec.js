import { describe, expect, it } from 'vitest'
import { defaultLayout, layoutSettings, normalizeLayout, validLayoutNames } from '../../../settings'

describe('业务工作台默认布局', () => {
  it('注册业务工作台并作为唯一默认回退布局', () => {
    expect(defaultLayout).toBe('business-workbench')
    expect(layoutSettings.defaultLayout).toBe(defaultLayout)
    expect(validLayoutNames).toContain(defaultLayout)
    expect(normalizeLayout()).toBe(defaultLayout)
    expect(normalizeLayout('default')).toBe(defaultLayout)
    expect(normalizeLayout('unknown-layout')).toBe(defaultLayout)
  })

  it('保留租户已明确选择的有效布局', () => {
    expect(normalizeLayout('normal')).toBe('normal')
    expect(normalizeLayout('top-menu')).toBe('top-menu')
    expect(normalizeLayout('side-flyout')).toBe('side-flyout')
    expect(validLayoutNames).toContain('side-flyout')
  })
})
