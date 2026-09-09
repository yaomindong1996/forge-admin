import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ExcelConfigStatusSwitch from '../ExcelConfigStatusSwitch.vue'

const put = vi.hoisted(() => vi.fn())
vi.mock('@/utils/request', () => ({ request: { put } }))

function mountSwitch(status = 1) {
  return mount(ExcelConfigStatusSwitch, {
    props: {
      row: { id: '101', exportName: '用户列表导出', configKey: 'sys_user_export', status },
      options: [{ label: '启用', value: 1 }, { label: '禁用', value: 0 }],
    },
  })
}

beforeEach(() => {
  put.mockReset()
  window.$dialog = { warning: vi.fn() }
  window.$message = { success: vi.fn(), error: vi.fn() }
})

describe('excel config status switch', () => {
  it('explains the business impact and sends no request when cancelled', async () => {
    const wrapper = mountSwitch()
    await wrapper.get('[role="switch"]').trigger('click')
    const dialog = window.$dialog.warning.mock.calls[0][0]
    expect(dialog.content).toContain('业务页面将暂时无法使用')
    expect(put).not.toHaveBeenCalled()
    expect(dialog.onNegativeClick()).toBeUndefined()
    await flushPromises()
    expect(wrapper.text()).toContain('已启用')
  })

  it.each([[1, 0, '停用'], [0, 1, '启用']])('updates %i to %i after confirmation', async (before, after, action) => {
    put.mockResolvedValue({ code: 200 })
    const wrapper = mountSwitch(before)
    await wrapper.get('[role="switch"]').trigger('click')
    await window.$dialog.warning.mock.calls[0][0].onPositiveClick()
    expect(put).toHaveBeenCalledWith('/system/excel/export-config/status', null, {
      params: { id: '101', status: after },
      needTip: false,
    })
    expect(window.$message.success).toHaveBeenCalledWith(`方案已${action}`)
    expect(wrapper.emitted('refresh')).toHaveLength(1)
  })

  it('keeps the old display and refreshes authoritative data on failure', async () => {
    put.mockRejectedValue(new Error('服务暂不可用'))
    const wrapper = mountSwitch()
    await wrapper.get('[role="switch"]').trigger('click')
    await window.$dialog.warning.mock.calls[0][0].onPositiveClick()
    expect(wrapper.text()).toContain('已启用')
    expect(window.$message.error).toHaveBeenCalledWith('服务暂不可用')
    expect(wrapper.emitted('refresh')).toHaveLength(1)
  })

  it('blocks duplicate confirmation while the request is pending', async () => {
    let resolveRequest
    put.mockImplementation(() => new Promise(resolve => resolveRequest = resolve))
    const wrapper = mountSwitch()
    await wrapper.get('[role="switch"]').trigger('click')
    const dialog = window.$dialog.warning.mock.calls[0][0]
    const saving = dialog.onPositiveClick()
    await flushPromises()
    expect(wrapper.get('[role="switch"]').attributes('aria-disabled')).toBe('true')
    expect(await dialog.onPositiveClick()).toBe(false)
    expect(put).toHaveBeenCalledTimes(1)
    resolveRequest({ code: 200 })
    await saving
  })
})
