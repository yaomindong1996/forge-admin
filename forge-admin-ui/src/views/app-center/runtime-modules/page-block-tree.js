/**
 * 页面区块树操作：根级 children、Tabs 页签、网格 cells 三个层级的统一遍历。
 * 这些函数均为纯数据操作，与组件状态解耦，供 application-runtime 与后续拆分的子模块共用。
 */

export function findPageBlockInTree(items = [], blockId = '') {
  for (const block of items || []) {
    if (block?.id === blockId)
      return block
    const nested = findPageBlockInTree(resolvePageBlockChildren(block), blockId)
    if (nested)
      return nested
  }
  return null
}

/**
 * 查找嵌套组件所在的栅格格子（用于左右锚点改列宽 span）。
 * @returns {{ gridId: string, cellKey: string, span: number, columns: number } | null}
 */
export function findGridCellHostForChild(items = [], blockId = '') {
  if (!blockId)
    return null
  let found = null
  visitPageBlocksInTree(items, (block) => {
    if (found || !['grid-layout', 'grid'].includes(block?.blockType))
      return
    const columns = Math.max(1, Number(block.props?.columns) || 24)
    const cells = Array.isArray(block.props?.cells) ? block.props.cells : []
    cells.forEach((cell) => {
      if (found)
        return
      const children = Array.isArray(cell?.children) ? cell.children : []
      if (!children.some(child => child?.id === blockId))
        return
      found = {
        gridId: block.id,
        cellKey: String(cell.key || ''),
        span: Math.max(1, Number(cell.span) || columns),
        columns,
      }
    })
  })
  return found
}

export function resolvePageBlockChildren(block = {}) {
  const children = Array.isArray(block.children) ? block.children : []
  const tabChildren = (block.props?.tabs || []).flatMap(tab => Array.isArray(tab.children) ? tab.children : [])
  const cellChildren = (block.props?.cells || []).flatMap(cell => Array.isArray(cell.children) ? cell.children : [])
  return [...children, ...tabChildren, ...cellChildren]
}

export function visitPageBlocksInTree(items = [], visitor) {
  ;(items || []).forEach((block) => {
    visitor(block)
    visitPageBlocksInTree(resolvePageBlockChildren(block), visitor)
  })
}

export function mapPageBlocksInTree(items = [], mapper) {
  return (items || []).map((block) => {
    let next = block
    if (Array.isArray(next.children) && next.children.length) {
      next = {
        ...next,
        children: mapPageBlocksInTree(next.children, mapper),
      }
    }
    if (Array.isArray(next.props?.tabs) && next.props.tabs.length) {
      next = {
        ...next,
        props: {
          ...(next.props || {}),
          tabs: next.props.tabs.map(tab => ({
            ...tab,
            children: mapPageBlocksInTree(tab.children || [], mapper),
          })),
        },
      }
    }
    if (Array.isArray(next.props?.cells) && next.props.cells.length) {
      next = {
        ...next,
        props: {
          ...(next.props || {}),
          cells: next.props.cells.map(cell => ({
            ...cell,
            children: mapPageBlocksInTree(cell.children || [], mapper),
          })),
        },
      }
    }
    return mapper(next)
  })
}

export function removePageBlockFromTree(items = [], blockId = '') {
  return (items || [])
    .filter(block => block?.id !== blockId)
    .map((block) => {
      let next = block
      if (Array.isArray(next.children)) {
        next = {
          ...next,
          children: removePageBlockFromTree(next.children, blockId),
        }
      }
      if (Array.isArray(next.props?.tabs)) {
        next = {
          ...next,
          props: {
            ...(next.props || {}),
            tabs: next.props.tabs.map(tab => ({
              ...tab,
              children: removePageBlockFromTree(tab.children || [], blockId),
            })),
          },
        }
      }
      if (Array.isArray(next.props?.cells)) {
        next = {
          ...next,
          props: {
            ...(next.props || {}),
            cells: next.props.cells.map(cell => ({
              ...cell,
              children: removePageBlockFromTree(cell.children || [], blockId),
            })),
          },
        }
      }
      return next
    })
}
