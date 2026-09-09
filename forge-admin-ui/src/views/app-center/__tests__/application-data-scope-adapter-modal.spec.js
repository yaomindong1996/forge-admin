import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import ApplicationDataScopeAdapterModal from '../application-workspace/ApplicationDataScopeAdapterModal.vue'

const PassThroughStub = { template: '<div><slot /></div>' }

function mountModal() {
  return mount(ApplicationDataScopeAdapterModal, {
    props: {
      show: true,
      object: {
        objectId: '91',
        objectCode: 'ORDER',
        objectName: '订单',
        sharedApplicationCount: 2,
        dataScopeAdapter: {
          dataScope: 'FOLLOW_SYSTEM',
          userField: null,
          orgField: null,
          regionField: null,
          fields: [
            { field: 'createBy', columnName: 'create_by', label: '创建人' },
            { field: 'createDept', columnName: 'create_dept', label: '创建部门' },
          ],
        },
      },
    },
    global: {
      stubs: {
        NAlert: PassThroughStub,
        NCard: { template: '<section><slot /><slot name="footer" /></section>' },
        NForm: PassThroughStub,
        NFormItemGi: PassThroughStub,
        NGrid: PassThroughStub,
        NModal: {
          props: { show: Boolean },
          template: '<div v-if="show"><slot /></div>',
        },
        NRadioButton: PassThroughStub,
        NRadioGroup: PassThroughStub,
        NSelect: {
          props: ['value', 'options'],
          emits: ['update:value'],
          template: `
            <select :value="value" @change="$emit('update:value', $event.target.value)">
              <option value="" />
              <option v-for="option in options" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
          `,
        },
        NButton: {
          emits: ['click'],
          template: '<button type="button" @click="$emit(\'click\')"><slot /></button>',
        },
        NSwitch: {
          props: ['value'],
          emits: ['update:value'],
          template: '<button type="button" class="n-switch" @click="$emit(\'update:value\', !value)">{{ value }}</button>',
        },
      },
    },
  })
}

describe('application data scope adapter modal', () => {
  it('validates required mappings and emits only the adapter DTO', async () => {
    const wrapper = mountModal()
    const saveButton = wrapper.findAll('button').at(-1)

    await saveButton.trigger('click')
    expect(wrapper.emitted('save')).toBeUndefined()
    expect(wrapper.text()).toContain('必须选择本人字段')

    const selects = wrapper.findAll('select')
    await selects[0].setValue('createBy')
    await saveButton.trigger('click')
    expect(wrapper.text()).toContain('必须选择组织字段')

    await selects[1].setValue('createDept')
    await saveButton.trigger('click')

    expect(wrapper.emitted('save')?.at(-1)?.[0]).toEqual({
      dataScope: 'FOLLOW_SYSTEM',
      userField: 'createBy',
      orgField: 'createDept',
      regionField: null,
      flowRelatedVisible: true,
    })
    expect(wrapper.text()).toContain('2 个应用共用')
  })
})
