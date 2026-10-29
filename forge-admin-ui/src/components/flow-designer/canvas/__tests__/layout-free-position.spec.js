import { describe, expect, it } from 'vitest'
import { layoutDesignerFlow } from '../designer-layout.js'

describe('layoutDesignerFlow + uiPosition', () => {
  it('有手动坐标时覆盖自动布局位置', () => {
    const flow = {
      processId: 'P',
      nodes: [
        { id: 'S', nodeType: 'start', uiPosition: { x: 40, y: 60 }, config: {} },
        { id: 'E', nodeType: 'end', uiPosition: { x: 40, y: 260 }, config: {} },
      ],
      edges: [{ id: 'F1', source: 'S', target: 'E' }],
    }
    const layout = layoutDesignerFlow(flow)
    expect(layout.nodePositions.get('S')).toMatchObject({ x: 40, y: 60 })
    // 旧数据无 w/h 时按大卡中心对齐到紧凑结束点
    const end = layout.nodePositions.get('E')
    expect(end.width).toBe(20)
    expect(end.height).toBe(20)
    expect(end.x).toBeGreaterThan(40)
    expect(end.y).toBeGreaterThan(260)
    expect(layout.edgePaths.get('F1')?.points?.length).toBeGreaterThanOrEqual(2)
  })

  it('结束节点已写入紧凑 w/h 时保留拖拽坐标', () => {
    const flow = {
      processId: 'P',
      nodes: [
        { id: 'S', nodeType: 'start', uiPosition: { x: 40, y: 60 }, config: {} },
        { id: 'E', nodeType: 'end', uiPosition: { x: 88, y: 280, w: 20, h: 20 }, config: {} },
      ],
      edges: [{ id: 'F1', source: 'S', target: 'E' }],
    }
    const layout = layoutDesignerFlow(flow)
    expect(layout.nodePositions.get('E')).toMatchObject({ x: 88, y: 280, width: 20, height: 20 })
  })

  it('结束节点：历史 left dock / 绕底拐点渲染时仍落到顶心', () => {
    const flow = {
      processId: 'P',
      nodes: [
        { id: 'A', nodeType: 'approver', uiPosition: { x: 40, y: 60, w: 200, h: 56 }, config: {} },
        { id: 'E', nodeType: 'end', uiPosition: { x: 300, y: 78, w: 20, h: 20 }, config: {} },
      ],
      edges: [{
        id: 'F1',
        source: 'A',
        target: 'E',
        uiSourceDock: { side: 'right', t: 0.5 },
        uiTargetDock: { side: 'left', t: 0.5 },
        uiWaypoints: [{ x: 310, y: 120 }],
      }],
    }
    const layout = layoutDesignerFlow(flow)
    const end = layout.nodePositions.get('E')
    const pts = layout.edgePaths.get('F1').points
    const last = pts[pts.length - 1]
    expect(last.x).toBeCloseTo(end.x + end.width / 2, 5)
    expect(last.y).toBeCloseTo(end.y, 5)
    expect(pts[pts.length - 2].y).toBeLessThanOrEqual(end.y)
  })
})
