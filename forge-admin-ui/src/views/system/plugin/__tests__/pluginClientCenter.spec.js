import { flushPromises, mount } from '@vue/test-utils'
import { NConfigProvider, NTag } from 'naive-ui'
import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { h, nextTick, reactive, ref } from 'vue'
import { usePluginCenterStore } from '@/stores/plugin/centerStore'
import PluginCenter from '../../plugin.vue'

const api = vi.hoisted(() => ({ listRuntimePlugins: vi.fn(), getRuntimePlugin: vi.fn() }))
vi.mock('@/api/system/plugin', () => api)
let user
let auth
vi.mock('@/store', () => ({ useUserStore: () => user, useAuthStore: () => auth }))
vi.mock('@/composables/useDict', () => ({
  getDictData: vi.fn(),
  useDict: () => ({ dict: ref({ sys_plugin_origin: [{ value: 'builtin', label: '系统内置' }] }) }),
}))
const records = [
  { id: 'plugin-system', name: '系统管理', origin: 'builtin', version: '1.2.0', features: [] },
  { id: 'plugin-flow', name: '流程引擎', origin: 'builtin', version: '1.2.0', features: [] },
]
let wrapper
beforeEach(() => {
  setActivePinia(createPinia())
  vi.clearAllMocks()
  user = reactive({ isAdmin: true, userId: 'test-user', tenantId: '1', permissions: ['*:*:*'] })
  auth = reactive({ accessToken: 'unit-session' })
  api.listRuntimePlugins.mockResolvedValue({ code: 200, data: { records, total: 32 } })
  api.getRuntimePlugin.mockImplementation(id => Promise.resolve({
    code: 200,
    data: records.find(row => row.id === id),
  }))
})
afterEach(() => {
  wrapper?.unmount()
  wrapper = null
})
function setup() {
  wrapper = mount({ render: () => h(NConfigProvider, null, { default: () => h(PluginCenter) }) }, {
    global: { components: { NTag } },
  })
  return wrapper
}

describe('a 首页与 B 详情真实组件', () => {
  it('初次加载使用模块卡骨架，返回后身份、简介与操作保持独立分区', async () => {
    let resolve
    api.listRuntimePlugins.mockReturnValueOnce(new Promise(done => resolve = done))
    setup()
    await nextTick()
    const skeletons = wrapper.findAll('.plugin-card--skeleton')
    expect(skeletons).toHaveLength(6)
    for (const card of skeletons) {
      expect(card.find('.plugin-card__body .plugin-card__content').exists()).toBe(true)
      expect(card.find('.plugin-card__footer').exists()).toBe(true)
    }
    resolve({ code: 200, data: { records, total: records.length } })
    await flushPromises()
    expect(wrapper.find('.plugin-card--skeleton').exists()).toBe(false)
    const card = wrapper.find('article.plugin-card')
    expect(card.find('.plugin-card__body .plugin-illustration').attributes('style')).toContain('width: 36px')
    expect(card.find('.plugin-card__identity h3').attributes('title')).toBe('系统管理')
    expect(card.find('.plugin-card__identity code').text()).toBe('plugin-system')
    expect(card.find('.plugin-card__summary').attributes('title')).toBe('用户、角色与权限，统一管理')
    expect(card.find('.forge-symbol').attributes('data-symbol')).toBe('shield')
    expect(card.findAll('.plugin-card__capabilities span')).toHaveLength(2)
    expect(wrapper.find('.plugin-center__artwork').attributes('alt')).toBe('')
    expect(card.find('.plugin-card__footer').text()).toContain('v1.2.0')
    expect(card.findAll('.plugin-card__footer button')).toHaveLength(2)
  })

  it('未知插件不显示内置能力，未授权详情操作禁用', async () => {
    api.listRuntimePlugins.mockResolvedValueOnce({ code: 200, data: {
      records: [{ id: 'plugin-system', name: '外部插件', origin: 'external' }],
      total: 1,
    } })
    user.permissions = ['system:plugin:list']
    setup()
    await flushPromises()
    const card = wrapper.find('article.plugin-card')
    expect(card.text()).toContain('版本未提供')
    expect(card.text()).toContain('功能介绍以发布方说明为准')
    expect(card.text()).not.toContain('用户与组织')
    expect(card.find('.forge-symbol').attributes('data-symbol')).toBe('plugins')
    expect(card.findAll('button').every(button => button.attributes('disabled') !== undefined)).toBe(true)
  })

  it('卡片进入同页详情，左侧切换，返回保持来源、关键词和页码', async () => {
    setup()
    await flushPromises()
    const store = usePluginCenterStore()
    store.query.keyword = 'plugin'
    store.query.origin = 'builtin'
    await store.changePage(2)
    const detail = wrapper.findAll('button').find(button => button.text() === '查看详情')
    await detail.trigger('click')
    await flushPromises()
    expect(wrapper.find('.master-detail-workspace').exists()).toBe(true)
    expect(wrapper.find('.plugin-detail').text()).toContain('集中维护组织')
    const next = wrapper.findAll('.plugin-navigation__item').find(button => button.text().includes('流程引擎'))
    await next.trigger('click')
    await flushPromises()
    expect(wrapper.find('.plugin-detail').text()).toContain('通过可视化流程设计')
    await wrapper.get('[aria-label="返回插件列表"]').trigger('click')
    expect(wrapper.find('.master-detail-workspace').exists()).toBe(false)
    expect(store.query).toMatchObject({ keyword: 'plugin', origin: 'builtin', pageNum: 2 })
    expect(api.listRuntimePlugins).toHaveBeenCalledTimes(2)
  })

  it('安装说明直达可读内容，卡片不嵌套按钮，不显示管理工作台', async () => {
    setup()
    await flushPromises()
    const install = wrapper.findAll('button').find(button => button.text() === '安装说明')
    expect(wrapper.find('button button').exists()).toBe(false)
    await install.trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('随项目提供，无需重复安装')
    expect(wrapper.find('input[type="file"]').exists()).toBe(false)
    for (const label of ['构建对比', '安装工作台', '运行时授权', '运维交付'])
      expect(wrapper.text()).not.toContain(label)
  })

  it('无权限不会请求；撤回权限立即清空详情并隔离旧响应', async () => {
    user.permissions = []
    setup()
    await flushPromises()
    expect(api.listRuntimePlugins).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('没有插件中心查询权限')
    user.permissions = ['*:*:*']
    await flushPromises()
    const store = usePluginCenterStore()
    let resolve
    api.getRuntimePlugin.mockReturnValueOnce(new Promise(done => resolve = done))
    const request = store.open('plugin-system')
    user.permissions = []
    resolve({ code: 200, data: records[0] })
    await request
    await nextTick()
    expect(store.detail).toBeNull()
    expect(store.records).toEqual([])
    expect(store.detailVisible).toBe(false)
  })

  it('刷新失败有重试，退出后的列表响应不回填', async () => {
    setup()
    await flushPromises()
    const store = usePluginCenterStore()
    api.listRuntimePlugins.mockResolvedValueOnce({ code: 500, message: '清单服务不可用' })
    await store.load()
    await nextTick()
    expect(wrapper.text()).toContain('清单服务不可用')
    const retry = wrapper.findAll('button').find(button => button.text() === '重新加载')
    await retry.trigger('click')
    await flushPromises()
    expect(store.records).toHaveLength(2)
    let resolve
    api.listRuntimePlugins.mockReturnValueOnce(new Promise(done => resolve = done))
    const request = store.load()
    wrapper.unmount()
    wrapper = null
    resolve({ code: 200, data: { records, total: 2 } })
    await request
    expect(store.records).toEqual([])
  })
})
