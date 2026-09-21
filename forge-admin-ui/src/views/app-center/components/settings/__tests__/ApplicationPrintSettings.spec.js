import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ApplicationPrintSettings from '../ApplicationPrintSettings.vue'

const api = vi.hoisted(() => ({
  businessApplicationWorkspaceByCode: vi.fn(),
}))

vi.mock('@/api/business-application', () => api)

const stubs = {
  ApplicationPrintPanel: {
    props: ['application', 'applicationObjects'],
    template: '<div data-testid="print-panel">{{ application.applicationCode }}:{{ applicationObjects.length }}</div>',
  },
  NSpin: { props: ['show'], template: '<div><slot /></div>' },
  NResult: { props: ['description'], template: '<div data-testid="load-error">{{ description }}<slot name="footer" /></div>' },
  NButton: { template: '<button><slot /></button>' },
  NEmpty: { props: ['description'], template: '<div>{{ description }}</div>' },
}

describe('applicationPrintSettings', () => {
  beforeEach(() => {
    api.businessApplicationWorkspaceByCode.mockReset()
  })

  it('loads the current workspace and delegates to the shared print panel', async () => {
    api.businessApplicationWorkspaceByCode.mockResolvedValue({
      data: {
        application: { id: '1', applicationCode: 'cgou_app_1ko3psh' },
        objects: [{ objectCode: 'purchase_order' }],
      },
    })
    const wrapper = mount(ApplicationPrintSettings, {
      props: { application: { applicationCode: 'cgou_app_1ko3psh' } },
      global: { stubs },
    })
    await flushPromises()

    expect(api.businessApplicationWorkspaceByCode).toHaveBeenCalledWith('cgou_app_1ko3psh')
    expect(wrapper.get('[data-testid="print-panel"]').text()).toBe('cgou_app_1ko3psh:1')
  })

  it('ignores a stale workspace response after switching applications', async () => {
    let resolveFirst
    const firstRequest = new Promise((resolve) => {
      resolveFirst = resolve
    })
    api.businessApplicationWorkspaceByCode
      .mockReturnValueOnce(firstRequest)
      .mockResolvedValueOnce({
        data: {
          application: { id: '2', applicationCode: 'app_new' },
          objects: [],
        },
      })
    const wrapper = mount(ApplicationPrintSettings, {
      props: { application: { applicationCode: 'app_old' } },
      global: { stubs },
    })

    await wrapper.setProps({ application: { applicationCode: 'app_new' } })
    await flushPromises()
    resolveFirst({
      data: {
        application: { id: '1', applicationCode: 'app_old' },
        objects: [{ objectCode: 'stale' }],
      },
    })
    await flushPromises()

    expect(wrapper.get('[data-testid="print-panel"]').text()).toBe('app_new:0')
  })

  it('shows a retryable error when the workspace request fails', async () => {
    api.businessApplicationWorkspaceByCode.mockRejectedValue(new Error('服务不可用'))
    const wrapper = mount(ApplicationPrintSettings, {
      props: { application: { applicationCode: 'cgou_app_1ko3psh' } },
      global: { stubs },
    })
    await flushPromises()

    expect(wrapper.get('[data-testid="load-error"]').text()).toContain('服务不可用')
    expect(wrapper.text()).toContain('重新加载')
  })
})
