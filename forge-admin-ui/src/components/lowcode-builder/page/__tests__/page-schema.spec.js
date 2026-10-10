import { describe, expect, it } from 'vitest'
import {
  applyGridLayoutToZones,
  buildGridSyncModelSchema,
  createDefaultListGridLayout,
  DATA_FIELD_BLOCK_TYPES,
  isPageFieldVisible,
  listPageBlockCatalog,
  resolveChildListDisplayHint,
  resolveChildListDisplayMode,
  resolveListFieldTitle,
  resolveListPageBlockMeta,
  syncGridLayoutWithModel,
  syncPageSchemaWithModel,
} from '../page-schema'
import { resolveSelectedFieldRefs } from '../fieldDrawerConfig'

describe('page builder data component catalog', () => {
  it('uses business-facing names while retaining technical names as secondary metadata', () => {
    expect(DATA_FIELD_BLOCK_TYPES).toEqual([
      'AiCrudPage',
      'AiForm',
      'AiTable',
      'data-table',
      'search-form',
      'detail-info',
    ])

    expect(resolveListPageBlockMeta('AiCrudPage')).toMatchObject({
      title: '数据列表',
      techTitle: 'AiCrudPage',
    })
    expect(resolveListPageBlockMeta('AiForm')).toMatchObject({
      title: '数据表单',
      techTitle: 'AiForm',
    })
    expect(resolveListPageBlockMeta('AiTable')).toMatchObject({
      title: '数据表格',
      techTitle: 'AiTable',
    })

    const visibleTitles = listPageBlockCatalog.map(item => item.title)
    expect(new Set(visibleTitles).size).toBe(visibleTitles.length)
  })
})

describe('default list columns', () => {
  it('excludes audit/system fields from the default CRUD list columns', () => {
    const modelSchema = {
      appType: 'SIMPLE',
      fields: [
        { field: 'id', columnName: 'id' },
        { field: 'customerName', columnName: 'customer_name', listVisible: true },
        { field: 'amount', columnName: 'amount', listVisible: true },
        { field: 'createBy', columnName: 'create_by' },
        { field: 'createTime', columnName: 'create_time' },
        { field: 'createDept', columnName: 'create_dept' },
        { field: 'updateBy', columnName: 'update_by' },
        { field: 'updateTime', columnName: 'update_time' },
      ],
    }
    const layout = createDefaultListGridLayout(modelSchema, { layoutType: 'simple-crud' })
    const crud = layout.items.find(item => item.blockType === 'AiCrudPage')
    expect(crud.fieldRefs).toContain('customerName')
    expect(crud.fieldRefs).toContain('amount')
    expect(crud.fieldRefs).toContain('id')
    expect(crud.fieldRefs).not.toContain('createBy')
    expect(crud.fieldRefs).not.toContain('createTime')
    expect(crud.fieldRefs).not.toContain('createDept')
    expect(crud.fieldRefs).not.toContain('updateBy')
    expect(crud.fieldRefs).not.toContain('updateTime')
  })

  it('does not default left-tree source to the current list object', () => {
    const layout = createDefaultListGridLayout({
      businessName: '订单',
      objectCode: 'order',
      fields: [
        { field: 'id', label: '编号', listVisible: true },
        { field: 'parentId', label: '父级', listVisible: false },
        { field: 'name', label: '名称', listVisible: true },
      ],
    }, { layoutType: 'tree-crud' })
    const tree = layout.items.find(item => item.blockType === 'tree-panel')
    expect(tree).toBeTruthy()
    expect(tree.props.sourceModelCode).toBe('')
    expect(tree.props.sourceConfigKey).toBe('')
  })

  it('preserves cross-object tree source when syncing layout with current model', () => {
    const modelSchema = {
      businessName: '订单',
      objectCode: 'order',
      fields: [
        { field: 'id', label: '编号', listVisible: true },
        { field: 'categoryId', label: '分类', listVisible: true },
        { field: 'orderName', label: '订单名', listVisible: true },
      ],
    }
    const layout = syncGridLayoutWithModel({
      cols: 12,
      rowHeight: 32,
      gap: 8,
      layoutType: 'tree-crud',
      items: [{
        id: 'block_tree',
        blockType: 'tree-panel',
        gridX: 0,
        gridY: 0,
        gridW: 3,
        gridH: 18,
        props: {
          sourceModelCode: 'category_tree',
          sourceModelName: '分类树',
          sourceConfigKey: 'category_tree',
          sourceObjectId: 99,
          treeApi: 'get@/ai/crud/category_tree/tree',
          keyField: 'categoryId',
          parentField: 'parentCategoryId',
          labelField: 'categoryName',
          targetField: 'categoryId',
          filterField: 'categoryId',
          treeTitle: '分类树',
        },
      }, {
        id: 'block_crud',
        blockType: 'AiCrudPage',
        gridX: 3,
        gridY: 0,
        gridW: 9,
        gridH: 10,
        fieldRefs: ['orderName'],
        props: {},
      }],
    }, modelSchema, { layoutType: 'tree-crud' })

    const tree = layout.items.find(item => item.blockType === 'tree-panel')
    expect(tree.props).toMatchObject({
      sourceModelCode: 'category_tree',
      sourceModelName: '分类树',
      sourceConfigKey: 'category_tree',
      sourceObjectId: 99,
      treeApi: 'get@/ai/crud/category_tree/tree',
      keyField: 'categoryId',
      parentField: 'parentCategoryId',
      labelField: 'categoryName',
      targetField: 'categoryId',
      filterField: 'categoryId',
    })
    expect(tree.props.sourceModelCode).not.toBe('order')
    expect(tree.props.labelField).not.toBe('orderName')
  })

  it('clears stale enableTreeAddChild on AiCrudPage when layout is tree-crud and right table is flat', () => {
    const modelSchema = {
      businessName: '订单',
      objectCode: 'order',
      fields: [
        { field: 'id', label: '编号', listVisible: true },
        { field: 'orderName', label: '订单名', listVisible: true },
      ],
    }
    const layout = syncGridLayoutWithModel({
      cols: 12,
      rowHeight: 32,
      gap: 8,
      layoutType: 'tree-crud',
      items: [{
        id: 'block_tree',
        blockType: 'tree-panel',
        gridX: 0,
        gridY: 0,
        gridW: 3,
        gridH: 18,
        props: {
          sourceModelCode: 'category_tree',
          sourceConfigKey: 'category_tree',
          keyField: 'id',
          parentField: 'parentId',
          labelField: 'name',
          filterField: 'categoryId',
          targetField: 'id',
        },
      }, {
        id: 'block_crud',
        blockType: 'AiCrudPage',
        gridX: 3,
        gridY: 0,
        gridW: 9,
        gridH: 10,
        fieldRefs: ['orderName'],
        props: { enableTreeAddChild: true },
      }],
    }, modelSchema, { layoutType: 'tree-crud' })

    const crud = layout.items.find(item => item.blockType === 'AiCrudPage')
    expect(crud.props.enableTreeAddChild).toBe(false)

    const zones = applyGridLayoutToZones(
      [{ zoneKey: 'table', enabled: true, fieldRefs: ['orderName'], props: { enableTreeAddChild: true } }],
      layout,
      modelSchema,
    )
    const tableZone = zones.find(zone => zone.zoneKey === 'table')
    expect(tableZone.props.enableTreeAddChild).toBe(false)
  })

  it('keeps enableTreeAddChild when tree-crud also enables embedded right tree', () => {
    const modelSchema = {
      businessName: '分类',
      objectCode: 'category',
      appType: 'TREE',
      treeConfig: {
        enabled: true,
        parentField: 'parentId',
        enableTreeAddChild: true,
      },
      fields: [
        { field: 'id', label: '编号', listVisible: true },
        { field: 'name', label: '名称', listVisible: true },
        { field: 'parentId', label: '父级', listVisible: false },
      ],
    }
    const layout = syncGridLayoutWithModel({
      cols: 12,
      rowHeight: 32,
      gap: 8,
      layoutType: 'tree-crud',
      items: [{
        id: 'block_tree',
        blockType: 'tree-panel',
        gridX: 0,
        gridY: 0,
        gridW: 3,
        gridH: 18,
        props: {
          sourceModelCode: 'category',
          sourceConfigKey: 'category',
          keyField: 'id',
          parentField: 'parentId',
          labelField: 'name',
        },
      }, {
        id: 'block_crud',
        blockType: 'AiCrudPage',
        gridX: 3,
        gridY: 0,
        gridW: 9,
        gridH: 10,
        fieldRefs: ['name'],
        props: { enableTreeAddChild: false },
      }],
    }, modelSchema, { layoutType: 'tree-crud' })

    const crud = layout.items.find(item => item.blockType === 'AiCrudPage')
    expect(crud.props.enableTreeAddChild).toBe(true)

    const zones = applyGridLayoutToZones(
      [{ zoneKey: 'table', enabled: true, fieldRefs: ['name'], props: { enableTreeAddChild: false } }],
      layout,
      modelSchema,
    )
    const tableZone = zones.find(zone => zone.zoneKey === 'table')
    expect(tableZone.props.enableTreeAddChild).toBe(true)
  })
})

describe('page grid field synchronization', () => {
  it('keeps current component fields when the stable page model has no fields', () => {
    const modelSchema = buildGridSyncModelSchema(
      { configKey: '', fields: [] },
      [{ field: 'customerName', label: '客户名称', listVisible: true }],
    )
    const layout = syncGridLayoutWithModel({
      cols: 12,
      rowHeight: 32,
      gap: 8,
      designWidth: 1366,
      layoutType: 'simple-crud',
      items: [{
        id: 'crud_1',
        blockType: 'AiCrudPage',
        label: '客户列表',
        gridX: 0,
        gridY: 0,
        gridW: 12,
        gridH: 12,
        fieldRefs: ['customerName'],
        props: {
          searchFieldRefs: ['customerName'],
          fieldSettings: { customerName: { visible: true } },
        },
      }],
    }, modelSchema)

    expect(layout.items[0].fieldRefs).toEqual(['customerName'])
    expect(layout.items[0].props.searchFieldRefs).toEqual(['customerName'])
    expect(layout.items[0].props.fieldSettings.customerName.visible).toBe(true)
  })

  it('keeps an explicitly selected child column even when it is hidden on the child object list', () => {
    const childField = 'detail_ujpc__fieldInput'
    const modelSchema = {
      fields: [
        { field: 'fieldInput', sourceField: 'fieldInput', label: '单行文本', listVisible: true, fieldScope: 'main' },
        { field: childField, sourceField: 'fieldInput', label: '单行文本', listVisible: false, fieldScope: 'child', modelCode: 'detail_ujpc', modelName: '测试子表' },
      ],
    }
    const layout = syncGridLayoutWithModel({
      items: [{
        id: 'crud_1',
        blockType: 'AiCrudPage',
        fieldRefs: ['fieldInput', childField],
        props: { fieldSettings: { [childField]: { visible: true } } },
      }],
    }, modelSchema)

    expect(layout.items[0].fieldRefs).toEqual(['fieldInput', childField])
  })

  it('uses the list grid selection to repair stale table zone field refs', () => {
    const childField = 'detail_ujpc__fieldInput'
    const modelSchema = {
      fields: [
        { field: 'fieldInput', sourceField: 'fieldInput', label: '单行文本', listVisible: true, fieldScope: 'main' },
        { field: childField, sourceField: 'fieldInput', label: '明细文本', listVisible: true, fieldScope: 'child', modelCode: 'detail_ujpc' },
      ],
    }
    const gridLayout = {
      items: [{
        id: 'crud_1',
        blockType: 'AiCrudPage',
        fieldRefs: ['fieldInput', childField],
        props: { searchFieldRefs: ['fieldInput'] },
      }],
    }
    const staleZones = [{
      zoneKey: 'table',
      componentKey: 'data-table',
      enabled: true,
      fieldRefs: ['fieldInput'],
      props: {},
    }]

    const zones = applyGridLayoutToZones(staleZones, gridLayout, modelSchema)
    expect(zones[0].fieldRefs).toEqual(['fieldInput', childField])

    const normalized = syncPageSchemaWithModel({
      layoutType: 'master-detail-crud',
      listLayoutMode: 'grid',
      listGridLayout: gridLayout,
      pages: [{ pageKey: 'list', gridLayout }],
      zones: staleZones,
    }, modelSchema)
    expect(normalized.zones.find(zone => zone.zoneKey === 'table')?.fieldRefs)
      .toEqual(['fieldInput', childField])
  })

  it('migrates legacy tree property names when rebuilding the table zone', () => {
    const modelSchema = {
      fields: [
        { field: 'id', label: '编号', listVisible: true },
        { field: 'parentId', label: '父级', listVisible: true },
        { field: 'name', label: '名称', listVisible: true },
      ],
    }
    const zones = applyGridLayoutToZones([
      { zoneKey: 'table', componentKey: 'data-table', fieldRefs: ['name'], props: {} },
    ], {
      items: [{
        blockType: 'tree-panel',
        props: {
          nodeKeyField: 'id',
          parentIdField: 'parentId',
          displayField: 'name',
          nodeValueField: 'id',
          rightFilterField: 'parentId',
          title: '组织树',
          lazy: true,
        },
      }, {
        blockType: 'AiCrudPage',
        fieldRefs: ['name'],
        props: {},
      }],
    }, modelSchema)

    expect(zones[0].props.treeConfig).toMatchObject({
      keyField: 'id',
      parentField: 'parentId',
      labelField: 'name',
      targetField: 'id',
      filterField: 'parentId',
      treeTitle: '组织树',
      loadMode: 'lazy',
    })
  })

  it('keeps embedded treeConfig on table zone when model tree is enabled without tree-panel', () => {
    const modelSchema = {
      appType: 'TREE',
      treeConfig: {
        enabled: true,
        keyField: 'id',
        parentField: 'parentId',
        labelField: 'name',
        childrenField: 'children',
        loadMode: 'full',
      },
      fields: [
        { field: 'id', label: '编号', listVisible: true },
        { field: 'parentId', label: '父级', listVisible: false },
        { field: 'name', label: '名称', listVisible: true },
      ],
    }
    const zones = applyGridLayoutToZones([
      { zoneKey: 'table', componentKey: 'AiCrudPage', fieldRefs: ['name'], props: {} },
    ], {
      items: [{
        blockType: 'AiCrudPage',
        fieldRefs: ['name'],
        props: {},
      }],
    }, modelSchema)

    expect(zones[0].props.treeConfig).toMatchObject({
      enabled: true,
      keyField: 'id',
      parentField: 'parentId',
      labelField: 'name',
      childrenField: 'children',
      loadMode: 'full',
    })
  })

  it('keeps searchFieldRefs for form-readonly business fields when toggling query role', () => {
    const modelSchema = {
      fields: [
        { field: 'fieldInput', sourceField: 'fieldInput', label: '单行文本', listVisible: true, readonly: true },
        { field: 'status', sourceField: 'status', label: '状态', listVisible: true },
      ],
    }
    expect(isPageFieldVisible(modelSchema.fields[0], 'search')).toBe(true)

    const layout = syncGridLayoutWithModel({
      items: [{
        id: 'crud_1',
        blockType: 'AiCrudPage',
        fieldRefs: ['fieldInput', 'status'],
        props: { searchFieldRefs: ['fieldInput'] },
      }],
    }, modelSchema)

    expect(layout.items[0].props.searchFieldRefs).toEqual(['fieldInput'])
    expect(resolveSelectedFieldRefs(layout.items[0], 'search', modelSchema.fields)).toEqual(['fieldInput'])
  })
})

describe('child list display mode', () => {
  it('defaults to aggregate and explains both modes', () => {
    expect(resolveChildListDisplayMode()).toBe('aggregate')
    expect(resolveChildListDisplayMode('expand')).toBe('expand')
    expect(resolveChildListDisplayHint('aggregate')).toContain('一条主表记录只占一行')
    expect(resolveChildListDisplayHint('expand')).toContain('分页按展开后的行数计算')
  })

  it('shows only the field label for a child-table list column', () => {
    expect(resolveListFieldTitle({
      field: 'detail_ujpc__fieldInput',
      sourceField: 'fieldInput',
      fieldScope: 'child',
      modelName: '测试子表',
      label: '测试子表 · 字段A',
      rawLabel: '字段A',
    })).toBe('字段A')

    expect(resolveListFieldTitle({
      field: 'detail_ujpc__fieldInput',
      sourceField: 'fieldInput',
      fieldScope: 'child',
      modelName: '测试子表',
      label: '测试子表.字段A',
    })).toBe('字段A')

    expect(resolveListFieldTitle({
      field: 'detail_ujpc__fieldInput',
      sourceField: 'fieldInput',
      fieldScope: 'child',
      modelName: '测试子表',
      label: '字段A',
    }, {
      title: '测试子表 · 字段A',
    })).toBe('字段A')
  })
})
