import { resolveListPageBlockMeta } from '@/components/lowcode-builder/page/page-schema'

/**
 * 页面流式画布的几何计算：默认尺寸、瀑布流 Y 坐标、碰撞整理与外层壳样式。
 * 均为纯函数，items（当前页面根级区块列表）由调用方传入。
 */

export function readPageBlockLength(value, fallback = 0) {
  const number = Number.parseFloat(String(value || fallback))
  return Number.isFinite(number) ? Math.round(number) : 0
}

export function resolveDefaultPageBlockHeight(block = {}) {
  const blockType = block.blockType || ''
  if (blockType === 'page-title')
    return 176
  if (['divider', 'custom-html'].includes(blockType))
    return 88
  if (['stats-strip', 'info-panel', 'AiForm'].includes(blockType))
    return 128
  if (['AiCrudPage', 'AiTable', 'data-table', 'search-form', 'toolbar'].includes(blockType))
    return 220
  return 116
}

export function resolveDefaultPageBlockYFromItems(items = [], index = 0) {
  return items
    .slice(0, Math.max(0, index))
    .reduce((top, item) => top + Number(item.props?.style?.pageFlowHeight || resolveDefaultPageBlockHeight(item)) + 16, 20)
}

export function resolvePageBlockFlowGeometry(block = {}, index = 0, items = []) {
  const style = block.props?.style || {}
  const meta = resolveListPageBlockMeta(block.blockType) || {}
  const x = Number.isFinite(Number(style.pageFlowX)) && Number(style.pageFlowX) >= 0
    ? Number(style.pageFlowX)
    : 24
  const y = Number.isFinite(Number(style.pageFlowY)) && Number(style.pageFlowY) >= 0
    ? Number(style.pageFlowY)
    : resolveDefaultPageBlockYFromItems(items, index)
  const widthMode = style.widthMode || 'full'
  const heightMode = style.heightMode || 'fixed'
  const explicitWidth = readPageBlockLength(style.pageFlowWidth) || readPageBlockLength(style.width)
  const explicitHeight = readPageBlockLength(style.pageFlowHeight) || readPageBlockLength(style.height)
  const width = widthMode === 'full'
    ? 100000
    : explicitWidth || Math.round((Math.min(12, Math.max(3, Number(meta.defaultW) || 6)) / 12) * 1200)
  const defaultHeight = resolveDefaultPageBlockHeight(block)
  const height = heightMode === 'auto'
    ? Math.min(explicitHeight || defaultHeight, 180)
    : heightMode === 'full'
      ? Math.max(explicitHeight || defaultHeight, 180)
      : explicitHeight || defaultHeight
  return { x, y, width, height, right: x + width, bottom: y + height }
}

/**
 * 根页面的组件是最终页面的布局，不是可重叠的自由画布。
 * 这里仅整理根级块；组合布局中的 children 继续由自己的容器布局管理。
 */
export function resolveRootPageBlockCollisions(items = [], changedBlockId = '') {
  const gap = 16
  const placed = []
  const sorted = items
    .map((block, index) => ({ block, index, geometry: resolvePageBlockFlowGeometry(block, index, items) }))
    .sort((left, right) => left.geometry.y - right.geometry.y || left.geometry.x - right.geometry.x || left.index - right.index)

  const resolvedById = new Map()
  sorted.forEach(({ block, geometry }) => {
    let nextY = geometry.y
    placed.forEach((previous) => {
      const horizontallyOverlapped = geometry.x < previous.right && geometry.right > previous.x
      const needsPushDown = horizontallyOverlapped && nextY < previous.bottom + gap
      if (needsPushDown)
        nextY = previous.bottom + gap
    })

    const resolved = { ...geometry, y: nextY, bottom: nextY + geometry.height }
    placed.push(resolved)
    if (nextY !== geometry.y || block.id === changedBlockId) {
      resolvedById.set(block.id, {
        ...block,
        props: {
          ...(block.props || {}),
          style: {
            ...(block.props?.style || {}),
            pageFlowY: Math.round(nextY),
          },
        },
      })
    }
  })

  return items.map(block => resolvedById.get(block.id) || block)
}

export function resolvePageBlockShellStyle(block = {}, items = []) {
  const meta = resolveListPageBlockMeta(block.blockType) || {}
  const style = block.props?.style || {}
  const customWidth = String(style.pageFlowWidth || '').trim()
  const customHeight = Number(style.pageFlowHeight)
  const customX = Number(style.pageFlowX)
  const customY = Number(style.pageFlowY)
  const widthMode = style.widthMode || 'full'
  const heightMode = style.heightMode || 'fixed'
  const frameWidth = readPageBlockLength(style.width)
  const frameHeight = readPageBlockLength(style.height)
  const index = items.findIndex(item => item.id === block.id)
  const position = {
    position: 'absolute',
    left: `${Number.isFinite(customX) && customX >= 0 ? customX : 24}px`,
    top: `${Number.isFinite(customY) && customY >= 0 ? customY : resolveDefaultPageBlockYFromItems(items, index)}px`,
    height: `${heightMode === 'auto' ? Math.min(frameHeight || resolveDefaultPageBlockHeight(block), 180) : customHeight > 0 ? customHeight : frameHeight > 0 ? frameHeight : resolveDefaultPageBlockHeight(block)}px`,
    textAlign: style.textAlign || block.props?.textAlign || block.props?.align || 'left',
  }
  if (heightMode === 'full') {
    position.height = 'auto'
    position.bottom = '24px'
  }
  // “填充容器”必须覆盖此前拖拽/固定宽度留下的 pageFlowWidth 和 X 偏移。
  // 否则组件虽已切到填充模式，运行预览仍会沿用旧的固定尺寸。
  if (widthMode === 'full')
    return { ...position, left: '24px', width: 'calc(100% - 48px)' }
  if (widthMode === 'auto')
    return { ...position, width: customWidth || `min(${Math.max(280, Math.min(560, frameWidth || 520))}px, calc(100% - 48px))` }
  if (widthMode === 'fixed' && frameWidth > 0)
    return { ...position, width: customWidth || `min(${frameWidth}px, calc(100% - 48px))` }
  const columns = Math.min(12, Math.max(3, Number(meta.defaultW) || 6))
  return {
    ...position,
    width: customWidth || `${Math.round((columns / 12) * 10000) / 100}%`,
  }
}
