import { describe, expect, it } from 'vitest'
import { findScrollParent, keepChildInHorizontalView, scrollElementIntoScroller } from '../form-section-scroll'

describe('form-section-scroll', () => {
  it('在最近的 overflow 容器内滚动，不带动分组条回顶', () => {
    const scroller = document.createElement('div')
    Object.assign(scroller.style, { overflowY: 'auto', height: '80px' })
    const spacer = document.createElement('div')
    spacer.style.height = '200px'
    const target = document.createElement('div')
    target.style.height = '40px'
    scroller.append(spacer, target)
    document.body.appendChild(scroller)
    Object.defineProperty(scroller, 'clientHeight', { value: 80 })
    Object.defineProperty(scroller, 'scrollHeight', { value: 240 })
    scroller.scrollTop = 0
    scroller.getBoundingClientRect = () => ({ top: 0, left: 0, bottom: 80, right: 100 })
    target.getBoundingClientRect = () => ({ top: 200, left: 0, bottom: 240, right: 100 })
    scroller.scrollTo = ({ top }) => {
      scroller.scrollTop = top
    }

    expect(findScrollParent(target)).toBe(scroller)
    expect(scrollElementIntoScroller(target, 4)).toBe(true)
    expect(scroller.scrollTop).toBe(196)
    scroller.remove()
  })

  it('只平移横向分组条，不调用 scrollIntoView', () => {
    const nav = document.createElement('div')
    const chip = document.createElement('button')
    nav.appendChild(chip)
    Object.defineProperty(nav, 'clientWidth', { value: 80 })
    Object.defineProperty(chip, 'offsetLeft', { value: 120 })
    Object.defineProperty(chip, 'offsetWidth', { value: 40 })
    nav.scrollLeft = 0
    keepChildInHorizontalView(nav, chip)
    expect(nav.scrollLeft).toBe(88)
  })
})
