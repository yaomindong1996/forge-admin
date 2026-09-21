import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'
import { usePrintWorkspaceStore } from '@/stores/print/printWorkspaceStore'
import { printWorkspaceSources } from '../printWorkspaceSources'

const model = { objectId: '9007199254740993', objectCode: 'purchase', configKey: 'purchase_cfg', objectName: '采购单' }
const page = id => ({ id, type: 'page', title: '采购管理', objectRef: { ...model } })
const app = (nodes = [page('page_purchase')], pages = {}) => ({ id: '2', options: JSON.stringify({ inAppBuilder: { nodes, pages } }) })
describe('应用工作台打印来源', () => {
  beforeEach(() => setActivePinia(createPinia()))
  it('从页面实际引用解析来源并保留大整数；同一对象跨页面独立', () => {
    const choices = printWorkspaceSources(app([page('page_a'), page('page_b')]), [model])
    expect(choices).toHaveLength(2)
    expect(choices.map(item => item.source.pageId)).toEqual(['page_a', 'page_b'])
    expect(choices[0].label).toBe('采购管理 / 采购单')
  })
  it('解析嵌套表单区块并去重，不把应用中无页面引用的对象变成来源', () => {
    const value = app([{ id: 'page_a', type: 'page', title: '工作页' }], { page_a: { layout: { items: [{ props: { objectRef: model } }, { props: { objectRef: model } }] } } })
    expect(printWorkspaceSources(value, [model, { ...model, objectCode: 'other' }])).toHaveLength(1)
    expect(printWorkspaceSources(app([]), [model])).toEqual([])
  })
  it('拒绝失效引用、对象/config 不匹配及非法页面标识', () => {
    const bad = page('page_a')
    bad.objectRef.valid = false
    expect(printWorkspaceSources(app([bad]), [model])).toEqual([])
    bad.objectRef.valid = true
    bad.objectRef.configKey = 'changed'
    expect(printWorkspaceSources(app([bad]), [model])).toEqual([])
    expect(printWorkspaceSources(app([page('../page')]), [model])).toEqual([])
  })
  it('只为旧单对象应用恢复来源，不覆盖用户已经删空的页面', () => {
    const application = { id: '2', options: { primaryObjectCode: 'purchase' } }
    expect(printWorkspaceSources(application, [model])[0].source.pageId).toBe('page_purchase')
    application.options.inAppBuilder = { legacyObjectPageMigrated: true }
    expect(printWorkspaceSources(application, [model])).toEqual([])
  })
  it('同应用刷新保留有效选择，删除来源/切应用立即清理选择', () => {
    const store = usePrintWorkspaceStore()
    store.sync(app(), [model])
    expect(store.source.pageId).toBe('page_purchase')
    store.sync(app([page('page_purchase'), page('page_b')]), [model])
    expect(store.source.pageId).toBe('page_purchase')
    store.sync(app([page('page_b'), page('page_c')]), [model])
    expect(store.source).toBeNull()
    store.selectedKey = store.sources[0].value
    store.sync({ ...app([page('page_b'), page('page_c')]), id: '3' }, [model])
    expect(store.source).toBeNull()
    store.clear()
    expect(store.sources).toEqual([])
  })
})
