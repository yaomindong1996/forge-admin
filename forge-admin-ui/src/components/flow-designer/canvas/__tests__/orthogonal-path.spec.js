import { describe, expect, it } from 'vitest'
import { buildOrthogonalPath, buildPathWithWaypoints, recomputeBounds } from '../orthogonal-path.js'

describe('buildOrthogonalPath', () => {
  it('垂直对齐时走直线', () => {
    const path = buildOrthogonalPath(
      { x: 0, y: 0, width: 100, height: 40 },
      { x: 0, y: 100, width: 100, height: 40 },
    )
    expect(path.type).toBe('straight')
    expect(path.points).toHaveLength(2)
    expect(path.points[0].y).toBe(40)
    expect(path.points[1].y).toBe(100)
  })

  it('目标在左侧时从左侧端口对接，不往右绕', () => {
    const path = buildOrthogonalPath(
      { x: 300, y: 0, width: 100, height: 40 },
      { x: 0, y: 0, width: 100, height: 40 },
    )
    expect(path.points[0].x).toBe(300)
    expect(path.points[path.points.length - 1].x).toBe(100)
    const xs = path.points.map(p => p.x)
    expect(Math.max(...xs)).toBeLessThanOrEqual(300)
  })

  it('目标在右侧时从右侧端口对接', () => {
    const path = buildOrthogonalPath(
      { x: 0, y: 0, width: 100, height: 40 },
      { x: 300, y: 0, width: 100, height: 40 },
    )
    expect(path.points[0].x).toBe(100)
    expect(path.points[path.points.length - 1].x).toBe(300)
  })

  it('错位上下时走正交折线', () => {
    const path = buildOrthogonalPath(
      { x: 0, y: 0, width: 100, height: 40 },
      { x: 200, y: 120, width: 100, height: 40 },
    )
    expect(path.type).toBe('orthogonal')
    expect(path.points.length).toBeGreaterThan(2)
  })

  it('结束节点：旁侧齐平也接到顶心，不挂左边', () => {
    const end = { x: 200, y: 40, width: 20, height: 20 }
    const path = buildOrthogonalPath(
      { x: 0, y: 20, width: 100, height: 56 },
      end,
      {
        endTarget: true,
        sourceDock: { side: 'right', t: 0.5 },
        targetDock: { side: 'left', t: 0.5 },
      },
    )
    const last = path.points[path.points.length - 1]
    expect(last).toMatchObject({ x: 210, y: 40 })
    // 末段须从上方竖直落入；禁止绕到圆心下方再抬头
    const prev = path.points[path.points.length - 2]
    expect(prev.y).toBeLessThanOrEqual(end.y)
    expect(path.points.every(p => p.y <= end.y + end.height / 2)).toBe(true)
  })

  it('结束节点：绕底 waypoint 丢弃并重走上方', () => {
    const source = { x: 0, y: 0, width: 100, height: 40 }
    const end = { x: 200, y: 40, width: 20, height: 20 }
    const path = buildPathWithWaypoints(
      source,
      end,
      [{ x: 210, y: 80 }, { x: 210, y: 60 }],
      { endTarget: true, targetDock: { side: 'bottom', t: 0.5 } },
    )
    const last = path.points[path.points.length - 1]
    expect(last).toMatchObject({ x: 210, y: 40 })
    const prev = path.points[path.points.length - 2]
    expect(prev.y).toBeLessThanOrEqual(end.y)
    expect(path.points.some(p => p.y > end.y + end.height)).toBe(false)
  })
})

describe('recomputeBounds', () => {
  it('聚合节点与路径边界', () => {
    const nodes = new Map([['a', { x: 10, y: 20, width: 100, height: 40 }]])
    const edges = new Map([['e', { points: [{ x: 0, y: 0 }, { x: 300, y: 200 }] }]])
    expect(recomputeBounds(nodes, edges)).toEqual({
      minX: 0,
      minY: 0,
      maxX: 300,
      maxY: 200,
    })
  })
})
