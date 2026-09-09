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
