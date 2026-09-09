/**
 * 画布区块树操作纯函数族（ListPageGridDesigner 与 listDesigner store 共用）。
 *
 * 区块树有三种嵌套容器：
 * - children：普通容器子级
 * - props.tabs[].children：Tabs 标签页子级
 * - props.cells[].children：网格单元格子级
 *
 * 本模块只做结构读写，不承载布局算法与组件状态。
 */

export function resolveNestedBlocks(block = {}) {
  const children = Array.isArray(block.children) ? block.children : []
  const tabChildren = (block.props?.tabs || []).flatMap(tab => Array.isArray(tab.children) ? tab.children : [])
  const cellChildren = (block.props?.cells || []).flatMap(cell => Array.isArray(cell.children) ? cell.children : [])
  return [...children, ...tabChildren, ...cellChildren]
}

export function findBlockInTree(list = [], id = '') {
  if (!id)
    return null
  for (const block of list || []) {
    if (block?.id === id)
      return block
    const nested = findBlockInTree(resolveNestedBlocks(block), id)
    if (nested)
      return nested
  }
  return null
}

export function collectBlocksInTree(block = {}) {
  if (!block?.id)
    return []
  return [block, ...resolveNestedBlocks(block).flatMap(child => collectBlocksInTree(child))]
}

export function mapBlocksInTree(list = [], mapper) {
  return (list || []).map(block => mapBlockInTree(block, mapper))
}

export function mapBlockInTree(block = {}, mapper) {
  let next = block
  if (Array.isArray(next.children) && next.children.length) {
    next = {
      ...next,
      children: mapBlocksInTree(next.children, mapper),
    }
  }
  if (Array.isArray(next.props?.tabs) && next.props.tabs.length) {
    next = {
      ...next,
      props: {
        ...(next.props || {}),
        tabs: next.props.tabs.map(tab => ({
          ...tab,
          children: mapBlocksInTree(tab.children || [], mapper),
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
          children: mapBlocksInTree(cell.children || [], mapper),
        })),
      },
    }
  }
  return mapper(next)
}

export function mapBlockSiblingsInTree(list = [], mapper) {
  const mappedList = mapper(list || [])
  return mappedList.map((block) => {
    let next = block
    if (Array.isArray(next.children) && next.children.length) {
      next = {
        ...next,
        children: mapBlockSiblingsInTree(next.children, mapper),
      }
    }
    if (Array.isArray(next.props?.tabs) && next.props.tabs.length) {
      next = {
        ...next,
        props: {
          ...(next.props || {}),
          tabs: next.props.tabs.map(tab => ({
            ...tab,
            children: mapBlockSiblingsInTree(tab.children || [], mapper),
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
            children: mapBlockSiblingsInTree(cell.children || [], mapper),
          })),
        },
      }
    }
    return next
  })
}

export function removeBlockFromTree(list = [], id = '') {
  return (list || [])
    .filter(block => block?.id !== id)
    .map((block) => {
      let next = block
      if (Array.isArray(next.children) && next.children.length) {
        next = {
          ...next,
          children: removeBlockFromTree(next.children, id),
        }
      }
      if (Array.isArray(next.props?.tabs) && next.props.tabs.length) {
        next = {
          ...next,
          props: {
            ...(next.props || {}),
            tabs: next.props.tabs.map(tab => ({
              ...tab,
              children: removeBlockFromTree(tab.children || [], id),
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
              children: removeBlockFromTree(cell.children || [], id),
            })),
          },
        }
      }
      return next
    })
}
