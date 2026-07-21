import { describe, expect, it } from 'vitest'
import {
  createNavigationNode,
  insertPageComponent,
  mergeInAppBuilderOptions,
  moveNavigationNode,
  normalizeInAppBuilder,
  removeNavigationNode,
} from '../in-app-builder-schema'

const APPLICATION = {
  applicationCode: 'crm',
  applicationName: 'CRM',
}

describe('in-app builder schema', () => {
  it('creates an immutable default home page for applications without saved options', () => {
    const source = { unrelated: { enabled: true } }
    const schema = normalizeInAppBuilder(source, APPLICATION, [])

    expect(schema.homePageId).toBe('page_home')
    expect(schema.nodes).toEqual(expect.arrayContaining([
      expect.objectContaining({ id: 'page_home', type: 'page', pageType: 'home', parentId: null }),
    ]))
    expect(source).toEqual({ unrelated: { enabled: true } })
  })

  it('creates a page in a selected page group and prevents moving home into it', () => {
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
    expect(() => moveNavigationNode(withPage, 'page_home', group.id)).toThrow('首页不能移动到页面组')
  })

  it('requires an explicit strategy when deleting a group with child pages', () => {
    const base = normalizeInAppBuilder({}, APPLICATION, [])
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

  it('inserts a registered component at the end of an empty page and selects it', () => {
    const base = normalizeInAppBuilder({}, APPLICATION, [])
    const result = insertPageComponent(base, 'page_home', {
      componentKey: 'page-title',
      label: '标题',
    })

    expect(result.selectedComponentId).toBeTruthy()
    expect(result.schema.pages.page_home.layout.items).toEqual([
      expect.objectContaining({ id: result.selectedComponentId, componentKey: 'page-title' }),
    ])
  })

  it('preserves unrelated application options when saving the builder schema', () => {
    const schema = normalizeInAppBuilder({}, APPLICATION, [])
    const options = mergeInAppBuilderOptions(JSON.stringify({ release: { version: 2 }, extension: { enabled: true } }), schema)

    expect(options).toMatchObject({
      release: { version: 2 },
      extension: { enabled: true },
      inAppBuilder: expect.objectContaining({ homePageId: 'page_home' }),
    })
  })
})
