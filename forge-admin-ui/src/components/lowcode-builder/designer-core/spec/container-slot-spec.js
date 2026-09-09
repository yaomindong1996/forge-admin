/**
 * @fileoverview 容器插槽规范
 * @description 定义每种容器类型的子节点存储结构。
 *   不同容器的 children 存储位置不同：
 *     - 通用容器（card/box/space）：直接 children 数组
 *     - Tabs 容器：children[tabPane].children（中间有 tabPane 层）
 *     - Grid 容器：props.cells[].children（cells 中每格有独立 children）
 *     - Collapse 容器：children[collapseItem].children（中间有 collapseItem 层）
 *     - Table 容器：children[tableCell].children（中间有 tableCell 层）
 *
 *   每个 slot 定义包含：
 *     - slotKey：插槽标识
 *     - childrenPath：获取 children 数组的路径函数
 *     - setChildrenPath：设置 children 数组的路径函数
 *     - wrapperType：中间包装组件类型（如 tabPane / collapseItem）
 *     - createWrapper：创建包装组件的工厂函数
 */

import { getComponentSpec } from './registry'

// ─── 通用容器的 children 路径 ────────────────────────────────

/**
 * 直接 children 路径（card / box / space 等通用容器）
 */
const directChildrenSlot = {
  slotKey: 'children',
  /** 获取 children 数组 */
  getChildren(node) {
    return Array.isArray(node?.children) ? node.children : []
  },
  /** 设置 children 数组，返回新节点 */
  setChildren(node, children) {
    return { ...node, children }
  },
  /** 无中间包装层 */
  wrapperType: null,
  createWrapper: null,
}

// ─── Tabs 容器的 tabPane 路径 ────────────────────────────────

const tabsSlot = {
  slotKey: 'tabs',
  getChildren(node) {
    return Array.isArray(node?.children) ? node.children : []
  },
  setChildren(node, children) {
    return { ...node, children }
  },
  /** 每个 children 项是 tabPane 包装节点 */
  wrapperType: 'tabPane',
  /**
   * 获取 tabPane 内部的实际子节点
   * @param {Object} pane - tabPane 节点
   */
  getPaneChildren(pane) {
    return Array.isArray(pane?.children) ? pane.children : []
  },
  /**
   * 设置 tabPane 内部子节点
   */
  setPaneChildren(pane, children) {
    return { ...pane, children }
  },
  /** 创建新 tabPane 包装节点 */
  createWrapper(index, parentId) {
    const id = `${parentId || 'tabs'}_pane_${index + 1}`
    return {
      id,
      componentKey: 'tabPane',
      type: 'tabPane',
      label: `标签 ${index + 1}`,
      props: { label: `标签 ${index + 1}`, name: id },
      children: [],
    }
  },
}

// ─── Collapse 容器的 collapseItem 路径 ────────────────────────

const collapseSlot = {
  slotKey: 'collapse',
  getChildren(node) {
    return Array.isArray(node?.children) ? node.children : []
  },
  setChildren(node, children) {
    return { ...node, children }
  },
  wrapperType: 'collapseItem',
  getPaneChildren(item) {
    return Array.isArray(item?.children) ? item.children : []
  },
  setPaneChildren(item, children) {
    return { ...item, children }
  },
  createWrapper(index, parentId) {
    const id = `${parentId || 'collapse'}_item_${index + 1}`
    return {
      id,
      componentKey: 'collapseItem',
      type: 'collapseItem',
      label: `分组 ${index + 1}`,
      props: { title: `分组 ${index + 1}`, name: id },
      children: [],
    }
  },
}

// ─── Grid 容器的 cells 路径 ──────────────────────────────────

const gridSlot = {
  slotKey: 'grid-cells',
  /**
   * Grid 容器的 children 存储在 props.cells[].children 中
   */
  getChildren(node) {
    const cells = node?.props?.cells
    if (!Array.isArray(cells)) return []
    return cells.flatMap(cell => cell.children || [])
  },
  /**
   * 获取指定 cell 的 children
   * @param {Object} node - grid 节点
   * @param {string} cellKey - cell 的 key
   */
  getCellChildren(node, cellKey) {
    const cells = node?.props?.cells || []
    const cell = cells.find(c => c.key === cellKey)
    return cell ? (cell.children || []) : []
  },
  /**
   * 设置指定 cell 的 children
   */
  setCellChildren(node, cellKey, children) {
    const cells = (node?.props?.cells || []).map(cell =>
      cell.key === cellKey ? { ...cell, children } : cell,
    )
    return { ...node, props: { ...(node?.props || {}), cells } }
  },
  setChildren(node, _children) {
    // Grid 不直接操作 children，通过 cells 操作
    return node
  },
  /** 获取 cells 数组 */
  getCells(node) {
    return Array.isArray(node?.props?.cells) ? node.props.cells : []
  },
  /** 创建初始 cells 结构 */
  createInitialCells(count = 2, parentId) {
    return Array.from({ length: count }).map((_, index) => ({
      key: `cell_${index + 1}`,
      title: `栅格 ${index + 1}`,
      span: Math.max(1, Math.floor(24 / count)),
      children: [],
    }))
  },
  wrapperType: null,
  createWrapper: null,
}

// ─── Table 容器的 cell 路径 ──────────────────────────────────

const tableSlot = {
  slotKey: 'table-cells',
  getChildren(node) {
    return Array.isArray(node?.children) ? node.children : []
  },
  setChildren(node, children) {
    return { ...node, children }
  },
  wrapperType: 'tableCell',
  getPaneChildren(cell) {
    return Array.isArray(cell?.children) ? cell.children : []
  },
  setPaneChildren(cell, children) {
    return { ...cell, children }
  },
  createWrapper(index, parentId) {
    const id = `${parentId || 'table'}_cell_${index + 1}`
    return {
      id,
      componentKey: 'tableCell',
      type: 'tableCell',
      label: `单元格 ${index + 1}`,
      props: { span: 1 },
      children: [],
    }
  },
}

// ─── Row/Col 容器的 col 路径（表单设计器专用）─────────────────

const rowSlot = {
  slotKey: 'row-cols',
  getChildren(node) {
    return Array.isArray(node?.children) ? node.children : []
  },
  setChildren(node, children) {
    return { ...node, children }
  },
  wrapperType: 'col',
  getPaneChildren(col) {
    return Array.isArray(col?.children) ? col.children : []
  },
  setPaneChildren(col, children) {
    return { ...col, children }
  },
  createWrapper(index, parentId) {
    const id = `${parentId || 'row'}_col_${index + 1}`
    return {
      id,
      componentKey: 'col',
      type: 'col',
      label: `第 ${index + 1} 列`,
      props: { span: 6 },
      children: [],
    }
  },
}

// ─── 列表页 Tabs 变体：props.tabs[].children ───────────────

/**
 * 列表页设计器中 tabs 的子节点存储在 props.tabs[].children，
 * 而不是表单设计器的 children[tabPane].children。
 * 两种结构并存，tree-ops 需要同时处理。
 */
const listpageTabsSlot = {
  slotKey: 'listpage-tabs',
  /**
   * 获取所有 tab 内的子节点（扁平）
   */
  getChildren(node) {
    const tabs = node?.props?.tabs
    if (!Array.isArray(tabs)) return []
    return tabs.flatMap(tab => tab.children || [])
  },
  setChildren(node, _children) {
    // listpage tabs 不直接操作 children，通过 props.tabs 操作
    return node
  },
  /** 获取指定 tab 的 children */
  getTabChildren(node, tabKey) {
    const tabs = node?.props?.tabs || []
    const tab = tabs.find(t => t.key === tabKey)
    return tab ? (tab.children || []) : []
  },
  /** 设置指定 tab 的 children */
  setTabChildren(node, tabKey, children) {
    const tabs = (node?.props?.tabs || []).map(tab =>
      tab.key === tabKey ? { ...tab, children } : tab,
    )
    return { ...node, props: { ...(node?.props || {}), tabs } }
  },
  /** 获取 tabs 数组 */
  getTabs(node) {
    return Array.isArray(node?.props?.tabs) ? node.props.tabs : []
  },
  /** 创建初始 tabs 结构 */
  createInitialTabs(count = 1, parentId) {
    return Array.from({ length: count }).map((_, index) => ({
      key: `${parentId || 'tabs'}_tab_${index + 1}`,
      title: `标签 ${index + 1}`,
      children: [],
    }))
  },
  wrapperType: null,
  createWrapper: null,
}

// ─── 插槽注册表 ──────────────────────────────────────────────

/** @type {Map<string, Object>} containerType → slot definition */
const slotRegistry = new Map([
  // ── 通用直接 children 容器（表单 + 列表页共用）──
  ['card', directChildrenSlot],
  ['box', directChildrenSlot],
  ['box-layout', directChildrenSlot],
  ['space', directChildrenSlot],

  // ── 表单设计器容器（children 包装层模式）──
  ['tabs', tabsSlot],
  ['collapse', collapseSlot],
  ['table', tableSlot],
  ['row', rowSlot],

  // ── 列表页设计器容器（props 存储模式）──
  ['listpage-tabs', listpageTabsSlot],

  // ── 特殊结构容器 ──
  ['grid', gridSlot],
  ['grid-layout', gridSlot],

  // ── 包装节点（也是容器，接受任意子组件）──
  ['tabPane', directChildrenSlot],
  ['collapseItem', directChildrenSlot],
  ['col', directChildrenSlot],
  ['tableCell', directChildrenSlot],
])

/**
 * 获取容器类型的插槽定义
 * @param {string} containerType - 组件 type（支持别名解析）
 * @returns {Object|null} slot definition
 */
export function getContainerSlot(containerType) {
  if (!containerType) return null
  // 先直接查
  if (slotRegistry.has(containerType)) {
    return slotRegistry.get(containerType)
  }
  // 通过注册表别名解析
  const spec = getComponentSpec(containerType)
  if (!spec) return null
  if (slotRegistry.has(spec.type)) {
    return slotRegistry.get(spec.type)
  }
  // 所有 container: true 的组件默认用 directChildren
  if (spec.container) {
    return directChildrenSlot
  }
  return null
}

/**
 * 检查组件类型是否为容器
 * @param {string} type
 * @returns {boolean}
 */
export function isContainer(type) {
  const spec = getComponentSpec(type)
  return spec?.container === true
}

/**
 * 获取容器的最大嵌套深度
 * @param {string} type
 * @returns {number}
 */
export function getMaxDepth(type) {
  const spec = getComponentSpec(type)
  return spec?.maxDepth ?? 4
}

/**
 * 检查父容器是否可以接受指定子组件类型
 * @param {string} parentType - 父容器 type
 * @param {string} childType - 子组件 type
 * @returns {boolean}
 */
export function canAcceptChild(parentType, childType) {
  const parentSpec = getComponentSpec(parentType)
  if (!parentSpec?.container) return false
  const accept = parentSpec.accept
  if (!accept || accept.length === 0) return true
  const childSpec = getComponentSpec(childType)
  const childKey = childSpec?.type || childType
  return accept.includes(childKey)
}

/**
 * 获取所有容器类型列表
 * @returns {string[]}
 */
export function listContainerTypes() {
  return Array.from(slotRegistry.keys())
}

/**
 * 获取容器的插槽定义和子节点创建信息
 * @param {string} containerType
 * @returns {{ slot: Object, hasWrapper: boolean, isCellBased: boolean, isPropsBased: boolean }}
 */
export function resolveContainerConfig(containerType) {
  const slot = getContainerSlot(containerType)
  if (!slot) return null
  return {
    slot,
    hasWrapper: !!slot.wrapperType,
    isCellBased: slot.slotKey === 'grid-cells',
    isPropsBased: slot.slotKey === 'listpage-tabs' || slot.slotKey === 'grid-cells',
  }
}

/**
 * 区分 tabs 容器的存储模式
 * @param {Object} node - 节点数据
 * @returns {'form'|'listpage'|'unknown'}
 *   form: children[tabPane].children
 *   listpage: props.tabs[].children
 */
export function detectTabsMode(node) {
  if (!node) return 'unknown'
  // 列表页：tabs 数据在 props.tabs
  if (Array.isArray(node.props?.tabs) && node.props.tabs.length > 0) return 'listpage'
  // 表单：tabs 数据在 children（tabPane 包装）
  if (Array.isArray(node.children) && node.children.some(c => c.componentKey === 'tabPane' || c.type === 'tabPane')) return 'form'
  // 默认按表单处理
  return 'form'
}

/**
 * 获取节点的「真实子节点列表」，统一三种存储路径。
 * 用于 tree-ops 遍历时获取所有子节点。
 * @param {Object} node
 * @returns {Array<{ slotKey: string, children: any[], context?: any }>}
 */
export function resolveNodeChildSlots(node) {
  if (!node || typeof node !== 'object') return []
  const results = []

  // 路径 1: children 数组（表单 tabs / collapse / row / table / card / box / 包装节点）
  if (Array.isArray(node.children) && node.children.length > 0) {
    results.push({ slotKey: 'children', children: node.children })
  }

  // 路径 2: props.tabs[].children（列表页 tabs）
  if (Array.isArray(node.props?.tabs)) {
    for (const tab of node.props.tabs) {
      if (Array.isArray(tab.children) && tab.children.length > 0) {
        results.push({ slotKey: 'props.tabs', children: tab.children, context: { tabKey: tab.key } })
      }
    }
  }

  // 路径 3: props.cells[].children（grid / grid-layout）
  if (Array.isArray(node.props?.cells)) {
    for (const cell of node.props.cells) {
      if (Array.isArray(cell.children) && cell.children.length > 0) {
        results.push({ slotKey: 'props.cells', children: cell.children, context: { cellKey: cell.key } })
      }
    }
  }

  return results
}
