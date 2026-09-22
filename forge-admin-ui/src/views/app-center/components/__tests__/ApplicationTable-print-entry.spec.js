import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
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

describe('applicationTable print entry', () => {
  it('does not put print templates on the application card', () => {
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
        stubs: {
          DictTag: true,
          IconRenderer: true,
          NDropdown: DropdownStub,
        },
      },
    })

    expect(wrapper.find('[data-action="print"]').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('打印模板')
  })
})
