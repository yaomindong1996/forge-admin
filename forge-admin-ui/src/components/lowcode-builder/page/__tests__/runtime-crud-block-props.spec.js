import { describe, expect, it } from 'vitest'
import {
  buildRuntimeCrudBlockProps,
  resolveEffectiveFormOpenMode,
  resolveRuntimeSearchSchema,
} from '../runtime-crud-block-props'

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
    expect(schema).toEqual([expect.objectContaining({ field: 'fieldA', label: 'local-A' })])
  })

  it('overlays searchFieldSettings label and dictSelect onto runtime searchSchema', () => {
    const schema = resolveRuntimeSearchSchema({
      hasExplicitSearchFieldRefs: true,
      blockProps: {
        searchFieldRefs: ['status'],
        searchFieldSettings: {
          status: {
            label: '业务状态',
            componentType: 'dictSelect',
            dictType: 'sys_normal_disable',
            queryType: 'eq',
          },
        },
      },
      runtimeSearchSchema: [
        { field: 'status', label: '状态', type: 'input', queryType: 'like' },
      ],
      aiSearchSchema: [],
    })
    expect(schema).toEqual([expect.objectContaining({
      field: 'status',
      label: '业务状态',
      type: 'dictSelect',
      componentType: 'dictSelect',
      dictType: 'sys_normal_disable',
      queryType: 'eq',
    })])
  })

  it('fills missing explicit search refs from aiSearchSchema so designer and preview stay aligned', () => {
    const schema = resolveRuntimeSearchSchema({
      hasExplicitSearchFieldRefs: true,
      blockProps: {
        searchFieldRefs: ['id', 'E2E_input', 'E2E_number'],
        searchFieldSettings: {
          E2E_input: { label: '输入标签' },
        },
      },
      runtimeSearchSchema: [
        { field: 'E2E_input', label: 'E2E_input', type: 'input', placeholder: 'PH_input_0' },
        { field: 'E2E_number', label: 'E2E_number', type: 'number', placeholder: 'PH_number_2' },
        { field: 'E2E_select', label: 'E2E_select', type: 'select', placeholder: 'PH_select_3' },
      ],
      aiSearchSchema: [
        { field: 'id', label: 'ID', type: 'input' },
        { field: 'E2E_input', label: 'E2E_input', type: 'input' },
        { field: 'E2E_number', label: 'E2E_number', type: 'number' },
      ],
    })
    expect(schema.map(item => item.field)).toEqual(['id', 'E2E_input', 'E2E_number'])
    expect(schema[1]).toEqual(expect.objectContaining({
      field: 'E2E_input',
      label: '输入标签',
      placeholder: 'PH_input_0',
    }))
  })
})

describe('buildRuntimeCrudBlockProps fieldSettings overlay', () => {
  it('applies list field title and width from fieldSettings', () => {
    const props = buildRuntimeCrudBlockProps({
      runtimeProps: {
        configKey: 'customer',
        columns: [{ field: 'customerName', title: '客户名' }],
        fieldCatalog: [{ field: 'customerName', label: '客户名', listVisible: true }],
        searchSchema: [],
        editSchema: [],
        apiConfig: {},
        options: {},
      },
      blockProps: {
        fieldSettings: {
          customerName: { title: '客户全称', width: 180, align: 'center' },
        },
      },
      configuredFieldRefs: ['customerName'],
      aiFormSchema: [],
      aiTableColumns: [],
      aiSearchSchema: [],
    })
    expect(props.columns).toEqual([expect.objectContaining({
      field: 'customerName',
      title: '客户全称',
      width: 180,
      align: 'center',
      titleAlign: 'center',
    })])
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

  it('keeps toolbar import/export/custom-query switches visible in static design preview', () => {
    const props = buildRuntimeCrudBlockProps({
      runtimeProps: {
        configKey: 'hr_business_object_bkhu',
        designPreview: true,
        columns: [{ field: 'id', title: 'ID' }],
        fieldCatalog: [{ field: 'id', label: 'ID', listVisible: true }],
        searchSchema: [],
        editSchema: [],
        apiConfig: {
          list: 'get@/ai/crud/hr_business_object_bkhu/page',
          export: 'get@/ai/crud/hr_business_object_bkhu/export',
        },
        options: {},
      },
      blockProps: {
        showImport: true,
        showExport: true,
        showExportTasks: true,
        enableCustomQuery: true,
      },
      configuredFieldRefs: ['id'],
      aiFormSchema: [],
      aiTableColumns: [],
      aiSearchSchema: [],
      runtimeInteractive: false,
    })
    expect(props.showImport).toBe(true)
    expect(props.showExport).toBe(true)
    expect(props.showExportTasks).toBe(true)
    expect(props.enableCustomQuery).toBe(true)
    expect(props.customQueryConfigKey).toBe('hr_business_object_bkhu')
    expect(props.exportTaskConfigKey).toBe('hr_business_object_bkhu')
  })
})

describe('resolveEffectiveFormOpenMode', () => {
  it('prefers form-designer runtime mode over list-block Spec drawer overrides', () => {
    expect(resolveEffectiveFormOpenMode(
      { formOpenMode: 'modal' },
      { formOpenMode: 'flat' },
    )).toBe('flat')
  })

  it('falls back to block props when runtime has no formOpenMode', () => {
    expect(resolveEffectiveFormOpenMode(
      { formOpenMode: 'drawer' },
      {},
    )).toBe('drawer')
  })

  it('prefers block form chrome when free-layout suppress list shell', () => {
    expect(resolveEffectiveFormOpenMode(
      { formOpenMode: 'drawer' },
      { formOpenMode: 'flat' },
      { preferBlockFormChrome: true },
    )).toBe('drawer')
  })
})

describe('buildRuntimeCrudBlockProps preferBlockFormChrome', () => {
  it('lets free-layout block modalWidth override runtime', () => {
    const props = buildRuntimeCrudBlockProps({
      runtimeProps: {
        api: '/demo',
        modalWidth: '600px',
        formOpenMode: 'flat',
        drawerPlacement: 'left',
        columns: [{ prop: 'id' }],
      },
      blockProps: {
        formOpenMode: 'drawer',
        modalWidth: '960px',
        drawerPlacement: 'right',
      },
      preferBlockFormChrome: true,
    })
    expect(props.formOpenMode).toBe('drawer')
    expect(props.modalType).toBe('drawer')
    expect(props.modalWidth).toBe('960px')
    expect(props.drawerPlacement).toBe('right')
  })
})
