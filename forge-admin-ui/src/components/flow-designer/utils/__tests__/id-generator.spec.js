import { describe, expect, it } from 'vitest'
import { collectExistingIds, createIdGenerator } from '../id-generator.js'

describe('createIdGenerator', () => {
  it('nextNodeId / nextEdgeId / nextBranchId 单调递增', () => {
    const g = createIdGenerator()
    expect(g.nextNodeId()).toBe('Node_1')
    expect(g.nextNodeId()).toBe('Node_2')
    expect(g.nextEdgeId()).toBe('Flow_1')
    expect(g.nextEdgeId()).toBe('Flow_2')
    expect(g.nextBranchId()).toBe('b1')
  })

  it('已用 ID 自动跳过', () => {
    const g = createIdGenerator({ usedIds: new Set(['Node_1', 'Node_2']) })
    expect(g.nextNodeId()).toBe('Node_3')
  })

  it('register 注册占用', () => {
    const g = createIdGenerator()
    g.register('Node_1')
    g.register('Node_5')
    expect(g.nextNodeId()).toBe('Node_2')
    g.nextNodeId() // Node_3
    g.nextNodeId() // Node_4
    expect(g.nextNodeId()).toBe('Node_6')
  })

  it('snapshot 反映计数 + 占用大小', () => {
    const g = createIdGenerator()
    g.nextNodeId()
    g.nextEdgeId()
    expect(g.snapshot().node).toBe(1)
    expect(g.snapshot().edge).toBe(1)
    expect(g.snapshot().usedSize).toBe(2)
  })
})

describe('collectExistingIds', () => {
  it('收集 nodes / edges / branchId', () => {
    const json = {
      nodes: [{ id: 'A' }, { id: 'B' }],
      edges: [{ id: 'F1', branchId: 'b1' }, { id: 'F2', branchId: null }],
    }
    const set = collectExistingIds(json)
    expect(set.has('A')).toBe(true)
    expect(set.has('B')).toBe(true)
    expect(set.has('F1')).toBe(true)
    expect(set.has('F2')).toBe(true)
    expect(set.has('b1')).toBe(true)
    expect(set.size).toBe(5)
  })

  it('null / undefined 安全', () => {
    expect(collectExistingIds(null).size).toBe(0)
    expect(collectExistingIds({}).size).toBe(0)
  })
})
