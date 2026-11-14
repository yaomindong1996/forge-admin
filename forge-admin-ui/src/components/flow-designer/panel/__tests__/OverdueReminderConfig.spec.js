import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { defineComponent, ref } from 'vue'
import OverdueReminderConfig from '../OverdueReminderConfig.vue'

vi.mock('@/api/message', () => ({
  default: {
    getTemplatePage: vi.fn(() => Promise.resolve({
      data: {
        records: [
          { templateCode: 'FLOW_TASK_OVERDUE', templateName: '流程任务逾期提醒', enabled: 1 },
          { templateCode: 'DISABLED_TEMPLATE', templateName: '禁用模板', enabled: 0 },
        ],
      },
    })),
  },
}))

vi.mock('@/composables/useDict', () => ({
  useDict: () => ({
    dict: ref({
      sys_message_channel: [
        { label: '站内信', value: 'WEB', status: 1 },
        { label: '邮件', value: 'EMAIL', status: 1 },
      ],
      sys_flow_overdue_repeat_mode: [
        { label: '仅一次', value: 'once', status: 1 },
        { label: '按间隔重复', value: 'interval', status: 1 },
      ],
    }),
    loading: ref(false),
  }),
}))

const STUBS = {
  'n-form': { template: '<form><slot /></form>' },
  'n-form-item': {
    props: ['label'],
    template: '<label><span>{{ label }}</span><slot /></label>',
  },
  'n-switch': {
    props: ['value', 'disabled'],
    emits: ['update:value'],
    template: '<input data-test="enabled" type="checkbox" :checked="value" :disabled="disabled" @change="$emit(\'update:value\', $event.target.checked)" />',
  },
  'n-input-number': {
    props: ['value', 'disabled'],
    emits: ['update:value'],
    template: '<input class="number-input" type="number" :value="value" :disabled="disabled" @input="$emit(\'update:value\', Number($event.target.value))" />',
  },
  'n-select': {
    props: ['value', 'options', 'disabled', 'multiple'],
    emits: ['update:value', 'focus', 'search'],
    template: '<select class="select-input" :multiple="multiple" :disabled="disabled" @change="$emit(\'update:value\', multiple ? Array.from($event.target.selectedOptions).map(o => o.value) : $event.target.value)"><option v-for="opt in options" :key="opt.value" :value="opt.value">{{ opt.label }}</option></select>',
  },
}

function mountConfig(initialConfig = {}) {
  const Parent = defineComponent({
    components: { OverdueReminderConfig },
    setup() {
      const config = ref({
        dueDateDays: 0,
        dueDateHours: 0,
        overdueReminderEnabled: false,
        overdueReminderTemplateCode: 'FLOW_TASK_OVERDUE',
        overdueReminderChannels: ['WEB'],
        overdueReminderRepeatMode: 'once',
        overdueReminderIntervalMinutes: 1440,
        overdueReminderMaxTimes: 1,
        ...initialConfig,
      })
      function updateConfig(patch) {
        config.value = { ...config.value, ...patch }
      }
      return { config, updateConfig }
    },
    template: `
      <OverdueReminderConfig
        :config="config"
        @update:config="updateConfig"
      />
    `,
  })

  return mount(Parent, {
    global: { stubs: STUBS },
  })
}

describe('overdueReminderConfig', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    // 推送方式与重复策略改用 DictSelect，其快捷新增能力依赖 Pinia
    setActivePinia(createPinia())
  })

  it('展示处理时限、模板、渠道和重复策略字段', () => {
    const wrapper = mountConfig()

    expect(wrapper.text()).toContain('处理时限')
    expect(wrapper.text()).toContain('逾期提醒')
    expect(wrapper.text()).toContain('消息模板')
    expect(wrapper.text()).toContain('推送方式')
    expect(wrapper.text()).toContain('重复策略')

    wrapper.unmount()
  })

  it('修改处理天数时同步 legacy dueDate 字段', async () => {
    const wrapper = mountConfig()
    const firstNumberInput = wrapper.findAll('.number-input')[0]

    await firstNumberInput.setValue(2)

    expect(wrapper.vm.config.dueDateDays).toBe(2)
    expect(wrapper.vm.config.dueDate).toBe(2)

    wrapper.unmount()
  })

  it('切回仅一次策略时最大次数重置为 1', async () => {
    const wrapper = mountConfig({
      overdueReminderEnabled: true,
      overdueReminderRepeatMode: 'interval',
      overdueReminderMaxTimes: 5,
    })
    const repeatSelect = wrapper.findAll('.select-input')[2]

    await repeatSelect.setValue('once')

    expect(wrapper.vm.config.overdueReminderRepeatMode).toBe('once')
    expect(wrapper.vm.config.overdueReminderMaxTimes).toBe(1)

    wrapper.unmount()
  })
})
