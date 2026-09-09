/**
 * @file 容器规范与树操作测试 — 验证嵌套结构完整性
 */

import { describe, it, expect } from 'vitest'
import {
  // 容器插槽
  getContainerSlot,
  isContainer,
  getMaxDepth,
  canAcceptChild,
  listContainerTypes,
  resolveContainerConfig,
  detectTabsMode,
  resolveNodeChildSlots,
  // 树操作
  walkTree,
  collectAllNodes,
  findNodeById,
  findNodePath,
  getNodeDepth,
  findParentNode,
  mapTree,
  insertNode,
  removeNode,
  moveNode,
  replaceNode,
  canInsertAtDepth,
  // 节点工厂
  createNode,
  createTabPane,
  createCollapseItem,
  createCol,
  createInitialChildren,
  createContainerNode,
} from '../index'

// ─── 容器插槽 ────────────────────────────────────────────

describe('Container Slot Spec', () => {
  it('should list all container types', () => {
    const types = listContainerTypes()
    expect(types).toContain('card')
    expect(types).toContain('tabs')
    expect(types).toContain('collapse')
    expect(types).toContain('grid')
    expect(types).toContain('grid-layout')
    expect(types).toContain('row')
    expect(types).toContain('table')
    expect(types).toContain('box')
    expect(types).toContain('box-layout')
    // 包装节点
    expect(types).toContain('tabPane')
    expect(types).toContain('collapseItem')
    expect(types).toContain('col')
    expect(types).toContain('tableCell')
  })

  it('isContainer should work', () => {
    expect(isContainer('card')).toBe(true)
    expect(isContainer('tabs')).toBe(true)
    expect(isContainer('divider')).toBe(false)
    expect(isContainer('spacer')).toBe(false)
  })

  it('getMaxDepth should return 4 by default', () => {
    expect(getMaxDepth('card')).toBe(4)
    expect(getMaxDepth('tabs')).toBe(4)
    expect(getMaxDepth('tabPane')).toBe(4)
  })

  it('canAcceptChild should validate', () => {
    // card 接受任意子组件
    expect(canAcceptChild('card', 'input')).toBe(true)
    expect(canAcceptChild('card', 'grid')).toBe(true)
    // tabs 只接受 tabPane
    expect(canAcceptChild('tabs', 'tabPane')).toBe(true)
    expect(canAcceptChild('tabs', 'input')).toBe(false)
    // collapse 只接受 collapseItem
    expect(canAcceptChild('collapse', 'collapseItem')).toBe(true)
    expect(canAcceptChild('collapse', 'card')).toBe(false)
    // 非容器不接受子组件
    expect(canAcceptChild('divider', 'input')).toBe(false)
  })

  it('getContainerSlot should return slot for known types', () => {
    expect(getContainerSlot('card')).not.toBeNull()
    expect(getContainerSlot('tabs')).not.toBeNull()
    expect(getContainerSlot('grid')).not.toBeNull()
    expect(getContainerSlot('grid-layout')).not.toBeNull()
  })

  it('detectTabsMode should distinguish form vs listpage', () => {
    // 表单模式：children 中有 tabPane
    const formNode = { children: [{ componentKey: 'tabPane', children: [] }] }
    expect(detectTabsMode(formNode)).toBe('form')
    // 列表页模式：props.tabs 有数据
    const listNode = { props: { tabs: [{ key: 'tab1', children: [] }] } }
    expect(detectTabsMode(listNode)).toBe('listpage')
  })

  it('resolveNodeChildSlots should find all children paths', () => {
    const node = {
      children: [{ id: 'c1' }],
      props: {
        tabs: [{ key: 't1', children: [{ id: 't1c1' }] }],
        cells: [{ key: 'cell1', children: [{ id: 'cell1c1' }] }],
      },
    }
    const slots = resolveNodeChildSlots(node)
    expect(slots.length).toBe(3) // children + tabs + cells
  })
})

// ─── 树操作 ──────────────────────────────────────────────

describe('Tree Operations', () => {
  // 构建测试树
  function buildTestTree() {
    return [
      {
        id: 'root1',
        componentKey: 'card',
        children: [
          { id: 'child1', componentKey: 'input', children: [] },
          { id: 'child2', componentKey: 'select', children: [] },
        ],
      },
      {
        id: 'root2',
        componentKey: 'tabs',
        props: {
          tabs: [
            { key: 'tab1', children: [{ id: 'tabChild1', componentKey: 'input', children: [] }] },
            { key: 'tab2', children: [] },
          ],
        },
        children: [],
      },
      {
        id: 'root3',
        componentKey: 'grid',
        props: {
          cells: [
            { key: 'cell1', children: [{ id: 'gridChild1', componentKey: 'card', children: [] }] },
            { key: 'cell2', children: [] },
          ],
        },
        children: [],
      },
    ]
  }

  it('walkTree should visit all nodes', () => {
    const tree = buildTestTree()
    const visited = []
    walkTree(tree, (node) => { visited.push(node.id) })
    expect(visited).toContain('root1')
    expect(visited).toContain('child1')
    expect(visited).toContain('child2')
    expect(visited).toContain('root2')
    expect(visited).toContain('tabChild1')
    expect(visited).toContain('root3')
    expect(visited).toContain('gridChild1')
    expect(visited.length).toBe(7)
  })

  it('collectAllNodes should flatten tree', () => {
    const tree = buildTestTree()
    const all = collectAllNodes(tree)
    expect(all.length).toBe(7)
  })

  it('findNodeById should work across all paths', () => {
    const tree = buildTestTree()
    expect(findNodeById(tree, 'child1')?.id).toBe('child1')
    expect(findNodeById(tree, 'tabChild1')?.id).toBe('tabChild1')
    expect(findNodeById(tree, 'gridChild1')?.id).toBe('gridChild1')
    expect(findNodeById(tree, 'nonexistent')).toBeNull()
  })

  it('findNodePath should return complete path', () => {
    const tree = buildTestTree()
    const path = findNodePath(tree, 'gridChild1')
    expect(path.length).toBe(2)
    expect(path[0].id).toBe('root3')
    expect(path[1].id).toBe('gridChild1')
  })

  it('getNodeDepth should return correct depth', () => {
    const tree = buildTestTree()
    expect(getNodeDepth(tree, 'root1')).toBe(0)
    expect(getNodeDepth(tree, 'child1')).toBe(1)
    expect(getNodeDepth(tree, 'tabChild1')).toBe(1)
    expect(getNodeDepth(tree, 'gridChild1')).toBe(1)
  })

  it('findParentNode should return parent', () => {
    const tree = buildTestTree()
    const parent = findParentNode(tree, 'child1')
    expect(parent?.id).toBe('root1')
    const tabParent = findParentNode(tree, 'tabChild1')
    expect(tabParent?.id).toBe('root2')
  })

  it('insertNode should add to children array', () => {
    const tree = buildTestTree()
    const newNode = { id: 'newChild', componentKey: 'input', children: [] }
    const newTree = insertNode(tree, 'root1', newNode)
    const root1 = findNodeById(newTree, 'root1')
    expect(root1.children.length).toBe(3)
    expect(root1.children[2].id).toBe('newChild')
  })

  it('insertNode should add to props.tabs', () => {
    const tree = buildTestTree()
    const newNode = { id: 'newTabChild', componentKey: 'input', children: [] }
    const newTree = insertNode(tree, 'root2', newNode, { tabKey: 'tab2' })
    const root2 = findNodeById(newTree, 'root2')
    const tab2 = root2.props.tabs.find(t => t.key === 'tab2')
    expect(tab2.children.length).toBe(1)
    expect(tab2.children[0].id).toBe('newTabChild')
  })

  it('insertNode should add to props.cells', () => {
    const tree = buildTestTree()
    const newNode = { id: 'newCellChild', componentKey: 'input', children: [] }
    const newTree = insertNode(tree, 'root3', newNode, { cellKey: 'cell2' })
    const root3 = findNodeById(newTree, 'root3')
    const cell2 = root3.props.cells.find(c => c.key === 'cell2')
    expect(cell2.children.length).toBe(1)
    expect(cell2.children[0].id).toBe('newCellChild')
  })

  it('removeNode should remove from all paths', () => {
    const tree = buildTestTree()
    const newTree = removeNode(tree, 'child1')
    expect(findNodeById(newTree, 'child1')).toBeNull()
    expect(findNodeById(newTree, 'root1')).not.toBeNull()
    // 移除 tabs 内节点
    const newTree2 = removeNode(tree, 'tabChild1')
    expect(findNodeById(newTree2, 'tabChild1')).toBeNull()
    // 移除 cells 内节点
    const newTree3 = removeNode(tree, 'gridChild1')
    expect(findNodeById(newTree3, 'gridChild1')).toBeNull()
  })

  it('replaceNode should swap node', () => {
    const tree = buildTestTree()
    const replacement = { id: 'child1', componentKey: 'textarea', children: [] }
    const newTree = replaceNode(tree, 'child1', replacement)
    const node = findNodeById(newTree, 'child1')
    expect(node.componentKey).toBe('textarea')
  })

  it('mapTree should transform all nodes', () => {
    const tree = buildTestTree()
    const newTree = mapTree(tree, node => ({ ...node, _mapped: true }))
    const all = collectAllNodes(newTree)
    all.forEach(node => {
      expect(node._mapped).toBe(true)
    })
  })

  it('canInsertAtDepth should check depth limits', () => {
    const tree = buildTestTree()
    expect(canInsertAtDepth(tree, 'root1', 4)).toBe(true) // depth 0, can add
    expect(canInsertAtDepth(tree, 'child1', 4)).toBe(true) // depth 1, can add
    expect(canInsertAtDepth(tree, 'root1', 1)).toBe(false) // maxDepth 1, root is 0, child would be 1, limit is < 1
  })
})

// ─── 节点工厂 ────────────────────────────────────────────

describe('Node Factory', () => {
  it('createNode should create minimal node', () => {
    const node = createNode('input')
    expect(node.id).toBeDefined()
    expect(node.componentKey).toBe('input')
    expect(Array.isArray(node.children)).toBe(true)
  })

  it('createTabPane should create wrapper', () => {
    const pane = createTabPane(0, 'tabs1')
    expect(pane.componentKey).toBe('tabPane')
    expect(pane.props.label).toBe('标签 1')
    expect(Array.isArray(pane.children)).toBe(true)
  })

  it('createCollapseItem should create wrapper', () => {
    const item = createCollapseItem(0, 'collapse1')
    expect(item.componentKey).toBe('collapseItem')
    expect(item.props.title).toBe('分组 1')
  })

  it('createCol should create with span', () => {
    const col = createCol(0, 'row1', 6)
    expect(col.componentKey).toBe('col')
    expect(col.props.span).toBe(6)
  })

  it('createInitialChildren should produce correct structure for card', () => {
    const result = createInitialChildren('card')
    expect(result.children).toEqual([])
  })

  it('createInitialChildren should produce tabPanes for tabs', () => {
    const result = createInitialChildren('tabs', { tabCount: 2 })
    expect(result.children.length).toBe(2)
    expect(result.children[0].componentKey).toBe('tabPane')
  })

  it('createInitialChildren should produce cols for row', () => {
    const result = createInitialChildren('row', { columnCount: 4 })
    expect(result.children.length).toBe(4)
    expect(result.children[0].componentKey).toBe('col')
  })

  it('createInitialChildren should produce cells for grid', () => {
    const result = createInitialChildren('grid', { columnCount: 3 })
    expect(result.props.cells.length).toBe(3)
    expect(result.children).toEqual([])
  })

  it('createContainerNode should create complete container', () => {
    const node = createContainerNode('tabs', { label: '我的标签页' })
    expect(node.componentKey).toBe('tabs')
    expect(node.label).toBe('我的标签页')
    expect(node.children.length).toBe(1) // 默认1个 tabPane
    expect(node.children[0].componentKey).toBe('tabPane')
  })
})
