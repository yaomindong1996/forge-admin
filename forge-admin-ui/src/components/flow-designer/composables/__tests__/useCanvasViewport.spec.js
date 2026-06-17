import { describe, expect, it } from 'vitest'
import { useCanvasViewport } from '../useCanvasViewport.js'

describe('useCanvasViewport - 缩放', () => {
  it('zoomIn / zoomOut 步长 0.1', () => {
    const v = useCanvasViewport()
    expect(v.scale.value).toBe(1)
    v.zoomIn()
    expect(v.scale.value).toBeCloseTo(1.1, 5)
    v.zoomOut()
    v.zoomOut()
    expect(v.scale.value).toBeCloseTo(0.9, 5)
  })

  it('受 minScale / maxScale 限制', () => {
    const v = useCanvasViewport({ minScale: 0.5, maxScale: 1.5 })
    for (let i = 0; i < 20; i += 1) v.zoomIn()
    expect(v.scale.value).toBe(1.5)
    for (let i = 0; i < 20; i += 1) v.zoomOut()
    expect(v.scale.value).toBe(0.5)
  })

  it('setScale 以指定锚点缩放，锚点屏幕位置不变', () => {
    const v = useCanvasViewport()
    // 初始 scale=1, translate=0,0；屏幕(100,100) → 画布(100,100)
    const beforeCanvas = v.screenToCanvas(100, 100)
    v.setScale(2, 100, 100)
    // 锚点屏幕坐标在缩放后仍对应原画布坐标
    const afterCanvas = v.screenToCanvas(100, 100)
    expect(afterCanvas.x).toBeCloseTo(beforeCanvas.x, 5)
    expect(afterCanvas.y).toBeCloseTo(beforeCanvas.y, 5)
  })

  it('scalePercent 为整数百分比', () => {
    const v = useCanvasViewport()
    expect(v.scalePercent.value).toBe(100)
    v.setScale(0.55)
    expect(v.scalePercent.value).toBe(55)
  })
})

describe('useCanvasViewport - 平移', () => {
  it('pan 累加偏移', () => {
    const v = useCanvasViewport()
    v.pan(10, 20)
    v.pan(5, -3)
    expect(v.translateX.value).toBe(15)
    expect(v.translateY.value).toBe(17)
  })

  it('resetView 回到初始状态', () => {
    const v = useCanvasViewport({ initialX: 100, initialY: 50, initialScale: 0.8 })
    v.pan(99, 99)
    v.zoomIn()
    v.resetView()
    expect(v.translateX.value).toBe(100)
    expect(v.translateY.value).toBe(50)
    expect(v.scale.value).toBe(0.8)
  })
})

describe('useCanvasViewport - 坐标转换', () => {
  it('screenToCanvas 与 canvasToScreen 互逆', () => {
    const v = useCanvasViewport()
    v.pan(50, 30)
    v.setScale(1.5)
    const c = v.screenToCanvas(200, 200)
    const s = v.canvasToScreen(c.x, c.y)
    expect(s.x).toBeCloseTo(200, 5)
    expect(s.y).toBeCloseTo(200, 5)
  })
})

describe('useCanvasViewport - fitToScreen', () => {
  it('内容居中并按比例缩放', () => {
    const v = useCanvasViewport()
    v.fitToScreen(800, 600, 400, 400, 0)
    // min(400/800, 400/600) = 0.5
    expect(v.scale.value).toBe(0.5)
    // 缩放后内容 800*0.5=400 / 600*0.5=300，在 400x400 视口居中 → (0, 50)
    expect(v.translateX.value).toBe(0)
    expect(v.translateY.value).toBe(50)
  })

  it('内容比视口小时不放大（最大 1）', () => {
    const v = useCanvasViewport()
    v.fitToScreen(100, 100, 800, 600, 0)
    expect(v.scale.value).toBe(1)
  })

  it('零尺寸入参不报错', () => {
    const v = useCanvasViewport()
    v.fitToScreen(0, 0, 0, 0)
    expect(v.scale.value).toBe(1)
  })
})

describe('useCanvasViewport - transformStyle', () => {
  it('生成 transform CSS 字符串', () => {
    const v = useCanvasViewport()
    v.pan(10, 20)
    v.setScale(1.5)
    expect(v.transformStyle.value).toContain('translate(10px, 20px)')
    expect(v.transformStyle.value).toContain('scale(1.5)')
    expect(v.transformStyle.value).toContain('transform-origin: 0 0')
  })
})
