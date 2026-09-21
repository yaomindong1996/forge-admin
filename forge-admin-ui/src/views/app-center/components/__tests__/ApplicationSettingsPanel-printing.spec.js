import { flushPromises, shallowMount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import ApplicationSettingsPanel from '../ApplicationSettingsPanel.vue'

const api = vi.hoisted(() => ({
  businessApplicationDetailByCode: vi.fn(),
  checkBusinessApplicationSlugAvailable: vi.fn(),
  saveBusinessApplicationPortalConfig: vi.fn(),
  updateBusinessApplication: vi.fn(),
}))

vi.mock('@/api/business-application', () => api)
vi.mock('naive-ui', async (importOriginal) => {
  const original = await importOriginal()
  return { ...original, useMessage: () => ({ error: vi.fn(), success: vi.fn() }) }
})

function createTestRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/app-center/application/:applicationCode/runtime', component: { template: '<div />' } }],
  })
}

describe('applicationSettingsPanel printing section', () => {
  it('opens printing from the route query and hides the unrelated save action', async () => {
    api.businessApplicationDetailByCode.mockResolvedValue({
      data: {
        id: '1',
        applicationCode: 'cgou_app_1ko3psh',
        applicationName: '采购应用',
        options: '{}',
        portalConfig: '{}',
      },
    })
    const router = createTestRouter()
    await router.push('/app-center/application/cgou_app_1ko3psh/runtime?view=settings&settingsSection=printing')
    await router.isReady()
    const wrapper = shallowMount(ApplicationSettingsPanel, {
      props: { application: { id: '1', applicationCode: 'cgou_app_1ko3psh' } },
      global: {
        plugins: [router],
        stubs: {
          NSpin: { template: '<div><slot /></div>' },
          NIcon: { template: '<span><slot /></span>' },
          NButton: { template: '<button><slot /></button>' },
          NResult: { template: '<div><slot name="footer" /></div>' },
          ApplicationPrintSettings: { template: '<div data-testid="print-settings" />' },
        },
      },
    })
    await flushPromises()

    expect(wrapper.get('[data-testid="print-settings"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('打印模板')
    expect(wrapper.text()).not.toContain('保存设置')
  })
})
