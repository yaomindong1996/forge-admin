import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import { getCurrentInstance } from 'vue'
import AiFormLayoutNodes from '../AiFormLayoutNodes.vue'

vi.mock('vue-router', () => ({
  useRoute: () => ({
    query: {},
    params: {},
    path: '/',
    fullPath: '/',
    name: 'ai-form-layout-nodes-test',
  }),
}))

const NGridStub = {
  name: 'NGrid',
  props: ['cols', 'xGap', 'yGap'],
  template: '<div class="n-grid-stub" :data-cols="String(cols)" :data-xgap="xGap === undefined ? \'\' : String(xGap)" :data-ygap="yGap === undefined ? \'\' : String(yGap)"><slot /></div>',
}

const NGiStub = {
  name: 'NGi',
  props: ['span'],
  setup() {
    // 把 v-for :key 暴露到 DOM，供 key 唯一性断言（duplicate key 是 keyed diff 未定义行为）
    const vnodeKey = getCurrentInstance().vnode.key
    return { vnodeKey }
  },
  template: '<div class="n-gi-stub" :data-span="String(span)" :data-key="String(vnodeKey)"><slot /></div>',
}

const AiFormItemStub = {
  name: 'AiFormItem',
  props: ['field', 'value', 'formData', 'context'],
  template: '<div class="ai-form-item-stub" />',
}

function mountNodes(nodes, gridCols = 1) {
  return mount(AiFormLayoutNodes, {
    props: { nodes, formValue: {}, gridCols },
    global: {
      stubs: { NGrid: NGridStub, NGi: NGiStub, AiFormItem: AiFormItemStub },
    },
  })
}

function fieldNode(key, span) {
  return {
    nodeType: 'field',
    key,
    field: key,
    label: key,
    componentType: 'input',
    layout: { span },
  }
}

describe('aiFormLayoutNodes span semantics (设计态=运行态栅格语义)', () => {
  it('row 内 span=24 的字段应占满整行（24 列容器 → n-gi span=24，而非历史补丁的 1）', () => {
    const wrapper = mountNodes([{
      nodeType: 'row',
      key: 'row1',
      componentKey: 'row',
      props: { columns: 24 },
      children: [fieldNode('full', 24), fieldNode('half', 12)],
    }], 1)
    const spans = wrapper.findAll('.n-gi-stub').map(el => el.attributes('data-span'))
    // 第一个 n-gi 是 row 容器本身（span=cols），其后为两个字段
    expect(spans).toEqual(['1', '24', '12'])
  })

  it('顶层 2 列下 span=12 的字段 clamp 到 2（与设计器 min(parentCols, span) 一致，而非补丁的 1）', () => {
    const wrapper = mountNodes([fieldNode('a', 12)], 2)
    expect(wrapper.find('.n-gi-stub').attributes('data-span')).toBe('2')
  })

  it('无 span 的字段默认占 1 格', () => {
    const wrapper = mountNodes([fieldNode('plain', undefined)], 2)
    expect(wrapper.find('.n-gi-stub').attributes('data-span')).toBe('1')
  })

  it('row 容器节点占满当前列数', () => {
    const wrapper = mountNodes([{
      nodeType: 'row',
      key: 'row1',
      componentKey: 'row',
      props: { columns: 24 },
      children: [],
    }], 3)
    expect(wrapper.find('.n-gi-stub').attributes('data-span')).toBe('3')
  })

  it('row 的子级 n-grid 使用 row 自身 columns（24），字段 span 原样生效', () => {
    const wrapper = mountNodes([{
      nodeType: 'row',
      key: 'row1',
      componentKey: 'row',
      props: { columns: 24 },
      children: [fieldNode('half', 12)],
    }], 2)
    const grids = wrapper.findAll('.n-grid-stub')
    // 外层 grid cols=2（顶层），内层递归 grid cols=24（row.columns）
    expect(grids.map(el => el.attributes('data-cols'))).toEqual(['2', '24'])
    const fieldGi = wrapper.findAll('.n-gi-stub')[1]
    expect(fieldGi.attributes('data-span')).toBe('12')
  })

  it('row 的 gutter/rowGap 分别透传到子级 n-grid 的 x-gap/y-gap（与设计器画布同 key）', () => {
    const wrapper = mountNodes([{
      nodeType: 'row',
      key: 'row1',
      componentKey: 'row',
      props: { columns: 24, gutter: 20, rowGap: 24 },
      children: [fieldNode('half', 12)],
    }], 2)
    const grids = wrapper.findAll('.n-grid-stub')
    // 内层递归 grid（row 子级）：x-gap=gutter、y-gap=rowGap
    expect(grids[1].attributes('data-xgap')).toBe('20')
    expect(grids[1].attributes('data-ygap')).toBe('24')
  })

  it('row 未配置 rowGap 时 y-gap 回落父级默认值', () => {
    const wrapper = mountNodes([{
      nodeType: 'row',
      key: 'row1',
      componentKey: 'row',
      props: { columns: 24 },
      children: [fieldNode('half', 12)],
    }], 2)
    const grids = wrapper.findAll('.n-grid-stub')
    expect(grids[1].attributes('data-ygap')).toBe('0')
  })
})

describe('aiFormLayoutNodes 节点 key 唯一性（复制组件后 field 重复场景）', () => {
  // 设计器复制组件只重写 id、保留 fieldBinding.fieldCode：
  // duplicateDesignerComponent → rewriteComponentIds（formDesignerSchema.js）。
  // 运行态 normalizeRuntimeComponent 生成 node.field 相同、node.id 不同的两个节点。
  function designerFieldNode(id, field, label) {
    return { nodeType: 'field', id, field, label, componentType: 'input', layout: { span: 1 } }
  }

  it('两个相同 fieldCode 不同 id 的字段节点（复制产物）都应渲染，key 取 id 保证唯一', () => {
    const wrapper = mountNodes([
      designerFieldNode('cmp_a', 'userName', '用户名'),
      designerFieldNode('cmp_a_copy', 'userName', '用户名'),
    ], 2)
    expect(wrapper.findAll('.n-gi-stub')).toHaveLength(2)
    expect(wrapper.findAll('.ai-form-item-stub')).toHaveLength(2)
  })

  it('复制节点插入后 keyed children 的 key 唯一（取 id）且不丢节点', async () => {
    const original = designerFieldNode('cmp_a', 'userName', '用户名')
    const status = designerFieldNode('cmp_b', 'status', '状态')
    const wrapper = mountNodes([original, status], 2)
    expect(wrapper.findAll('.n-gi-stub')).toHaveLength(2)

    // 模拟复制 userName 后插到中间。
    // 旧实现（field 优先）keys=[userName, userName, status] 重复 —— duplicate key 是
    // Vue keyed diff 的未定义行为，真实组件树下触发 patchKeyedChildren 崩溃
    // （isSameVNodeType null / patchElement 对 null el 设 __vnode）。
    const copy = designerFieldNode('cmp_a_copy', 'userName', '用户名')
    await wrapper.setProps({ nodes: [original, copy, status] })
    expect(wrapper.findAll('.n-gi-stub')).toHaveLength(3)
    const keys = wrapper.findAll('.n-gi-stub').map(el => el.attributes('data-key'))
    expect(new Set(keys).size).toBe(keys.length)
    expect(keys).toEqual(['cmp_a', 'cmp_a_copy', 'cmp_b'])
  })

  it('无 id 的手写 schema 仍回退 field 作 key（历史行为兼容）', () => {
    const wrapper = mountNodes([{ nodeType: 'field', field: 'legacy', label: '旧字段', componentType: 'input', layout: { span: 1 } }], 2)
    expect(wrapper.findAll('.n-gi-stub')).toHaveLength(1)
  })
})
