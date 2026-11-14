import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { defineComponent, nextTick, ref } from 'vue'
import ConditionConfig from '../ConditionConfig.vue'

const DOLLAR = '$'

const STUBS = {
  'n-form-item': { template: '<div class="n-form-item"><slot /></div>' },
  'n-input': {
    props: ['value', 'disabled', 'size', 'type', 'autosize'],
    emits: ['update:value'],
    template: '<input class="n-input" :value="value" :disabled="disabled" v-bind="$attrs" @input="$emit(\'update:value\', $event.target.value)" />',
  },
  'n-select': {
    props: ['value', 'options', 'disabled', 'size', 'filterable'],
    emits: ['update:value'],
    template: `
      <select class="n-select" :value="value" :disabled="disabled" v-bind="$attrs" @change="$emit('update:value', $event.target.value)">
        <option v-for="option in options" :key="option.value" :value="option.value">{{ option.label }}</option>
      </select>
    `,
  },
  'n-radio': {
    props: ['checked', 'disabled'],
    emits: ['click'],
    template: '<label class="n-radio" @click="!disabled && $emit(\'click\', $event)"><input type="radio" :checked="checked" :disabled="disabled"><slot /></label>',
  },
  'n-tag': { template: '<span class="n-tag"><slot /></span>' },
}

const fields = [
  { field: 'amount', label: '总金额', dataType: 'number', required: true },
  { field: 'jtpNo', label: 'JTPO单号', dataType: 'string' },
]

function mountConfig(options = {}) {
  const Parent = defineComponent({
    components: { ConditionConfig },
    setup() {
      const edges = ref(options.edges || [
        { id: 'F1', source: 'GW', target: 'T1', condition: '', isDefault: false },
      ])
      const node = options.node || { id: 'GW', nodeType: 'condition', name: '条件分支', config: {} }
      const nodes = options.nodes || [
        { id: 'GW', name: '条件分支' },
        { id: 'T1', name: '财务审批' },
        { id: 'T2', name: '法务审批' },
      ]
      const formFieldCatalog = options.formFieldCatalog ?? fields
      const focusEdgeId = options.focusEdgeId || ''

      function updateEdge(edgeId, patch) {
        edges.value = edges.value.map(edge => edge.id === edgeId ? { ...edge, ...patch } : edge)
      }

      return { edges, node, nodes, formFieldCatalog, focusEdgeId, updateEdge }
    },
    template: `
      <ConditionConfig
        :node="node"
        :outgoing-edges="edges"
        :nodes="nodes"
        :form-field-catalog="formFieldCatalog"
        :focus-edge-id="focusEdgeId"
        @update:edge="updateEdge"
      />
    `,
  })

  return mount(Parent, {
    global: { stubs: STUBS },
  })
}

describe('conditionConfig', () => {
  it('包容分支也能配置分支条件与默认分支，并行分支只展示分支列表', () => {
    const inclusive = mountConfig({ node: { id: 'GW', nodeType: 'inclusive', name: '包容分支', config: {} } })
    expect(inclusive.text()).toContain('开发者高级')
    expect(inclusive.text()).not.toContain('不需要配置条件表达式')
    expect(inclusive.find('.n-radio input').attributes('disabled')).toBeUndefined()
    inclusive.unmount()

    const parallel = mountConfig({ node: { id: 'GW', nodeType: 'parallel', name: '并行分支', config: {} } })
    expect(parallel.text()).toContain('不需要配置条件表达式')
    expect(parallel.find('.n-radio input').attributes('disabled')).toBeDefined()
    parallel.unmount()
  })

  it('有动态表单字段时展示字段条件配置器', () => {
    const wrapper = mountConfig()

    expect(wrapper.text()).toContain('可使用 2 个表单字段生成表达式')
    expect(wrapper.text()).toContain('审批结果')
    expect(wrapper.text()).toContain('业务字段')
    expect(wrapper.text()).toContain('开发者高级')
    expect(wrapper.text()).toContain('同意通过')
    expect(wrapper.text()).toContain('驳回修改')

    wrapper.unmount()
  })

  it('审批结果选择驳回修改时生成 Flowable 条件表达式', async () => {
    const wrapper = mountConfig()

    expect(wrapper.find('[data-test="mode-approval-result"]').classes()).toContain('active')

    await wrapper.find('[data-test="approval-result-reject"]').trigger('click')
    await nextTick()

    expect(wrapper.vm.edges[0].condition).toBe(`${DOLLAR}{approvalResult == 'reject'}`)
    expect(wrapper.vm.edges[0].conditionMode).toBe('approvalResult')
    expect(wrapper.vm.edges[0].approvalResult).toBe('reject')
    expect(wrapper.vm.edges[0].approvalResultLabel).toBe('驳回修改')

    wrapper.unmount()
  })

  it('已有驳回表达式重新打开时回显为审批结果模式', () => {
    const wrapper = mountConfig({
      edges: [
        { id: 'F1', source: 'GW', target: 'T1', condition: `${DOLLAR}{approvalResult == 'reject'}`, isDefault: false },
      ],
    })

    expect(wrapper.find('[data-test="mode-approval-result"]').classes()).toContain('active')
    expect(wrapper.find('[data-test="approval-result-reject"]').classes()).toContain('active')
    expect(wrapper.text()).toContain('审批人要求申请人修改后走这条分支')

    wrapper.unmount()
  })

  it('切换到业务字段后展示字段条件配置器', async () => {
    const wrapper = mountConfig()

    await wrapper.find('[data-test="mode-rules"]').trigger('click')
    await nextTick()

    expect(wrapper.text()).toContain('总金额（必填）')
    expect(wrapper.text()).toContain('JTPO单号')

    wrapper.unmount()
  })

  it('字段条件输入数值后生成 SpEL 表达式', async () => {
    const wrapper = mountConfig()

    await wrapper.find('[data-test="mode-rules"]').trigger('click')
    await nextTick()
    await wrapper.find('[data-test="rule-value"]').setValue('3000')
    await nextTick()

    expect(wrapper.vm.edges[0].condition).toBe(`${DOLLAR}{amount == 3000}`)
    expect(wrapper.vm.edges[0].conditionMode).toBe('rules')
    expect(wrapper.vm.edges[0].conditionRules[0]).toMatchObject({
      field: 'amount',
      operator: 'eq',
      value: '3000',
    })

    wrapper.unmount()
  })

  it('区间条件生成起止范围表达式', async () => {
    const wrapper = mountConfig()

    await wrapper.find('[data-test="mode-rules"]').trigger('click')
    await nextTick()
    await wrapper.find('[data-test="rule-operator"]').setValue('between')
    await nextTick()
    await wrapper.find('[data-test="rule-value"]').setValue('0')
    await wrapper.find('[data-test="rule-end-value"]').setValue('3000')
    await nextTick()

    expect(wrapper.vm.edges[0].condition).toBe(`${DOLLAR}{(amount >= 0 && amount <= 3000)}`)

    wrapper.unmount()
  })

  it('删除最后一条规则后清空该分支条件', async () => {
    const wrapper = mountConfig()

    await wrapper.find('[data-test="mode-rules"]').trigger('click')
    await nextTick()
    await wrapper.find('[data-test="rule-value"]').setValue('3000')
    await nextTick()
    await wrapper.find('.condition-rule-remove').trigger('click')
    await nextTick()

    expect(wrapper.vm.edges[0].condition).toBe('')
    expect(wrapper.vm.edges[0].conditionRules).toEqual([])
    expect(wrapper.text()).toContain('暂无条件，点击“添加条件”后重新配置')

    wrapper.unmount()
  })

  it('默认分支也可以配置字段条件', async () => {
    const wrapper = mountConfig({
      edges: [
        { id: 'F1', source: 'GW', target: 'T1', condition: '', isDefault: true },
      ],
    })

    expect(wrapper.text()).toContain('默认分支')
    expect(wrapper.text()).toContain('审批结果')

    await wrapper.find('[data-test="mode-rules"]').trigger('click')
    await nextTick()
    await wrapper.find('[data-test="rule-value"]').setValue('5000')
    await nextTick()

    expect(wrapper.vm.edges[0].isDefault).toBe(true)
    expect(wrapper.vm.edges[0].condition).toBe(`${DOLLAR}{amount == 5000}`)

    wrapper.unmount()
  })

  it('设置为默认分支时保留已有条件表达式', async () => {
    const wrapper = mountConfig({
      edges: [
        { id: 'F1', source: 'GW', target: 'T1', condition: `${DOLLAR}{amount > 1000}`, isDefault: false },
      ],
    })

    await wrapper.find('.condition-default-row .n-radio').trigger('click')
    await nextTick()

    expect(wrapper.vm.edges[0].isDefault).toBe(true)
    expect(wrapper.vm.edges[0].condition).toBe(`${DOLLAR}{amount > 1000}`)

    wrapper.unmount()
  })

  it('传入聚焦分支时只展示对应分支配置', () => {
    const wrapper = mountConfig({
      focusEdgeId: 'F2',
      edges: [
        { id: 'F1', source: 'GW', target: 'T1', condition: '', isDefault: false },
        { id: 'F2', source: 'GW', target: 'T2', condition: '', isDefault: false },
      ],
    })

    expect(wrapper.text()).toContain('正在配置 分支 2')
    expect(wrapper.text()).toContain('下游节点：法务审批')
    expect(wrapper.text()).not.toContain('下游节点：财务审批')
    expect(wrapper.findAll('.condition-branch')).toHaveLength(1)

    wrapper.unmount()
  })

  it('已有手写表达式默认进入高级表达式模式并保持可编辑', async () => {
    const wrapper = mountConfig({
      edges: [
        { id: 'F1', source: 'GW', target: 'T1', condition: `${DOLLAR}{days > 3}`, isDefault: false },
      ],
    })

    expect(wrapper.find('[data-test="mode-advanced"]').classes()).toContain('active')
    await wrapper.find('.n-input').setValue(`${DOLLAR}{amount > 1000}`)
    await nextTick()

    expect(wrapper.vm.edges[0].condition).toBe(`${DOLLAR}{amount > 1000}`)
    expect(wrapper.vm.edges[0].conditionMode).toBe('advanced')
    expect(wrapper.vm.edges[0].advancedCondition).toBe(`${DOLLAR}{amount > 1000}`)

    wrapper.unmount()
  })

  it('高级表达式切换到表单字段条件再切回时保留原内容', async () => {
    const advancedExpression = `${DOLLAR}{days > 3 && applicant == 'tom'}`
    const wrapper = mountConfig({
      edges: [
        {
          id: 'F1',
          source: 'GW',
          target: 'T1',
          condition: advancedExpression,
          conditionMode: 'advanced',
          isDefault: false,
        },
      ],
    })

    expect(wrapper.find('[data-test="mode-advanced"]').classes()).toContain('active')
    expect(wrapper.find('.n-input').element.value).toBe(advancedExpression)

    await wrapper.find('[data-test="mode-rules"]').trigger('click')
    await nextTick()
    expect(wrapper.vm.edges[0].conditionMode).toBe('rules')
    expect(wrapper.vm.edges[0].advancedCondition).toBe(advancedExpression)

    await wrapper.find('[data-test="rule-value"]').setValue('3000')
    await nextTick()
    expect(wrapper.vm.edges[0].condition).toBe(`${DOLLAR}{amount == 3000}`)
    expect(wrapper.vm.edges[0].rulesCondition).toBe(`${DOLLAR}{amount == 3000}`)

    await wrapper.find('[data-test="mode-advanced"]').trigger('click')
    await nextTick()

    expect(wrapper.vm.edges[0].conditionMode).toBe('advanced')
    expect(wrapper.vm.edges[0].condition).toBe(advancedExpression)
    expect(wrapper.find('.n-input').element.value).toBe(advancedExpression)

    wrapper.unmount()
  })

  it('表单字段表达式重新打开时回显为字段条件模式', async () => {
    const wrapper = mountConfig({
      edges: [
        { id: 'F1', source: 'GW', target: 'T1', condition: `${DOLLAR}{amount > 1000}`, isDefault: false },
      ],
    })

    expect(wrapper.find('[data-test="mode-rules"]').classes()).toContain('active')
    expect(wrapper.find('[data-test="rule-field"]').element.value).toBe('amount')
    expect(wrapper.find('[data-test="rule-operator"]').element.value).toBe('gt')
    expect(wrapper.find('[data-test="rule-value"]').element.value).toBe('1000')

    await wrapper.find('[data-test="rule-value"]').setValue('2000')
    await nextTick()
    expect(wrapper.vm.edges[0].condition).toBe(`${DOLLAR}{amount > 2000}`)
    expect(wrapper.vm.edges[0].conditionMode).toBe('rules')

    wrapper.unmount()
  })
})
