import { flushPromises, mount } from '@vue/test-utils'
import * as naive from 'naive-ui'
import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useMenuWorkspaceStore } from '@/stores/system/menuWorkspaceStore'
import { buildNavigationTree, getResourceTypeConfig } from '../menu-tree-presentation'
import MenuPage from '../menu.vue'

const mocks = vi.hoisted(() => ({ get: vi.fn(), post: vi.fn(), getMenu: vi.fn() }))
vi.mock('@/utils', () => ({ request: mocks }))
vi.mock('@/api', () => ({ default: { getMenu: mocks.getMenu } }))
vi.mock('@/store', () => ({
  useUserStore: () => ({ userInfo: { userClient: 'pc' } }),
  usePermissionStore: () => ({ setMenuData: vi.fn() }),
}))
vi.mock('@/utils/menu-route-options', () => ({ getMenuRouteOptions: () => [] }))
vi.mock('@/composables', async () => {
  const { ref } = await import('vue')
  return { useDict: () => ({ dict: ref({
    sys_resource_type: [{ value: '1', label: '目录' }, { value: '2', label: '菜单' }, { value: '3', label: '按钮' }],
    sys_show_hide: [{ value: '1', label: '显示' }, { value: '0', label: '隐藏' }],
  }) }) }
})
vi.mock('@/components/ai-form', () => ({ AiForm: { name: 'AiForm', template: '<div />' } }))
vi.mock('@/components/IconSelector.vue', () => ({ default: { template: '<div />' } }))
vi.mock('@/components/image-upload/index.vue', () => ({ default: { template: '<div />' } }))
vi.mock('@/components/IconRenderer.vue', () => ({ default: { props: ['icon'], template: '<i :class="icon" />' } }))

const resources = [{ id: 1, parentId: 0, resourceName: '系统管理', resourceType: 1, clientCode: 'pc', visible: 1, children: [{ id: 2, parentId: 1, resourceName: '用户管理', resourceType: 2, clientCode: 'pc', visible: 1, path: '/system/user', children: [{ id: 3, parentId: 2, resourceName: '查询用户', resourceType: 3, visible: 1 }] }, { id: 4, parentId: 1, resourceName: '空菜单', resourceType: 2, visible: 0, children: [] }] }]
let wrapper
beforeEach(() => {
  vi.clearAllMocks()
  setActivePinia(createPinia())
  mocks.get.mockImplementation(async url => ({ code: 200, data: url === '/system/client/list'
    ? [{ clientCode: 'pc', clientName: '管理端' }, { clientCode: 'h5', clientName: '移动端' }]
    : structuredClone(resources) }))
  mocks.post.mockResolvedValue({ code: 200, data: {} })
  window.$message = { success: vi.fn(), error: vi.fn() }
})
afterEach(async () => {
  wrapper?.unmount()
  await flushPromises()
  document.body.innerHTML = ''
})
async function mountPage() {
  wrapper = mount(MenuPage, { attachTo: document.body, global: {
    components: Object.fromEntries(Object.entries(naive).filter(([name]) => /^N[A-Z]/.test(name))),
  } })
  await flushPromises()
  return wrapper.vm
}

describe('菜单工作台真实组件编排', () => {
  it('默认直属列表，名称进入下级，面包屑可以返回且不请求写接口', async () => {
    await mountPage()
    expect(wrapper.findAll('.resource-list-row')).toHaveLength(1)
    await wrapper.find('.resource-name').trigger('click')
    expect(wrapper.findAll('.resource-list-row')).toHaveLength(2)
    expect(wrapper.find('.context-breadcrumbs').text()).toContain('系统管理')
    await wrapper.find('.context-breadcrumbs button').trigger('click')
    expect(wrapper.findAll('.resource-list-row')).toHaveLength(1)
    expect(mocks.post).not.toHaveBeenCalled()
  })
  it('选中叶菜单只展示空下级，不回退全量资源', async () => {
    const vm = await mountPage()
    vm.enterResource(resources[0].children[1])
    await flushPromises()
    expect(wrapper.findAll('.resource-list-row')).toHaveLength(0)
    expect(wrapper.find('.resource-list-empty').text()).toContain('当前层级暂无资源')
  })
  it('详情按需打开，关闭/客户端切换不遗留过期浮层', async () => {
    const vm = await mountPage()
    const workspace = useMenuWorkspaceStore()
    expect(workspace.detailVisible).toBe(false)
    await wrapper.find('.resource-list-row').trigger('click')
    expect(workspace.detailVisible).toBe(true)
    expect(document.body.textContent).toContain('编辑资源')
    vm.handleClientTabChange('h5')
    await flushPromises()
    expect(workspace.detailVisible).toBe(false)
  })
  it('筛选只在当前范围递归，重置恢复直属列表', async () => {
    const vm = await mountPage()
    vm.enterResource(resources[0])
    vm.resourceTypeFilter = 3
    await flushPromises()
    expect(vm.activeFilterCount).toBe(1)
    expect(wrapper.find('.resource-list-row').text()).toContain('查询用户')
    vm.resetResourceFilters()
    await flushPromises()
    expect(vm.activeFilterCount).toBe(0)
    expect(wrapper.findAll('.resource-list-row')).toHaveLength(2)
  })
  it('批量动作只在勾选后显示，取消选择不请求删除', async () => {
    const vm = await mountPage()
    expect(wrapper.find('.resource-selection-bar').text()).not.toContain('批量删除')
    vm.handleDisplayRowsCheckedChange(true)
    await flushPromises()
    expect(wrapper.find('.resource-selection-bar').text()).toContain('批量删除')
    vm.clearCheckedResources()
    await flushPromises()
    expect(wrapper.find('.resource-selection-bar').text()).not.toContain('批量删除')
    expect(mocks.post).not.toHaveBeenCalled()
  })
  it('新增顶级不继承选中上级，原新增子项仍继承当前节点', async () => {
    const vm = await mountPage()
    vm.enterResource(resources[0].children[0])
    vm.handleAddRoot()
    await flushPromises()
    expect(vm.formData.parentId).toBe(0)
    vm.handleAdd(vm.currentNode)
    await flushPromises()
    expect(vm.formData.parentId).toBe(2)
    expect(mocks.post).not.toHaveBeenCalled()
  })
})

describe('菜单树展示规则', () => {
  it('只显示目录/菜单，搜索保留命中祖先，不改变源数据', () => {
    const original = JSON.stringify(resources)
    const tree = buildNavigationTree(resources, '用户')
    expect(tree[0].children).toHaveLength(1)
    expect(tree[0].children[0].children).toEqual([])
    expect(JSON.stringify(resources)).toBe(original)
  })
  it('语义图标无随机彩色块，未知类型安全回退', () => {
    expect(getResourceTypeConfig(1)).toEqual({ icon: 'i-lucide:folder' })
    expect(getResourceTypeConfig(4)).toEqual({ icon: 'i-lucide:braces' })
    expect(getResourceTypeConfig(null)).toEqual({ icon: 'i-lucide:layers' })
  })
})
