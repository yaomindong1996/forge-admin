import { describe, expect, it } from 'vitest'
import {
  appendDesignPreviewToApiValue,
  applyDesignerVisibilityToFields,
  applyTableColumnLayout,
  buildCrudSearchTypeRequestParams,
  buildRuntimeCrudProps,
  ensureManagedFlowStatusColumns,
  ensureFieldRefColumns,
  filterCrudItemsByFieldRefs,
  resolveColumnsByFieldRefs,
  includeCompiledChildColumnRefs,
  includeManagedRuntimeFieldRefs,
  isDesignPreviewCrudProps,
  isManagedBusinessFlowField,
  mergeDesignerEditSchema,
  overlayFormDesignerOnRuntimeCrudProps,
  resolveBoundFormDesignerSchema,
  resolveCrudPreviewReloadKey,
  resolveCrudSearchFieldCatalog,
  resolveDesignerFormGovernance,
  resolveRuntimeBlockApi,
  shouldUseStaticCrudPreview,
} from '../runtime-crud-props'

describe('runtime CRUD design preview props', () => {
  it('merges options.defaultSort into publicParams for left-tree sort', () => {
    const props = buildRuntimeCrudProps({
      configKey: 'org_tree',
      options: {
        defaultSort: { orderByColumn: 'sortNo', isAsc: 'asc' },
        publicParams: { foo: 1 },
      },
    })
    expect(props.publicParams).toMatchObject({
      orderByColumn: 'sortNo',
      isAsc: 'asc',
      foo: 1,
    })
  })

  it('adds the design preview marker to every draft CRUD endpoint', () => {
    const props = buildRuntimeCrudProps({
      configKey: 'crm_customer',
      apiConfig: {
        list: 'get@/ai/crud/当前配置/page',
        detail: 'get@/ai/crud/当前配置/:id',
        create: 'post@/ai/crud/当前配置',
        update: 'put@/ai/crud/当前配置',
        delete: 'delete@/ai/crud/当前配置/:id?force=false',
      },
    }, { designPreview: true })

    expect(props.designPreview).toBe(true)
    expect(props.apiConfig).toEqual({
      list: 'get@/ai/crud/crm_customer/page?designPreview=1',
      detail: 'get@/ai/crud/crm_customer/:id?designPreview=1',
      create: 'post@/ai/crud/crm_customer?designPreview=1',
      update: 'put@/ai/crud/crm_customer?designPreview=1',
      delete: 'delete@/ai/crud/crm_customer/:id?force=false&designPreview=1',
    })
  })

  it('does not change published runtime endpoints or duplicate the marker', () => {
    const published = buildRuntimeCrudProps({
      configKey: 'crm_customer',
      apiConfig: { list: 'get@/ai/crud/当前配置/page' },
    })

    expect(published.designPreview).toBe(false)
    expect(published.apiConfig.list).toBe('get@/ai/crud/crm_customer/page')
    expect(appendDesignPreviewToApiValue('get@/ai/crud/crm_customer/page?designPreview=1'))
      .toBe('get@/ai/crud/crm_customer/page?designPreview=1')
  })

  it('adds the design preview marker to a block-level base API', () => {
    expect(resolveRuntimeBlockApi(
      'post@/ai/crud/当前配置',
      'crm_customer',
      true,
    )).toBe('post@/ai/crud/crm_customer?designPreview=1')
    expect(resolveRuntimeBlockApi(
      'post@/ai/crud/当前配置?designPreview=1',
      'crm_customer',
      true,
    )).toBe('post@/ai/crud/crm_customer?designPreview=1')
  })

  it('treats both compiled draft config and designer fallback as design preview', () => {
    expect(isDesignPreviewCrudProps({ designPreview: true })).toBe(true)
    expect(isDesignPreviewCrudProps({ draftOnly: true })).toBe(true)
    expect(isDesignPreviewCrudProps({ designPreview: false })).toBe(false)
  })

  it('keeps static preview protection on a non-interactive design canvas', () => {
    expect(shouldUseStaticCrudPreview({
      blockType: 'AiCrudPage',
      runtimeInteractive: false,
      previewLiveData: false,
      hasConfiguredRequest: true,
    })).toBe(true)
  })

  it('does not treat a published interactive application as static preview', () => {
    expect(shouldUseStaticCrudPreview({
      blockType: 'AiCrudPage',
      runtimeInteractive: true,
      previewLiveData: false,
      hasConfiguredRequest: true,
    })).toBe(false)
  })

  it('allows an explicitly enabled live preview to call configured APIs', () => {
    expect(shouldUseStaticCrudPreview({
      blockType: 'AiCrudPage',
      runtimeInteractive: false,
      previewLiveData: true,
      hasConfiguredRequest: true,
    })).toBe(false)
  })

  it('passes published master-detail children through runtime CRUD props', () => {
    const children = [{ relationKey: 'order_items', modelCode: 'ORDER_ITEM' }]
    const props = buildRuntimeCrudProps({
      options: { masterDetailConfig: { children } },
    })

    expect(props.childrenConfig).toEqual(children)
  })

  it('uses explicit search field refs independently from table columns', () => {
    const fields = [
      { field: 'id', label: 'ID' },
      { field: 'customerName', label: '客户名称' },
      { field: 'orderNo', label: '订单号' },
    ]

    expect(resolveCrudSearchFieldCatalog(fields, {
      fieldRefs: ['id'],
      props: { searchFieldRefs: ['customerName', 'orderNo'] },
    }).map(field => field.field)).toEqual(['customerName', 'orderNo'])
  })

  it('applies page-level query type, component and field mapping to the runtime schema', () => {
    const fields = [
      { field: 'customerName', label: '客户名称', componentType: 'input', queryType: 'like' },
      { field: 'orderNo', label: '订单号', componentType: 'input', queryType: 'eq' },
    ]

    expect(resolveCrudSearchFieldCatalog(fields, {
      fieldRefs: ['customerName'],
      props: {
        searchFieldRefs: ['customerName'],
        searchFieldSettings: {
          customerName: {
            queryType: 'like',
            componentType: 'input',
            queryField: 'orderNo',
          },
        },
      },
    })).toEqual([expect.objectContaining({
      field: 'orderNo',
      fieldCode: 'orderNo',
      sourceField: 'customerName',
      label: '客户名称',
      queryType: 'like',
      componentType: 'input',
    })])
  })

  it('allows list designer to override search component to dictSelect with label', () => {
    const fields = [
      { field: 'status', label: '状态', componentType: 'input', queryType: 'eq' },
    ]

    expect(resolveCrudSearchFieldCatalog(fields, {
      props: {
        searchFieldRefs: ['status'],
        searchFieldSettings: {
          status: {
            label: '启用状态',
            componentType: 'dictSelect',
            dictType: 'sys_normal_disable',
          },
        },
      },
    })).toEqual([expect.objectContaining({
      field: 'status',
      label: '启用状态',
      componentType: 'dictSelect',
      dictType: 'sys_normal_disable',
    })])
  })

  it('keeps treeSelect search fields as treeSelect with optionSource', () => {
    const fields = [
      {
        field: 'parentId',
        label: '上级',
        componentType: 'treeSelect',
        dataType: 'bigint',
        queryType: 'eq',
        basicProps: {
          optionSource: {
            type: 'tree',
            api: 'get@/ai/crud/demo/tree',
          },
        },
      },
    ]

    expect(resolveCrudSearchFieldCatalog(fields, {
      props: { searchFieldRefs: ['parentId'] },
    })).toEqual([expect.objectContaining({
      field: 'parentId',
      componentType: 'treeSelect',
      queryType: 'eq',
      optionSource: expect.objectContaining({
        type: 'tree',
        api: 'get@/ai/crud/demo/tree',
      }),
    })])
  })

  it('serializes only supported page query operators as dynamic CRUD control metadata', () => {
    expect(buildCrudSearchTypeRequestParams([
      { field: 'customerName', queryType: 'like' },
      { field: 'createdAt', queryType: 'between' },
      { field: 'unsafeField', queryType: 'drop table' },
      { field: '', queryType: 'eq' },
    ])).toEqual({
      _searchTypes: JSON.stringify({
        customerName: 'like',
        createdAt: 'between',
      }),
    })
  })

  it('keeps an explicitly empty search field selection empty', () => {
    const fields = [
      { field: 'id', label: 'ID' },
      { field: 'customerName', label: '客户名称' },
    ]

    expect(resolveCrudSearchFieldCatalog(fields, {
      fieldRefs: ['id', 'customerName'],
      props: { searchFieldRefs: [] },
    })).toEqual([])
  })

  it('falls back to model searchable fields for legacy blocks without search field refs', () => {
    const fields = [
      { field: 'id', label: 'ID', searchable: false },
      { field: 'customerName', label: '客户名称', searchable: true },
      { field: 'orderNo', label: '订单号', searchable: false },
    ]

    expect(resolveCrudSearchFieldCatalog(fields, {
      fieldRefs: ['id', 'orderNo'],
      props: {},
    }).map(field => field.field)).toEqual(['customerName'])
  })

  it('keeps the preview reload key stable when only preview result state changes', () => {
    const source = {
      blockType: 'AiCrudPage',
      props: {
        previewLiveData: true,
        previewMode: 'realList',
        previewRecordId: '1001',
        listApi: 'get@/ai/crud/order/page',
        lastPreviewStatus: 'loading',
        lastPreviewMessage: '正在请求真实接口预览',
      },
    }
    const completed = {
      ...source,
      props: {
        ...source.props,
        lastPreviewStatus: 'success',
        lastPreviewMessage: '接口预览成功，读取 10 条数据',
      },
    }

    expect(resolveCrudPreviewReloadKey(completed)).toBe(resolveCrudPreviewReloadKey(source))
  })

  it('handles missing runtime CRUD props in static designer preview', () => {
    expect(resolveCrudPreviewReloadKey({ props: {} }, null)).toBe(JSON.stringify({
      enabled: false,
      mode: 'mock',
      recordId: '',
      listApi: '',
    }))
  })

  it('keeps action columns while dropping deleted form fields from runtime schemas', () => {
    expect(filterCrudItemsByFieldRefs([
      { field: 'fieldSlider', title: '滑块' },
      { field: 'fieldNumber', title: '数字' },
      { key: 'action', type: 'action', title: '操作' },
    ], ['fieldSlider']).map(item => item.field || item.key)).toEqual(['fieldSlider', 'action'])
  })

  it('resolves table columns by fieldRefs order and synthesizes missing ones like id', () => {
    const columns = resolveColumnsByFieldRefs(
      [
        { field: 'customerName', title: '客户名' },
        { key: 'actions', title: '操作', fixed: 'right', type: 'action' },
      ],
      ['id', 'customerName'],
      [
        { field: 'id', label: 'ID', listVisible: true },
        { field: 'customerName', label: '客户名', listVisible: true },
      ],
    )
    expect(columns.map(item => item.field || item.key)).toEqual(['id', 'customerName', 'actions'])
    expect(columns.find(item => item.field === 'id')?.title).toBe('ID')

    // 兼容旧 ensureFieldRefColumns：目录缺失时仍按已选 fieldRefs 顺序补列
    expect(ensureFieldRefColumns(
      [{ field: 'name', title: '名称' }],
      ['id', 'name'],
      [],
    ).map(item => item.field)).toEqual(['id', 'name'])

    // listVisible=false 的字段不补列
    expect(resolveColumnsByFieldRefs(
      [{ field: 'name', title: '名称' }],
      ['id', 'name'],
      [{ field: 'id', label: 'ID', listVisible: false }],
    ).map(item => item.field)).toEqual(['name'])
  })

  it('keeps a managed flow status column when the application block has an older field snapshot', () => {
    expect(includeManagedRuntimeFieldRefs(
      ['fieldRate'],
      [
        { field: 'fieldRate', listVisible: true },
        {
          field: 'flowStatus',
          listVisible: true,
          fieldStatus: 'ENABLED',
          advancedProps: { managedBy: 'BUSINESS_FLOW' },
        },
      ],
    )).toEqual(['fieldRate', 'flowStatus'])

    expect(includeManagedRuntimeFieldRefs(
      ['fieldRate'],
      [{
        field: 'flowStatus',
        listVisible: true,
        dictType: 'business_flow_status',
      }],
    )).toEqual(['fieldRate', 'flowStatus'])

    expect(includeManagedRuntimeFieldRefs(
      ['fieldRate'],
      [{
        field: 'flowStatus',
        listVisible: true,
        advancedProps: { managedBy: 'BUSINESS_FLOW' },
      }],
      { flowStatus: { visible: false } },
    )).toEqual(['fieldRate'])
  })

  it('synthesizes a missing flowStatus column from the field catalog', () => {
    expect(isManagedBusinessFlowField({ field: 'flowStatus', dictType: 'business_flow_status' })).toBe(true)
    const columns = ensureManagedFlowStatusColumns(
      [
        { key: 'name', prop: 'name', title: '名称' },
        { key: 'actions', type: 'action', title: '操作', fixed: 'right' },
      ],
      [{
        field: 'flowStatus',
        fieldName: '流程状态',
        listVisible: true,
        dictType: 'business_flow_status',
      }],
    )
    expect(columns.map(item => item.key || item.prop)).toEqual(['name', 'flowStatus', 'actions'])
    expect(columns[1].render).toEqual({ type: 'dictTag', dictType: 'business_flow_status' })

    expect(ensureManagedFlowStatusColumns(
      [{ key: 'name', prop: 'name', title: '名称' }],
      [{ field: 'flowStatus', listVisible: true, dictType: 'business_flow_status' }],
      { flowStatus: { visible: false } },
    ).map(item => item.key)).toEqual(['name'])
  })

  it('treats name-only flowStatus as platform managed even without listVisible flag', () => {
    expect(isManagedBusinessFlowField({ field: 'flowStatus' })).toBe(true)
    const columns = ensureManagedFlowStatusColumns(
      [
        { key: 'name', prop: 'name', title: '名称' },
        { key: 'actions', type: 'action', title: '操作', fixed: 'right' },
      ],
      [{ field: 'flowStatus', fieldStatus: 'ENABLED' }],
    )
    expect(columns.map(item => item.key || item.prop)).toEqual(['name', 'flowStatus', 'actions'])
    expect(includeManagedRuntimeFieldRefs(
      ['name'],
      [{ field: 'flowStatus', fieldStatus: 'ENABLED' }],
    )).toEqual(['name', 'flowStatus'])
  })

  it('canonicalizes legacy flow_status metadata so stale refs cannot hide the column', () => {
    expect(isManagedBusinessFlowField({
      field: 'flow_status',
      columnName: 'flow_status',
      dictType: 'business_flow_status',
    })).toBe(true)

    const refs = includeManagedRuntimeFieldRefs(
      ['name'],
      [{ field: 'flow_status', columnName: 'flow_status', listVisible: true }],
    )
    expect(refs).toEqual(['name', 'flowStatus'])

    const columns = ensureManagedFlowStatusColumns(
      [{ key: 'flow_status', dataIndex: 'flow_status', title: '流程状态' }],
      [{ field: 'flow_status', columnName: 'flow_status', listVisible: true }],
    )
    expect(columns.filter(item => item.key === 'flowStatus' || item.key === 'flow_status')).toHaveLength(1)
    expect(columns[0]).toMatchObject({ key: 'flowStatus', prop: 'flowStatus', dataIndex: 'flowStatus' })
    expect(includeManagedRuntimeFieldRefs(
      ['name'],
      [{ field: 'flow_status', listVisible: true, dictType: 'business_flow_status' }],
      { flow_status: { visible: false } },
    )).toEqual(['name'])
  })

  it('keeps compiled child-table columns when the page block snapshot only has main fields', () => {
    const columns = [
      { key: 'fieldInput', title: '文本' },
      { key: 'order_item__qty', dataIndex: 'order_item__qty', title: '数量' },
      { key: 'action', type: 'action', title: '操作' },
    ]
    const refs = includeCompiledChildColumnRefs(['fieldInput'], columns)
    expect(filterCrudItemsByFieldRefs(columns, refs).map(item => item.key)).toEqual([
      'fieldInput',
      'order_item__qty',
      'action',
    ])
    expect(includeCompiledChildColumnRefs([], columns)).toEqual([])
  })

  it('changes the preview reload key only when a real request condition changes', () => {
    const source = {
      props: {
        previewLiveData: true,
        previewMode: 'realList',
        previewRecordId: '1001',
        listApi: 'get@/ai/crud/order/page',
      },
    }
    const sourceKey = resolveCrudPreviewReloadKey(source)

    expect(resolveCrudPreviewReloadKey({
      props: { ...source.props, previewRecordId: '1002' },
    })).not.toBe(sourceKey)
    expect(resolveCrudPreviewReloadKey({
      props: { ...source.props, listApi: 'get@/ai/crud/order-v2/page' },
    })).not.toBe(sourceKey)
    expect(resolveCrudPreviewReloadKey({
      props: { ...source.props, previewLiveData: false },
    })).not.toBe(sourceKey)
  })

  it('keeps table row spacing and applies block alignment over compiled columns', () => {
    const props = buildRuntimeCrudProps({
      options: { tableRowGap: 18 },
      columnsSchema: [
        { prop: 'name', label: '名称', align: 'left', titleAlign: 'left' },
        { prop: 'status', label: '状态' },
      ],
    })
    const columns = applyTableColumnLayout(props.columns, {
      globalAlign: 'center',
      fieldSettings: { status: { align: 'right' } },
    })

    expect(props.tableRowGap).toBe(18)
    expect(columns).toEqual([
      expect.objectContaining({ prop: 'name', align: 'center', titleAlign: 'center' }),
      expect.objectContaining({ prop: 'status', align: 'right', titleAlign: 'right' }),
    ])
  })

  it('drops columns that list designer explicitly hid via fieldSettings.visible=false', () => {
    const columns = applyTableColumnLayout(
      [
        { prop: 'name', title: '名称' },
        { prop: 'flowStatus', title: '流程状态' },
        { prop: 'actions', type: 'action', fixed: 'right' },
      ],
      { fieldSettings: { flowStatus: { visible: false } } },
    )
    expect(columns.map(item => item.prop || item.key)).toEqual(['name', 'actions'])
  })

  it('applies list designer title/width/renderType from fieldSettings', () => {
    const columns = applyTableColumnLayout(
      [{ prop: 'status', title: '状态' }],
      {
        fieldSettings: {
          status: {
            title: '流程状态',
            width: 120,
            renderType: 'dictTag',
            ellipsis: false,
          },
        },
      },
    )
    expect(columns).toEqual([expect.objectContaining({
      prop: 'status',
      title: '流程状态',
      width: 120,
      renderType: 'dictTag',
      ellipsis: false,
    })])
  })

  it('passes compiled process runtime actions through to AiCrudPage', () => {
    const props = buildRuntimeCrudProps({
      objectCode: 'order',
      options: {
        runtimeActions: [{ key: 'startProcess:submit_approval', actionType: 'START_PROCESS' }],
        detailActions: [{ key: 'startProcess:submit_approval:detail', position: 'detail' }],
        formActions: [{ key: 'startProcess:submit_approval:form', position: 'form' }],
      },
    })
    expect(props.businessObjectCode).toBe('order')
    expect(props.runtimeActions).toEqual([
      { key: 'startProcess:submit_approval', actionType: 'START_PROCESS' },
    ])
    expect(props.detailActions).toEqual([
      { key: 'startProcess:submit_approval:detail', position: 'detail' },
    ])
    expect(props.formActions).toEqual([
      { key: 'startProcess:submit_approval:form', position: 'form' },
    ])
  })

  it('prefers the form designer layout over flattened runtime options', () => {
    const props = buildRuntimeCrudProps({
      options: {
        formOpenMode: 'modal',
        modalWidth: '800px',
        editGridCols: 1,
        editLabelWidth: 'auto',
        editLabelPlacement: 'left',
        editSize: 'medium',
        editXGap: 12,
        editYGap: 8,
        formDesignerSchema: {
          formKey: 'order_default_form',
          layout: {
            formOpenMode: 'drawer',
            modalWidth: '1200px',
            drawerPlacement: 'left',
            gridColumns: 2,
            labelWidth: 220,
            labelPlacement: 'left',
            labelAlign: 'left',
            size: 'small',
            enableCollapse: true,
            showFeedback: false,
            columnGap: 16,
            rowGap: 16,
          },
        },
      },
    })

    expect(props.formOpenMode).toBe('drawer')
    expect(props.modalType).toBe('drawer')
    expect(props.modalWidth).toBe('1200px')
    expect(props.detailModalWidth).toBe('min(1080px, 92vw)')
    expect(props.drawerPlacement).toBe('left')
    expect(props.editGridCols).toBe(2)
    expect(props.editLabelWidth).toBe(220)
    expect(props.editLabelPlacement).toBe('left')
    expect(props.editLabelAlign).toBe('left')
    expect(props.editSize).toBe('small')
    expect(props.editEnableCollapse).toBe(true)
    expect(props.editShowFeedback).toBe(false)
    expect(props.editXGap).toBe(16)
    expect(props.protocolVersion).toBe('1')
    expect(props.uiDocument).toMatchObject({
      version: '1',
      uiType: 'lowcode-form',
      formKey: 'order_default_form',
    })
    expect(props.editYGap).toBe(16)
  })

  it('resolves the designer layout from the default form of a multi-form schema', () => {
    const props = buildRuntimeCrudProps({
      options: {
        formDesignerSchema: {
          defaultFormKey: 'order_edit_form',
          forms: [
            { formKey: 'order_create_form', schema: { layout: { modalWidth: '600px' } } },
            { formKey: 'order_edit_form', schema: { layout: { modalWidth: '960px', formOpenMode: 'drawer' } } },
          ],
        },
      },
    })

    expect(props.modalWidth).toBe('960px')
    expect(props.formOpenMode).toBe('drawer')
  })

  it('falls back to flattened runtime options when no designer schema exists', () => {
    const props = buildRuntimeCrudProps({
      options: {
        formOpenMode: 'drawer',
        modalWidth: '720px',
        editGridCols: 3,
        editSize: 'small',
        editEnableCollapse: true,
      },
    })

    expect(props.formOpenMode).toBe('drawer')
    expect(props.modalWidth).toBe('720px')
    expect(props.editGridCols).toBe(3)
    expect(props.editSize).toBe('small')
    expect(props.editEnableCollapse).toBe(true)
    expect(props.detailModalWidth).toBe('min(1080px, 92vw)')
  })

  it('passes form designer governance events as beforeRenderForm hook', async () => {
    const props = buildRuntimeCrudProps({
      configKey: 'demo',
      options: {
        formDesignerSchema: {
          formKey: 'main',
          settings: {
            governance: {
              events: [{
                id: 'evt_after',
                hook: 'afterLoad',
                action: 'setFieldValue',
                handler: 'fieldInput=E2E_AFTERLOAD',
              }],
            },
          },
        },
      },
    })
    expect(typeof props.beforeRenderForm).toBe('function')
    await expect(props.beforeRenderForm(null)).resolves.toEqual({ fieldInput: 'E2E_AFTERLOAD' })
  })

  it('passes form designer governance fieldEvents and formInit through to AiCrudPage', () => {
    const formInit = {
      contextDefaults: [
        { id: 'd1', field: 'ownerUser', source: 'CONTEXT_PATH', path: 'currentUser.userId', enabled: true },
      ],
    }
    const props = buildRuntimeCrudProps({
      options: {
        formDesignerSchema: {
          defaultFormKey: 'order_edit_form',
          forms: [
            {
              formKey: 'order_create_form',
              schema: { settings: { governance: { formInit: { contextDefaults: [{ id: 'legacy' }] } } } },
            },
            {
              formKey: 'order_edit_form',
              schema: {
                settings: {
                  governance: {
                    fieldEvents: [{ id: 'evt1', trigger: 'FORM_LOAD' }],
                    formInit,
                  },
                },
              },
            },
          ],
        },
      },
    })

    // governance 必须与 layout 一样取 defaultFormKey 选中的表单，而非第一个表单
    expect(props.fieldEvents).toEqual([{ id: 'evt1', trigger: 'FORM_LOAD' }])
    expect(props.formInit).toEqual(formInit)
  })

  it('resolves governance from a single-form designer schema with legacy top-level fallback', () => {
    const governance = { fieldEvents: [{ id: 'evt1' }], formInit: { contextDefaults: [] } }

    expect(resolveDesignerFormGovernance({
      formKey: 'default',
      layout: {},
      settings: { governance },
    })).toBe(governance)

    expect(resolveDesignerFormGovernance({
      layout: {},
      governance,
    })).toBe(governance)
  })

  it('returns safe empty governance defaults when the designer schema is missing', () => {
    expect(resolveDesignerFormGovernance(null)).toEqual({})
    expect(resolveDesignerFormGovernance({ forms: [] })).toEqual({})

    const props = buildRuntimeCrudProps({ options: {} })
    expect(props.fieldEvents).toEqual([])
    expect(props.formInit).toEqual({})
  })

  it('applies designer visibility.readonly onto runtime editSchema fields', () => {
    const props = buildRuntimeCrudProps({
      editSchema: [
        { field: 'name', type: 'input', label: '名称' },
        { field: 'status', type: 'select', label: '状态' },
        { field: 'hiddenField', type: 'input', label: '隐藏' },
      ],
      options: {
        formDesignerSchema: {
          components: [
            {
              componentKey: 'input',
              fieldBinding: { fieldCode: 'name' },
              visibility: { readonly: true },
            },
            {
              componentKey: 'select',
              fieldBinding: { fieldCode: 'status' },
            },
            {
              componentKey: 'input',
              fieldBinding: { fieldCode: 'hiddenField' },
              visibility: { hidden: true },
            },
          ],
        },
      },
    })

    expect(props.editSchema.find(item => item.field === 'name')).toMatchObject({
      readonly: true,
      disabled: true,
    })
    expect(props.editSchema.find(item => item.field === 'status')).toMatchObject({
      field: 'status',
    })
    expect(props.editSchema.find(item => item.field === 'status')?.readonly).toBeFalsy()
    expect(props.editSchema.find(item => item.field === 'hiddenField')).toBeUndefined()
    expect(props.uiDocument.components[0].editable).toBe(false)
    expect(props.uiDocument.components[1].editable).toBe(true)
  })

  it('designer visibility.readonly=false unlocks fields even when base editSchema is readonly', () => {
    const props = buildRuntimeCrudProps({
      editSchema: [
        {
          field: 'fieldMoney',
          type: 'number',
          label: '金额',
          readonly: true,
          disabled: true,
          props: { disabled: true, readonly: true },
        },
      ],
      options: {
        formDesignerSchema: {
          components: [
            {
              componentKey: 'money',
              label: 'E2E_money',
              fieldBinding: { fieldCode: 'fieldMoney' },
              visibility: { hidden: false, readonly: false },
              props: { disabled: false, readonly: false, e2eProbe: 1 },
            },
          ],
        },
      },
    })

    expect(props.editSchema.find(item => item.field === 'fieldMoney')).toMatchObject({
      readonly: false,
      disabled: false,
      props: {
        disabled: false,
        readonly: false,
        e2eProbe: 1,
      },
    })
    expect(props.uiDocument.components[0].editable).toBe(true)
  })

  it('merges designer-only fields into editSchema so new inputs are not dropped by uiDocument', () => {
    const props = buildRuntimeCrudProps({
      editSchema: [
        { field: 'name', type: 'input', label: '名称' },
      ],
      options: {
        formDesignerSchema: {
          components: [
            {
              componentKey: 'input',
              label: '名称',
              fieldBinding: { fieldCode: 'name' },
            },
            {
              componentKey: 'input',
              label: '新字段',
              fieldBinding: { fieldCode: 'fieldInputNew' },
              visibility: { readonly: true },
            },
          ],
        },
      },
    })

    expect(props.editSchema.map(item => item.field)).toEqual(['name', 'fieldInputNew'])
    expect(props.editSchema.find(item => item.field === 'fieldInputNew')).toMatchObject({
      label: '新字段',
      type: 'input',
      readonly: true,
      disabled: true,
    })
  })

  it('applyDesignerVisibilityToFields is a no-op without designer schema', () => {
    const fields = [{ field: 'a', type: 'input' }]
    expect(applyDesignerVisibilityToFields(fields, null)).toEqual(fields)
    expect(mergeDesignerEditSchema(fields, null)).toEqual(fields)
  })

  it('overlayFormDesignerOnRuntimeCrudProps switches add/edit form to bound page form asset', () => {
    const base = buildRuntimeCrudProps({
      configKey: 'customer',
      editSchema: [{ field: 'oldName', type: 'input', label: '旧名称' }],
      options: {
        formDesignerSchema: {
          components: [
            { componentKey: 'input', label: '旧名称', fieldBinding: { fieldCode: 'oldName' } },
          ],
        },
      },
    })
    const overlaid = overlayFormDesignerOnRuntimeCrudProps(base, {
      components: [
        { componentKey: 'input', label: '客户名', fieldBinding: { fieldCode: 'customerName' } },
        { componentKey: 'input', label: '手机号', fieldBinding: { fieldCode: 'mobile' } },
      ],
      layout: { formOpenMode: 'drawer', modalWidth: '960px' },
    })

    expect(overlaid.editSchema.map(item => item.field)).toEqual(['customerName', 'mobile'])
    expect(overlaid.pageFormAssetBound).toBe(true)
    expect(overlaid.formOpenMode).toBe('drawer')
    expect(overlaid.modalWidth).toBe('960px')
    expect(overlaid.apiConfig).toEqual(base.apiConfig)
    expect(overlaid.configKey).toBe('customer')
  })

  it('resolveBoundFormDesignerSchema reads formAssetId or single form fallback', () => {
    const assets = [
      { id: 'form_a', formDesignerSchema: { components: [{ fieldBinding: { fieldCode: 'a' } }] } },
      { id: 'form_b', formDesignerSchema: { components: [{ fieldBinding: { fieldCode: 'b' } }] } },
    ]
    expect(resolveBoundFormDesignerSchema({ props: { formAssetId: 'form_b' } }, assets))
      .toEqual(assets[1].formDesignerSchema)
    expect(resolveBoundFormDesignerSchema({}, [assets[0]])).toEqual(assets[0].formDesignerSchema)
    expect(resolveBoundFormDesignerSchema({}, assets)).toBeNull()
  })
})
