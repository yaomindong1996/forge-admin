import { describe, expect, it } from 'vitest'
import { CONFIG_RENDERER_MAP, getConfigComponent } from '../config-renderer-map.js'

describe('cONFIG_RENDERER_MAP', () => {
  it('包含全部 12 种 nodeType 映射', () => {
    const expected = [
      'start',
      'end',
      'approver',
      'carbonCopy',
      'condition',
      'parallel',
      'inclusive',
      'service',
      'script',
      'subProcess',
      'callActivity',
      'advanced',
    ]
    for (const t of expected)
      expect(CONFIG_RENDERER_MAP[t]).toBeTruthy()
  })

  it('对象不可被修改（Object.freeze）', () => {
    expect(Object.isFrozen(CONFIG_RENDERER_MAP)).toBe(true)
  })

  it('parallel / inclusive 与 condition 共用 ConditionConfig', () => {
    expect(CONFIG_RENDERER_MAP.parallel).toBe(CONFIG_RENDERER_MAP.condition)
    expect(CONFIG_RENDERER_MAP.inclusive).toBe(CONFIG_RENDERER_MAP.condition)
  })
})

describe('getConfigComponent', () => {
  it('已知 nodeType 返回对应组件', () => {
    expect(getConfigComponent('approver')).toBe(CONFIG_RENDERER_MAP.approver)
    expect(getConfigComponent('start')).toBe(CONFIG_RENDERER_MAP.start)
  })
  it('未知 nodeType 返回 null', () => {
    expect(getConfigComponent('no-such-type')).toBe(null)
    expect(getConfigComponent(undefined)).toBe(null)
  })
})
