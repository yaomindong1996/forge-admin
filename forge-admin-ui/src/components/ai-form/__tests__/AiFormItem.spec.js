import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import AiFormItem from '../AiFormItem.vue'

vi.mock('vue-router', () => ({
  useRoute: () => ({
    query: {},
    params: {},
    path: '/',
    fullPath: '/',
    name: 'ai-form-item-test',
  }),
}))

vi.mock('@/api/business-app', () => ({
  queryBusinessRecordSelector: vi.fn(),
}))

const NFormItemStub = {
  name: 'NFormItem',
  template: '<div><slot name="label" /><slot /></div>',
}

const NInputNumberStub = {
  name: 'NInputNumber',
  props: ['value', 'min', 'max', 'step'],
  template: '<div class="n-input-number-stub"><slot name="prefix" /><input /><slot name="suffix" /></div>',
}

const NInputStub = {
  name: 'NInput',
  props: ['value', 'type', 'clearable', 'showCount'],
  template: `<div class="n-input-stub" :data-type="type">
    <slot name="prefix" />
    <input />
    <slot name="suffix" />
    <slot name="clear-icon" />
    <slot name="password-visible-icon" />
    <slot name="password-invisible-icon" />
    <slot name="separator" />
  </div>`,
}

const NSelectStub = {
  name: 'NSelect',
  props: ['value', 'options'],
  template: `<div class="n-select-stub">
    <slot name="header" />
    <slot name="action" />
    <slot name="empty" />
    <slot name="arrow" />
  </div>`,
}

const IconRendererStub = {
  name: 'IconRenderer',
  props: ['icon', 'size'],
  template: '<i class="icon-renderer-stub" :data-icon="icon" />',
}

const AiFormDatePickerStub = {
  name: 'AiFormDatePicker',
  props: [
    'value',
    'formattedValue',
    'type',
    'valueFormat',
    'dateIcon',
    'dateFooter',
    'prevYearIcon',
    'prevMonthIcon',
    'nextMonthIcon',
    'nextYearIcon',
    'separator',
  ],
  emits: ['update:formattedValue'],
  template: `<div class="n-date-picker-stub" :data-type="type">
    <i v-if="dateIcon" class="icon-renderer-stub" :data-icon="dateIcon" />
    <span v-if="dateFooter">{{ dateFooter }}</span>
    <i v-if="prevMonthIcon" class="icon-renderer-stub" :data-icon="prevMonthIcon" />
    <i v-if="nextMonthIcon" class="icon-renderer-stub" :data-icon="nextMonthIcon" />
    <span v-if="separator">{{ separator }}</span>
  </div>`,
}

const naiveStubs = Object.fromEntries([
  'NInput',
  'NInputNumber',
  'NSelect',
  'NRadio',
  'NSpace',
  'NRadioGroup',
  'NRadioButton',
  'NCheckbox',
  'NCheckboxGroup',
  'NSwitch',
  'NDatePicker',
  'NTimePicker',
  'NButton',
  'NUpload',
  'NSlider',
  'NRate',
  'NColorPicker',
  'NCascader',
  'NTreeSelect',
  'NTransfer',
  'NInputGroup',
  'NIcon',
  'NTooltip',
].map(name => [name, true]))

describe('aiFormItem number field compatibility', () => {
  it('renders input-number as NInputNumber and forwards numeric constraints', () => {
    const wrapper = mount(AiFormItem, {
      props: {
        field: {
          field: 'sort',
          label: '排序',
          type: 'input-number',
          props: {
            min: 0,
            max: 100,
            step: 5,
          },
        },
        value: 10,
      },
      global: {
        stubs: {
          ...naiveStubs,
          NFormItem: NFormItemStub,
          NInputNumber: NInputNumberStub,
          AiRecordSelectorModal: true,
        },
      },
    })

    const numberInput = wrapper.findComponent(NInputNumberStub)
    expect(numberInput.exists()).toBe(true)
    expect(numberInput.props()).toMatchObject({
      value: 10,
      min: 0,
      max: 100,
      step: 5,
    })
    expect(wrapper.find('.n-input-number-stub').exists()).toBe(true)
  })

  it('renders money via componentKey and coerces string amount', () => {
    const wrapper = mount(AiFormItem, {
      props: {
        field: {
          field: 'amount',
          label: '金额',
          type: 'input',
          componentKey: 'money',
          props: { precision: 2 },
        },
        value: '128.50',
      },
      global: {
        stubs: {
          ...naiveStubs,
          NFormItem: NFormItemStub,
          NInputNumber: NInputNumberStub,
          AiRecordSelectorModal: true,
        },
      },
    })

    const numberInput = wrapper.findComponent(NInputNumberStub)
    expect(numberInput.exists()).toBe(true)
    expect(numberInput.props('value')).toBe(128.5)
  })

  it('renders prefix / suffix slots for input-number', () => {
    const wrapper = mount(AiFormItem, {
      props: {
        field: {
          field: 'rate',
          label: '费率',
          type: 'input-number',
          props: {
            prefix: '￥',
            suffix: '%',
            showButton: false,
          },
        },
        value: 12,
      },
      global: {
        stubs: {
          ...naiveStubs,
          NFormItem: NFormItemStub,
          NInputNumber: NInputNumberStub,
          AiRecordSelectorModal: true,
        },
      },
    })

    const html = wrapper.find('.n-input-number-stub').html()
    expect(html).toContain('￥')
    expect(html).toContain('%')
  })

  it('renders input slot icons and password type', () => {
    const wrapper = mount(AiFormItem, {
      props: {
        field: {
          field: 'pwd',
          label: '密码',
          type: 'input',
          props: {
            inputType: 'password',
            prefix: '+86',
            prefixIcon: 'call',
            clearIcon: 'close',
            passwordVisibleIcon: 'eye',
            passwordInvisibleIcon: 'eye-off',
            separator: '~',
            pair: true,
          },
        },
        value: '',
      },
      global: {
        stubs: {
          ...naiveStubs,
          NFormItem: NFormItemStub,
          NInput: NInputStub,
          IconRenderer: IconRendererStub,
          AiRecordSelectorModal: true,
        },
      },
    })

    const input = wrapper.findComponent(NInputStub)
    expect(input.props('type')).toBe('password')
    const html = wrapper.find('.n-input-stub').html()
    expect(html).toContain('+86')
    expect(html).toContain('data-icon="call"')
    expect(html).toContain('data-icon="close"')
    expect(html).toContain('data-icon="eye"')
    expect(html).toContain('~')
  })

  it('renders select menu slots', () => {
    const wrapper = mount(AiFormItem, {
      props: {
        field: {
          field: 'status',
          label: '状态',
          type: 'select',
          props: {
            dropdownHeader: '常用状态',
            dropdownAction: '管理字典',
            emptyText: '暂无选项',
            arrowIcon: 'chevron-down',
            options: [{ label: '启用', value: '1' }],
          },
        },
        value: null,
      },
      global: {
        stubs: {
          ...naiveStubs,
          NFormItem: NFormItemStub,
          NSelect: NSelectStub,
          IconRenderer: IconRendererStub,
          AiRecordSelectorModal: true,
        },
      },
    })

    const html = wrapper.find('.n-select-stub').html()
    expect(html).toContain('常用状态')
    expect(html).toContain('管理字典')
    expect(html).toContain('暂无选项')
    expect(html).toContain('data-icon="chevron-down"')
  })

  it('renders date-picker slots via AiFormDatePicker', () => {
    const wrapper = mount(AiFormItem, {
      props: {
        field: {
          field: 'range',
          label: '区间',
          type: 'daterange',
          props: {
            dateIcon: 'calendar',
            dateFooter: '按自然日',
            prevMonthIcon: 'chevron-back',
            nextMonthIcon: 'chevron-forward',
            separator: '至',
          },
        },
        value: null,
      },
      global: {
        stubs: {
          ...naiveStubs,
          NFormItem: NFormItemStub,
          AiFormDatePicker: AiFormDatePickerStub,
          AiRecordSelectorModal: true,
        },
      },
    })

    const picker = wrapper.findComponent(AiFormDatePickerStub)
    expect(picker.props()).toMatchObject({
      dateIcon: 'calendar',
      dateFooter: '按自然日',
      prevMonthIcon: 'chevron-back',
      nextMonthIcon: 'chevron-forward',
      separator: '至',
    })
    const html = wrapper.find('.n-date-picker-stub').html()
    expect(html).toContain('data-icon="calendar"')
    expect(html).toContain('按自然日')
    expect(html).toContain('至')
  })
})

describe('aiFormItem formatted date storage', () => {
  function mountDateField(field, value) {
    return mount(AiFormItem, {
      props: {
        field,
        value,
      },
      global: {
        stubs: {
          ...naiveStubs,
          NFormItem: NFormItemStub,
          AiFormDatePicker: AiFormDatePickerStub,
          AiRecordSelectorModal: true,
        },
      },
    })
  }

  it('binds and emits year values as formatted strings', async () => {
    const wrapper = mountDateField({ field: 'year', label: '年份', type: 'year' }, '2026')
    const picker = wrapper.findComponent(AiFormDatePickerStub)

    expect(picker.props()).toMatchObject({
      value: undefined,
      formattedValue: '2026',
      type: 'year',
      valueFormat: 'yyyy',
    })
    picker.vm.$emit('update:formattedValue', '2027')
    await wrapper.vm.$nextTick()

    expect(wrapper.emitted('update:value')).toEqual([['2027']])
  })

  it('keeps date ranges as formatted string arrays', async () => {
    const value = ['2026-08-01', '2026-08-31']
    const wrapper = mountDateField({ field: 'period', label: '日期范围', type: 'daterange' }, value)
    const picker = wrapper.findComponent(AiFormDatePickerStub)

    expect(picker.props('value')).toBeUndefined()
    expect(picker.props('formattedValue')).toEqual(value)
    const nextValue = ['2026-09-01', '2026-09-30']
    picker.vm.$emit('update:formattedValue', nextValue)
    await wrapper.vm.$nextTick()

    expect(wrapper.emitted('update:value')).toEqual([[nextValue]])
  })
})

describe('aiFormItem managed field events', () => {
  function mountEventField(context) {
    return mount(AiFormItem, {
      props: {
        field: { field: 'mobile', label: '手机号', type: 'input' },
        value: '13800000000',
        formData: { mobile: '13800000000' },
        context,
      },
      global: {
        stubs: {
          ...naiveStubs,
          NFormItem: NFormItemStub,
          NButton: {
            template: '<button><slot /></button>',
          },
          AiRecordSelectorModal: true,
        },
      },
    })
  }

  it('dispatches BLUR and SCAN_COMPLETE through the shared field event entry', async () => {
    const dispatchFieldEvent = vi.fn()
    const wrapper = mountEventField({
      hasFieldEvent: trigger => ['BLUR', 'SCAN_COMPLETE'].includes(trigger),
      dispatchFieldEvent,
      getFieldEventState: () => ({ status: 'idle', loading: false, message: '' }),
    })

    await wrapper.find('.ai-form-control').trigger('focusout')
    await wrapper.find('.ai-form-control').trigger('keyup', { key: 'Enter' })

    expect(dispatchFieldEvent).toHaveBeenNthCalledWith(1, 'BLUR', 'mobile')
    expect(dispatchFieldEvent).toHaveBeenNthCalledWith(2, 'SCAN_COMPLETE', 'mobile')
  })

  it('renders a compact MANUAL action and controlled field feedback', async () => {
    const dispatchFieldEvent = vi.fn()
    const wrapper = mountEventField({
      hasFieldEvent: trigger => trigger === 'MANUAL',
      dispatchFieldEvent,
      getFieldEventState: () => ({ status: 'not_found', loading: false, message: '未匹配到数据' }),
    })

    expect(wrapper.text()).toContain('查询')
    expect(wrapper.text()).toContain('未匹配到数据')
    await wrapper.find('.ai-form-field-event__action').trigger('click')
    expect(dispatchFieldEvent).toHaveBeenCalledWith('MANUAL', 'mobile')
  })

  it('writes an injected scan result and dispatches SCAN_COMPLETE context', async () => {
    const dispatchFieldEvent = vi.fn()
    const scanField = vi.fn(async () => ({ value: 'SKU-001', type: 'barCode', platform: 'H5' }))
    const wrapper = mountEventField({
      hasFieldEvent: trigger => trigger === 'SCAN_COMPLETE',
      scanField,
      dispatchFieldEvent,
      getFieldEventState: () => ({ status: 'idle', loading: false, message: '' }),
    })

    const button = wrapper.find('.ai-form-field-event__action')
    expect(button.text()).toContain('扫码')
    await button.trigger('click')

    expect(scanField).toHaveBeenCalledWith(expect.objectContaining({ field: 'mobile' }))
    expect(wrapper.emitted('update:value')).toEqual([['SKU-001']])
    expect(dispatchFieldEvent).toHaveBeenCalledWith('SCAN_COMPLETE', 'mobile', {
      scan: { value: 'SKU-001', type: 'barCode', platform: 'H5' },
    })
  })

  it('renders the barcode scanner field and writes the scanned value', async () => {
    const scanField = vi.fn(async field => ({
      value: '6901234567890',
      type: 'EAN_13',
      platform: 'H5',
      field: field.field,
    }))
    const dispatchFieldEvent = vi.fn()
    const wrapper = mount(AiFormItem, {
      props: {
        field: {
          field: 'barcode',
          label: '商品条码',
          type: 'barcodeScanner',
          props: { allowManualInput: true, timeoutMs: 5000 },
        },
        value: '',
        formData: { barcode: '' },
        context: {
          scanField,
          dispatchFieldEvent,
          hasFieldEvent: () => false,
          getFieldEventState: () => ({ status: 'idle', loading: false, message: '' }),
        },
      },
      global: {
        stubs: {
          ...naiveStubs,
          NFormItem: NFormItemStub,
          NButton: {
            template: '<button class="barcode-scan-button"><slot /></button>',
          },
          AiRecordSelectorModal: true,
        },
      },
    })

    expect(wrapper.find('.ai-form-barcode-scanner-field').exists()).toBe(true)
    await wrapper.find('.barcode-scan-button').trigger('click')

    expect(scanField).toHaveBeenCalledWith(expect.objectContaining({
      field: 'barcode',
      type: 'barcodeScanner',
    }))
    expect(wrapper.emitted('update:value')).toEqual([['6901234567890']])
    expect(dispatchFieldEvent).toHaveBeenCalledWith('SCAN_COMPLETE', 'barcode', {
      scan: expect.objectContaining({ value: '6901234567890', platform: 'H5' }),
    })
  })
})

describe('aiFormItem multi-select storage', () => {
  const NSelectStub = {
    name: 'NSelect',
    props: ['value', 'multiple', 'options'],
    emits: ['update:value'],
    template: '<div class="n-select-stub" />',
  }

  it('echoes comma-separated values as arrays and stores them as comma-separated text', async () => {
    const wrapper = mount(AiFormItem, {
      props: {
        field: {
          field: 'status',
          label: '状态',
          type: 'select',
          multiple: true,
          options: [
            { label: '草稿', value: 'draft' },
            { label: '已发布', value: 'published' },
          ],
        },
        value: 'draft,published',
      },
      global: {
        stubs: {
          ...naiveStubs,
          NFormItem: NFormItemStub,
          NSelect: NSelectStub,
          AiRecordSelectorModal: true,
        },
      },
    })

    const select = wrapper.findComponent(NSelectStub)
    expect(select.props('multiple')).toBe(true)
    expect(select.props('value')).toEqual(['draft', 'published'])

    select.vm.$emit('update:value', ['draft'])
    await wrapper.vm.$nextTick()
    expect(wrapper.emitted('update:value')).toEqual([['draft']])
  })
})

describe('aiFormItem switch field', () => {
  const NSwitchStub = {
    name: 'NSwitch',
    props: ['value', 'checkedValue', 'uncheckedValue'],
    emits: ['update:value'],
    template: '<button class="n-switch-stub" type="button" />',
  }

  it('keeps numeric 0/1 pair even when props still carry boolean checked values', () => {
    const wrapper = mount(AiFormItem, {
      props: {
        field: {
          field: 'fieldSwitch',
          label: '开关',
          type: 'switch',
          props: {
            checkedValue: true,
            uncheckedValue: false,
            defaultValue: 1,
          },
        },
        value: 1,
      },
      global: {
        stubs: {
          ...naiveStubs,
          NFormItem: NFormItemStub,
          NSwitch: NSwitchStub,
          AiRecordSelectorModal: true,
        },
      },
    })

    const sw = wrapper.findComponent(NSwitchStub)
    expect(sw.props('checkedValue')).toBe(1)
    expect(sw.props('uncheckedValue')).toBe(0)
    expect(sw.props('value')).toBe(1)
    expect(wrapper.find('.ai-form-item--switch').exists()).toBe(true)
  })
})

describe('aiFormItem transfer placeholder', () => {
  it('maps placeholder to Naive Transfer filter placeholders', () => {
    const NTransferStub = {
      name: 'NTransfer',
      props: ['sourceFilterPlaceholder', 'targetFilterPlaceholder', 'options', 'value'],
      template: '<div class="n-transfer-stub" />',
    }
    const wrapper = mount(AiFormItem, {
      props: {
        field: {
          field: 'roles',
          label: '角色',
          type: 'transfer',
          placeholder: 'PH_transfer_47',
          props: {
            placeholder: 'PH_transfer_47',
            options: [
              { label: 'A', value: 'a' },
              { label: 'B', value: 'b' },
            ],
          },
        },
        value: [],
      },
      global: {
        stubs: {
          ...naiveStubs,
          NFormItem: NFormItemStub,
          NTransfer: NTransferStub,
          AiRecordSelectorModal: true,
        },
      },
    })

    const transfer = wrapper.findComponent({ name: 'NTransfer' })
    expect(transfer.exists()).toBe(true)
    expect(transfer.props('sourceFilterPlaceholder')).toBe('PH_transfer_47')
    expect(transfer.props('targetFilterPlaceholder')).toBe('PH_transfer_47')
  })
})

describe('aiFormItem detail readonly', () => {
  it('disables input when context.isDetail is true', () => {
    const wrapper = mount(AiFormItem, {
      props: {
        field: {
          field: 'title',
          label: '标题',
          type: 'input',
        },
        value: 'demo',
        context: {
          isDetail: true,
          modalStatus: 'detail',
        },
      },
      global: {
        stubs: {
          ...naiveStubs,
          NFormItem: NFormItemStub,
          NInput: {
            name: 'NInput',
            props: ['value', 'disabled', 'readonly'],
            template: '<input class="n-input-stub" :disabled="disabled" />',
          },
          AiRecordSelectorModal: true,
        },
      },
    })

    expect(wrapper.find('.ai-form-item--readonly').exists()).toBe(true)
    expect(wrapper.find('.n-input-stub').attributes('disabled')).toBeDefined()
  })
})
