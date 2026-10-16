import { describe, expect, it } from 'vitest'
import { framesHorizontallyOverlap, resolveVerticalPushY } from '../listPageFramePush'

describe('listPageFramePush', () => {
  it('detects horizontal overlap', () => {
    expect(framesHorizontallyOverlap(
      { x: 0, width: 100 },
      { x: 80, width: 100 },
    )).toBe(true)
    expect(framesHorizontallyOverlap(
      { x: 0, width: 100 },
      { x: 100, width: 100 },
    )).toBe(false)
  })

  it('pushes lower full-width blocks when upper height grows into them', () => {
    const pushed = resolveVerticalPushY([
      { id: 'a', x: 0, y: 0, width: 1200, height: 640 },
      { id: 'b', x: 0, y: 640, width: 1200, height: 72 },
      { id: 'c', x: 0, y: 720, width: 1200, height: 72 },
    ], 12)

    expect(pushed.get('a')).toBe(0)
    expect(pushed.get('b')).toBe(652)
    expect(pushed.get('c')).toBe(736)
  })

  it('resolves existing overlaps without moving the top block', () => {
    const pushed = resolveVerticalPushY([
      { id: 'metrics', x: 0, y: 2360, width: 1200, height: 200 },
      { id: 'button', x: 0, y: 2480, width: 220, height: 32 },
      { id: 'group', x: 0, y: 2520, width: 500, height: 72 },
    ], 12)

    expect(pushed.get('metrics')).toBe(2360)
    expect(pushed.get('button')).toBe(2572)
    expect(pushed.get('group')).toBe(2616)
  })

  it('keeps side-by-side blocks that do not overlap horizontally', () => {
    const pushed = resolveVerticalPushY([
      { id: 'left', x: 0, y: 0, width: 400, height: 100 },
      { id: 'right', x: 420, y: 0, width: 400, height: 100 },
    ], 12)

    expect(pushed.get('left')).toBe(0)
    expect(pushed.get('right')).toBe(0)
  })
})
