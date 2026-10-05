import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'
import { useAppStore } from '@/store/modules/app'
import ResponsiveMenuToggle from '../ResponsiveMenuToggle.vue'

const media = vi.hoisted(() => ({ narrow: null }))
vi.mock('@vueuse/core', async importOriginal => ({
  ...await importOriginal(),
  useMediaQuery: () => media.narrow,
}))
vi.mock('@/store', async () => ({ useAppStore: (await import('@/store/modules/app')).useAppStore }))
vi.mock('@/layouts/immersive/components/DrawerMenu.vue', () => ({ default: {
  name: 'DrawerMenu',
  props: ['show'],
  emits: ['update:show'],
  template: '<button v-if="show" class="select-leaf" @click="$emit(\'update:show\', false)">选择菜单</button>',
} }))

describe('响应式菜单可达性', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    media.narrow = ref(false)
  })

  it('桌面收起后入口仍存在，点击可以展开', async () => {
    const wrapper = mount(ResponsiveMenuToggle)
    try {
      const store = useAppStore()
      await wrapper.find('button[aria-label="收起菜单"]').trigger('click')
      expect(store.collapsed).toBe(true)
      await wrapper.find('button[aria-label="展开菜单"]').trigger('click')
      expect(store.collapsed).toBe(false)
    }
    finally { wrapper.unmount() }
  })

  it('窄屏抽屉开关不改变桌面收起状态，选择后仍能重新打开', async () => {
    media.narrow.value = true
    const store = useAppStore()
    store.collapsed = true
    const wrapper = mount(ResponsiveMenuToggle)
    try {
      await wrapper.find('button[aria-label="打开菜单"]').trigger('click')
      expect(wrapper.find('button[aria-label="打开菜单"]').attributes('aria-expanded')).toBe('true')
      expect(store.collapsed).toBe(true)
      await wrapper.find('.select-leaf').trigger('click')
      expect(wrapper.find('.select-leaf').exists()).toBe(false)
      await wrapper.find('button[aria-label="打开菜单"]').trigger('click')
      expect(wrapper.find('.select-leaf').exists()).toBe(true)
    }
    finally { wrapper.unmount() }
  })

  it('放大窗口后关闭窄屏遮罩，桌面偏好保持不变', async () => {
    media.narrow.value = true
    const wrapper = mount(ResponsiveMenuToggle)
    try {
      await wrapper.find('button[aria-label="打开菜单"]').trigger('click')
      media.narrow.value = false
      await flushPromises()
      expect(wrapper.find('.select-leaf').exists()).toBe(false)
      expect(wrapper.find('button[aria-label="收起菜单"]').exists()).toBe(true)
      expect(useAppStore().collapsed).toBe(false)
    }
    finally { wrapper.unmount() }
  })
})
