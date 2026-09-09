import { ref } from 'vue'

export const designerDropKey = ref('')
export const designerDragSourceId = ref('')
export const designerDragPreviewComponent = ref(null)
export const designerDropError = ref('')

// 拖拽跟随影子的宽度钳制边界：全局上限避免宽组件影子铺满画布，
// 下限保证窄目标下影子仍可辨认
export const MAX_DRAG_IMAGE_WIDTH = 320
export const MIN_DRAG_IMAGE_WIDTH = 120

/**
 * 计算拖拽影子的等比缩放系数：
 * - 影子视觉宽度不超过全局上限（MAX_DRAG_IMAGE_WIDTH）；
 * - 悬停目标（如栅格格子）更窄时，影子钳制到目标宽度，避免视觉超出栅格；
 * - 目标过窄时保底最小视觉宽度，且绝不放大（fit ≤ 1）。
 *
 * @param {number} baseWidth 影子原始屏幕宽度（被拖组件的 getBoundingClientRect().width）
 * @param {number} targetWidth 悬停目标宽度，0/未传表示无目标（仅全局上限）
 * @returns {number} [0.1, 1] 区间内的缩放系数
 */
export function resolveDragPreviewFitScale(baseWidth = 0, targetWidth = 0) {
  const base = Math.max(1, Number(baseWidth) || 1)
  let fit = Math.min(1, MAX_DRAG_IMAGE_WIDTH / base)
  const target = Number(targetWidth)
  if (Number.isFinite(target) && target > 0)
    fit = Math.min(fit, target / base)
  fit = Math.max(fit, Math.min(1, MIN_DRAG_IMAGE_WIDTH / base))
  return Math.max(0.1, Math.min(1, Number.isFinite(fit) ? fit : 1))
}

let dropErrorTimer = null

export function setDesignerDropKey(key = '') {
  designerDropKey.value = key
}

export function clearDesignerDropKey() {
  designerDropKey.value = ''
}

export function setDesignerDropError(message = '') {
  designerDropError.value = message
  if (dropErrorTimer)
    clearTimeout(dropErrorTimer)
  if (message) {
    dropErrorTimer = setTimeout(() => {
      designerDropError.value = ''
      dropErrorTimer = null
    }, 1800)
  }
}

export function clearDesignerDropError() {
  if (dropErrorTimer) {
    clearTimeout(dropErrorTimer)
    dropErrorTimer = null
  }
  designerDropError.value = ''
}

export function setDesignerDragSource(sourceId = '') {
  designerDragSourceId.value = sourceId
}

export function clearDesignerDragSource() {
  designerDragSourceId.value = ''
}

export function setDesignerDragPreview(component = null) {
  designerDragPreviewComponent.value = component || null
}

export function clearDesignerDragPreview() {
  designerDragPreviewComponent.value = null
}
