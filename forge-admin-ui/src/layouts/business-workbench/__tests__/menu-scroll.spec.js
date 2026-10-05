import { describe, expect, it, vi } from 'vitest'
import { scrollMenuWithWheel } from '../menu-scroll'

function wheel(overrides = {}) {
  return {
    deltaX: 0,
    deltaY: 60,
    deltaMode: 0,
    preventDefault: vi.fn(),
    currentTarget: { scrollWidth: 500, clientWidth: 200, scrollLeft: 0 },
    ...overrides,
  }
}
describe('工作台菜单滚轮', () => {
  it('普通鼠标纵向滚轮转为横向，保留左右方向并限制范围', () => {
    const event = wheel()
    expect(scrollMenuWithWheel(event)).toBe(true)
    expect(event.currentTarget.scrollLeft).toBe(60)
    event.deltaY = 1000
    scrollMenuWithWheel(event)
    expect(event.currentTarget.scrollLeft).toBe(300)
    event.deltaY = -1000
    scrollMenuWithWheel(event)
    expect(event.currentTarget.scrollLeft).toBe(0)
    expect(event.preventDefault).toHaveBeenCalledTimes(3)
  })
  it.each([0, 300])('横向边界 %s 不吞掉页面滚轮', (left) => {
    const event = wheel({ deltaY: left ? 60 : -60 })
    event.currentTarget.scrollLeft = left
    expect(scrollMenuWithWheel(event)).toBe(false)
    expect(event.preventDefault).not.toHaveBeenCalled()
  })
  it('没有溢出时保持页面默认滚动', () => {
    const event = wheel({ currentTarget: { scrollWidth: 200, clientWidth: 200, scrollLeft: 0 } })
    expect(scrollMenuWithWheel(event)).toBe(false)
    expect(event.preventDefault).not.toHaveBeenCalled()
  })
  it.each([{ ctrlKey: true }, { shiftKey: true }, { deltaX: 80 }])('缩放/原生横滚 %j 不重复转换', (patch) => {
    const event = wheel(patch)
    expect(scrollMenuWithWheel(event)).toBe(false)
    expect(event.currentTarget.scrollLeft).toBe(0)
    expect(event.preventDefault).not.toHaveBeenCalled()
  })
  it.each([[1, 32], [2, 300]])('deltaMode=%s 转换单位', (mode, expected) => {
    const event = wheel({ deltaMode: mode, deltaY: 2 })
    scrollMenuWithWheel(event)
    expect(event.currentTarget.scrollLeft).toBe(expected)
  })
})
