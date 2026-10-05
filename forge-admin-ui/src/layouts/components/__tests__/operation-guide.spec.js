import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { reactive } from 'vue'
import { useAppStore } from '@/store/modules/app'
import BeginnerGuide from '../BeginnerGuide.vue'
import { createOperationGuide } from '../operation-guide'
import OperationGuideDialog from '../OperationGuideDialog.vue'

const navigation = vi.hoisted(() => ({ route: null }))
vi.mock('vue-router', () => ({ useRoute: () => navigation.route }))
vi.mock('@/store', async () => ({ useAppStore: (await import('@/store/modules/app')).useAppStore }))
const modal = {
  props: ['show'],
  emits: ['update:show'],
  template: '<div v-if="show"><slot /><slot name="footer" /></div>',
}
const button = { props: ['disabled'], template: '<button :disabled="disabled"><slot /></button>' }
const options = { global: { stubs: { NModal: modal, NButton: button } } }
const click = (wrapper, label) => wrapper.findAll('button').find(item => item.text() === label).trigger('click')

describe('全局操作指引', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    navigation.route = reactive({ fullPath: '/home' })
  })

  it('关闭启动浮层后仍由 Store 保持指引，移除入口也不影响状态', async () => {
    const trigger = mount(BeginnerGuide)
    await trigger.find('button').trigger('click')
    expect(trigger.emitted('start')).toHaveLength(1)
    trigger.unmount()
    expect(useAppStore().guideOpen).toBe(true)
  })

  it('下一步、上一步、步骤跳转及完成可用，重新打开从首步开始', async () => {
    const store = useAppStore()
    store.guideOpen = true
    const wrapper = mount(OperationGuideDialog, options)
    try {
      expect(wrapper.find('.guide-content h3').text()).toBe('找到业务入口')
      expect(wrapper.findAll('button').find(item => item.text() === '上一步').attributes('disabled')).toBeDefined()
      await click(wrapper, '下一步')
      expect(wrapper.find('.guide-content h3').text()).toBe('查看通知与待办')
      await click(wrapper, '上一步')
      expect(wrapper.find('.guide-counter').text()).toBe('1 / 4')
      await click(wrapper, '调整布局与配色')
      expect(wrapper.find('.guide-counter').text()).toBe('4 / 4')
      await click(wrapper, '完成')
      expect(store.guideOpen).toBe(false)
      store.guideOpen = true
      await flushPromises()
      expect(wrapper.find('.guide-counter').text()).toBe('1 / 4')
    }
    finally { wrapper.unmount() }
  })

  it('打开外观走根级状态，关闭指引而不是叠加两个弹层', async () => {
    const store = useAppStore()
    store.guideOpen = true
    const wrapper = mount(OperationGuideDialog, options)
    try {
      await click(wrapper, '调整布局与配色')
      await click(wrapper, '打开布局与外观')
      expect(store.guideOpen).toBe(false)
      expect(store.appearanceOpen).toBe(true)
    }
    finally { wrapper.unmount() }
  })

  it.each(['route', 'layout'])('%s 变化会关闭指引，不留遮罩', async (kind) => {
    const store = useAppStore()
    store.guideOpen = true
    const wrapper = mount(OperationGuideDialog, options)
    try {
      if (kind === 'route') {
        navigation.route.fullPath = '/profile'
      }
      else {
        store.layout = 'bento'
      }
      await flushPromises()
      expect(store.guideOpen).toBe(false)
    }
    finally { wrapper.unmount() }
  })

  it.each(['simple', 'bento', 'immersive', 'full'])('%s 说明与实际入口位置对应', (layout) => {
    const steps = createOperationGuide(layout)
    expect(steps).toHaveLength(4)
    expect(new Set(steps.map(item => item.key)).size).toBe(4)
    expect(steps[1].content).toContain(['simple', 'bento'].includes(layout) ? '侧栏底部' : '顶栏')
    if (['immersive', 'bento'].includes(layout)) {
      expect(steps[0].content).toContain('打开完整导航')
    }
  })

  it.each(['business-workbench', 'top-menu', 'top-side-menu'])('%s 不错误引导用户寻找桌面侧栏', (layout) => {
    expect(createOperationGuide(layout)[0].content).toContain('从顶部导航选择业务模块')
  })

  it('清理账号关闭指引，且指引状态不持久化', () => {
    const store = useAppStore()
    store.guideOpen = true
    store.resetAccountState()
    expect(store.guideOpen).toBe(false)
  })
})
