import { describe, expect, it } from 'vitest'
import {
  buildPathD,
  EDGE_COLORS,
  getEdgeColor,
  getEdgeDashArray,
  getEdgeMidpoint,
} from '../path-builder.js'

describe('buildPathD - straight', () => {
  it('两点直线', () => {
    const d = buildPathD([{ x: 0, y: 0 }, { x: 100, y: 100 }], 'straight')
    expect(d).toBe('M 0,0 L 100,100')
  })

  it('多点 straight 仍只取首尾', () => {
    const d = buildPathD([{ x: 0, y: 0 }, { x: 50, y: 50 }, { x: 100, y: 100 }], 'straight')
    expect(d).toBe('M 0,0 L 100,100')
  })
})

describe('buildPathD - orthogonal', () => {
  it('多段折线', () => {
    const d = buildPathD([
      { x: 0, y: 0 },
      { x: 0, y: 50 },
      { x: 100, y: 50 },
      { x: 100, y: 100 },
    ], 'orthogonal')
    expect(d).toBe('M 0,0 L 0,50 L 100,50 L 100,100')
  })

  it('两点 orthogonal 退化为 straight', () => {
    const d = buildPathD([{ x: 0, y: 0 }, { x: 100, y: 100 }], 'orthogonal')
    expect(d).toBe('M 0,0 L 100,100')
  })
})

describe('buildPathD - bezier / rounded / 边界', () => {
  it('bezier 生成 C 控制', () => {
    const d = buildPathD([{ x: 0, y: 0 }, { x: 100, y: 100 }], 'bezier')
    expect(d).toContain('C')
  })

  it('rounded 在拐点用 Q', () => {
    const d = buildPathD([
      { x: 0, y: 0 },
      { x: 0, y: 50 },
      { x: 100, y: 50 },
      { x: 100, y: 100 },
    ], 'rounded', { cornerRadius: 6 })
    expect(d).toContain('Q')
  })

  it('rounded 不足 3 点退化', () => {
    const d = buildPathD([{ x: 0, y: 0 }, { x: 50, y: 50 }], 'rounded')
    expect(d).toBe('M 0,0 L 50,50')
  })

  it('空 / 单点 / null 返回空字符串', () => {
    expect(buildPathD([], 'straight')).toBe('')
    expect(buildPathD([{ x: 1, y: 1 }], 'orthogonal')).toBe('')
    expect(buildPathD(null, 'orthogonal')).toBe('')
  })
})

describe('getEdgeMidpoint', () => {
  it('两点取中点', () => {
    expect(getEdgeMidpoint([{ x: 0, y: 0 }, { x: 100, y: 0 }])).toEqual({ x: 50, y: 0 })
  })

  it('多段累积长度取中点', () => {
    const m = getEdgeMidpoint([{ x: 0, y: 0 }, { x: 0, y: 100 }, { x: 100, y: 100 }])
    expect(m).toEqual({ x: 0, y: 100 })
  })

  it('空 / 单点 / 重合点退化', () => {
    expect(getEdgeMidpoint([])).toEqual({ x: 0, y: 0 })
    expect(getEdgeMidpoint([{ x: 5, y: 5 }])).toEqual({ x: 5, y: 5 })
    expect(getEdgeMidpoint([{ x: 5, y: 5 }, { x: 5, y: 5 }])).toEqual({ x: 5, y: 5 })
  })
})

describe('getEdgeColor', () => {
  it('status 优先', () => {
    expect(getEdgeColor({}, 'completed')).toBe(EDGE_COLORS.completed)
    expect(getEdgeColor({}, 'rejected')).toBe(EDGE_COLORS.rejected)
  })
  it('isDefault 边橙色', () => {
    expect(getEdgeColor({ isDefault: true })).toBe(EDGE_COLORS.defaultBranch)
  })
  it('普通边灰色', () => {
    expect(getEdgeColor({})).toBe(EDGE_COLORS.default)
  })
})

describe('getEdgeDashArray', () => {
  it('pending / rejected / skipped → 虚线', () => {
    expect(getEdgeDashArray('pending')).toBe('6 4')
    expect(getEdgeDashArray('rejected')).toBe('6 4')
    expect(getEdgeDashArray('skipped')).toBe('6 4')
  })
  it('completed / running / null → 实线', () => {
    expect(getEdgeDashArray('completed')).toBe(null)
    expect(getEdgeDashArray('running')).toBe(null)
    expect(getEdgeDashArray()).toBe(null)
  })
})
