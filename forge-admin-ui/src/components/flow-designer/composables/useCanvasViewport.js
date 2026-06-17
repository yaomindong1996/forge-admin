/**
 * useCanvasViewport — 画布缩放 / 平移 / 坐标转换
 *
 * 状态：
 *   scale       缩放比例（受 minScale / maxScale 限制）
 *   translateX  画布水平偏移（屏幕坐标系，未缩放）
 *   translateY  画布垂直偏移
 *
 * transformStyle 计算属性：'transform: translate(...) scale(...)'
 *
 * 关键 API：
 *   zoomIn() / zoomOut() / resetView()
 *   setScale(s, centerX, centerY)：以 (centerX, centerY) 屏幕坐标为锚点缩放（避免画布跳跃）
 *   pan(dx, dy)：相对偏移
 *   fitToScreen(contentW, contentH, viewportW, viewportH)：内容居中并缩放到适应
 *   screenToCanvas(x, y) / canvasToScreen(x, y)：屏幕 ↔ 画布坐标转换
 */

import { computed, ref } from 'vue'

const DEFAULT_OPTS = {
  minScale: 0.3,
  maxScale: 2.0,
  zoomStep: 0.1,
  initialScale: 1,
  initialX: 0,
  initialY: 0,
}

export function useCanvasViewport(options = {}) {
  const opts = { ...DEFAULT_OPTS, ...options }

  const scale = ref(opts.initialScale)
  const translateX = ref(opts.initialX)
  const translateY = ref(opts.initialY)

  const transformStyle = computed(
    () => `transform: translate(${translateX.value}px, ${translateY.value}px) scale(${scale.value}); transform-origin: 0 0;`,
  )

  const scalePercent = computed(() => Math.round(scale.value * 100))

  function clamp(v) {
    if (v < opts.minScale)
      return opts.minScale
    if (v > opts.maxScale)
      return opts.maxScale
    return v
  }

  /**
   * 以指定屏幕坐标为锚点缩放。
   * 公式（保持锚点屏幕位置不变）：
   *   newTranslate = anchor - (anchor - oldTranslate) * (newScale / oldScale)
   */
  function setScale(next, centerX = null, centerY = null) {
    const ns = clamp(Number(next) || scale.value)
    if (ns === scale.value)
      return
    if (centerX != null && centerY != null) {
      const factor = ns / scale.value
      translateX.value = centerX - (centerX - translateX.value) * factor
      translateY.value = centerY - (centerY - translateY.value) * factor
    }
    scale.value = ns
  }

  function zoomIn(centerX = null, centerY = null) {
    setScale(roundStep(scale.value + opts.zoomStep), centerX, centerY)
  }

  function zoomOut(centerX = null, centerY = null) {
    setScale(roundStep(scale.value - opts.zoomStep), centerX, centerY)
  }

  function pan(dx, dy) {
    translateX.value += Number(dx) || 0
    translateY.value += Number(dy) || 0
  }

  function resetView() {
    scale.value = opts.initialScale
    translateX.value = opts.initialX
    translateY.value = opts.initialY
  }

  function fitToScreen(contentW, contentH, viewportW, viewportH, padding = 40) {
    if (!contentW || !contentH || !viewportW || !viewportH)
      return
    const availW = Math.max(viewportW - padding * 2, 1)
    const availH = Math.max(viewportH - padding * 2, 1)
    const ns = clamp(Math.min(availW / contentW, availH / contentH, 1))
    scale.value = ns
    translateX.value = (viewportW - contentW * ns) / 2
    translateY.value = (viewportH - contentH * ns) / 2
  }

  function screenToCanvas(x, y) {
    return {
      x: (x - translateX.value) / scale.value,
      y: (y - translateY.value) / scale.value,
    }
  }

  function canvasToScreen(x, y) {
    return {
      x: x * scale.value + translateX.value,
      y: y * scale.value + translateY.value,
    }
  }

  return {
    scale,
    translateX,
    translateY,
    transformStyle,
    scalePercent,
    setScale,
    zoomIn,
    zoomOut,
    pan,
    resetView,
    fitToScreen,
    screenToCanvas,
    canvasToScreen,
  }
}

/** 浮点累积修正：保留 2 位小数避免出现 0.30000000000000004 */
function roundStep(v) {
  return Math.round(v * 100) / 100
}
