import { describe, expect, it } from 'vitest'
import {
  findNearestSegment,
  insertBendAt,
  listCornerBendIndexes,
  listSegmentMidHandles,
  moveOrthoSegment,
  pinHoverOnSegment,
  projectHoverOnOrthoSegment,
  simplifyOrthoPoints,
  toStoredWaypoints,
} from '../edge-segment-edit.js'

describe('edge-segment-edit', () => {
  it('找最近水平线段', () => {
    const pts = [
      { x: 0, y: 0 },
      { x: 0, y: 40 },
      { x: 80, y: 40 },
      { x: 80, y: 100 },
    ]
    const seg = findNearestSegment(pts, 40, 42, 20)
    expect(seg.index).toBe(1)
    expect(seg.horizontal).toBe(true)
  })

  it('平移中间水平段', () => {
    const pts = [
      { x: 0, y: 0 },
      { x: 0, y: 40 },
      { x: 80, y: 40 },
      { x: 80, y: 100 },
    ]
    const next = moveOrthoSegment(pts, 1, { x: 40, y: 70 })
    expect(next[1].y).toBe(70)
    expect(next[2].y).toBe(70)
  })

  it('拖靠近起点的竖直段会插入拐点', () => {
    const pts = [
      { x: 10, y: 0 },
      { x: 10, y: 50 },
      { x: 90, y: 50 },
    ]
    const next = moveOrthoSegment(pts, 0, { x: 40, y: 20 })
    expect(next.length).toBeGreaterThan(pts.length)
    expect(toStoredWaypoints(next).length).toBeGreaterThan(0)
  })

  it('反复拖首段不会爆点', () => {
    let pts = [
      { x: 10, y: 0 },
      { x: 10, y: 50 },
      { x: 90, y: 50 },
    ]
    for (let y = 20; y <= 40; y += 2)
      pts = moveOrthoSegment(pts, 0, { x: 40, y })
    expect(pts.length).toBeLessThanOrEqual(5)
    expect(toStoredWaypoints(pts).length).toBeLessThanOrEqual(3)
  })

  it('simplify 去掉共线点', () => {
    const pts = [
      { x: 0, y: 0 },
      { x: 0, y: 20 },
      { x: 0, y: 40 },
      { x: 40, y: 40 },
    ]
    const out = simplifyOrthoPoints(pts)
    expect(out).toEqual([
      { x: 0, y: 0 },
      { x: 0, y: 40 },
      { x: 40, y: 40 },
    ])
  })

  it('只列出方向转折点', () => {
    const pts = [
      { x: 0, y: 0 },
      { x: 0, y: 40 },
      { x: 80, y: 40 },
      { x: 80, y: 100 },
    ]
    expect(listCornerBendIndexes(pts)).toEqual([1, 2])
  })

  it('横竖段给出中点胶囊', () => {
    const pts = [
      { x: 0, y: 0 },
      { x: 0, y: 80 },
      { x: 100, y: 80 },
    ]
    const mids = listSegmentMidHandles(pts)
    expect(mids.length).toBe(2)
    expect(mids[0].horizontal).toBe(false)
    expect(mids[1].horizontal).toBe(true)
  })

  it('点线上可插入拐点并保留', () => {
    const pts = [
      { x: 0, y: 0 },
      { x: 0, y: 100 },
    ]
    const { points, bendIndex } = insertBendAt(pts, 0, 40, 20)
    expect(bendIndex).toBe(0)
    expect(points.length).toBe(3)
    expect(toStoredWaypoints(points).length).toBe(1)
  })

  it('悬停蓝块投影到横段并跟手', () => {
    const pts = [
      { x: 0, y: 40 },
      { x: 100, y: 40 },
    ]
    const h = projectHoverOnOrthoSegment(pts, 35, 42, 20)
    expect(h).toBeTruthy()
    expect(h.horizontal).toBe(true)
    expect(h.x).toBeGreaterThan(20)
    expect(h.x).toBeLessThan(50)
    expect(Math.abs(h.y - 40)).toBeLessThan(1)
  })

  it('拖段后面钉在线上，y/x 随折点更新', () => {
    const before = [
      { x: 0, y: 40 },
      { x: 100, y: 40 },
      { x: 100, y: 120 },
    ]
    const after = [
      { x: 0, y: 70 },
      { x: 100, y: 70 },
      { x: 100, y: 120 },
    ]
    const pinned = pinHoverOnSegment(after, 0, 0.4, true)
    expect(pinned).toBeTruthy()
    expect(Math.abs(pinned.y - 70)).toBeLessThan(1)
    expect(pinHoverOnSegment(before, 0, 0.5, true).y).toBe(40)
  })

  it('斜线段不出蓝块，避免抢走拐点', () => {
    const diagonal = [
      { x: 0, y: 0 },
      { x: 100, y: 80 },
    ]
    expect(projectHoverOnOrthoSegment(diagonal, 50, 40, 20)).toBeNull()

    // 邻近有横段时，落在斜线上仍不应吸附到横段蓝块
    const mixed = [
      { x: 0, y: 0 },
      { x: 80, y: 60 },
      { x: 160, y: 60 },
    ]
    expect(projectHoverOnOrthoSegment(mixed, 40, 30, 20)).toBeNull()
    const onHoriz = projectHoverOnOrthoSegment(mixed, 120, 60, 20)
    expect(onHoriz).toBeTruthy()
    expect(onHoriz.horizontal).toBe(true)
    expect(onHoriz.segIndex).toBe(1)
  })
})
