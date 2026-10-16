import { describe, expect, it } from 'vitest'
import {
  mapUiDocumentComponentsToAiFormSchema,
  resolveAiFormSchemaFromUiDocument,
  resolveBusinessTaskAiFormSchema,
} from '../index'

describe('resolveAiFormSchemaFromUiDocument', () => {
  it('无 context / 无 fields 时返回空数组', () => {
    expect(resolveAiFormSchemaFromUiDocument(null)).toEqual([])
    expect(resolveBusinessTaskAiFormSchema({})).toEqual([])
  })

  it('无 protocolVersion 时原样返回 fields', () => {
    const fields = [{ field: 'b' }, { field: 'a' }]
    expect(resolveAiFormSchemaFromUiDocument({ fields })).toEqual(fields)
  })

  it('按 sections 重排', () => {
    const fields = [
      { field: 'amount' },
      { field: 'title' },
      { field: 'extra' },
    ]
    const result = resolveAiFormSchemaFromUiDocument({
      protocolVersion: '1',
      uiDocument: {
        version: '1',
        sections: [{ fields: ['title', 'amount'] }],
        components: [],
      },
      fields,
    })
    expect(result.map(f => f.field)).toEqual(['title', 'amount', 'extra'])
  })

  it('空布局容器（card/tabs/collapse）仍保留，避免预览静默丢壳', () => {
    const result = mapUiDocumentComponentsToAiFormSchema([
      { type: 'elCard', label: 'E2E_elCard', props: { title: 'E2E_elCard' }, children: [] },
      { type: 'tabs', label: 'E2E_tabs', children: [{ type: 'tabPane', label: 'E2E_tabPane', children: [] }] },
      { type: 'collapse', label: 'E2E_collapse', children: [{ type: 'collapseItem', label: 'E2E_collapseItem', children: [] }] },
      { type: 'row', label: 'E2E_row', children: [{ type: 'col', label: 'E2E_col', children: [] }] },
    ], [])
    expect(result.map(n => n.nodeType)).toEqual(['card', 'tabs', 'collapse', 'row'])
    expect(result[0]).toMatchObject({ label: 'E2E_elCard', children: [] })
    expect(result[1].children[0]).toMatchObject({ nodeType: 'tabPane', label: 'E2E_tabPane' })
    expect(result[2].children[0]).toMatchObject({ nodeType: 'collapseItem', label: 'E2E_collapseItem' })
    expect(result[3].children[0]).toMatchObject({ nodeType: 'col', label: 'E2E_col' })
  })

  it('未配置列的关联子表仍保留，避免设计态可见、预览/发布静默丢失', () => {
    const result = mapUiDocumentComponentsToAiFormSchema([
      {
        type: 'subTable',
        id: 'cmp_subTable_1',
        label: 'E2E_subTable',
        props: {
          header: 'E2E_subTable',
          relationKey: 'e2e_items',
          e2eMarker: 'E2E_subTable',
          columns: [
            { fieldCode: 'itemName', label: '明细名称', type: 'input' },
          ],
        },
        children: [],
      },
    ], [])
    expect(result).toHaveLength(1)
    expect(result[0]).toMatchObject({
      nodeType: 'subTable',
      type: 'subTable',
      label: 'E2E_subTable',
      props: {
        relationKey: 'e2e_items',
        e2eMarker: 'E2E_subTable',
      },
    })
  })

  it('优先 components 布局树，字段定义仍取自 fields', () => {
    const fields = [
      { field: 'amount', writable: false, type: 'number' },
      { field: 'title', writable: true, type: 'input' },
      { field: 'extra', writable: false },
    ]
    const result = resolveAiFormSchemaFromUiDocument({
      protocolVersion: '1',
      uiDocument: {
        version: '1',
        components: [
          {
            type: 'card',
            children: [
              { type: 'input', field: 'title', visible: true, editable: true },
              { type: 'input', field: 'amount', visible: true, editable: false },
            ],
          },
        ],
      },
      fields,
    })
    expect(result[0].nodeType).toBe('card')
    expect(result[0].children[0]).toMatchObject({ field: 'title', writable: true })
    expect(result[0].children[1]).toMatchObject({
      field: 'amount',
      readonly: true,
      disabled: true,
    })
    // 未入画布的 extra 不应再被追加到审批表单
    expect(result).toHaveLength(1)
    expect(result.flatMap(node => node.children || []).map(f => f.field)).toEqual(['title', 'amount'])
  })

  it('有组件树时不回灌 fields 里未入表单的字段', () => {
    const result = resolveAiFormSchemaFromUiDocument({
      protocolVersion: '1',
      uiDocument: {
        version: '1',
        components: [
          { type: 'input', field: 'title', editable: true },
        ],
      },
      fields: [
        { field: 'title', type: 'input' },
        { field: 'fieldSwitch', type: 'switch', required: true },
        { field: 'fieldInput5', type: 'input' },
      ],
    })
    expect(result.map(f => f.field)).toEqual(['title'])
  })

  it('叠回 uiDocument 节点 props / 组件类型，避免 optionSource、fieldMappings、userSelect 丢失', () => {
    const fields = [
      { field: 'ownerId', type: 'select', label: '负责人', props: { placeholder: '请选择' } },
      {
        field: 'metricId',
        type: 'select',
        label: '指标',
        props: { optionSource: { type: 'QUERY_SOURCE', sourceKey: 'old' } },
      },
    ]
    const result = resolveAiFormSchemaFromUiDocument({
      protocolVersion: '1',
      uiDocument: {
        version: '1',
        components: [
          {
            type: 'userSelect',
            field: 'ownerId',
            label: '负责人',
            props: { multiple: false },
          },
          {
            type: 'select',
            field: 'metricId',
            props: {
              optionSource: {
                type: 'QUERY_SOURCE',
                sourceType: 'BUSINESS_OBJECT',
                sourceKey: 'metric_detail',
                valueField: 'id',
                labelField: 'fieldInput',
              },
              fieldMappings: [{ sourceField: 'fieldInput', targetField: 'fieldInput3' }],
            },
          },
        ],
      },
      fields,
    })
    expect(result[0]).toMatchObject({
      field: 'ownerId',
      type: 'userSelect',
      props: { placeholder: '请选择', multiple: false },
    })
    expect(result[1]).toMatchObject({
      field: 'metricId',
      type: 'select',
      props: {
        optionSource: {
          sourceKey: 'metric_detail',
          labelField: 'fieldInput',
        },
        fieldMappings: [{ sourceField: 'fieldInput', targetField: 'fieldInput3' }],
      },
    })
  })

  it('设计器组件不在 fields 目录时仍按 uiDocument 渲染，不再静默丢字段', () => {
    const fields = [{ field: 'b' }, { field: 'a' }]
    const result = resolveAiFormSchemaFromUiDocument({
      protocolVersion: '1',
      uiDocument: {
        version: '1',
        sections: [{ fields: ['a', 'b'] }],
        components: [{ type: 'card', children: [{ type: 'switch', field: 'fieldSwitch', label: '开关', editable: true }] }],
      },
      fields,
    })
    expect(result[0].nodeType).toBe('card')
    expect(result[0].children[0]).toMatchObject({
      field: 'fieldSwitch',
      type: 'switch',
      label: '开关',
    })
    expect(result.map(f => f.field).filter(Boolean).concat(
      result.flatMap(f => (f.children || []).map(c => c.field)).filter(Boolean),
    )).toContain('fieldSwitch')
  })

  it('fields 为空时仍可用纯 uiDocument 组件树渲染', () => {
    const result = resolveAiFormSchemaFromUiDocument({
      protocolVersion: '1',
      uiDocument: {
        version: '1',
        components: [
          { type: 'input', field: 'newField', label: '新字段', editable: true },
        ],
      },
      fields: [],
    })
    expect(result).toMatchObject([{ field: 'newField', type: 'input', label: '新字段' }])
  })

  it('money / inputNumber 归一为 number，避免审批详情金额空白', () => {
    const result = resolveAiFormSchemaFromUiDocument({
      protocolVersion: '1',
      uiDocument: {
        version: '1',
        components: [
          { type: 'money', field: 'amount', label: '金额', editable: false, props: { precision: 2 } },
          { type: 'input', field: 'cash', label: '现金', componentKey: 'money', editable: true },
        ],
      },
      fields: [
        { field: 'amount', type: 'input', writable: false },
        { field: 'cash', type: 'input', writable: true },
      ],
    })
    expect(result).toEqual(expect.arrayContaining([
      expect.objectContaining({ field: 'amount', type: 'number' }),
      expect.objectContaining({ field: 'cash', type: 'number', componentKey: 'money' }),
    ]))
  })

  it('弱 type + 强 componentKey 覆盖开关/人员/组织/上传/日期', () => {
    const result = resolveAiFormSchemaFromUiDocument({
      protocolVersion: '1',
      uiDocument: {
        version: '1',
        components: [
          { type: 'input', field: 'enabled', componentKey: 'switch', label: '启用' },
          { type: 'input', field: 'ownerId', componentKey: 'forgeUserSelect', label: '负责人' },
          { type: 'input', field: 'deptId', componentKey: 'deptSelect', label: '部门' },
          { type: 'input', field: 'attach', componentKey: 'upload', label: '附件' },
          { type: 'input', field: 'planDate', componentKey: 'date', label: '计划日期' },
        ],
      },
      fields: [
        { field: 'enabled', type: 'input' },
        { field: 'ownerId', type: 'input' },
        { field: 'deptId', type: 'input' },
        { field: 'attach', type: 'input' },
        { field: 'planDate', type: 'input' },
      ],
    })
    expect(result).toEqual(expect.arrayContaining([
      expect.objectContaining({ field: 'enabled', type: 'switch' }),
      expect.objectContaining({ field: 'ownerId', type: 'userSelect' }),
      expect.objectContaining({ field: 'deptId', type: 'orgTreeSelect' }),
      expect.objectContaining({ field: 'attach', type: 'fileUpload' }),
      expect.objectContaining({ field: 'planDate', type: 'date' }),
    ]))
  })
})

describe('mapUiDocumentComponentsToAiFormSchema', () => {
  it('无业务字段的空布局容器仍保留为空壳', () => {
    expect(mapUiDocumentComponentsToAiFormSchema(
      [{ type: 'row', children: [] }],
      [{ field: 'a' }],
    )).toEqual([
      expect.objectContaining({ type: 'row', nodeType: 'row', children: [] }),
    ])
  })

  it('fields 缺失时从 uiDocument 节点合成字段定义', () => {
    const result = mapUiDocumentComponentsToAiFormSchema(
      [{ type: 'switch', field: 'fieldSwitch', label: '开关', editable: true }],
      [],
    )
    expect(result).toMatchObject([{ field: 'fieldSwitch', type: 'switch', label: '开关' }])
  })

  it('未绑定字段的控件与页面挂件仍映射进 schema', () => {
    const result = mapUiDocumentComponentsToAiFormSchema(
      [
        { type: 'textarea', id: 'cmp_ta', label: 'E2E_textarea' },
        { type: 'qrcode', id: 'cmp_qr', label: 'E2E_qrcode', props: { content: 'hello' } },
      ],
      [],
    )
    expect(result).toEqual(expect.arrayContaining([
      expect.objectContaining({ field: 'cmp_ta', type: 'textarea', label: 'E2E_textarea' }),
      expect.objectContaining({ field: 'cmp_qr', nodeType: 'widget', componentKey: 'qrcode', label: 'E2E_qrcode' }),
    ]))
  })
})
