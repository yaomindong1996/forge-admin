import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, h, KeepAlive, nextTick, ref } from 'vue'
import { imageGenerate, imageGenerateGetResult } from '@/api/ai/media'
import { useAuthStore } from '@/store/modules/auth'
import { useUserStore } from '@/store/modules/user'
import ToolCard from '../../agent-chat/ToolCard.vue'
import ImageJobResult from '../ImageJobResult.vue'

vi.mock('@/api/ai/media', () => ({ imageGenerate: vi.fn(), imageGenerateGetResult: vi.fn() }))
vi.mock('@/store/modules/auth', async () => {
  const { defineStore } = await import('pinia')
  return { useAuthStore: defineStore('test-auth', { state: () => ({ accessToken: undefined }) }) }
})
vi.mock('@/components/common/AuthImage.vue', () => ({ default: defineComponent({
  name: 'AuthImage',
  props: ['src'],
  setup: props => () => h('img', { 'data-file-id': props.src }),
}) }))
vi.mock('@/components/DictTag.vue', () => ({ default: defineComponent({
  name: 'DictTag',
  props: ['dictType', 'value'],
  setup: props => () => h('span', props.value),
}) }))

let pinia
let wrapper
const success = { code: 200, data: { id: '31', status: 'success', resultFileId: '9007199254740993' } }

beforeEach(() => {
  vi.useFakeTimers()
  vi.resetAllMocks()
  pinia = createPinia()
  setActivePinia(pinia)
  useAuthStore().accessToken = 'synthetic-session-only'
  useUserStore().userInfo = { id: '41', tenantId: '7', permissions: ['ai:image:generate'] }
})
afterEach(() => {
  wrapper?.unmount()
  pinia._s.forEach(instance => instance.$dispose())
  vi.clearAllTimers()
  vi.useRealTimers()
})

function render(component = ImageJobResult, props = { recordId: '31' }) {
  wrapper = mount(component, { props, global: { plugins: [pinia] } })
  return wrapper
}

describe('图片结果真实组件接线', () => {
  it('用 AuthImage.src 而不是无效 file-id，终态只通知一次', async () => {
    imageGenerateGetResult.mockResolvedValue(success)
    render()
    await vi.advanceTimersByTimeAsync(0)
    await nextTick()
    expect(wrapper.find('img').attributes('data-file-id')).toBe('9007199254740993')
    expect(wrapper.findComponent({ name: 'DictTag' }).props('dictType')).toBe('ai_image_generate_status')
    expect(wrapper.emitted('settled')).toHaveLength(1)
    await vi.advanceTimersByTimeAsync(5000)
    expect(imageGenerateGetResult).toHaveBeenCalledTimes(1)
  })

  it('查询失败提供重新查询按钮，点击不生成新图片', async () => {
    imageGenerateGetResult.mockRejectedValueOnce(new Error('synthetic'))
      .mockResolvedValueOnce(success)
    render()
    await vi.advanceTimersByTimeAsync(0)
    await nextTick()
    expect(wrapper.text()).toContain('任务可能仍在执行')
    await wrapper.get('button').trigger('click')
    await vi.advanceTimersByTimeAsync(0)
    await nextTick()
    expect(wrapper.find('img').exists()).toBe(true)
    expect(imageGenerate).not.toHaveBeenCalled()
  })

  it('工具卡历史成功状态能显示任务结果；无查询权限只提示', async () => {
    imageGenerateGetResult.mockResolvedValue(success)
    useUserStore().userInfo.permissions = []
    render(ToolCard, { toolCall: { name: 'image_generate', status: 'success', result: JSON.stringify({
      recordId: '31',
      status: 'pending',
      resultEndpoint: '/ai/image-generate/31',
    }) } })
    await vi.advanceTimersByTimeAsync(0)
    expect(wrapper.text()).toContain('没有图片结果查询权限')
    expect(imageGenerateGetResult).not.toHaveBeenCalled()
    useUserStore().userInfo.permissions = ['ai:image:generate']
    await nextTick()
    await vi.advanceTimersByTimeAsync(0)
    expect(wrapper.find('img').exists()).toBe(true)
  })

  it('keepAlive 停用停止查询，再进入恢复；卸载后不再调用', async () => {
    imageGenerateGetResult.mockResolvedValue({ code: 200, data: { id: '31', status: 'generating' } })
    const visible = ref(true)
    const harness = defineComponent({ setup: () => () => h(KeepAlive, null, {
      default: () => visible.value ? h(ImageJobResult, { recordId: '31' }) : h('div', '其它页面'),
    }) })
    render(harness, {})
    await vi.advanceTimersByTimeAsync(0)
    expect(imageGenerateGetResult).toHaveBeenCalledTimes(1)
    visible.value = false
    await nextTick()
    await vi.advanceTimersByTimeAsync(6000)
    expect(imageGenerateGetResult).toHaveBeenCalledTimes(1)
    visible.value = true
    await nextTick()
    await vi.advanceTimersByTimeAsync(0)
    expect(imageGenerateGetResult).toHaveBeenCalledTimes(2)
    wrapper.unmount()
    wrapper = null
    await vi.advanceTimersByTimeAsync(6000)
    expect(imageGenerateGetResult).toHaveBeenCalledTimes(2)
  })

  it('账号切换清除已经显示的图片，不能继续显示上一账号缓存', async () => {
    imageGenerateGetResult.mockResolvedValueOnce(success)
      .mockResolvedValueOnce({ code: 200, data: { id: '31', status: 'generating' } })
    render()
    await vi.advanceTimersByTimeAsync(0)
    expect(wrapper.find('img').exists()).toBe(true)
    useUserStore().userInfo.id = '42'
    await nextTick()
    expect(wrapper.find('img').exists()).toBe(false)
    await vi.advanceTimersByTimeAsync(0)
    expect(imageGenerateGetResult).toHaveBeenCalledTimes(2)
    expect(wrapper.find('img').exists()).toBe(false)
  })
})
