import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import PageWidgetRenderer from '../PageWidgetRenderer.vue'

vi.mock('@/utils', () => ({ request: vi.fn() }))

// eslint-disable-next-line import/first
import { request } from '@/utils'

// eslint-disable-next-line no-template-curly-in-string -- paramsText 是运行时占位符语法，不是模板字面量
function mountWidget(dataContext, paramsText = '{"deptId": "${deptId}"}') {
  return mount(PageWidgetRenderer, {
    props: {
      componentKey: 'stat',
      propsData: {
        dataBinding: {
          enabled: true,
          sourceType: 'remote',
          api: 'get@/api/employee/list',
          paramsText,
          dataPath: 'data',
        },
      },
      dataContext,
    },
    global: {
      config: {
        warnHandler: () => {},
      },
    },
  })
}

async function flushAsync() {
  await new Promise(resolve => setTimeout(resolve, 0))
}

describe('page-widget-renderer 任意组件间级联联动', () => {
  it('引用字段的值变化触发重新请求，未引用字段变化不触发', async () => {
    request.mockReset()
    request.mockResolvedValue({ data: { records: [] } })

    const wrapper = mountWidget({ deptId: 'D1', keyword: 'a' })
    await vi.waitFor(() => expect(request).toHaveBeenCalledTimes(1))
    // 参数里引用了表单字段 deptId，其当前值应注入请求参数
    expect(request.mock.calls[0][0].params).toEqual({ deptId: 'D1' })

    // 未被引用的字段变化：不重新请求（精准联动，避免过度触发）
    await wrapper.setProps({ dataContext: { deptId: 'D1', keyword: 'b' } })
    await flushAsync()
    expect(request).toHaveBeenCalledTimes(1)

    // 被引用字段变化：自动重新请求并注入新值
    await wrapper.setProps({ dataContext: { deptId: 'D2', keyword: 'b' } })
    await vi.waitFor(() => expect(request).toHaveBeenCalledTimes(2))
    expect(request.mock.calls[1][0].params).toEqual({ deptId: 'D2' })

    wrapper.unmount()
  })

  it('未引用任何字段的参数不随上下文变化重新请求', async () => {
    request.mockReset()
    request.mockResolvedValue({ data: { records: [] } })

    // 参数为固定值、未引用任何表单字段：上下文变化不应触发重新请求
    const wrapper = mountWidget({ deptId: 'D1' }, '{"type": "user"}')
    await vi.waitFor(() => expect(request).toHaveBeenCalledTimes(1))
    expect(request.mock.calls[0][0].params).toEqual({ type: 'user' })

    await wrapper.setProps({ dataContext: { deptId: 'D9' } })
    await flushAsync()
    expect(request).toHaveBeenCalledTimes(1)

    wrapper.unmount()
  })
})
