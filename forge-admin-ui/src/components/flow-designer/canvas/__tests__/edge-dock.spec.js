import { describe, expect, it } from 'vitest'
import {
  END_INCOMING_DOCK,
  dockOnSideFromPoint,
  dockOnSidePreferCenter,
  facingCenterDocks,
  nearestDockOnBox,
  pickFreeTOnSide,
  pointFromDock,
  resolveDockAvoidingOccupied,
  resolveDockPoints,
} from '../edge-dock.js'

describe('edge-dock', () => {
  const box = { x: 100, y: 50, width: 200, height: 80 }

  it('右边中点', () => {
    const p = pointFromDock(box, { side: 'right', t: 0.5 })
    expect(p).toMatchObject({ x: 300, y: 90, side: 'right' })
  })

  it('投影到最近边（非中心）', () => {
    // 紧贴右边外侧、偏上 → right，t < 0.5
    const dock = nearestDockOnBox(box, 305, 70)
    expect(dock.side).toBe('right')
    expect(dock.t).toBeLessThan(0.5)
  })

  it('投影到顶边偏移位置', () => {
    const dock = nearestDockOnBox(box, 250, 10)
    expect(dock.side).toBe('top')
    expect(dock.t).toBeGreaterThan(0.5)
  })

  it('指定边按指针投影，不强制中心', () => {
    const dock = dockOnSideFromPoint(box, 'top', 140, 0)
    expect(dock.side).toBe('top')
    expect(dock.t).toBeCloseTo(0.2, 2)
  })

  it('靠近中心时吸到四边中心点', () => {
    const dock = dockOnSidePreferCenter(box, 'bottom', 180, 140)
    expect(dock).toEqual({ side: 'bottom', t: 0.5 })
  })

  it('源在上目标在下时对接底→顶中心，避免穿心', () => {
    const source = { x: 100, y: 40, width: 200, height: 56 }
    const target = { x: 100, y: 200, width: 200, height: 56 }
    expect(facingCenterDocks(source, target)).toEqual({
      sourceDock: { side: 'bottom', t: 0.5 },
      targetDock: { side: 'top', t: 0.5 },
    })
  })

  it('中心已被占用时选最近空闲 t', () => {
    expect(pickFreeTOnSide([0.5], 0.5)).not.toBe(0.5)
    expect(resolveDockAvoidingOccupied({ side: 'bottom', t: 0.5 }, [0.5])).toMatchObject({
      side: 'bottom',
    })
    expect(resolveDockAvoidingOccupied({ side: 'bottom', t: 0.5 }, [0.5]).t).toBe(0.35)
  })

  it('结束节点：facing / resolve 强制顶心，无视 left dock', () => {
    const source = { x: 0, y: 40, width: 100, height: 56 }
    const end = { x: 200, y: 50, width: 20, height: 20 }
    expect(facingCenterDocks(source, end, { endTarget: true }).targetDock).toEqual(END_INCOMING_DOCK)
    const { end: pt } = resolveDockPoints(
      source,
      end,
      { side: 'right', t: 0.5 },
      { side: 'left', t: 0.5 },
      { endTarget: true },
    )
    expect(pt).toMatchObject({ x: 210, y: 50, side: 'top' })
  })
})
