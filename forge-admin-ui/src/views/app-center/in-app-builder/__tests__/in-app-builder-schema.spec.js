import { describe, expect, it } from 'vitest'
import {
  createInAppFormAsset,
  createNavigationNode,
  hasPendingLegacyObjectPageMigration,
  insertPageComponent,
  isOrphanPageFormObject,
  mergeInAppBuilderOptions,
  moveNavigationNode,
  normalizeFlowInteraction,
  normalizeInAppBuilder,
  normalizeNodeAccess,
  removeNavigationNode,
  resolveNavigationDeleteImpact,
  updateInAppFormAsset,
} from '../in-app-builder-schema'
import { createPageShapeBuilder } from '../page-shape-design'

const APPLICATION = {
  applicationCode: 'crm',
  applicationName: 'CRM',
}

describe('in-app builder schema', () => {
  it('normalizes and persists application flow interaction without inventing runtime requests', () => {
    const flowInteraction = normalizeFlowInteraction({
      approvalActions: [{ actionId: 'approve', operation: 'approve', label: '同意', permissionCode: 'order:approve' }],
      timeline: { enabled: true },
      nodePermissions: [{ nodeKey: 'manager', visibleSectionIds: ['base', 'base'], readonlySectionIds: ['amount'] }],
      callbacks: { approvedActionCode: 'sync_stock' },
    })
    const schema = normalizeInAppBuilder({ inAppBuilder: { flowInteraction } }, APPLICATION, [])
    const options = mergeInAppBuilderOptions({ unrelated: true }, schema)

    expect(schema.flowInteraction.approvalActions[0]).toMatchObject({
      permissionKey: 'order:approve',
      permissionStrategy: 'hide',
    })
    expect(schema.flowInteraction.nodePermissions[0].visibleSectionIds).toEqual(['base'])
    expect(options.inAppBuilder.flowInteraction).toEqual(schema.flowInteraction)
    expect(options.unrelated).toBe(true)
  })

  it('keeps an application without saved pages empty', () => {
    const source = { unrelated: { enabled: true } }
    const schema = normalizeInAppBuilder(source, APPLICATION, [])

    expect(schema.homePageId).toBeNull()
    expect(schema.nodes).toEqual([])
    expect(source).toEqual({ unrelated: { enabled: true } })
  })

  it('restores a legacy primary-object application into the new page tree', () => {
    const schema = normalizeInAppBuilder({
      primaryObjectCode: 'PS_PRESALE_ORDER',
      inAppBuilder: { nodes: [], pages: {}, formAssets: [] },
    }, {
      applicationName: '门店预售登记',
      icon: 'ionicons5:CartOutline',
    }, [{
      objectId: '1910000000000001111',
      objectRole: 'PRIMARY',
      objectCode: 'PS_PRESALE_ORDER',
      objectName: '预售单',
      configKey: 'ps_presale_order',
      layoutType: 'master-detail-crud',
      options: '{"pageKey":"list"}',
    }])

    expect(schema.homePageId).toBe('page_ps_presale_order')
    expect(schema.legacyObjectPageMigrated).toBe(true)
    expect(schema.nodes).toEqual([
      expect.objectContaining({
        id: 'page_ps_presale_order',
        title: '预售单',
        pageType: 'object',
        pageTemplate: 'master-detail',
        mountTarget: 'BOTH',
        legacyObjectPage: true,
        objectRef: expect.objectContaining({
          objectId: '1910000000000001111',
          objectCode: 'PS_PRESALE_ORDER',
          configKey: 'ps_presale_order',
          hasBusinessData: true,
        }),
      }),
    ])
    expect(schema.pages.page_ps_presale_order.title).toBe('预售单')
  })

  it('does not recreate a legacy object page after the migrated page was deliberately removed', () => {
    const schema = normalizeInAppBuilder({
      primaryObjectCode: 'ORDER',
      inAppBuilder: {
        legacyObjectPageMigrated: true,
        nodes: [],
        pages: {},
      },
    }, APPLICATION, [{
      objectId: '12',
      objectRole: 'PRIMARY',
      objectCode: 'ORDER',
      objectName: '订单',
      configKey: 'order',
    }])

    expect(schema.nodes).toEqual([])
    expect(schema.pages).toEqual({})
    expect(schema.legacyObjectPageMigrated).toBe(true)
  })

  it('marks the one-time legacy page projection for draft persistence', () => {
    const schema = normalizeInAppBuilder({
      primaryObjectCode: 'ORDER',
      inAppBuilder: { nodes: [], pages: {} },
    }, APPLICATION, [{
      objectId: '12',
      objectRole: 'PRIMARY',
      objectCode: 'ORDER',
      objectName: '订单',
      configKey: 'order',
    }])

    expect(hasPendingLegacyObjectPageMigration({
      primaryObjectCode: 'ORDER',
      inAppBuilder: { nodes: [], pages: {} },
    }, schema)).toBe(true)
    expect(hasPendingLegacyObjectPageMigration({
      primaryObjectCode: 'ORDER',
      inAppBuilder: { legacyObjectPageMigrated: true },
    }, schema)).toBe(false)
  })

  it('creates the first page as the default entry and allows it to be organized into a group', () => {
    const base = normalizeInAppBuilder({}, APPLICATION, [])
    const withGroup = createNavigationNode(base, { type: 'group', title: '销售管理' })
    const group = withGroup.nodes.find(node => node.type === 'group')
    const withPage = createNavigationNode(withGroup, {
      type: 'page',
      pageType: 'content',
      parentId: group.id,
      title: '销售概览',
    })

    expect(withPage.nodes.find(node => node.title === '销售概览')).toMatchObject({ parentId: group.id })
    expect(withPage.nodes.find(node => node.title === '销售概览')).toMatchObject({ systemMenuVisible: false })
    expect(withPage.homePageId).toBe(withPage.nodes.find(node => node.title === '销售概览').id)
    expect(moveNavigationNode(withPage, withPage.homePageId, null).nodes.find(node => node.id === withPage.homePageId)).toMatchObject({ parentId: null })
  })

  it('creates a data page under a newly-created unsaved group and preserves both after a save round trip', () => {
    const base = normalizeInAppBuilder({}, APPLICATION, [])
    const grouped = createNavigationNode(base, { type: 'group', title: '售前管理' })
    const group = grouped.nodes.find(node => node.type === 'group')
    const created = createPageShapeBuilder(grouped, {
      pageName: '预售登记',
      pageType: 'form',
      objectName: '预售登记',
      objectCode: 'presaleRegistration',
      parentId: group.id,
    })
    const savedOptions = mergeInAppBuilderOptions({}, created.schema)
    const reloaded = normalizeInAppBuilder(savedOptions, APPLICATION, [])

    expect(created.schema.nodes.find(node => node.id === created.pageId)).toMatchObject({ parentId: group.id })
    expect(reloaded.nodes.find(node => node.id === group.id)).toMatchObject({ type: 'group', title: '售前管理' })
    expect(reloaded.nodes.find(node => node.id === created.pageId)).toMatchObject({ parentId: group.id })
    expect(reloaded.formAssets.some(asset => asset.id === created.formAssetId)).toBe(true)
  })

  it('preserves only an explicitly enabled system menu flag', () => {
    const schema = normalizeInAppBuilder({
      inAppBuilder: {
        nodes: [
          { id: 'page_default', type: 'page', title: '默认页面' },
          { id: 'page_menu', type: 'page', title: '菜单页面', systemMenuVisible: true },
        ],
      },
    }, APPLICATION, [])

    expect(schema.nodes).toEqual(expect.arrayContaining([
      expect.objectContaining({ id: 'page_default', systemMenuVisible: false }),
      expect.objectContaining({ id: 'page_menu', systemMenuVisible: true }),
    ]))
  })

  it('round-trips client-specific menu mount settings', () => {
    const schema = normalizeInAppBuilder({
      inAppBuilder: {
        nodes: [{
          id: 'page_mount',
          type: 'page',
          title: '移动入口',
          settings: {
            systemMenuVisible: true,
            mountTarget: 'BOTH',
            menuName: '登记入口',
            menuParentId: '42',
            mobileMenuParentId: '84',
            menuSort: 7,
          },
        }],
      },
    }, APPLICATION, [])
    const reloaded = normalizeInAppBuilder(mergeInAppBuilderOptions({}, schema), APPLICATION, [])
    expect(reloaded.nodes[0]).toMatchObject({
      systemMenuVisible: true,
      mountTarget: 'BOTH',
      menuName: '登记入口',
      menuParentId: '42',
      mobileMenuParentId: '84',
      menuSort: 7,
    })
  })

  it('round-trips page print watermark with the page settings snapshot', () => {
    const schema = normalizeInAppBuilder({
      inAppBuilder: {
        nodes: [{
          id: 'page_print',
          type: 'page',
          title: '采购单',
          printWatermark: { enabled: true, text: '内部资料', showUsername: false, showTime: true, density: 'dense', fontSizePt: 18, color: '#112233' },
        }],
      },
    }, APPLICATION, [])
    const reloaded = normalizeInAppBuilder(mergeInAppBuilderOptions({}, schema), APPLICATION, [])
    expect(reloaded.nodes[0].printWatermark).toEqual({
      enabled: true,
      text: '内部资料',
      showUsername: false,
      showTime: true,
      density: 'dense',
      fontSizePt: 18,
      color: '#112233',
    })
  })

  it('requires an explicit strategy when deleting a group with child pages', () => {
    const base = createNavigationNode(normalizeInAppBuilder({}, APPLICATION, []), { type: 'page', title: '总览' })
    const withGroup = createNavigationNode(base, { type: 'group', title: '销售管理' })
    const group = withGroup.nodes.find(node => node.type === 'group')
    const withChild = createNavigationNode(withGroup, {
      type: 'page',
      pageType: 'content',
      parentId: group.id,
      title: '商机',
    })

    expect(() => removeNavigationNode(withChild, group.id)).toThrow('请选择页面组删除后的子页面处理方式')
    const moved = removeNavigationNode(withChild, group.id, { type: 'move-children', targetParentId: null })
    expect(moved.nodes.find(node => node.title === '商机')).toMatchObject({ parentId: null })
  })

  it('allows deleting the final page and restores the empty application state', () => {
    const withPage = createNavigationNode(normalizeInAppBuilder({}, APPLICATION, []), { type: 'page', title: '总览' })
    const emptied = removeNavigationNode(withPage, withPage.homePageId)

    expect(emptied.nodes).toEqual([])
    expect(emptied.pages).toEqual({})
    expect(emptied.homePageId).toBeNull()
  })

  it('keeps object page templates as a lightweight business object reference', () => {
    const schema = createNavigationNode(normalizeInAppBuilder({}, APPLICATION, []), {
      type: 'page',
      title: '供应商列表',
      pageType: 'object',
      pageTemplate: 'crud',
      objectRef: { objectId: '12', objectCode: 'pw_supplier', configKey: 'pw_supplier', pageMode: 'crud' },
    })
    const page = schema.nodes[0]

    expect(page).toMatchObject({ pageTemplate: 'crud', pageType: 'object' })
    expect(page.objectRef).toEqual(expect.objectContaining({ objectId: '12', objectCode: 'pw_supplier', configKey: 'pw_supplier', pageMode: 'crud' }))
    expect(page.objectRef).not.toHaveProperty('modelSchema')
  })

  it('inserts a registered component at the end of an empty page and selects it', () => {
    const base = createNavigationNode(normalizeInAppBuilder({}, APPLICATION, []), { type: 'page', title: '总览' })
    const result = insertPageComponent(base, base.homePageId, {
      componentKey: 'page-title',
      label: '标题',
    })

    expect(result.selectedComponentId).toBeTruthy()
    expect(result.schema.pages[base.homePageId].layout.items).toEqual([
      expect.objectContaining({ id: result.selectedComponentId, componentKey: 'page-title' }),
    ])
  })

  it('preserves unrelated application options when saving the builder schema', () => {
    const schema = createNavigationNode(normalizeInAppBuilder({}, APPLICATION, []), { type: 'page', title: '总览' })
    const options = mergeInAppBuilderOptions(JSON.stringify({ release: { version: 2 }, extension: { enabled: true } }), schema)

    expect(options).toMatchObject({
      release: { version: 2 },
      extension: { enabled: true },
      inAppBuilder: expect.objectContaining({ homePageId: schema.homePageId }),
    })
  })

  it('preserves the reused list designer grid layout on application pages', () => {
    const schema = normalizeInAppBuilder({
      inAppBuilder: {
        homePageId: 'page_home',
        nodes: [{ id: 'page_home', type: 'page', pageType: 'home', title: '首页', parentId: null, sort: 0 }],
        pages: {
          page_home: {
            layout: {
              gridLayout: { designWidth: 1366, items: [{ id: 'block_title', blockType: 'page-title' }] },
            },
          },
        },
      },
    }, APPLICATION, [])

    expect(schema.pages.page_home.layout.gridLayout).toMatchObject({
      designWidth: 1366,
      items: [{ id: 'block_title', blockType: 'page-title' }],
    })
  })

  it('keeps form assets outside navigation and persists their reusable designer schema', () => {
    const base = normalizeInAppBuilder({}, APPLICATION, [])
    const created = createInAppFormAsset(base, {
      name: '客户录入表单',
      formDesignerSchema: { formKey: 'customer_form', components: [] },
    })
    const updated = updateInAppFormAsset(created.schema, created.formAssetId, {
      formDesignerSchema: {
        formKey: 'customer_form',
        formName: '客户录入表单',
        components: [{ id: 'cmp_name', componentKey: 'input', fieldBinding: { fieldCode: 'customerName' } }],
      },
    })

    expect(updated.nodes.some(node => node.id === created.formAssetId)).toBe(false)
    expect(updated.formAssets).toEqual([
      expect.objectContaining({ id: created.formAssetId, name: '客户录入表单', formKey: 'customer_form' }),
    ])
    expect(mergeInAppBuilderOptions({}, updated).inAppBuilder.formAssets[0].formDesignerSchema.components).toHaveLength(1)
  })

  it('does not recycle page or form ids after a chinese page is removed', () => {
    const base = normalizeInAppBuilder({}, APPLICATION, [])
    const first = createPageShapeBuilder(base, {
      pageName: '审批',
      pageType: 'form',
      objectName: '审批',
      objectCode: 'approval',
    })
    const removed = removeNavigationNode(first.schema, first.pageId)
    removed.formAssets = (removed.formAssets || []).filter(asset => asset.id !== first.formAssetId)
    const second = createPageShapeBuilder(removed, {
      pageName: '审批',
      pageType: 'form',
      objectName: '审批',
      objectCode: 'approval_new',
    })

    expect(second.pageId).not.toBe(first.pageId)
    expect(second.formAssetId).not.toBe(first.formAssetId)
    expect(second.pageId).toMatch(/^page_page_[a-z0-9]{6}$/)
    expect(second.formAssetId).toMatch(/^form_form_[a-z0-9]{6}$/)
  })

  it('hides page-form objects after their source page is removed', () => {
    const schema = {
      nodes: [{ id: 'page_keep', type: 'page', objectRef: { objectId: '2' } }],
      pages: { page_keep: {} },
    }
    expect(isOrphanPageFormObject({
      objectId: '1',
      options: JSON.stringify({ managedBy: 'PAGE_FORM', sourcePageId: 'page_gone' }),
    }, schema)).toBe(true)
    expect(isOrphanPageFormObject({
      objectId: '2',
      options: JSON.stringify({ managedBy: 'PAGE_FORM', sourcePageId: 'page_gone' }),
    }, schema)).toBe(false)
    expect(isOrphanPageFormObject({
      objectId: '3',
      options: JSON.stringify({ managedBy: 'MANUAL' }),
    }, schema)).toBe(false)
  })

  it('previews delete impact for page-form objects and bound tables', () => {
    const schema = {
      nodes: [
        { id: 'group_root', type: 'group', title: '分组', parentId: null },
        {
          id: 'page_kpi',
          type: 'page',
          title: 'KPI',
          parentId: 'group_root',
          objectRef: { objectId: '10', objectCode: 'kpi' },
        },
      ],
      pages: { page_kpi: {} },
    }
    const objects = [
      {
        objectId: '10',
        objectCode: 'kpi',
        objectName: '指标',
        tableName: 'biz_kpi',
        options: JSON.stringify({ managedBy: 'PAGE_FORM', sourcePageId: 'page_kpi' }),
      },
    ]

    const pageImpact = resolveNavigationDeleteImpact(schema, 'page_kpi', undefined, objects)
    expect(pageImpact.removedPageIds).toEqual(['page_kpi'])
    expect(pageImpact.impactedObjects).toEqual([
      expect.objectContaining({
        objectCode: 'kpi',
        tableName: 'biz_kpi',
        reason: 'page-form',
      }),
    ])

    const moveChildren = resolveNavigationDeleteImpact(
      schema,
      'group_root',
      { type: 'move-children', targetParentId: null },
      objects,
    )
    expect(moveChildren.removedPageIds).toEqual([])
    expect(moveChildren.impactedObjects).toEqual([])

    const deleteChildren = resolveNavigationDeleteImpact(
      schema,
      'group_root',
      { type: 'delete-children' },
      objects,
    )
    expect(deleteChildren.removedPageIds).toEqual(['page_kpi'])
    expect(deleteChildren.impactedObjects).toHaveLength(1)
  })

  it('normalizes node access control and keeps roles grants across save round trips', () => {
    const schema = normalizeInAppBuilder({
      inAppBuilder: {
        nodes: [
          { id: 'page_sales', type: 'page', title: '销售', access: { mode: 'roles', roleIds: ['2', 3, 3, null] } },
          { id: 'page_hr', type: 'page', title: '人事', access: { mode: 'unknown' } },
          { id: 'page_finance', type: 'page', title: '财务' },
        ],
      },
    }, APPLICATION, [])

    expect(schema.nodes.find(node => node.id === 'page_sales').access).toEqual({ mode: 'roles', roleIds: [2, 3] })
    expect(schema.nodes.find(node => node.id === 'page_hr').access).toEqual({ mode: 'inherit', roleIds: [] })
    expect(schema.nodes.find(node => node.id === 'page_finance').access).toEqual({ mode: 'inherit', roleIds: [] })

    const options = mergeInAppBuilderOptions({}, schema)
    expect(options.inAppBuilder.nodes.find(node => node.id === 'page_sales').access).toEqual({ mode: 'roles', roleIds: [2, 3] })
  })

  it('defaults new navigation nodes to inherit access and accepts explicit roles input', () => {
    const base = normalizeInAppBuilder({}, APPLICATION, [])
    const inherited = createNavigationNode(base, { type: 'page', title: '默认页' })
    const restricted = createNavigationNode(inherited, {
      type: 'page',
      title: '受限页',
      access: { mode: 'roles', roleIds: [7] },
    })

    expect(inherited.nodes.find(node => node.title === '默认页').access).toEqual({ mode: 'inherit', roleIds: [] })
    expect(restricted.nodes.find(node => node.title === '受限页').access).toEqual({ mode: 'roles', roleIds: [7] })
    expect(normalizeNodeAccess(null)).toEqual({ mode: 'inherit', roleIds: [] })
  })
})
