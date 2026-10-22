import { describe, expect, it } from 'vitest'
import { FIELD_COMPONENT_PALETTE_GROUPS } from '../../components/designer/form-first/fieldComponentCatalog'
import { buildBusinessObjectDesignerPayloadFromFormAsset } from '../page-form-object-promotion'
import {
  createPageShapeBuilder,
  ensureFreeLayoutPageNode,
  isObjectBoundPageLayoutPolluted,
  pageHasFreeLayoutCompanionBlocks,
  normalizePageShapeSelection,
  PAGE_SHAPE_TYPES,
  resolvePageShapeFromNode,
  restoreObjectBoundPageLayout,
} from '../page-shape-design'

function emptyBuilder() {
  return {
    schemaVersion: 2,
    homePageId: null,
    nodes: [],
    pages: {},
    formAssets: [],
    flowInteraction: {},
  }
}

describe('page shape design draft', () => {
  it('exposes the documented page shapes including left-tree-right-table and the five minimum field components', () => {
    expect(PAGE_SHAPE_TYPES.map(item => item.value)).toEqual(['form', 'list', 'list-form', 'tree-list', 'tree-table', 'custom'])

    const componentKeys = FIELD_COMPONENT_PALETTE_GROUPS.flatMap(group => group.items.map(item => item.componentKey))
    expect(componentKeys).toEqual(expect.arrayContaining(['input', 'number', 'date', 'select', 'switch']))
  })

  it('normalizes an editable object code from the page name', () => {
    const selection = normalizePageShapeSelection({ pageName: '客户管理', pageType: 'form' })
    expect(selection).toMatchObject({
      pageName: '客户管理',
      objectName: '客户管理',
      pageType: 'form',
    })
    // 自动生成的编码带随机后缀，避免同套件下重复
    expect(selection.objectCode).toMatch(/^customer_management_[a-z0-9]{4}$/)
    expect(normalizePageShapeSelection({
      pageName: '客户管理',
      objectCode: 'crm_customer',
      pageType: 'form',
    }).objectCode).toBe('crm_customer')
  })

  it('creates a form page, visible object reference, form asset and form-only CRUD block', () => {
    const result = createPageShapeBuilder(emptyBuilder(), {
      pageName: '客户登记',
      objectName: '客户登记',
      objectCode: 'customer_register',
      pageType: 'form',
    })

    expect(result.pageId).toBeTruthy()
    expect(result.formAssetId).toBeTruthy()
    expect(result.schema.nodes).toEqual([
      expect.objectContaining({
        id: result.pageId,
        title: '客户登记',
        pageType: 'object',
        objectRef: expect.objectContaining({
          objectCode: 'customer_register',
          objectName: '客户登记',
          pageMode: 'form',
        }),
      }),
    ])
    expect(result.schema.formAssets[0]).toMatchObject({
      id: result.formAssetId,
      name: '客户登记',
      formDesignerSchema: expect.objectContaining({ formName: '客户登记' }),
    })
    expect(result.schema.pages[result.pageId].layout.gridLayout.items[0]).toMatchObject({
      blockType: 'AiCrudPage',
      props: expect.objectContaining({
        formOnly: true,
        formAssetId: result.formAssetId,
        objectRef: expect.objectContaining({ objectCode: 'customer_register' }),
      }),
    })
  })

  it('imports an existing table into the form canvas with inferred fields', () => {
    const result = createPageShapeBuilder(emptyBuilder(), {
      pageName: '客户档案',
      objectName: '客户',
      objectCode: 'customer',
      pageType: 'list',
      dataSourceMode: 'EXISTING_TABLE',
      runtimeDatasourceId: 31,
      importTableName: 'crm_customer',
      fields: [{
        fieldCode: 'customerName',
        fieldName: '客户名称',
        columnName: 'customer_name',
        fieldType: 'TEXT',
        componentType: 'input',
        formVisible: true,
      }],
    })

    expect(result.selection).toMatchObject({
      createMode: 'DB_IMPORT',
      runtimeDatasourceId: 31,
      importTableName: 'crm_customer',
    })
    expect(result.schema.nodes[0].objectRef).toMatchObject({
      createMode: 'DB_IMPORT',
      runtimeDatasourceId: 31,
      importTableName: 'crm_customer',
    })
    expect(result.schema.formAssets[0].formDesignerSchema.components).toEqual([
      expect.objectContaining({
        componentKey: 'input',
        label: '客户名称',
        fieldBinding: expect.objectContaining({
          fieldCode: 'customerName',
          columnName: 'customer_name',
          createIfMissing: false,
        }),
      }),
    ])
    expect(result.schema.pages[result.pageId].layout.gridLayout.items[0].props.fieldRefs).toEqual(['customerName'])
  })

  it('uses an inline form workspace for list-form pages and no object for custom pages', () => {
    const listForm = createPageShapeBuilder(emptyBuilder(), {
      pageName: '客户管理',
      objectCode: 'customer',
      pageType: 'list-form',
    })
    expect(listForm.schema.pages[listForm.pageId].layout.gridLayout.items[0].props.formOpenMode).toBe('flat')

    const custom = createPageShapeBuilder(emptyBuilder(), {
      pageName: '数据看板',
      pageType: 'custom',
    })
    expect(custom.formAssetId).toBe('')
    expect(custom.schema.formAssets).toEqual([])
    expect(custom.schema.nodes[0]).toMatchObject({ pageType: 'content', pageTemplate: 'custom', pageShape: 'custom', objectRef: null })
  })

  it('presets tree fields and tree-crud layout for left-tree-right-table pages', () => {
    const result = createPageShapeBuilder(emptyBuilder(), {
      pageName: '组织分类',
      objectName: '组织分类',
      objectCode: 'org_category',
      pageType: 'tree-table',
    })

    expect(result.schema.nodes[0]).toMatchObject({
      pageType: 'object',
      pageTemplate: 'tree-table',
      pageShape: 'tree-table',
      objectRef: expect.objectContaining({
        objectCode: 'org_category',
        pageMode: 'tree-table',
      }),
    })
    expect(result.schema.pages[result.pageId].layout.gridLayout.layoutType).toBe('tree-crud')
    const block = result.schema.pages[result.pageId].layout.gridLayout.items[0]
    expect(block).toMatchObject({
      blockType: 'AiCrudPage',
      props: expect.objectContaining({
        layoutType: 'tree-crud',
        enableTreeAddChild: false,
        formOpenMode: 'flat',
        fieldRefs: ['name', 'sortNo', 'parentId'],
        treeConfig: expect.objectContaining({
          keyField: 'id',
          parentField: 'parentId',
          labelField: 'name',
          filterField: 'parentId',
        }),
      }),
    })
    expect(result.schema.formAssets[0].formDesignerSchema.components.map(item => item.fieldBinding.fieldCode))
      .toEqual(['name', 'sortNo', 'parentId'])
    expect(result.schema.formAssets[0].formDesignerSchema.components.every(
      item => item.fieldBinding.createIfMissing === true,
    )).toBe(true)
  })

  it('presets embedded tree list with enabled treeConfig', () => {
    const result = createPageShapeBuilder(emptyBuilder(), {
      pageName: '部门',
      objectName: '部门',
      objectCode: 'dept',
      pageType: 'tree-list',
    })

    expect(result.schema.nodes[0]).toMatchObject({
      pageShape: 'tree-list',
      pageTemplate: 'tree-list',
      objectRef: expect.objectContaining({ pageMode: 'tree-list' }),
    })
    expect(result.schema.pages[result.pageId].layout.gridLayout.layoutType).toBe('list-form')
    const block = result.schema.pages[result.pageId].layout.gridLayout.items[0]
    expect(block.props).toMatchObject({
      layoutType: 'list-form',
      enableTreeAddChild: true,
      fieldRefs: ['name', 'sortNo', 'parentId'],
      treeConfig: expect.objectContaining({
        enabled: true,
        parentField: 'parentId',
        labelField: 'name',
        enableTreeAddChild: true,
      }),
    })
  })

  it('forces free-layout shape when designTab is page even if node looks like list-form', () => {
    const node = {
      id: 'page_free',
      type: 'page',
      pageType: 'object',
      pageTemplate: 'list-form',
      pageShape: '',
      objectRef: { pageMode: 'crud', objectCode: 'order' },
    }
    expect(resolvePageShapeFromNode(node)).toBe('list-form')
    expect(resolvePageShapeFromNode(node, { designTab: 'page' })).toBe('custom')
    expect(ensureFreeLayoutPageNode(node)).toBe(node)
  })

  it('keeps tree shortcuts as object shapes when objectRef is temporarily empty', () => {
    expect(resolvePageShapeFromNode({
      type: 'page',
      pageType: 'object',
      pageTemplate: 'tree-list',
      pageShape: '',
      objectRef: null,
    })).toBe('tree-list')
    expect(resolvePageShapeFromNode({
      type: 'page',
      pageType: 'object',
      pageTemplate: 'tree-table',
      pageShape: 'tree-table',
      objectRef: null,
    })).toBe('tree-table')
  })

  it('restores a polluted list-form page back to a single AiCrudPage', () => {
    const pollutedNode = {
      id: 'page_orders',
      type: 'page',
      title: '订单',
      pageType: 'content',
      pageTemplate: 'custom',
      pageShape: 'custom',
      objectRef: { objectCode: 'order', objectName: '订单', pageMode: 'crud' },
    }
    const schema = {
      nodes: [pollutedNode],
      pages: {
        page_orders: {
          title: '订单',
          layout: {
            gridLayout: {
              items: [
                { id: 'b1', blockType: 'stats-strip', props: {} },
                { id: 'b2', blockType: 'custom-html', props: { formAssetId: 'form_order' } },
              ],
            },
          },
        },
      },
      formAssets: [{ id: 'form_order', name: '订单表单' }],
    }
    expect(isObjectBoundPageLayoutPolluted(pollutedNode, schema.pages.page_orders)).toBe(true)
    const restored = restoreObjectBoundPageLayout(schema, 'page_orders')
    expect(restored.nodes[0]).toMatchObject({
      pageType: 'object',
      pageShape: 'list-form',
      pageTemplate: 'list-form',
      objectRef: expect.objectContaining({ objectCode: 'order', pageMode: 'crud' }),
    })
    expect(restored.pages.page_orders.layout.gridLayout.items).toEqual([
      expect.objectContaining({
        blockType: 'AiCrudPage',
        props: expect.objectContaining({
          formAssetId: 'form_order',
          formOpenMode: 'flat',
        }),
      }),
    ])
    expect(isObjectBoundPageLayoutPolluted(restored.nodes[0], restored.pages.page_orders)).toBe(false)
  })

  it('allows free-layout companion widgets on object list pages when AiCrudPage remains', () => {
    const node = {
      id: 'page_leave_home',
      type: 'page',
      title: '请假中心',
      pageType: 'object',
      pageShape: 'list',
      pageTemplate: 'list',
      objectRef: { objectCode: 'leave_request', objectName: '请假单', pageMode: 'list' },
    }
    const page = {
      title: '请假中心',
      layout: {
        gridLayout: {
          items: [
            { id: 'b1', blockType: 'bar-chart', props: {} },
            { id: 'b2', blockType: 'line-chart', props: {} },
            { id: 'b3', blockType: 'stats-strip', props: {} },
            { id: 'b4', blockType: 'AiCrudPage', props: { formAssetId: 'form_leave' } },
          ],
        },
      },
    }
    expect(isObjectBoundPageLayoutPolluted(node, page)).toBe(false)
    // 运行态已是自由布局伴生页时，编辑入口应识别并进页面设计（而非纯列表壳）
    expect(pageHasFreeLayoutCompanionBlocks(page)).toBe(true)
    expect(resolvePageShapeFromNode(node)).toBe('list')
  })

  it('does not treat a pure AiCrudPage list as free-layout companions', () => {
    expect(pageHasFreeLayoutCompanionBlocks({
      layout: {
        gridLayout: {
          items: [{ id: 'c1', blockType: 'AiCrudPage', props: {} }],
        },
      },
    })).toBe(false)
    expect(pageHasFreeLayoutCompanionBlocks({
      layout: {
        gridLayout: {
          items: [
            { id: 'c1', blockType: 'AiCrudPage', props: {} },
            { id: 't1', blockType: 'data-table', props: {} },
          ],
        },
      },
    })).toBe(false)
  })

  it('marks object page polluted when AiCrudPage is missing', () => {
    const node = {
      id: 'page_orders',
      type: 'page',
      title: '订单',
      pageType: 'object',
      pageShape: 'list',
      objectRef: { objectCode: 'order', pageMode: 'list' },
    }
    const page = {
      layout: {
        gridLayout: {
          items: [{ id: 'b1', blockType: 'bar-chart', props: {} }],
        },
      },
    }
    expect(isObjectBoundPageLayoutPolluted(node, page)).toBe(true)
  })

  it('keeps an existing object field when the form reuses it instead of creating it', () => {
    const result = buildBusinessObjectDesignerPayloadFromFormAsset({
      formDesignerSchema: {
        components: [{
          componentKey: 'input',
          label: '客户名称',
          fieldBinding: {
            mode: 'field',
            fieldCode: 'customerName',
            createIfMissing: false,
          },
        }],
      },
    }, [{
      fieldCode: 'customerName',
      fieldName: '客户名称',
      fieldType: 'TEXT',
      dataType: 'varchar',
      length: 128,
      precision: 0,
      systemField: false,
    }])

    expect(result.fields).toEqual(expect.arrayContaining([
      expect.objectContaining({ fieldCode: 'customerName', fieldType: 'TEXT' }),
    ]))
  })
})
