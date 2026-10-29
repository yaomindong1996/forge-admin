import { describe, expect, it } from 'vitest'
import { ARROW_SIZE, buildArrowPolygon, shortenPointsForArrow } from '../edge-arrow.js'

describe('edge-arrow', () => {
  it('缩短末段后终点停在箭头根部', () => {
    const pts = [
      { x: 0, y: 0 },
      { x: 0, y: 100 },
    ]
    const shortened = shortenPointsForArrow(pts, 10)
    expect(shortened[1].x).toBe(0)
    expect(shortened[1].y).toBe(90)
    expect(pts[1].y).toBe(100)
  })

  it('箭头 tip 仍在真实端点', () => {
    const poly = buildArrowPolygon([
      { x: 0, y: 0 },
      { x: 0, y: 100 },
    ], ARROW_SIZE)
    expect(poly.startsWith('0,100 ')).toBe(true)
  })
})
