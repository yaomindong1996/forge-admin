import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import ForgeFormCanvasNode from '../ForgeFormCanvasNode.vue'

vi.mock('vue-router', () => ({
  useRoute: () => ({ query: {}, params: {}, path: '/', fullPath: '/', name: 'canvas-node-remove-test' }),
  useRouter: () => ({ push: vi.fn(), replace: vi.fn() }),
}))

const naiveStubs = {
  NDropdown: { name: 'NDropdown', props: ['options', 'trigger'], template: '<div class="n-dropdown-stub"><slot /></div>' },
  NCard: { name: 'NCard', props: ['title'], template: '<div class="n-card-stub"><slot name="header" /><slot /></div>' },
  NTabs: { name: 'NTabs', props: ['defaultValue'], template: '<div class="n-tabs-stub"><slot /></div>' },
  NTabPane: { name: 'NTabPane', props: ['name', 'tab'], template: '<div class="n-tab-pane-stub"><slot /></div>' },
  NCollapse: { name: 'NCollapse', props: ['expandedNames'], template: '<div class="n-collapse-stub"><slot /></div>' },
  NCollapseItem: { name: 'NCollapseItem', props: ['name', 'title'], template: '<div class="n-collapse-item-stub"><slot /></div>' },
  NEmpty: true,
}

const componentStubs = {
  AiFormItem: { name: 'AiFormItem', props: ['field'], template: '<div class="ai-form-item-stub">{{ field.label }}</div>' },
  AiFormGroupTitle: true,
  AiFormSectionTitle: true,
  AiCrudPage: true,
  PageWidgetRenderer: true,
}

function buildNestedSchema() {
  const field = {
    id: 'cmp_input_1',
    componentKey: 'input',
    label: '名称',
    fieldBinding: { mode: 'field', fieldCode: 'fieldInput1', locked: false },
    props: { placeholder: '请填写名称' },
    layout: { span: 1, align: 'left' },
    validation: { required: false },
    visibility: { hidden: false },
    children: [],
  }
  const row = {
    id: 'cmp_row_1',
    componentKey: 'row',
    label: '4 列栅格',
    props: { gutter: 16, columns: 24 },
    layout: { span: 2, align: 'left' },
    children: [1, 2, 3, 4].map(index => ({
      id: `cmp_row_1_col_${index}`,
      componentKey: 'col',
      label: `第 ${index} 列`,
      props: { span: 6 },
      layout: { span: 6, align: 'left' },
      children: index === 1 ? [field] : [],
    })),
  }
  return { layout: { gridColumns: 2 }, components: [row] }
}

function mountNode(schema, component, options = {}) {
  return mount(ForgeFormCanvasNode, {
    props: {
      component,
      fields: [],
      schema,
      selectedId: '',
      depth: 0,
      parentId: '',
      index: 0,
      ...options.props,
    },
    global: {
      stubs: { ...naiveStubs, ...componentStubs },
    },
    ...options.mount,
  })
}

describe('forge form canvas node removal in grid', () => {
  it('col renders a compact delete-only overlay while nested field renders full actions', () => {
    const schema = buildNestedSchema()
    const row = schema.components[0]
    const wrapper = mountNode(schema, row)

    // row 顶层节点有操作浮层
    const rowArticle = wrapper.find('article[data-v-app], article.canvas-node').exists()
    expect(rowArticle).toBe(true)

    // col 节点：精简操作条（仅删除按钮），无复制/菜单/拖拽把手
    const colNodes = wrapper.findAllComponents(ForgeFormCanvasNode).filter(w => w.props('component')?.componentKey === 'col')
    expect(colNodes.length).toBe(4)
    const emptyColNode = colNodes.find(w => w.props('component')?.id === 'cmp_row_1_col_2')
    expect(emptyColNode.find('.node-overlay.column-overlay').exists()).toBe(true)
    expect(emptyColNode.find('.node-overlay .menu-trigger').exists()).toBe(false)
    expect(emptyColNode.find('.node-overlay .drag-handle').exists()).toBe(false)
    // 4 列栅格删除中间列不受限
    const colDeleteBtn = emptyColNode.find('.column-overlay button.icon-action.danger')
    expect(colDeleteBtn.exists()).toBe(true)
    expect(colDeleteBtn.attributes('disabled')).toBeUndefined()

    // col 内字段节点：完整操作浮层（含复制/菜单/把手）
    const fieldNode = wrapper.findAllComponents(ForgeFormCanvasNode).find(w => w.props('component')?.componentKey === 'input')
    expect(fieldNode).toBeTruthy()
    expect(fieldNode.find('.node-overlay').exists()).toBe(true)
    expect(fieldNode.find('.node-overlay.column-overlay').exists()).toBe(false)
    const deleteBtn = fieldNode.find('button.icon-action.danger')
    expect(deleteBtn.exists()).toBe(true)
    expect(deleteBtn.attributes('disabled')).toBeUndefined()
  })

  it('removes a field nested inside a grid column through the quick action', () => {
    const schema = buildNestedSchema()
    const row = schema.components[0]
    const wrapper = mountNode(schema, row)

    const fieldNode = wrapper.findAllComponents(ForgeFormCanvasNode).find(w => w.props('component')?.componentKey === 'input')
    const deleteBtn = fieldNode.find('button.icon-action.danger')
    expect(deleteBtn.exists()).toBe(true)

    // 嵌套节点 update:schema 逐层转发到顶层 wrapper
    deleteBtn.trigger('click')

    const emits = wrapper.emitted('update:schema')
    expect(emits).toBeTruthy()
    const nextSchema = emits.at(-1)[0]
    expect(nextSchema).toBeTruthy()
    const nextRow = nextSchema.components.find(item => item.id === 'cmp_row_1')
    expect(nextRow).toBeTruthy()
    const allIds = []
    const walk = (components = []) => {
      components.forEach((component) => {
        allIds.push(component.id)
        walk(component.children || [])
      })
    }
    walk(nextSchema.components)
    expect(allIds).not.toContain('cmp_input_1')
    expect(allIds).toContain('cmp_row_1_col_1')
  })

  it('allows removing a locked field (data-bound fields stay in the asset shelf)', () => {
    const schema = buildNestedSchema()
    const field = schema.components[0].children[0].children[0]
    field.fieldBinding.locked = true
    const row = schema.components[0]
    const wrapper = mountNode(schema, row)

    const fieldNode = wrapper.findAllComponents(ForgeFormCanvasNode).find(w => w.props('component')?.componentKey === 'input')
    const deleteBtn = fieldNode.find('button.icon-action.danger')
    // locked 只限制改组件/存储类型，画布删除不再被禁用
    expect(deleteBtn.attributes('disabled')).toBeUndefined()

    deleteBtn.trigger('click')
    const emits = wrapper.emitted('update:schema')
    expect(emits).toBeTruthy()
    const allIds = []
    const walk = (components = []) => {
      components.forEach((component) => {
        allIds.push(component.id)
        walk(component.children || [])
      })
    }
    walk(emits.at(-1)[0].components)
    expect(allIds).not.toContain('cmp_input_1')
  })

  it('deletes a grid column together with its nested components', () => {
    const schema = buildNestedSchema()
    const row = schema.components[0]
    // 字段放在第 2 列，删除第 2 列时该字段随格子一并删除（不再合并到相邻列）
    const field = schema.components[0].children[0].children[0]
    schema.components[0].children[0].children = []
    schema.components[0].children[1].children = [field]
    const wrapper = mountNode(schema, row)

    const colNodes = wrapper.findAllComponents(ForgeFormCanvasNode).filter(w => w.props('component')?.componentKey === 'col')
    const secondColNode = colNodes.find(w => w.props('component')?.id === 'cmp_row_1_col_2')
    const colDeleteBtn = secondColNode.find('.column-overlay button.icon-action.danger')
    expect(colDeleteBtn.exists()).toBe(true)
    // 带内容的格子删除按钮提示「一并删除」
    expect(colDeleteBtn.attributes('title')).toContain('一并删除')
    colDeleteBtn.trigger('click')

    const emits = wrapper.emitted('update:schema')
    expect(emits).toBeTruthy()
    const nextSchema = emits.at(-1)[0]
    const nextRow = nextSchema.components.find(item => item.id === 'cmp_row_1')
    expect(nextRow.children.filter(child => child.componentKey === 'col')).toHaveLength(3)
    expect(nextRow.children.map(child => child.id)).not.toContain('cmp_row_1_col_2')
    // 第 2 列的字段随格子一起删除，不会搬家到其它格子
    const allIds = []
    const walk = (components = []) => {
      components.forEach((component) => {
        allIds.push(component.id)
        walk(component.children || [])
      })
    }
    walk(nextSchema.components)
    expect(allIds).not.toContain('cmp_input_1')
  })

  it('disables column removal when only one column remains', () => {
    const schema = buildNestedSchema()
    const row = schema.components[0]
    // 只保留第 1 列
    row.children = [row.children[0]]
    const wrapper = mountNode(schema, row)

    const colNodes = wrapper.findAllComponents(ForgeFormCanvasNode).filter(w => w.props('component')?.componentKey === 'col')
    const onlyColNode = colNodes.find(w => w.props('component')?.id === 'cmp_row_1_col_1')
    const colDeleteBtn = onlyColNode.find('.column-overlay button.icon-action.danger')
    expect(colDeleteBtn.exists()).toBe(true)
    expect(colDeleteBtn.attributes('disabled')).toBeDefined()

    colDeleteBtn.trigger('click')
    // 禁用按钮不产生 schema 变更
    expect(wrapper.emitted('update:schema')).toBeFalsy()
  })

  it('keeps the node-col class hook so grid column overlay spacing styles stay applied', () => {
    const schema = buildNestedSchema()
    const row = schema.components[0]
    const wrapper = mountNode(schema, row)

    // .canvas-node.node-col 是格子顶部操作条预留空间（padding-top）与 hover/选中态样式的
    // 唯一 CSS 钩子；动态类名一旦丢失，「格子删除按钮与列内组件操作条互相遮挡」会立即回归
    const colNodes = wrapper.findAllComponents(ForgeFormCanvasNode).filter(w => w.props('component')?.componentKey === 'col')
    expect(colNodes.length).toBeGreaterThan(0)
    colNodes.forEach((colNode) => {
      const article = colNode.find('article.canvas-node')
      expect(article.exists()).toBe(true)
      expect(article.classes()).toContain('node-col')
      expect(article.classes()).toContain('structural-slot')
    })
  })
})
