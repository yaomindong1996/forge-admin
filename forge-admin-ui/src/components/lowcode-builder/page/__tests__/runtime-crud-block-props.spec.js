import { describe, expect, it } from 'vitest'
import { buildRuntimeCrudBlockProps, resolveRuntimeSearchSchema } from '../runtime-crud-block-props'

describe('resolveRuntimeSearchSchema', () => {
  it('prefers runtime searchSchema ordered by explicit searchFieldRefs', () => {
    const schema = resolveRuntimeSearchSchema({
      hasExplicitSearchFieldRefs: true,
      blockProps: { searchFieldRefs: ['fieldB', 'fieldA'] },
      runtimeSearchSchema: [
        { field: 'fieldA', label: 'A', queryType: 'like' },
        { field: 'fieldB', label: 'B', queryType: 'eq' },
        { field: 'fieldC', label: 'C', queryType: 'eq' },
      ],
      aiSearchSchema: [{ field: 'fieldA', label: 'local-A' }],
    })
    expect(schema.map(item => item.field)).toEqual(['fieldB', 'fieldA'])
    expect(schema[0].queryType).toBe('eq')
    expect(schema[1].queryType).toBe('like')
  })

  it('keeps explicitly empty searchFieldRefs empty', () => {
    expect(resolveRuntimeSearchSchema({
      hasExplicitSearchFieldRefs: true,
      blockProps: { searchFieldRefs: [] },
      runtimeSearchSchema: [{ field: 'fieldA', label: 'A' }],
      aiSearchSchema: [{ field: 'fieldA', label: 'A' }],
    })).toEqual([])
  })

  it('falls back to aiSearchSchema when runtime searchSchema is empty', () => {
    const schema = resolveRuntimeSearchSchema({
      hasExplicitSearchFieldRefs: true,
      blockProps: { searchFieldRefs: ['fieldA'] },
      runtimeSearchSchema: [],
      aiSearchSchema: [{ field: 'fieldA', label: 'local-A' }],
    })
    expect(schema).toEqual([{ field: 'fieldA', label: 'local-A' }])
  })
})

describe('buildRuntimeCrudBlockProps page form binding', () => {
  it('does not clip editSchema by list fieldRefs when page form asset is bound', () => {
    const props = buildRuntimeCrudBlockProps({
      runtimeProps: {
        configKey: 'customer',
        pageFormAssetBound: true,
        editSchema: [
          { field: 'customerName', type: 'input', label: '客户名' },
          { field: 'mobile', type: 'input', label: '手机号' },
        ],
        columns: [{ field: 'oldName', title: '旧名称' }],
        searchSchema: [],
        apiConfig: {},
        options: {},
      },
      blockProps: { formAssetId: 'form_new' },
      configuredFieldRefs: ['oldName'],
      aiFormSchema: [],
      aiTableColumns: [],
      aiSearchSchema: [],
    })
    expect(props.editSchema.map(item => item.field)).toEqual(['customerName', 'mobile'])
  })

  it('keeps list fieldRefs like id even when published columnsSchema omitted them', () => {
    const props = buildRuntimeCrudBlockProps({
      runtimeProps: {
        configKey: 'customer',
        columns: [
          { field: 'customerName', title: '客户名' },
          { key: 'actions', title: '操作', fixed: 'right', type: 'action' },
        ],
        fieldCatalog: [
          { field: 'id', label: 'ID', listVisible: true },
          { field: 'customerName', label: '客户名', listVisible: true },
        ],
        searchSchema: [],
        editSchema: [],
        apiConfig: {},
        options: {},
      },
      blockProps: {},
      configuredFieldRefs: ['id', 'customerName'],
      aiFormSchema: [],
      aiTableColumns: [],
      aiSearchSchema: [],
    })
    expect(props.columns.map(item => item.field || item.key)).toEqual(['id', 'customerName', 'actions'])
    expect(props.columns.find(item => item.field === 'id')?.title).toBe('ID')
  })

  it('respects explicit list fieldRefs during design preview even when tableZone columns are wider', () => {
    const props = buildRuntimeCrudBlockProps({
      runtimeProps: {
        configKey: 'customer',
        designPreview: true,
        columns: [
          { field: 'id', title: 'ID' },
          { field: 'fieldInput', title: '输入框' },
          { field: 'createTime', title: '创建时间' },
          { key: 'actions', title: '操作', fixed: 'right', type: 'action' },
        ],
        fieldCatalog: [
          { field: 'id', label: 'ID', listVisible: true },
          { field: 'fieldInput', label: '输入框', listVisible: true },
          { field: 'createTime', label: '创建时间', listVisible: true },
        ],
        searchSchema: [],
        editSchema: [],
        apiConfig: {},
        options: {},
      },
      blockProps: { searchFieldRefs: ['fieldMoney'] },
      configuredFieldRefs: ['fieldInput'],
      aiFormSchema: [],
      aiTableColumns: [],
      aiSearchSchema: [],
    })
    expect(props.columns.map(item => item.field || item.key)).toEqual(['fieldInput', 'actions'])
  })

  it('drops polluted fieldMoney from page block when pageSchema table only has 输入框', () => {
    const props = buildRuntimeCrudBlockProps({
      runtimeProps: {
        configKey: 'customer',
        columns: [
          { field: 'fieldInput', title: '输入框' },
          { field: 'fieldMoney', title: 'fieldMoney' },
          { key: 'actions', title: '操作', fixed: 'right', type: 'action' },
        ],
        fieldCatalog: [
          { field: 'fieldInput', label: '输入框', listVisible: true },
          { field: 'fieldMoney', label: '金额', listVisible: true },
        ],
        pageSchema: {
          zones: [
            { zoneKey: 'search', fieldRefs: ['fieldMoney'] },
            { zoneKey: 'table', fieldRefs: ['fieldInput'] },
          ],
        },
        searchSchema: [],
        editSchema: [],
        apiConfig: {},
        options: {},
      },
      blockProps: { searchFieldRefs: ['fieldMoney'] },
      configuredFieldRefs: ['fieldInput', 'fieldMoney'],
      aiFormSchema: [],
      aiTableColumns: [],
      aiSearchSchema: [],
    })
    expect(props.columns.map(item => item.field || item.key)).toEqual(['fieldInput', 'actions'])
  })

  it('keeps a narrowed page-block selection that is a subset of pageSchema table refs', () => {
    const props = buildRuntimeCrudBlockProps({
      runtimeProps: {
        configKey: 'customer',
        columns: [
          { field: 'id', title: 'ID' },
          { field: 'fieldInput', title: '输入框' },
          { field: 'createTime', title: '创建时间' },
          { key: 'actions', title: '操作', fixed: 'right', type: 'action' },
        ],
        fieldCatalog: [
          { field: 'id', label: 'ID', listVisible: true },
          { field: 'fieldInput', label: '输入框', listVisible: true },
          { field: 'createTime', label: '创建时间', listVisible: true },
        ],
        pageSchema: {
          zones: [
            { zoneKey: 'table', fieldRefs: ['id', 'fieldInput', 'createTime'] },
          ],
        },
        searchSchema: [],
        editSchema: [],
        apiConfig: {},
        options: {},
      },
      blockProps: { searchFieldRefs: ['fieldMoney'] },
      configuredFieldRefs: ['fieldInput'],
      aiFormSchema: [],
      aiTableColumns: [],
      aiSearchSchema: [],
    })
    expect(props.columns.map(item => item.field || item.key)).toEqual(['fieldInput', 'actions'])
  })
})
