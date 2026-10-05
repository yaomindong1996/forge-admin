import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { reactive } from 'vue'
import { useAppStore } from '@/store/modules/app'
import CompactLayoutTools from '../CompactLayoutTools.vue'

const navigation = vi.hoisted(() => ({ route: null }))
vi.mock('vue-router', () => ({ useRoute: () => navigation.route }))
vi.mock('@/store', async () => ({ useAppStore: (await import('@/store/modules/app')).useAppStore }))
vi.mock('../TenantSwitcher.vue', () => ({ default: { template: '<button>当前租户</button>' } }))
vi.mock('../OrgSwitcher.vue', () => ({ default: { template: '<button>当前组织</button>' } }))
vi.mock('../UserAvatar.vue', () => ({ default: { template: '<button>个人资料</button>' } }))
vi.mock('../BeginnerGuide.vue', () => ({ default: { template: '<button>操作指引</button>' } }))

const popover = {
  props: ['show'],
  emits: ['update:show'],
  template: `<div>
    <div @click="$emit('update:show', !show)"><slot name="trigger" /></div>
    <div v-if="show"><slot /></div>
  </div>`,
}
const options = { global: { stubs: { NPopover: popover } } }

describe('无顶栏账户工具', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    navigation.route = reactive({ fullPath: '/' })
  })

  it('打开外观时关闭账户面板，通过根级 Store 保留外观抽屉', async () => {
    const wrapper = mount(CompactLayoutTools, options)
    try {
      await wrapper.find('button[aria-label="账户与工具"]').trigger('click')
      expect(wrapper.text()).toContain('当前租户')
      expect(wrapper.text()).toContain('当前组织')
      const appearance = wrapper.findAll('button').find(button => button.text() === '布局与外观')
      await appearance.trigger('click')
      expect(useAppStore().appearanceOpen).toBe(true)
      expect(wrapper.find('button[aria-label="账户与工具"]').attributes('aria-expanded')).toBe('false')
    }
    finally { wrapper.unmount() }
  })

  it('主题操作调用共用状态，导航后面板收起', async () => {
    const wrapper = mount(CompactLayoutTools, options)
    try {
      const store = useAppStore()
      store.isDark = false
      await wrapper.find('button[aria-label="账户与工具"]').trigger('click')
      await wrapper.find('button[aria-label="切换深色模式"]').trigger('click')
      expect(store.isDark).toBe(true)
      expect(wrapper.text()).toContain('切换浅色模式')
      navigation.route.fullPath = '/profile'
      await flushPromises()
      expect(wrapper.find('.compact-account-tools').exists()).toBe(false)
    }
    finally { wrapper.unmount() }
  })
})
