import { describe, expect, it } from 'vitest'
import { createIdGenerator } from '../../utils/id-generator.js'
import { deleteGateway, deletePlain, makeEdge } from '../flow-designer-helpers.js'

function fixture() {
  // Start → GW → [A → M, B → M] → M → End
  return {
    processId: 'P',
    nodes: [
      { id: 'S', nodeType: 'start', config: {} },
      { id: 'GW', nodeType: 'condition', config: {} },
      { id: 'A', nodeType: 'approver', config: {} },
      { id: 'B', nodeType: 'approver', config: {} },
      { id: 'M', nodeType: 'approver', config: {} },
      { id: 'E', nodeType: 'end', config: {} },
    ],
    edges: [
      makeEdge('F1', 'S', 'GW'),
      makeEdge('F2', 'GW', 'A', { branchId: 'b1' }),
      makeEdge('F3', 'GW', 'B', { branchId: 'b2', isDefault: true }),
      makeEdge('F4', 'A', 'M'),
      makeEdge('F5', 'B', 'M'),
      makeEdge('F6', 'M', 'E'),
    ],
  }
}

describe('deleteGateway', () => {
  it('删除网关 + 全部分支节点，入边接到汇合点 M', () => {
    const json = fixture()
    const idGen = createIdGenerator({ usedIds: new Set(json.edges.map(e => e.id).concat(json.nodes.map(n => n.id))) })
    deleteGateway(json, 'GW', idGen)
    // GW / A / B 都应被删除；M 保留
    const ids = json.nodes.map(n => n.id)
    expect(ids).toEqual(['S', 'M', 'E'])
    // S → M 直连
    expect(json.edges.find(e => e.source === 'S' && e.target === 'M')).toBeTruthy()
    // M → E 仍在
    expect(json.edges.find(e => e.source === 'M' && e.target === 'E')).toBeTruthy()
  })
})

describe('deletePlain - 笛卡儿重连', () => {
  it('1 入 1 出节点删除后只有一条新边', () => {
    const json = {
      processId: 'P',
      nodes: [
        { id: 'A', nodeType: 'start', config: {} },
        { id: 'B', nodeType: 'approver', config: {} },
        { id: 'C', nodeType: 'end', config: {} },
      ],
      edges: [
        makeEdge('F1', 'A', 'B'),
        makeEdge('F2', 'B', 'C'),
      ],
    }
    const idGen = createIdGenerator({ usedIds: new Set(['A', 'B', 'C', 'F1', 'F2']) })
    deletePlain(json, 'B', idGen)
    expect(json.nodes.map(n => n.id)).toEqual(['A', 'C'])
    expect(json.edges).toHaveLength(1)
    expect(json.edges[0]).toMatchObject({ source: 'A', target: 'C' })
  })

  it('保留入边 condition / branchId', () => {
    const json = {
      processId: 'P',
      nodes: [{ id: 'A', config: {} }, { id: 'B', config: {} }, { id: 'C', config: {} }],
      edges: [
        makeEdge('F1', 'A', 'B', { condition: 'x', branchId: 'b1', isDefault: true }),
        makeEdge('F2', 'B', 'C'),
      ],
    }
    const idGen = createIdGenerator({ usedIds: new Set(['A', 'B', 'C', 'F1', 'F2']) })
    deletePlain(json, 'B', idGen)
    expect(json.edges[0].condition).toBe('x')
    expect(json.edges[0].branchId).toBe('b1')
    expect(json.edges[0].isDefault).toBe(true)
  })
})
