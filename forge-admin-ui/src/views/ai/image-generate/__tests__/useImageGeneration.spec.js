import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, h, nextTick } from 'vue'
import { imageGenerate } from '@/api/ai/media'
import { modelListByProvider } from '@/api/ai/model-routing'
import { useAuthStore } from '@/store/modules/auth'
import { useUserStore } from '@/store/modules/user'
import { useImageGeneration } from '../useImageGeneration'

vi.mock('@/api/ai/media', () => ({ imageGenerate: vi.fn(), imageGenerateGetResult: vi.fn() }))
vi.mock('@/api/ai/model-routing', () => ({ modelListByProvider: vi.fn() }))
vi.mock('@/store/modules/auth', async () => {
  const { defineStore } = await import('pinia')
  return { useAuthStore: defineStore('test-auth', { state: () => ({ accessToken: undefined }) }) }
})

let pinia
let wrapper
let flow
let messages
function deferred() {
  let resolve
  const promise = new Promise((done) => {
    resolve = done
  })
  return { resolve, promise }
}
beforeEach(() => {
  vi.resetAllMocks()
  pinia = createPinia()
  setActivePinia(pinia)
  useAuthStore().accessToken = 'synthetic-session-only'
  useUserStore().userInfo = { id: '41', tenantId: '7', permissions: ['ai:image:generate'] }
  messages = { warning: vi.fn(), error: vi.fn() }
  modelListByProvider.mockResolvedValue({ code: 200, data: [
    { id: '9007199254740993', modelType: 'image_generation', modelName: '合成模型' },
    { id: '32', modelType: 'chat', modelName: '非图片模型' },
    { id: '33', modelType: 'image', modelName: '历史图片类型' },
  ] })
  wrapper = mount(defineComponent({ setup() {
    flow = useImageGeneration(messages)
    return () => h('div')
  } }), { global: { plugins: [pinia] } })
  flow.form.value = { prompt: ' 合成提示词 ', negativePrompt: '', size: '1024x1024', modelId: '9007199254740993' }
})
afterEach(() => {
  wrapper?.unmount()
  pinia._s.forEach(instance => instance.$dispose())
})

describe('图片显式提交', () => {
  it('选择启用图片模型且保留 Long ID，重复点击只能 POST 一次', async () => {
    const pending = deferred()
    imageGenerate.mockReturnValueOnce(pending.promise)
    const first = flow.generate()
    await flow.generate()
    expect(imageGenerate).toHaveBeenCalledTimes(1)
    expect(imageGenerate.mock.calls[0][0]).toMatchObject({ modelId: '9007199254740993', prompt: '合成提示词' })
    pending.resolve({ code: 200, data: '9007199254740995' })
    await first
    expect(flow.recordId.value).toBe('9007199254740995')
    expect(flow.modelOptions.value.map(model => model.value)).toEqual(['9007199254740993', '33'])
    expect(flow.canHistory.value).toBe(false)
  })

  it('接口不确认提交时停止 loading，不自动再次提交', async () => {
    imageGenerate.mockRejectedValue(new Error('synthetic failed submit'))
    await flow.generate()
    expect(flow.submitting.value).toBe(false)
    expect(flow.recordId.value).toBe('')
    expect(imageGenerate).toHaveBeenCalledTimes(1)
    expect(messages.error).toHaveBeenCalledWith(expect.stringContaining('不会自动重复提交'))
  })

  it('请求过程中换账号不显示旧任务或保留旧输入', async () => {
    const pending = deferred()
    imageGenerate.mockReturnValueOnce(pending.promise)
    const request = flow.generate()
    useUserStore().userInfo.id = '42'
    await nextTick()
    pending.resolve({ code: 200, data: '31' })
    await request
    expect(flow.recordId.value).toBe('')
    expect(flow.form.value.prompt).toBe('')
    expect(flow.submitting.value).toBe(false)
  })

  it('无权限或空提示词不调用生成接口', async () => {
    flow.form.value.prompt = '  '
    await flow.generate()
    expect(messages.warning).toHaveBeenCalledTimes(1)
    useUserStore().userInfo.permissions = []
    await flow.generate()
    expect(imageGenerate).not.toHaveBeenCalled()
  })
})
