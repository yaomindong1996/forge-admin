import { readdirSync, readFileSync } from 'node:fs'
import { flushPromises, mount } from '@vue/test-utils'
import * as naive from 'naive-ui'
import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, h, nextTick, reactive } from 'vue'
import { useUserManagementStore } from '@/stores/system/userManagementStore'
import SystemUser from '../user.vue'

const mocks = vi.hoisted(() => ({ get: vi.fn(), post: vi.fn(), login: null, refresh: vi.fn() }))
vi.mock('@/utils', () => ({ request: { get: mocks.get, post: mocks.post } }))
vi.mock('@/store', () => ({ useUserStore: () => mocks.login }))
vi.mock('@/composables/useDict', async () => {
  const { ref } = await import('vue')
  return { useDict: () => ({ dict: ref({ sys_user_type: [{ value: '2', label: '普通用户' }] }) }) }
})
vi.mock('@/components/ai-form', async () => {
  const { defineComponent, h } = await import('vue')
  return {
    AiCrudPage: defineComponent({
      name: 'AiCrudPage',
      inheritAttrs: false,
      props: ['apiConfig', 'searchSchema', 'columns', 'editSchema', 'beforeSubmit', 'beforeLoadList'],
      emits: ['selection-change'],
      setup(_props, { slots, expose }) {
        expose({ refresh: mocks.refresh, clearSelection: vi.fn(), getSearchParams: () => ({}) })
        return () => h('div', { class: 'ai-crud-page' }, [slots['toolbar-start']?.(), slots['toolbar-end']?.()])
      },
    }),
  }
})
vi.mock('../components/RolePermissionModal.vue', () => ({ default: { name: 'RolePermissionModal', template: '<div />' } }))

let wrapper
let pinia
beforeEach(() => {
  vi.clearAllMocks()
  pinia = createPinia()
  setActivePinia(pinia)
  mocks.login = reactive({ userId: 9, isAdmin: true, activeOrgId: 10, userInfo: { tenantId: 1 } })
  window.$message = { success: vi.fn(), warning: vi.fn(), error: vi.fn() }
  window.matchMedia = vi.fn().mockImplementation(query => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  }))
  mocks.get.mockImplementation(async (url) => {
    if (url === '/system/org/lazyTree') {
      return { code: 200, data: [{ id: 10, orgName: '测试组织', children: [] }] }
    }
    if (url === '/system/role/page') {
      return { code: 200, data: { list: [{ id: 3, roleName: '测试角色' }], total: 1 } }
    }
    if (url.endsWith('/org-bindings')) {
      return { code: 200, data: [{ orgId: 10, orgName: '测试组织', isMain: 1, roleNames: ['测试角色'] }] }
    }
    if (url.endsWith('/org-roles')) {
      return { code: 200, data: [3] }
    }
    return { code: 200, data: [] }
  })
  mocks.post.mockResolvedValue({ code: 200 })
})
afterEach(async () => {
  wrapper?.unmount()
  wrapper = null
  await flushPromises()
  document.body.innerHTML = ''
  vi.restoreAllMocks()
})
function mountPage() {
  const warnings = []
  const Host = defineComponent({
    setup: () => () => h(naive.NConfigProvider, null, { default: () => h(SystemUser) }),
  })
  wrapper = mount(Host, {
    attachTo: document.body,
    global: {
      plugins: [pinia],
      components: Object.fromEntries(Object.entries(naive).filter(([name]) => /^N[A-Z]/.test(name))),
      config: { warnHandler: message => warnings.push(message) },
    },
  })
  return warnings
}

describe('用户管理组件集成', () => {
  it('首次挂载保留 CRUD 协议，组织树和列表直接共享 Pinia 状态', async () => {
    const warnings = mountPage()
    await flushPromises()
    const store = useUserManagementStore()
    const list = wrapper.findComponent({ name: 'AiCrudPage' })
    expect(list.props('apiConfig')).toMatchObject({ list: 'get@/system/user/page', detail: 'post@/system/user/getById', add: 'post@/system/user/add', update: 'post@/system/user/edit', delete: 'post@/system/user/removeBatch' })
    store.handleOrgNodeSelect([10])
    await nextTick()
    expect(wrapper.find('.org-filter-tip').text()).toContain('测试组织')
    expect(mocks.refresh).toHaveBeenCalledOnce()
    await wrapper.find('.org-tree-all-node').trigger('click')
    expect(store.selectedOrgNode).toBeNull()
    expect(mocks.get.mock.calls.filter(([url]) => url === '/system/org/lazyTree')).toHaveLength(1)
    expect(warnings).toEqual([])
  })

  it('关系弹窗首次打开、页签切换与角色表复用不丢失事件', async () => {
    const warnings = mountPage()
    await flushPromises()
    const store = useUserManagementStore()
    await store.handleRelation({ id: 8, username: 'fixture-user' })
    await flushPromises()
    expect(document.querySelector('.user-relation-modal .n-tabs')).toBeTruthy()
    expect(document.querySelector('.relation-overview-copy').textContent).toContain('fixture-user')
    const roleTab = [...document.querySelectorAll('.user-relation-modal .n-tabs-tab')].find(node => node.textContent.includes('按组织授权'))
    roleTab.click()
    await flushPromises()
    expect(store.relationActiveTab).toBe('auth')
    expect(document.querySelector('.auth-modal-content .n-data-table').textContent).toContain('测试角色')
    expect(document.querySelector('.auth-modal-content .n-data-table').textContent).toContain('权限配置')
    store.relationModalVisible = false
    await nextTick()
    expect(store.relationModalVisible).toBe(false)
    expect(mocks.post).not.toHaveBeenCalled()
    expect(warnings).toEqual([])
  })

  it('密码弹窗校验引用保持局部，取消和卸载清理草稿', async () => {
    const warnings = mountPage()
    await flushPromises()
    const store = useUserManagementStore()
    store.resetPwdForm = { id: 8, password: 'test-only' }
    store.resetPwdModalVisible = true
    await flushPromises()
    const passwordModal = wrapper.findComponent({ name: 'UserPasswordModal' })
    expect(passwordModal.exists()).toBe(true)
    store.resetPwdModalVisible = false
    await nextTick()
    expect(store.resetPwdForm.password).toBe('')
    wrapper.unmount()
    wrapper = null
    expect(pinia.state.value).not.toHaveProperty(store.$id)
    expect(warnings).toEqual([])
  })
})

function sourceUrl(relativePath) {
  return new URL(relativePath, import.meta.url)
}

describe('用户管理结构回归约束', () => {
  it('页面和新拆组件不超过 800 行，组件目录被路由扫描排除', () => {
    const directory = sourceUrl('../user/components/')
    const files = readdirSync(directory).filter(name => name.endsWith('.vue'))
    expect(files).toHaveLength(13)
    for (const file of [sourceUrl('../user.vue'), ...files.map(name => new URL(name, directory))]) {
      expect(readFileSync(file, 'utf8').trimEnd().split('\n').length).toBeLessThanOrEqual(800)
    }
    const config = readFileSync(sourceUrl('../../../../vite.config.js'), 'utf8')
    expect(config).toContain('**/components/**')
  })

  it('领域样式限定用户页和 Teleport 命名空间', () => {
    const directory = sourceUrl('../user/styles/')
    for (const file of readdirSync(directory).filter(name => name.endsWith('.css'))) {
      const source = readFileSync(new URL(file, directory), 'utf8')
      expect(source).not.toContain(':deep(')
      const selectors = source
        .replace(/\/\*[\s\S]*?\*\//g, ' ')
        .replace(/\s+/g, ' ')
        .split('{')
        .map(chunk => chunk.split('}').pop().trim())
        .filter(selector => selector && !selector.startsWith('@'))
      expect(selectors.length).toBeGreaterThan(0)
      for (const selector of selectors) {
        expect(selector).toMatch(/^(\.system-user-page\b|\.user-relation-modal\b|\.ai-crud-edit-form\.user-edit-form\b|:where\(\.system-user-page|\.dark :where\(\.system-user-page)/)
      }
    }
  })
})
