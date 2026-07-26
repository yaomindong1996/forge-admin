import { describe, expect, it } from 'vitest'
import {
  createInAppFormAsset,
  createNavigationNode,
  insertPageComponent,
  mergeInAppBuilderOptions,
  moveNavigationNode,
  normalizeInAppBuilder,
  removeNavigationNode,
  updateInAppFormAsset,
} from '../in-app-builder-schema'

const APPLICATION = {
  applicationCode: 'crm',
  applicationName: 'CRM',
}

describe('in-app builder schema', () => {
  it('keeps an application without saved pages empty', () => {
    const source = { unrelated: { enabled: true } }
    const schema = normalizeInAppBuilder(source, APPLICATION, [])

    expect(schema.homePageId).toBeNull()
    expect(schema.nodes).toEqual([])
    expect(source).toEqual({ unrelated: { enabled: true } })
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
    expect(withPage.homePageId).toBe(withPage.nodes.find(node => node.title === '销售概览').id)
    expect(moveNavigationNode(withPage, withPage.homePageId, null).nodes.find(node => node.id === withPage.homePageId)).toMatchObject({ parentId: null })
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
})
