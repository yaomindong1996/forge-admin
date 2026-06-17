import { describe, expect, it } from 'vitest'
import { calculateLayout } from '../layout-algorithm.js'

function linearJson() {
  return {
    processId: 'P',
    nodes: [
      { id: 'S', nodeType: 'start', name: '', config: {} },
      { id: 'T1', nodeType: 'approver', name: '', config: {} },
      { id: 'T2', nodeType: 'approver', name: '', config: {} },
      { id: 'E', nodeType: 'end', name: '', config: {} },
    ],
    edges: [
      { id: 'F1', source: 'S', target: 'T1' },
      { id: 'F2', source: 'T1', target: 'T2' },
      { id: 'F3', source: 'T2', target: 'E' },
    ],
  }
}

function parallelJson() {
  return {
    processId: 'P',
    nodes: [
      { id: 'S', nodeType: 'start', config: {} },
      { id: 'GW', nodeType: 'parallel', config: {} },
      { id: 'A', nodeType: 'approver', config: {} },
      { id: 'B', nodeType: 'approver', config: {} },
      { id: 'C', nodeType: 'approver', config: {} },
      { id: 'M', nodeType: 'parallel', config: { mergeNode: true } },
      { id: 'E', nodeType: 'end', config: {} },
    ],
    edges: [
      { id: 'F1', source: 'S', target: 'GW' },
      { id: 'F2', source: 'GW', target: 'A' },
      { id: 'F3', source: 'GW', target: 'B' },
      { id: 'F4', source: 'GW', target: 'C' },
      { id: 'F5', source: 'A', target: 'M' },
      { id: 'F6', source: 'B', target: 'M' },
      { id: 'F7', source: 'C', target: 'M' },
      { id: 'F8', source: 'M', target: 'E' },
    ],
  }
}

describe('calculateLayout - 线性流程', () => {
  it('y 单调递增', () => {
    const out = calculateLayout(linearJson())
    const ys = ['S', 'T1', 'T2', 'E'].map(id => out.nodePositions.get(id).y)
    for (let i = 1; i < ys.length; i += 1)
      expect(ys[i]).toBeGreaterThan(ys[i - 1])
  })

  it('线性节点 x 一致（都在中线）', () => {
    const out = calculateLayout(linearJson())
    const xs = ['S', 'T1', 'T2', 'E'].map(id => out.nodePositions.get(id).x)
    for (const x of xs)
      expect(x).toBe(xs[0])
  })

  it('每条边都有 waypoints', () => {
    const out = calculateLayout(linearJson())
    expect(out.edgeWaypoints.size).toBe(3)
    for (const wp of out.edgeWaypoints.values())
      expect(wp.length).toBeGreaterThanOrEqual(2)
  })
})

describe('calculateLayout - 并行三分支', () => {
  it('3 条分支 x 不重叠', () => {
    const out = calculateLayout(parallelJson())
    const xs = ['A', 'B', 'C'].map(id => out.nodePositions.get(id).x)
    const set = new Set(xs)
    expect(set.size).toBe(3)
  })

  it('汇合节点 M 的 x 和网关 GW 一致（都回到中线）', () => {
    const out = calculateLayout(parallelJson())
    const gw = out.nodePositions.get('GW')
    const m = out.nodePositions.get('M')
    expect(m.x).toBe(gw.x)
  })

  it('所有节点都被布局', () => {
    const out = calculateLayout(parallelJson())
    expect(out.nodePositions.size).toBe(7)
  })
})

describe('calculateLayout - 退化场景', () => {
  it('空 flowJson 返回空结果', () => {
    expect(calculateLayout(null).nodePositions.size).toBe(0)
    expect(calculateLayout({ nodes: [], edges: [] }).nodePositions.size).toBe(0)
  })

  it('孤立节点放右侧', () => {
    const json = {
      processId: 'P',
      nodes: [
        { id: 'S', nodeType: 'start', config: {} },
        { id: 'E', nodeType: 'end', config: {} },
        { id: 'X', nodeType: 'advanced', config: {} },
      ],
      edges: [{ id: 'F', source: 'S', target: 'E' }],
    }
    const out = calculateLayout(json)
    expect(out.nodePositions.size).toBe(3)
    const x = out.nodePositions.get('X')
    const s = out.nodePositions.get('S')
    expect(x.x).toBeGreaterThan(s.x)
  })
})
