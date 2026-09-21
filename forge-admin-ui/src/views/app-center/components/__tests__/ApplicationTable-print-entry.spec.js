import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import ApplicationTable from '../ApplicationTable.vue'

const DropdownStub = {
  props: ['options'],
  emits: ['select'],
  template: `
    <div>
      <button
        v-for="option in options.filter(item => item.type !== 'divider')"
        :key="option.key"
        :data-action="option.key"
        @click="$emit('select', option.key)"
      >
        {{ option.label }}
      </button>
      <slot />
    </div>
  `,
}

function createTestRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { name: 'AppCenter', path: '/app-center', component: { template: '<div />' } },
      {
        name: 'BusinessApplicationRuntime',
        path: '/app-center/application/:applicationCode/runtime',
        component: { template: '<div />' },
      },
    ],
  })
}

describe('applicationTable print entry', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('shows the print action in more and opens the current application settings section', async () => {
    const router = createTestRouter()
    await router.push('/app-center')
    await router.isReady()
    const open = vi.spyOn(window, 'open').mockImplementation(() => null)
    const wrapper = mount(ApplicationTable, {
      props: {
        applications: [{
          id: '1',
          applicationCode: 'cgou_app_1ko3psh',
          applicationName: '采购应用',
          designStatus: 'DRAFT',
        }],
      },
      global: {
        plugins: [router],
        stubs: {
          DictTag: true,
          IconRenderer: true,
          NDropdown: DropdownStub,
        },
      },
    })

    expect(wrapper.get('[data-action="print"]').text()).toBe('打印模板')
    await wrapper.get('[data-action="print"]').trigger('click')

    expect(open).toHaveBeenCalledWith(
      '/app-center/application/cgou_app_1ko3psh/runtime?view=settings&settingsSection=printing',
      '_blank',
      'noopener,noreferrer',
    )
  })
})
