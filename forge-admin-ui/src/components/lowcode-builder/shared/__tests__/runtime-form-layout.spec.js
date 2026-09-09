import { describe, expect, it } from 'vitest'
import { hydrateRuntimeFormLayout } from '../runtime-form-layout'

function flatFields() {
  return [
    { field: 'name', label: '名称', type: 'input', span: 1 },
    { field: 'age', label: '年龄', type: 'number', span: 1 },
    { field: 'remark', label: '备注', type: 'textarea', span: 1 },
  ]
}

describe('runtime form layout hydration', () => {
  it('merges flat fields into a row layout tree with hydrated field definitions', () => {
    const layout = [
      {
        nodeType: 'row',
        componentKey: 'fcRow',
        key: 'row_1',
        props: { columns: 2, gutter: 16 },
        children: [
          { nodeType: 'field', key: 'f1', field: 'name', span: 1 },
          { nodeType: 'field', key: 'f2', field: 'age', span: 1 },
        ],
      },
    ]

    const nodes = hydrateRuntimeFormLayout(flatFields(), layout)

    // row 内水合 name/age，树外 remark 追加末尾
    expect(nodes).toHaveLength(2)
    const row = nodes[0]
    expect(row.nodeType).toBe('row')
    expect(row.props.columns).toBe(2)
    expect(row.children).toHaveLength(2)
    expect(row.children[0]).toMatchObject({
      nodeType: 'field',
      field: 'name',
      label: '名称',
      type: 'input',
      key: 'f1',
    })
    expect(row.children[1]).toMatchObject({ field: 'age', label: '年龄' })
  })

  it('returns flat fields unchanged when no layout tree is provided', () => {
    const fields = flatFields()
    expect(hydrateRuntimeFormLayout(fields, [])).toEqual(fields)
    expect(hydrateRuntimeFormLayout(fields, undefined)).toEqual(fields)
  })

  it('returns the input as-is when it is already a layout tree', () => {
    const tree = [
      {
        nodeType: 'row',
        props: { columns: 2 },
        children: [{ nodeType: 'field', field: 'name', label: '名称', type: 'input' }],
      },
    ]
    expect(hydrateRuntimeFormLayout(tree, [{ nodeType: 'row', children: [] }])).toEqual(tree)
  })

  it('appends fields missing from the layout tree at the end', () => {
    const layout = [
      {
        nodeType: 'row',
        props: { columns: 2 },
        children: [{ nodeType: 'field', field: 'name', span: 1 }],
      },
    ]

    const nodes = hydrateRuntimeFormLayout(flatFields(), layout)

    // row 内水合 name，树外 age/remark 按原顺序追加
    expect(nodes).toHaveLength(3)
    expect(nodes[0].nodeType).toBe('row')
    expect(nodes.slice(1).map(node => node.field)).toEqual(['age', 'remark'])
  })

  it('drops layout nodes referencing unknown fields and removes empty containers', () => {
    const layout = [
      {
        nodeType: 'row',
        props: { columns: 2 },
        children: [
          { nodeType: 'field', field: 'ghost', span: 1 },
        ],
      },
      {
        nodeType: 'row',
        props: { columns: 2 },
        children: [
          { nodeType: 'field', field: 'age', span: 1 },
        ],
      },
    ]

    const nodes = hydrateRuntimeFormLayout(flatFields(), layout)

    // 引用不存在字段的 row 整体删除，只剩有效 row + 树外追加字段
    expect(nodes).toHaveLength(3)
    expect(nodes[0].children[0].field).toBe('age')
    expect(nodes.slice(1).map(node => node.field)).toEqual(['name', 'remark'])
  })

  it('keeps standalone nodes (divider) even without children', () => {
    const layout = [
      { nodeType: 'divider', key: 'd1', props: { title: '分节' } },
      {
        nodeType: 'row',
        props: { columns: 2 },
        children: [{ nodeType: 'field', field: 'name', span: 1 }],
      },
    ]

    const nodes = hydrateRuntimeFormLayout(flatFields(), layout)

    expect(nodes[0].nodeType).toBe('divider')
    expect(nodes[0].props.title).toBe('分节')
  })

  it('hydrates nested card > row structures', () => {
    const layout = [
      {
        nodeType: 'card',
        componentKey: 'elCard',
        key: 'card_1',
        label: '基础信息',
        children: [
          {
            nodeType: 'row',
            props: { columns: 2 },
            children: [
              { nodeType: 'field', field: 'name', span: 1 },
              { nodeType: 'field', field: 'age', span: 1 },
            ],
          },
        ],
      },
    ]

    const nodes = hydrateRuntimeFormLayout(flatFields(), layout)

    expect(nodes).toHaveLength(2) // card + 树外 remark
    expect(nodes[0].nodeType).toBe('card')
    expect(nodes[0].label).toBe('基础信息')
    const row = nodes[0].children[0]
    expect(row.nodeType).toBe('row')
    expect(row.children.map(child => child.field)).toEqual(['name', 'age'])
  })

  it('is resilient to null entries in the field list', () => {
    const layout = [
      {
        nodeType: 'row',
        props: { columns: 2 },
        children: [{ nodeType: 'field', field: 'name', span: 1 }],
      },
    ]

    const nodes = hydrateRuntimeFormLayout([...flatFields(), null], layout)

    // null 条目被过滤；树外 age/remark 追加
    expect(nodes).toHaveLength(3)
    expect(nodes.slice(1).map(node => node.field)).toEqual(['age', 'remark'])
  })
})
