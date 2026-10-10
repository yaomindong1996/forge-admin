import { flushPromises, mount } from '@vue/test-utils'
import { NConfigProvider, NTag } from 'naive-ui'
import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { h, reactive, ref } from 'vue'
import PluginLicenseSummary from '../components/PluginLicenseSummary.vue'

const api = vi.hoisted(() => ({ getRuntimeLicenseStatus: vi.fn() }))
vi.mock('@/api/system/runtimeLicense', () => api)
let user
let auth
vi.mock('@/store', () => ({ useUserStore: () => user, useAuthStore: () => auth }))
vi.mock('@/composables/useDict', () => ({
  getDictData: vi.fn(),
  useDict: () => ({ dict: ref({
    sys_runtime_license_mode: [{ value: 'license', label: '许可证验证' }],
    sys_runtime_license_state: [{ value: 'expired', label: '已过期' }, { value: 'valid', label: '有效' }],
  }) }),
}))
let wrapper
beforeEach(() => {
  setActivePinia(createPinia())
  vi.clearAllMocks()
  user = reactive({ isAdmin: true, userId: 'unit-user', tenantId: '1', permissions: ['system:plugin:list'] })
  auth = reactive({ accessToken: 'unit-session' })
})
afterEach(() => wrapper?.unmount())
function setup() {
  const plugin = { id: 'sample', name: '测试插件', features: [{ code: 'sample.read' }] }
  wrapper = mount({
    render: () => h(NConfigProvider, null, { default: () => h(PluginLicenseSummary, { plugin }) }),
  }, { global: { components: { NTag } } })
}

describe('客户端只读授权视图', () => {
  it('按当前插件精确筛选，过期如实展示，时间格式化且不显示其它授权', async () => {
    const scope = { pluginIds: ['sample'], featureCodes: ['sample.read', 'private.other'] }
    const entries = [
      { position: 1, state: 'expired', scope, terms: { notBefore: 0, validUntil: 1000, maintenanceUntil: 1000 } },
      { position: 2, state: 'valid', scope: { pluginIds: ['sample-other'], featureCodes: ['secret.scope'] } },
    ]
    api.getRuntimeLicenseStatus.mockResolvedValue({
      code: 200,
      data: { mode: 'license', report: { entries, checkedAt: '2026-10-10T00:00:00Z' } },
    })
    setup()
    await flushPromises()
    expect(wrapper.text()).toContain('已过期')
    expect(wrapper.text()).toContain('sample.read')
    expect(wrapper.text()).not.toContain('private.other')
    expect(wrapper.text()).not.toContain('secret.scope')
    expect(wrapper.text()).not.toContain('T00:00')
    expect(wrapper.findAll('button').map(button => button.text())).toEqual(['刷新状态'])
  })

  it.each(['community', 'custom', 'unavailable'])('没有签名报告时不伪造授权：%s', async (mode) => {
    api.getRuntimeLicenseStatus.mockResolvedValue({ code: 200, data: { mode, report: null } })
    setup()
    await flushPromises()
    expect(wrapper.find('.plugin-license__entry').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('永久使用')
    expect(wrapper.text()).toMatch(/未加载商业许可证|自定义授权机制|未提供可查询/)
  })

  it('未知归属文件和空列表可解释，不展示无效文件的声明', async () => {
    const report = { entries: [{ position: 1, state: 'invalid', scope: null }] }
    api.getRuntimeLicenseStatus.mockResolvedValue({ code: 200, data: { mode: 'license', report } })
    setup()
    await flushPromises()
    expect(wrapper.text()).toContain('无法确认其插件归属')
    expect(wrapper.text()).toContain('未查询到此插件的匹配许可证')
  })

  it('错误可重试，无权限不会请求', async () => {
    api.getRuntimeLicenseStatus.mockRejectedValueOnce(new Error('授权服务不可用'))
      .mockResolvedValueOnce({ code: 200, data: { mode: 'community', report: null } })
    setup()
    await flushPromises()
    expect(wrapper.text()).toContain('授权状态加载失败')
    const retry = wrapper.findAll('button').find(button => button.text() === '重新加载')
    await retry.trigger('click')
    await flushPromises()
    expect(wrapper.text()).toContain('未加载商业许可证')
    wrapper.unmount()
    user.permissions = []
    setup()
    await flushPromises()
    expect(api.getRuntimeLicenseStatus).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('无权查看实例授权')
  })
})
