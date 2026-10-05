import { existsSync, readFileSync } from 'node:fs'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'
import { useAppStore } from '@/store/modules/app'
import HeaderTools from '../HeaderTools.vue'

const viewport = vi.hoisted(() => ({ compact: null }))
vi.mock('@vueuse/core', async load => ({
  ...await load(),
  useMediaQuery: () => viewport.compact,
}))
vi.mock('@/store', async () => ({ useAppStore: (await import('@/store/modules/app')).useAppStore }))
// 切断查询/路由副作用；本用例检查 HeaderTools 编排，实际账户与主题行为由原组件测试覆盖。
vi.mock('../MenuSearch.vue', () => ({ default: { template: '<button aria-label="搜索全部菜单" />' } }))
vi.mock('../MessageNotification.vue', () => ({ default: { template: '<button aria-label="通知中心" />' } }))
vi.mock('../TenantSwitcher.vue', () => ({ default: { template: '<span>当前租户</span>' } }))
vi.mock('../OrgSwitcher.vue', () => ({ default: { template: '<span>当前组织</span>' } }))
vi.mock('../UserAvatar.vue', () => ({ default: { template: '<button aria-label="个人中心" />' } }))
vi.mock('../CompactLayoutTools.vue', () => ({ default: { template: '<button aria-label="账户与工具" />' } }))
const read = file => readFileSync(new URL(file, import.meta.url), 'utf8')

// 用户明确移除指引后，契约转为“无残留且实际工具继续可用”，不跳过旧失败用例。
describe('移除操作指引后的工具回归', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    viewport.compact = ref(false)
  })

  it('组件、弹窗、说明模块已删除，没有公共导出或状态残留', () => {
    for (const file of ['../BeginnerGuide.vue', '../OperationGuideDialog.vue', '../operation-guide.js']) {
      expect(existsSync(new URL(file, import.meta.url))).toBe(false)
    }
    expect(read('../index.js')).not.toContain('BeginnerGuide')
    expect(read('../../../App.vue')).not.toContain('OperationGuideDialog')
    expect(useAppStore().$state).not.toHaveProperty('guideOpen')
  })

  it('废弃第三方库从依赖、锁文件和预构建配置移除', () => {
    for (const file of ['../../../../package.json', '../../../../pnpm-lock.yaml', '../../../../vite.config.js']) {
      expect(read(file)).not.toContain('vue3-intro-step')
    }
  })

  it('桌面全屏直接可达，布局与外观仍打开根级面板', async () => {
    const wrapper = mount(HeaderTools, { props: { showAppearance: true } })
    try {
      expect(wrapper.find('button[aria-label="进入全屏"]').exists()).toBe(true)
      expect(wrapper.find('button[aria-label="更多工具"]').exists()).toBe(false)
      expect(wrapper.text()).not.toContain('操作指引')
      await wrapper.find('button[aria-label="布局与外观"]').trigger('click')
      expect(useAppStore().appearanceOpen).toBe(true)
    }
    finally { wrapper.unmount() }
  })

  it('窄屏搜索、通知和账户入口各一个，不重复渲染桌面工具', () => {
    viewport.compact.value = true
    const wrapper = mount(HeaderTools)
    try {
      for (const label of ['搜索全部菜单', '通知中心', '账户与工具']) {
        expect(wrapper.findAll(`button[aria-label="${label}"]`)).toHaveLength(1)
      }
      expect(wrapper.find('button[aria-label="进入全屏"]').exists()).toBe(false)
      expect(wrapper.find('button[aria-label="个人中心"]').exists()).toBe(false)
      expect(wrapper.text()).not.toContain('操作指引')
    }
    finally { wrapper.unmount() }
  })
})
