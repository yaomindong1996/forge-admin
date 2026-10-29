import { describe, expect, it } from 'vitest'
import { computeAlignmentGuides } from '../alignment-guides.js'

describe('alignment-guides', () => {
  it('水平中心接近时给出竖向标线并吸附', () => {
    const dragging = { x: 203, y: 40, width: 200, height: 56 }
    const others = [{ id: 'A', x: 100, y: 200, width: 200, height: 56 }]
    const result = computeAlignmentGuides(dragging, others, 6)
    expect(result.snapX).toBe(200)
    expect(result.lines.some(line => line.orientation === 'v')).toBe(true)
  })

  it('竖直对齐时给出横向标线', () => {
    const dragging = { x: 40, y: 198, width: 200, height: 56 }
    const others = [{ id: 'A', x: 400, y: 200, width: 200, height: 56 }]
    const result = computeAlignmentGuides(dragging, others, 6)
    expect(result.snapY).toBe(200)
    expect(result.lines.some(line => line.orientation === 'h')).toBe(true)
  })

  it('偏差过大时不吸附', () => {
    const dragging = { x: 220, y: 40, width: 200, height: 56 }
    const others = [{ id: 'A', x: 100, y: 200, width: 200, height: 56 }]
    const result = computeAlignmentGuides(dragging, others, 6)
    expect(result.snapX).toBeNull()
    expect(result.lines).toHaveLength(0)
  })
})
