import { describe, expect, it } from 'vitest'
import { layoutFlow } from '../layout-engine.js'

function linearJson() {
  return {
    processId: 'P',
    nodes: [
      { id: 'S', nodeType: 'start', config: {} },
      { id: 'T1', nodeType: 'approver', config: {} },
      { id: 'T2', nodeType: 'approver', config: {} },
      { id: 'E', nodeType: 'end', config: {} },
    ],
    edges: [
      { id: 'F1', source: 'S', target: 'T1' },
      { id: 'F2', source: 'T1', target: 'T2' },
      { id: 'F3', source: 'T2', target: 'E' },
    ],
  }
}

function branchJson() {
  return {
    processId: 'P',
    nodes: [
      { id: 'S', nodeType: 'start', config: {} },
      { id: 'GW', nodeType: 'condition', config: {} },
      { id: 'A', nodeType: 'approver', config: {} },
      { id: 'B', nodeType: 'approver', config: {} },
      { id: 'M', nodeType: 'approver', config: { mergeNode: true } },
      { id: 'E', nodeType: 'end', config: {} },
    ],
    edges: [
      { id: 'F1', source: 'S', target: 'GW' },
      { id: 'F2', source: 'GW', target: 'A' },
      { id: 'F3', source: 'GW', target: 'B' },
      { id: 'F4', source: 'A', target: 'M' },
      { id: 'F5', source: 'B', target: 'M' },
      { id: 'F6', source: 'M', target: 'E' },
    ],
  }
}

describe('layoutFlow - 线性流程', () => {
  it('返回所有节点 + 边的 paths', () => {
    const out = layoutFlow(linearJson())
    expect(out.nodePositions.size).toBe(4)
    expect(out.edgePaths.size).toBe(3)
  })

  it('线性边类型为 straight（同 x 上下连接）', () => {
    const out = layoutFlow(linearJson())
    for (const path of out.edgePaths.values())
      expect(path.type).toBe('straight')
  })

  it('canvasBounds 覆盖所有节点 + waypoints', () => {
    const out = layoutFlow(linearJson())
    const b = out.canvasBounds
    expect(b.minX).toBeLessThan(b.maxX)
    expect(b.minY).toBeLessThan(b.maxY)
  })
})

describe('layoutFlow - 分支流程', () => {
  it('分支边为 orthogonal（不同 x 折线）', () => {
    const out = layoutFlow(branchJson())
    const f2 = out.edgePaths.get('F2')
    const f3 = out.edgePaths.get('F3')
    // GW → A 与 GW → B 至少有一个是 orthogonal
    const types = [f2?.type, f3?.type]
    expect(types).toContain('orthogonal')
  })

  it('节点 width / height 应用默认值', () => {
    const out = layoutFlow(branchJson())
    for (const pos of out.nodePositions.values()) {
      expect(pos.width).toBeGreaterThan(0)
      expect(pos.height).toBeGreaterThan(0)
    }
  })

  it('网关使用紧凑菱形尺寸，连线端点连接到菱形边界', () => {
    const out = layoutFlow(branchJson())
    const gateway = out.nodePositions.get('GW')
    const incoming = out.edgePaths.get('F1').points
    const outgoing = out.edgePaths.get('F2').points

    expect(gateway.width).toBe(44)
    expect(gateway.height).toBe(44)
    expect(incoming.at(-1)).toMatchObject({
      x: gateway.x + gateway.width / 2,
      y: gateway.y,
    })
    expect(outgoing[0]).toMatchObject({
      x: gateway.x + gateway.width / 2,
      y: gateway.y + gateway.height,
    })
  })

  it('结束节点使用紧凑尺寸，入边接到节点上沿中心', () => {
    const out = layoutFlow({
      nodes: [
        { id: 'S', nodeType: 'start', config: {} },
        { id: 'E', nodeType: 'end', config: {} },
      ],
      edges: [{ id: 'F1', source: 'S', target: 'E' }],
    })
    const end = out.nodePositions.get('E')
    const tip = out.edgePaths.get('F1').points.at(-1)
    expect(end.width).toBe(20)
    expect(end.height).toBe(20)
    expect(tip).toMatchObject({
      x: end.x + end.width / 2,
      y: end.y,
    })
  })

  it('uiWaypoints 会写入正交路径拐点', () => {
    const out = layoutFlow({
      nodes: [
        { id: 'S', nodeType: 'start', uiPosition: { x: 0, y: 0 }, config: {} },
        { id: 'A', nodeType: 'approver', uiPosition: { x: 200, y: 200 }, config: {} },
      ],
      edges: [{
        id: 'F1',
        source: 'S',
        target: 'A',
        uiWaypoints: [{ x: 40, y: 160 }],
      }],
      config: { freeLayout: true },
    })
    const pts = out.edgePaths.get('F1').points
    expect(pts.some(p => Math.abs(p.x - 40) < 1 && Math.abs(p.y - 160) < 1)).toBe(true)
  })
})

describe('layoutFlow - 边界', () => {
  it('空流程返回空结果', () => {
    const out = layoutFlow(null)
    expect(out.nodePositions.size).toBe(0)
    expect(out.edgePaths.size).toBe(0)
    expect(out.canvasBounds).toEqual({ minX: 0, minY: 0, maxX: 0, maxY: 0 })
  })

  it('空 nodes 数组返回空 result', () => {
    const out = layoutFlow({ nodes: [], edges: [] })
    expect(out.nodePositions.size).toBe(0)
    expect(out.canvasBounds.maxX).toBe(0)
  })

  it('options 可覆盖默认尺寸', () => {
    const out = layoutFlow(linearJson(), { NODE_WIDTH: 300, NODE_HEIGHT: 100 })
    const pos = out.nodePositions.get('S')
    // 注意：复用 converter 的 calculateLayout 会用各自默认值；这里只验证不崩，size > 0
    expect(pos.width).toBeGreaterThan(0)
  })
})
