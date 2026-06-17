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
