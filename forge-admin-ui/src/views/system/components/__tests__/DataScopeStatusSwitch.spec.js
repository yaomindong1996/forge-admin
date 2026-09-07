import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import DataScopeStatusSwitch from '../DataScopeStatusSwitch.vue'

const post = vi.hoisted(() => vi.fn())
vi.mock('@/utils/request', () => ({ request: { post } }))

function mountSwitch(enabled = 1) {
  return mount(DataScopeStatusSwitch, {
    props: {
      row: { id: '9007199254741001', resourceName: '示例订单', enabled },
      options: [{ label: '启用', value: 1 }, { label: '禁用', value: 0 }],
    },
  })
}

beforeEach(() => {
  post.mockReset()
  window.$dialog = { warning: vi.fn() }
  window.$message = { success: vi.fn(), error: vi.fn() }
})

describe('data scope status switch', () => {
  it('explains the permission impact and does not save when cancelled', async () => {
    const wrapper = mountSwitch()
    await wrapper.get('[role="switch"]').trigger('click')
    const dialog = window.$dialog.warning.mock.calls[0][0]
    expect(dialog.content).toContain('可能看到更多数据')
    expect(wrapper.text()).toContain('已启用')
    expect(post).not.toHaveBeenCalled()
    expect(dialog.onNegativeClick()).toBeUndefined()
    await flushPromises()
    expect(wrapper.get('[role="switch"]').attributes('aria-disabled')).toBe('false')
    expect(wrapper.emitted('refresh')).toBeUndefined()
  })

  it.each([[1, 0, '已禁用'], [0, 1, '已启用']])('saves %i to %i only after confirmation', async (before, after, label) => {
    post.mockResolvedValue({ code: 200 })
    const wrapper = mountSwitch(before)
    await wrapper.get('[role="switch"]').trigger('click')
    await window.$dialog.warning.mock.calls[0][0].onPositiveClick()
    await flushPromises()
    expect(post).toHaveBeenCalledWith('/system/dataScopeConfig/status', {
      id: '9007199254741001',
      enabled: after,
      expectedEnabled: before,
    }, { needTip: false })
    expect(wrapper.text()).toContain(label)
    expect(wrapper.emitted('refresh')).toHaveLength(1)
    expect(wrapper.emitted('updated')).toHaveLength(1)
  })

  it('keeps the old status on failure and queries the authoritative state', async () => {
    post.mockRejectedValue(new Error('配置已保存，但数据权限刷新失败'))
    const wrapper = mountSwitch()
    await wrapper.get('[role="switch"]').trigger('click')
    await window.$dialog.warning.mock.calls[0][0].onPositiveClick()
    await flushPromises()
    expect(wrapper.text()).toContain('已启用')
    expect(window.$message.error).toHaveBeenCalledWith('配置已保存，但数据权限刷新失败')
    expect(window.$message.success).not.toHaveBeenCalled()
    expect(wrapper.emitted('refresh')).toHaveLength(1)
    await wrapper.setProps({ row: { id: '9007199254741001', enabled: 0 } })
    expect(wrapper.text()).toContain('已禁用')
  })

  it('blocks duplicate confirmation while the request is pending', async () => {
    let resolveRequest
    post.mockImplementation(() => new Promise(resolve => resolveRequest = resolve))
    const wrapper = mountSwitch()
    await wrapper.get('[role="switch"]').trigger('click')
    const dialog = window.$dialog.warning.mock.calls[0][0]
    const saving = dialog.onPositiveClick()
    await flushPromises()
    expect(wrapper.get('[role="switch"]').attributes('aria-disabled')).toBe('true')
    expect(await dialog.onPositiveClick()).toBe(false)
    expect(post).toHaveBeenCalledTimes(1)
    resolveRequest({ code: 200 })
    await saving
  })

  it('does not treat a rejected business response as a successful update', async () => {
    post.mockResolvedValue({ code: 409, message: '规则已被修改，请刷新列表' })
    const wrapper = mountSwitch()
    await wrapper.get('[role="switch"]').trigger('click')
    await window.$dialog.warning.mock.calls[0][0].onPositiveClick()
    expect(window.$message.success).not.toHaveBeenCalled()
    expect(window.$message.error).toHaveBeenCalledWith('规则已被修改，请刷新列表')
  })
})
