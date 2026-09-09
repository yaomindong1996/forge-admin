/**
 * @fileoverview 容器初始子节点工厂 + 节点创建工具
 * @description 为每种容器类型生成初始 children 结构。
 *   参考 designerLayoutFactory.js 的 createForgeLayoutComponent，
 *   统一为规范化的节点工厂。
 */

// ─── ID 生成 ─────────────────────────────────────────────────

let _counter = 0

/**
 * 生成唯一节点 ID
 * @param {string} [prefix='node'] - ID 前缀
 * @returns {string}
 */
export function generateNodeId(prefix = 'node') {
  _counter += 1
  return `${prefix}_${Date.now()}_${_counter}`
}

/**
 * 重置计数器（测试用）
 */
export function resetNodeCounter() {
  _counter = 0
}

// ─── 基础节点创建 ────────────────────────────────────────────

/**
 * 创建一个最小可用节点
 * @param {string} type - 组件类型（componentKey / blockType）
 * @param {Object} [overrides] - 覆盖字段
 * @returns {Object}
 */
export function createNode(type, overrides = {}) {
  const id = overrides.id || generateNodeId(type)
  return {
    id,
    componentKey: type,
    type,
    label: overrides.label || type,
    props: overrides.props || {},
    layout: overrides.layout || { span: 1, align: 'left' },
    children: overrides.children !== undefined ? overrides.children : [],
    ...overrides,
  }
}

// ─── 包装节点创建 ────────────────────────────────────────────

/**
 * 创建 tabPane 包装节点
 */
export function createTabPane(index = 0, parentId) {
  const id = generateNodeId(`${parentId || 'tabs'}_pane`)
  return {
    id,
    componentKey: 'tabPane',
    type: 'tabPane',
    label: `标签 ${index + 1}`,
    props: { label: `标签 ${index + 1}`, name: id },
    layout: { span: 24, align: 'left' },
    children: [],
  }
}

/**
 * 创建 collapseItem 包装节点
 */
export function createCollapseItem(index = 0, parentId) {
  const id = generateNodeId(`${parentId || 'collapse'}_item`)
  return {
    id,
    componentKey: 'collapseItem',
    type: 'collapseItem',
    label: `分组 ${index + 1}`,
    props: { title: `分组 ${index + 1}`, name: id },
    layout: { span: 24, align: 'left' },
    children: [],
  }
}

/**
 * 创建 col 列包装节点
 */
export function createCol(index = 0, parentId, span = 6) {
  const id = generateNodeId(`${parentId || 'row'}_col`)
  return {
    id,
    componentKey: 'col',
    type: 'col',
    label: `第 ${index + 1} 列`,
    props: { span },
    layout: { span, align: 'left' },
    children: [],
  }
}

/**
 * 创建 tableCell / tableGrid 单元格包装节点
 */
export function createTableCell(index = 0, parentId) {
  const id = generateNodeId(`${parentId || 'table'}_cell`)
  return {
    id,
    componentKey: 'tableGrid',
    type: 'tableCell',
    label: `单元格 ${index + 1}`,
    props: { span: 1 },
    layout: { span: 1, align: 'left' },
    children: [],
  }
}

// ─── 列表页 tabs props 结构创建 ─────────────────────────────

/**
 * 创建列表页 tabs 的 props.tabs 结构
 */
export function createListpageTab(index = 0, parentId) {
  const key = `${parentId || 'tabs'}_tab_${index + 1}`
  return {
    key,
    title: `标签 ${index + 1}`,
    children: [],
  }
}

// ─── Grid cells 结构创建 ─────────────────────────────────────

/**
 * 创建 grid cells 数组
 */
export function createGridCells(count = 4, parentId) {
  const span = Math.max(1, Math.floor(24 / count))
  return Array.from({ length: count }).map((_, index) => ({
    key: `${parentId || 'grid'}_cell_${index + 1}`,
    title: `栅格 ${index + 1}`,
    span,
    children: [],
  }))
}

// ─── 容器初始子节点工厂 ──────────────────────────────────────

/**
 * 根据容器类型创建初始子节点结构。
 * 返回 { children, props } 覆盖对象，调用方将其合并到节点上。
 *
 * @param {string} containerType - 容器类型
 * @param {Object} [options]
 * @param {string} [options.parentId] - 父节点 ID（用于生成子节点 ID 前缀）
 * @param {number} [options.columnCount=4] - row / grid 的默认列数
 * @param {number} [options.cellCount=2] - table 的默认单元格数
 * @param {number} [options.tabCount=1] - tabs 的默认标签数
 * @returns {{ children: Array, props?: Object }}
 */
export function createInitialChildren(containerType, options = {}) {
  const { parentId, columnCount = 4, cellCount = 2, tabCount = 1 } = options
  const pid = parentId || generateNodeId(containerType)

  switch (containerType) {
    // ── 直接 children 容器 ──
    case 'card':
    case 'box':
    case 'box-layout':
    case 'space':
      return { children: [] }

    // ── Tabs（表单设计器：children 包装 tabPane）──
    case 'tabs':
      return {
        children: Array.from({ length: tabCount }).map((_, i) => createTabPane(i, pid)),
      }

    // ── 列表页 Tabs（props.tabs 结构）──
    case 'listpage-tabs':
      return {
        children: [],
        props: {
          tabs: Array.from({ length: tabCount }).map((_, i) => createListpageTab(i, pid)),
        },
      }

    // ── Collapse（children 包装 collapseItem）──
    case 'collapse':
      return {
        children: Array.from({ length: 1 }).map((_, i) => createCollapseItem(i, pid)),
      }

    // ── Row（children 包装 col）──
    case 'row':
      return {
        children: Array.from({ length: columnCount }).map((_, i) => {
          const span = Math.max(1, Math.floor(24 / columnCount))
          return createCol(i, pid, span)
        }),
      }

    // ── Table（children 包装 tableCell）──
    case 'table':
      return {
        children: Array.from({ length: cellCount }).map((_, i) => createTableCell(i, pid)),
      }

    // ── Grid / Grid-layout（props.cells 结构）──
    case 'grid':
    case 'grid-layout':
      return {
        children: [],
        props: {
          cells: createGridCells(columnCount, pid),
        },
      }

    // ── 包装节点（内部直接 children）──
    case 'tabPane':
    case 'collapseItem':
    case 'col':
    case 'tableCell':
    case 'tableGrid':
      return { children: [] }

    default:
      return { children: [] }
  }
}

/**
 * 创建完整的容器节点（含初始子结构）
 * @param {string} containerType
 * @param {Object} [overrides]
 * @returns {Object}
 */
export function createContainerNode(containerType, overrides = {}) {
  const id = overrides.id || generateNodeId(containerType)
  const initial = createInitialChildren(containerType, { ...overrides, parentId: id })
  return {
    id,
    componentKey: containerType,
    type: containerType,
    label: overrides.label || containerType,
    props: {
      ...(overrides.props || {}),
      ...(initial.props || {}),
    },
    layout: overrides.layout || { span: 24, align: 'left' },
    children: initial.children,
    ...overrides,
    // 确保 children 和 props 不被 overrides 覆盖
    ...(initial.children !== undefined ? { children: initial.children } : {}),
    ...(initial.props ? { props: { ...(overrides.props || {}), ...initial.props } } : {}),
  }
}
