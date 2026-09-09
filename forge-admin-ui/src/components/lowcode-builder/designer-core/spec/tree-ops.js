/**
 * @fileoverview Schema 树操作工具
 * @description 统一的树形结构遍历、查找、插入、移动、删除操作。
 *   同时处理三种嵌套存储路径：
 *     1. node.children（表单 tabs / collapse / row / table / card / box / 包装节点）
 *     2. node.props.tabs[].children（列表页 tabs）
 *     3. node.props.cells[].children（grid / grid-layout）
 *
 *   所有操作都是不可变的（返回新对象），不修改原树。
 */

import { resolveNodeChildSlots } from './container-slot-spec'

// ─── 内部工具 ────────────────────────────────────────────────

/**
 * 获取节点的唯一标识字段。
 * 兼容 componentKey / blockType / type 三种命名。
 */
function nodeType(node) {
  return node?.componentKey || node?.blockType || node?.type || ''
}

/**
 * 浅拷贝节点（保留引用，不深拷贝 children/props）
 */
function cloneNode(node) {
  return { ...node }
}

// ─── 遍历 ────────────────────────────────────────────────────

/**
 * 深度优先遍历 schema 树。
 * 自动识别三种嵌套路径，递归遍历所有子节点。
 *
 * @param {Array} nodes - 顶层节点数组
 * @param {Function} visitor - 访问器 (node, depth, parent) => void | false
 *   返回 false 可中止该子树的继续遍历
 * @param {number} [depth=0] - 当前深度
 * @param {Object|null} [parent=null] - 父节点
 */
export function walkTree(nodes, visitor, depth = 0, parent = null) {
  if (!Array.isArray(nodes)) return
  for (const node of nodes) {
    if (!node || typeof node !== 'object') continue
    const result = visitor(node, depth, parent)
    if (result === false) continue
    // 递归 children
    if (Array.isArray(node.children)) {
      walkTree(node.children, visitor, depth + 1, node)
    }
    // 递归 props.tabs[].children
    if (Array.isArray(node.props?.tabs)) {
      for (const tab of node.props.tabs) {
        if (Array.isArray(tab.children)) {
          walkTree(tab.children, visitor, depth + 1, node)
        }
      }
    }
    // 递归 props.cells[].children
    if (Array.isArray(node.props?.cells)) {
      for (const cell of node.props.cells) {
        if (Array.isArray(cell.children)) {
          walkTree(cell.children, visitor, depth + 1, node)
        }
      }
    }
  }
}

/**
 * 收集树中所有节点（扁平列表）
 * @param {Array} nodes
 * @returns {Array}
 */
export function collectAllNodes(nodes) {
  const result = []
  walkTree(nodes, (node) => { result.push(node) })
  return result
}

/**
 * 收集树中所有节点的 ID
 * @param {Array} nodes
 * @returns {Set<string>}
 */
export function collectAllNodeIds(nodes) {
  const ids = new Set()
  walkTree(nodes, (node) => { if (node.id) ids.add(node.id) })
  return ids
}

// ─── 查找 ────────────────────────────────────────────────────

/**
 * 按 ID 查找节点
 * @param {Array} nodes
 * @param {string} id
 * @returns {Object|null}
 */
export function findNodeById(nodes, id) {
  if (!id) return null
  let found = null
  walkTree(nodes, (node) => {
    if (node.id === id) {
      found = node
      return false
    }
  })
  return found
}

/**
 * 查找节点路径（从根到目标节点的完整路径）
 * @param {Array} nodes
 * @param {string} id
 * @returns {Array} 路径数组 [root, ..., target]，未找到返回空数组
 */
export function findNodePath(nodes, id) {
  if (!id || !Array.isArray(nodes)) return []

  function search(list, path) {
    for (const node of list) {
      if (!node || typeof node !== 'object') continue
      const currentPath = [...path, node]
      if (node.id === id) return currentPath

      // 递归 children
      if (Array.isArray(node.children)) {
        const found = search(node.children, currentPath)
        if (found.length > 0) return found
      }
      // 递归 props.tabs[].children
      if (Array.isArray(node.props?.tabs)) {
        for (const tab of node.props.tabs) {
          if (Array.isArray(tab.children)) {
            const found = search(tab.children, currentPath)
            if (found.length > 0) return found
          }
        }
      }
      // 递归 props.cells[].children
      if (Array.isArray(node.props?.cells)) {
        for (const cell of node.props.cells) {
          if (Array.isArray(cell.children)) {
            const found = search(cell.children, currentPath)
            if (found.length > 0) return found
          }
        }
      }
    }
    return []
  }

  return search(nodes, [])
}

/**
 * 计算节点在树中的深度（根节点深度为 0）
 * @param {Array} nodes
 * @param {string} id
 * @returns {number} 未找到返回 -1
 */
export function getNodeDepth(nodes, id) {
  const path = findNodePath(nodes, id)
  return path.length > 0 ? path.length - 1 : -1
}

/**
 * 查找节点的直接父节点
 * @param {Array} nodes
 * @param {string} id
 * @returns {Object|null}
 */
export function findParentNode(nodes, id) {
  const path = findNodePath(nodes, id)
  return path.length >= 2 ? path[path.length - 2] : null
}

/**
 * 检查某个节点是否是另一个节点的祖先
 * @param {Array} nodes
 * @param {string} ancestorId
 * @param {string} descendantId
 * @returns {boolean}
 */
export function isAncestorOf(nodes, ancestorId, descendantId) {
  const path = findNodePath(nodes, descendantId)
  return path.some(node => node.id === ancestorId)
}

// ─── 不可变更新 ──────────────────────────────────────────────

/**
 * 递归 map 树中的每个节点（不可变）。
 * 同时处理三种嵌套路径。等价于 ListPageGridDesigner 中的 mapBlocksInTree。
 *
 * @param {Array} nodes
 * @param {Function} mapper - (node) => newNode
 * @returns {Array}
 */
export function mapTree(nodes, mapper) {
  if (!Array.isArray(nodes)) return []
  return nodes.map(node => {
    if (!node || typeof node !== 'object') return node
    let next = cloneNode(node)
    // 递归 children
    if (Array.isArray(next.children) && next.children.length > 0) {
      next = { ...next, children: mapTree(next.children, mapper) }
    }
    // 递归 props.tabs[].children
    if (Array.isArray(next.props?.tabs) && next.props.tabs.length > 0) {
      next = {
        ...next,
        props: {
          ...(next.props || {}),
          tabs: next.props.tabs.map(tab => ({
            ...tab,
            children: Array.isArray(tab.children) ? mapTree(tab.children, mapper) : tab.children,
          })),
        },
      }
    }
    // 递归 props.cells[].children
    if (Array.isArray(next.props?.cells) && next.props.cells.length > 0) {
      next = {
        ...next,
        props: {
          ...(next.props || {}),
          cells: next.props.cells.map(cell => ({
            ...cell,
            children: Array.isArray(cell.children) ? mapTree(cell.children, mapper) : cell.children,
          })),
        },
      }
    }
    return mapper(next)
  })
}

/**
 * 递归 map 兄弟节点列表（先 map 列表，再递归进入子树）。
 * 等价于 ListPageGridDesigner 中的 mapBlockSiblingsInTree。
 *
 * @param {Array} nodes
 * @param {Function} mapper - (siblingsArray) => newSiblingsArray
 * @returns {Array}
 */
export function mapSiblingsInTree(nodes, mapper) {
  if (!Array.isArray(nodes)) return []
  const mappedList = mapper(nodes)
  return mappedList.map(node => {
    if (!node || typeof node !== 'object') return node
    let next = cloneNode(node)
    if (Array.isArray(next.children) && next.children.length > 0) {
      next = { ...next, children: mapSiblingsInTree(next.children, mapper) }
    }
    if (Array.isArray(next.props?.tabs) && next.props.tabs.length > 0) {
      next = {
        ...next,
        props: {
          ...(next.props || {}),
          tabs: next.props.tabs.map(tab => ({
            ...tab,
            children: Array.isArray(tab.children) ? mapSiblingsInTree(tab.children, mapper) : tab.children,
          })),
        },
      }
    }
    if (Array.isArray(next.props?.cells) && next.props.cells.length > 0) {
      next = {
        ...next,
        props: {
          ...(next.props || {}),
          cells: next.props.cells.map(cell => ({
            ...cell,
            children: Array.isArray(cell.children) ? mapSiblingsInTree(cell.children, mapper) : cell.children,
          })),
        },
      }
    }
    return next
  })
}

// ─── 插入 ────────────────────────────────────────────────────

/**
 * 向指定父节点插入子节点。
 * 根据父节点类型自动选择正确的存储路径：
 *   - card/box/box-layout/tabPane/collapseItem/col → children 数组
 *   - tabs (form mode) → children 数组（tabPane 内）
 *   - tabs (listpage mode) → props.tabs[].children
 *   - grid / grid-layout → props.cells[].children
 *
 * @param {Array} tree - 顶层节点数组
 * @param {string} parentId - 目标父节点 ID
 * @param {Object} newNode - 要插入的节点
 * @param {Object} [options]
 * @param {number} [options.index=-1] - 插入位置（-1 为末尾）
 * @param {string} [options.tabKey] - 列表页 tabs 的目标 tab key
 * @param {string} [options.cellKey] - grid 的目标 cell key
 * @returns {Array} 新树
 */
export function insertNode(tree, parentId, newNode, options = {}) {
  const { index = -1, tabKey, cellKey } = options
  return mapTree(tree, (node) => {
    if (node.id !== parentId) return node
    return insertIntoNode(node, newNode, { index, tabKey, cellKey })
  })
}

/**
 * 向单个节点内插入子节点（不可变）
 */
function insertIntoNode(node, newNode, { index = -1, tabKey, cellKey }) {
  const type = nodeType(node)

  // grid / grid-layout → props.cells
  if (['grid', 'grid-layout'].includes(type) && Array.isArray(node.props?.cells)) {
    const cells = node.props.cells.map(cell => {
      const match = cell.key === cellKey || (!cellKey && cell === node.props.cells[0])
      if (!match) return cell
      const children = [...(cell.children || [])]
      const pos = index < 0 ? children.length : index
      children.splice(pos, 0, newNode)
      return { ...cell, children }
    })
    return { ...node, props: { ...(node.props || {}), cells } }
  }

  // 列表页 tabs → props.tabs
  if (type === 'tabs' && Array.isArray(node.props?.tabs) && node.props.tabs.length > 0) {
    const tabs = node.props.tabs.map((tab, i) => {
      const match = tab.key === tabKey || (!tabKey && i === 0)
      if (!match) return tab
      const children = [...(tab.children || [])]
      const pos = index < 0 ? children.length : index
      children.splice(pos, 0, newNode)
      return { ...tab, children }
    })
    return { ...node, props: { ...(node.props || {}), tabs } }
  }

  // 其余容器 → children 数组
  const children = [...(node.children || [])]
  const pos = index < 0 ? children.length : index
  children.splice(pos, 0, newNode)
  return { ...node, children }
}

// ─── 删除 ────────────────────────────────────────────────────

/**
 * 从树中删除指定 ID 的节点（不可变）。
 * 同时处理三种嵌套路径。
 *
 * @param {Array} tree
 * @param {string} nodeId
 * @returns {Array} 新树
 */
export function removeNode(tree, nodeId) {
  if (!nodeId) return tree
  return removeNodeRecursive(tree, nodeId)
}

function removeNodeRecursive(nodes, nodeId) {
  if (!Array.isArray(nodes)) return []
  return nodes
    .filter(node => node?.id !== nodeId)
    .map(node => {
      let next = cloneNode(node)
      if (Array.isArray(next.children) && next.children.length > 0) {
        next = { ...next, children: removeNodeRecursive(next.children, nodeId) }
      }
      if (Array.isArray(next.props?.tabs) && next.props.tabs.length > 0) {
        next = {
          ...next,
          props: {
            ...(next.props || {}),
            tabs: next.props.tabs.map(tab => ({
              ...tab,
              children: Array.isArray(tab.children) ? removeNodeRecursive(tab.children, nodeId) : tab.children,
            })),
          },
        }
      }
      if (Array.isArray(next.props?.cells) && next.props.cells.length > 0) {
        next = {
          ...next,
          props: {
            ...(next.props || {}),
            cells: next.props.cells.map(cell => ({
              ...cell,
              children: Array.isArray(cell.children) ? removeNodeRecursive(cell.children, nodeId) : cell.children,
            })),
          },
        }
      }
      return next
    })
}

// ─── 移动 ────────────────────────────────────────────────────

/**
 * 移动节点到新的父容器（不可变）。
 * 先删除，再插入。会检查循环引用。
 *
 * @param {Array} tree
 * @param {string} nodeId - 要移动的节点 ID
 * @param {string} targetParentId - 目标父节点 ID
 * @param {Object} [options]
 * @param {number} [options.index=-1]
 * @param {string} [options.tabKey]
 * @param {string} [options.cellKey]
 * @returns {Array} 新树
 */
export function moveNode(tree, nodeId, targetParentId, options = {}) {
  if (!nodeId || !targetParentId || nodeId === targetParentId) return tree
  // 防止循环：不能移动到自己的后代
  if (isAncestorOf(tree, nodeId, targetParentId)) return tree

  const source = findNodeById(tree, nodeId)
  if (!source) return tree

  const cleaned = removeNode(tree, nodeId)
  return insertNode(cleaned, targetParentId, source, options)
}

// ─── 替换 ────────────────────────────────────────────────────

/**
 * 替换树中指定 ID 的节点（不可变）
 * @param {Array} tree
 * @param {string} nodeId
 * @param {Object|Function} replacement - 新节点对象 或 (oldNode) => newNode
 * @returns {Array} 新树
 */
export function replaceNode(tree, nodeId, replacement) {
  if (!nodeId) return tree
  const replacer = typeof replacement === 'function' ? replacement : () => replacement
  return mapTree(tree, (node) => {
    if (node.id !== nodeId) return node
    return replacer(node)
  })
}

// ─── 深度检查 ────────────────────────────────────────────────

/**
 * 检查在指定父节点下添加子节点是否会超出 maxDepth 限制
 * @param {Array} tree
 * @param {string} parentId
 * @param {number} maxDepth
 * @returns {boolean} true = 可以添加（不超限）
 */
export function canInsertAtDepth(tree, parentId, maxDepth = 4) {
  const depth = getNodeDepth(tree, parentId)
  if (depth < 0) return false
  return depth + 1 < maxDepth
}

/**
 * 计算树的最大深度
 * @param {Array} nodes
 * @returns {number}
 */
export function getMaxTreeDepth(nodes) {
  let max = 0
  walkTree(nodes, (_node, depth) => {
    if (depth > max) max = depth
  })
  return max
}
