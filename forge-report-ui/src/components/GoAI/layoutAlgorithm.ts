import { AIComponentSchema } from '@/api/ai/ai.d'

/**
 * 自动布局算法
 * 当 AI 返回的组件不含位置信息时，提供兜底自动布局
 * 策略：网格划分，将画布划分为 cols 列，依次填入组件
 */

interface LayoutCell {
  x: number
  y: number
  w: number
  h: number
  occupied: boolean
}

export function autoLayout(
  components: AIComponentSchema[],
  canvasW: number,
  canvasH: number
): AIComponentSchema[] {
  const padding = 40
  const gap = 20

  // 顶部区域留给标题（约 80px）
  const headerHeight = 80
  const contentTop = padding + headerHeight
  const contentHeight = canvasH - contentTop - padding
  const contentWidth = canvasW - padding * 2

  // 根据组件数量决定列数
  const count = components.length
  let cols = 3
  if (count <= 4) cols = 2
  if (count > 9) cols = 4

  const cellWidth = (contentWidth - gap * (cols - 1)) / cols
  // 自动计算行数
  const rows = Math.ceil(count / cols)
  const cellHeight = (contentHeight - gap * (rows - 1)) / rows

  const result: AIComponentSchema[] = []
  let colIndex = 0
  let rowIndex = 0

  for (let i = 0; i < components.length; i++) {
    const comp = { ...components[i] }

    // 如果已有位置信息则保留
    if (comp.x !== undefined && comp.y !== undefined && comp.x > 0 && comp.y > 0) {
      result.push(comp)
      continue
    }

    // 计算网格位置
    const x = padding + colIndex * (cellWidth + gap)
    const y = contentTop + rowIndex * (cellHeight + gap)

    comp.x = Math.round(x)
    comp.y = Math.round(y)
    comp.w = Math.round(cellWidth)
    comp.h = Math.round(cellHeight)

    result.push(comp)

    colIndex++
    if (colIndex >= cols) {
      colIndex = 0
      rowIndex++
    }
  }

  return result
}

/**
 * 标题布局：将标题组件居中放置在顶部
 */
export function layoutHeader(
  titleComp: AIComponentSchema,
  canvasW: number
): AIComponentSchema {
  const result = { ...titleComp }
  result.y = 20
  result.x = Math.round((canvasW - (result.w || 700)) / 2)
  result.h = result.h || 60
  result.w = result.w || 700
  return result
}
