import { describe, expect, it } from 'vitest'
import { MAX_DRAG_IMAGE_WIDTH, MIN_DRAG_IMAGE_WIDTH, resolveDragPreviewFitScale } from '../designerDragState'

describe('resolveDragPreviewFitScale', () => {
  it('keeps scale 1 when the dragged component fits within the global cap', () => {
    expect(resolveDragPreviewFitScale(200)).toBe(1)
    expect(resolveDragPreviewFitScale(MAX_DRAG_IMAGE_WIDTH)).toBe(1)
  })

  it('caps wide drag images to the global max width', () => {
    expect(resolveDragPreviewFitScale(640)).toBe(0.5)
    expect(resolveDragPreviewFitScale(960)).toBeCloseTo(MAX_DRAG_IMAGE_WIDTH / 960)
  })

  it('clamps the shadow to a narrower hover target such as a grid column', () => {
    // 宽 600 的组件拖入 200 宽的栅格格子：影子贴合格子宽度，不再超出栅格
    expect(resolveDragPreviewFitScale(600, 200)).toBeCloseTo(200 / 600)
    // 目标比全局上限还窄时以目标为准
    expect(resolveDragPreviewFitScale(640, 160)).toBe(0.25)
  })

  it('never scales up beyond the original size', () => {
    expect(resolveDragPreviewFitScale(100, 400)).toBe(1)
  })

  it('keeps a minimum readable shadow width on very narrow targets', () => {
    // 目标 40 宽、组件 400 宽：保底 120 视觉宽，避免影子缩到无法辨认
    expect(resolveDragPreviewFitScale(400, 40)).toBeCloseTo(MIN_DRAG_IMAGE_WIDTH / 400)
  })

  it('ignores invalid target widths and falls back to the global cap only', () => {
    expect(resolveDragPreviewFitScale(640, 0)).toBe(0.5)
    expect(resolveDragPreviewFitScale(640, Number.NaN)).toBe(0.5)
    expect(resolveDragPreviewFitScale(640, -10)).toBe(0.5)
  })

  it('treats invalid base widths defensively', () => {
    expect(resolveDragPreviewFitScale(0)).toBe(1)
    expect(resolveDragPreviewFitScale(-5)).toBe(1)
    expect(resolveDragPreviewFitScale(Number.NaN, 200)).toBe(1)
  })

  it('always returns a scale within [0.1, 1]', () => {
    expect(resolveDragPreviewFitScale(10000, 1)).toBeGreaterThanOrEqual(0.1)
    expect(resolveDragPreviewFitScale(10000, 1)).toBeLessThanOrEqual(1)
  })
})
