import { flushPromises, mount } from '@vue/test-utils'
import { NButton, NDescriptions, NSkeleton } from 'naive-ui'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import HelloPlugin from '../../index.vue'

const request = vi.hoisted(() => ({ get: vi.fn() }))
vi.mock('@/utils/request', () => ({ request }))

describe('社区示例插件页面', () => {
  let wrapper
  const response = { code: 200, data: { id: 'hello', version: '1.0.0', coreVersion: '1.2.0' } }

  beforeEach(() => {
    request.get.mockReset()
    request.get.mockResolvedValue(response)
  })

  afterEach(() => wrapper?.unmount())

  it('通过宿主请求工具获取并显示实际响应', async () => {
    wrapper = mount(HelloPlugin)
    await flushPromises()
    expect(request.get).toHaveBeenCalledTimes(1)
    expect(request.get).toHaveBeenCalledWith('/plugin/hello/info', { needTip: false })
    expect(wrapper.getComponent(NDescriptions).text()).toContain('hello')
    expect(wrapper.text()).toContain('1.0.0')
    expect(wrapper.text()).toContain('1.2.0')
  })

  it('等待响应时展示骨架屏，重复刷新不产生并发请求', async () => {
    let resolve
    request.get.mockReturnValue(new Promise(done => resolve = done))
    wrapper = mount(HelloPlugin)
    await wrapper.vm.$nextTick()
    expect(wrapper.findComponent(NSkeleton).exists()).toBe(true)
    expect(wrapper.getComponent(NButton).props('disabled')).toBe(true)
    await wrapper.vm.loadInfo()
    expect(request.get).toHaveBeenCalledTimes(1)
    resolve(response)
    await flushPromises()
    expect(wrapper.findComponent(NSkeleton).exists()).toBe(false)
  })

  it('鉴权失败不会伪造成功数据，可通过重试恢复', async () => {
    request.get.mockRejectedValueOnce({ code: 403, message: '没有访问权限' })
    wrapper = mount(HelloPlugin)
    await flushPromises()
    expect(wrapper.text()).toContain('没有访问权限')
    expect(wrapper.findComponent(NDescriptions).exists()).toBe(false)
    const retry = wrapper.findAllComponents(NButton).find(button => button.text() === '重试')
    await retry.trigger('click')
    await flushPromises()
    expect(request.get).toHaveBeenCalledTimes(2)
    expect(wrapper.getComponent(NDescriptions).text()).toContain('hello')
  })

  it('刷新失败清空旧结果并展示失败提示', async () => {
    wrapper = mount(HelloPlugin)
    await flushPromises()
    request.get.mockRejectedValueOnce(new Error('网络异常'))
    await wrapper.getComponent(NButton).trigger('click')
    await flushPromises()
    expect(wrapper.findComponent(NDescriptions).exists()).toBe(false)
    expect(wrapper.text()).toContain('网络异常')
  })

  it.each([
    { code: 200, message: '操作成功', data: null },
    { code: 200, data: { id: 'hello', version: '1.0.0' } },
    { code: 200, data: { id: 'hello', version: 1, coreVersion: '1.2.0' } },
  ])('拒绝不完整/错误类型响应，不把操作成功作为错误原因', async (value) => {
    request.get.mockResolvedValueOnce(value)
    wrapper = mount(HelloPlugin)
    await flushPromises()
    expect(wrapper.text()).toContain('插件信息响应不完整')
    expect(wrapper.findComponent(NDescriptions).exists()).toBe(false)
    expect(wrapper.text()).not.toContain('操作成功')
  })

  it('业务失败沿用接口原因', async () => {
    request.get.mockResolvedValueOnce({ code: 500, message: '插件不可用' })
    wrapper = mount(HelloPlugin)
    await flushPromises()
    expect(wrapper.text()).toContain('插件不可用')
    expect(wrapper.findComponent(NDescriptions).exists()).toBe(false)
  })
})
