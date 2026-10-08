import { flushPromises, mount } from '@vue/test-utils'
import { NConfigProvider, NTag } from 'naive-ui'
import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { h, nextTick, reactive, ref } from 'vue'
import { useRuntimeLicenseStore } from '@/stores/plugin/runtimeLicenseStore'
import RuntimeLicenseStatus from '../components/RuntimeLicenseStatus.vue'

const api = vi.hoisted(() => ({ getRuntimeLicenseStatus: vi.fn() }))
vi.mock('@/api/system/runtimeLicense', () => api)
let user
let auth
vi.mock('@/store', () => ({ useUserStore: () => user, useAuthStore: () => auth }))
vi.mock('@/composables/useDict', () => ({
  getDictData: vi.fn(),
  useDict: () => ({ dict: ref({
    sys_runtime_license_mode: [
      { value: 'community', label: '社区默认' },
      { value: 'license', label: '许可证验证' },
    ],
    sys_runtime_license_state: [{ value: 'valid', label: '有效', listClass: 'success' }],
    sys_plugin_edition: [{ value: 'community', label: '社区版' }, { value: 'ee', label: '商业版' }],
  }) }),
}))
let wrapper
beforeEach(() => {
  setActivePinia(createPinia())
  vi.clearAllMocks()
  user = reactive({ isAdmin: true, userId: '7', tenantId: '1', permissions: ['system:plugin:list'] })
  auth = reactive({ accessToken: 'unit-session-one' })
})
afterEach(() => {
  wrapper?.unmount()
  wrapper = null
})
function setup() {
  wrapper = mount({ render: () => h(NConfigProvider, null, { default: () => h(RuntimeLicenseStatus) }) }, {
    global: { components: { NTag } },
  })
  return wrapper
}
function status() {
  const configuration = {
    binding: { customerId: '42', installationId: '00000000-0000-4000-8000-000000000001' },
    loadedAt: '2026-10-08T00:00:00Z',
    trustedPublicKeys: 1,
    rejectedPublicKeys: 0,
    filesConfigurationValid: true,
  }
  const entry = {
    position: 1,
    state: 'valid',
    licenseId: '00000000-0000-4000-8000-000000000002',
    scope: { pluginIds: ['unit-plugin'], featureCodes: ['ee.unit.view'] },
    terms: { issuedAt: 0, notBefore: 0, validUntil: null, maintenanceUntil: 0 },
  }
  const report = { configuration, checkedAt: '2026-10-08T00:00:10Z', entries: [entry] }
  return { code: 200, data: { mode: 'license', edition: 'ee', report } }
}

describe('运行时授权真实组件与 Pinia 组合', () => {
  it('无权限不查询，社区模式不展示文件或写操作', async () => {
    user.isAdmin = false
    setup()
    await flushPromises()
    expect(wrapper.text()).toContain('只有平台超级管理员')
    expect(api.getRuntimeLicenseStatus).not.toHaveBeenCalled()
    wrapper.unmount()
    user.isAdmin = true
    const data = { mode: 'community', edition: 'community', report: null }
    api.getRuntimeLicenseStatus.mockResolvedValue({ code: 200, data })
    setup()
    await flushPromises()
    expect(wrapper.text()).toContain('社区默认')
    expect(wrapper.text()).toContain('当前实例未使用文件许可证验证组件')
    expect(wrapper.find('input[type=file]').exists()).toBe(false)
    expect(wrapper.findAll('button').map(button => button.text())).toEqual(['刷新状态'])
  })
  it('管理员缺少查询权限不发请求，撤销后同步清空并拒绝旧响应回填', async () => {
    user.permissions = []
    setup()
    await flushPromises()
    expect(api.getRuntimeLicenseStatus).not.toHaveBeenCalled()
    user.permissions = ['system:plugin:list']
    api.getRuntimeLicenseStatus.mockResolvedValue(status())
    const store = useRuntimeLicenseStore()
    await store.load()
    expect(store.data.report.entries).toHaveLength(1)
    let resolve
    api.getRuntimeLicenseStatus.mockReturnValue(new Promise(done => resolve = done))
    const pending = store.load()
    user.permissions = []
    user.permissions = ['system:plugin:list']
    resolve(status())
    await pending
    expect(store.data).toBeNull()
  })
})

describe('运行时授权诊断展示与生命周期', () => {
  it('字典中文状态、绑定、期限与真实表格列渲染，功能范围点击可看完整值', async () => {
    api.getRuntimeLicenseStatus.mockResolvedValue(status())
    setup()
    await flushPromises()
    expect(wrapper.text()).toContain('许可证验证')
    expect(wrapper.text()).toContain('客户账号 ID')
    expect(wrapper.text()).toContain('有效')
    expect(wrapper.text()).toContain('永久使用')
    expect(wrapper.text()).toContain('unit-plugin')
    expect(wrapper.text()).not.toContain('T00:00')
    const button = wrapper.findAll('button').find(item => item.text().includes('1 项功能'))
    await button.trigger('click')
    await nextTick()
    expect(document.body.textContent).toContain('ee.unit.view')
  })
  it('同账号 Token ABA 切换立即清空，组件退出后请求不回填', async () => {
    api.getRuntimeLicenseStatus.mockResolvedValue(status())
    setup()
    await flushPromises()
    const store = useRuntimeLicenseStore()
    let resolve
    api.getRuntimeLicenseStatus.mockReturnValue(new Promise(done => resolve = done))
    const request = store.load()
    auth.accessToken = 'unit-session-two'
    auth.accessToken = 'unit-session-one'
    resolve(status())
    await request
    expect(store.data).toBeNull()
    expect(wrapper.text()).not.toContain('unit-plugin')
    api.getRuntimeLicenseStatus.mockReturnValue(new Promise(done => resolve = done))
    const closing = store.load()
    wrapper.unmount()
    wrapper = null
    resolve(status())
    await closing
    expect(store.data).toBeNull()
  })
  it('未知文本仅转义展示，无效绑定有固定说明和空表态', async () => {
    const result = status()
    result.data.report.configuration.binding = null
    result.data.report.configuration.filesConfigurationValid = false
    result.data.report.entries = []
    api.getRuntimeLicenseStatus.mockResolvedValue(result)
    setup()
    await flushPromises()
    expect(wrapper.text()).toContain('客户与项目绑定配置无效')
    expect(wrapper.text()).toContain('当前启动快照没有已加载的授权文件')
    useRuntimeLicenseStore().error = '<img src=x onerror=alert(1)>'
    await nextTick()
    expect(wrapper.find('img').exists()).toBe(false)
    expect(wrapper.text()).toContain('<img src=x onerror=alert(1)>')
  })
})
