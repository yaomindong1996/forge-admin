import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { getNodePresentation } from '../../constants/node-presentation.js'
import BasicConfig from '../BasicConfig.vue'
import NodeConfigDrawer from '../NodeConfigDrawer.vue'
import ServiceConfig from '../ServiceConfig.vue'

const stubs = {
  'n-drawer': { template: '<aside><slot /></aside>' },
  'n-drawer-content': { template: '<div><slot /></div>' },
  'n-form-item': { template: '<label><slot /></label>' },
  'n-input': true,
  'n-select': true,
  'n-switch': true,
  'n-button': {
    emits: ['click'],
    template: '<button type="button" @click="$emit(\'click\')"><slot /></button>',
  },
  'ApproverConfig': true,
}

function mountDrawer(extra = {}) {
  return mount(NodeConfigDrawer, {
    props: { visible: true, node: {
      id: 'service_1',
      nodeType: 'service',
      name: '服务任务',
      config: { implementationType: 'class', implementation: 'fixture.Delegate' },
    }, ...extra },
    global: { stubs },
  })
}

describe('nodeConfigDrawer 布局与草稿契约', () => {
  it('普通节点使用统一内容容器，标题复用画布图标和颜色', () => {
    const wrapper = mountDrawer()
    expect(wrapper.find('.node-config-fields').classes()).not.toContain('has-integrated-tabs')
    expect(wrapper.findAllComponents(BasicConfig)).toHaveLength(1)
    expect(wrapper.find('.flow-property-panel-shell__icon i').classes())
      .toContain(getNodePresentation('service').icon)
    expect(wrapper.find('.flow-property-panel-shell').attributes('style'))
      .toContain(getNodePresentation('service').color)
    wrapper.unmount()
  })

  it('审批节点由自身页签维护内边距，不重复渲染基础表单', () => {
    const wrapper = mountDrawer({ node: { id: 'A', nodeType: 'approver', name: '审批', config: {} } })
    expect(wrapper.find('.node-config-fields').classes()).toContain('has-integrated-tabs')
    expect(wrapper.findComponent(BasicConfig).exists()).toBe(false)
    expect(wrapper.find('approver-config-stub').exists()).toBe(true)
    wrapper.unmount()
  })

  it('调整字段后仅确认时输出草稿，保留原节点和原配置', async () => {
    const wrapper = mountDrawer()
    const original = wrapper.props('node')
    wrapper.findComponent(BasicConfig).vm.$emit('update:node', { ...original, name: '新名称' })
    await wrapper.vm.$nextTick()
    wrapper.findComponent(ServiceConfig).vm.$emit('update:config', { implementation: 'fixture.NewDelegate' })
    await wrapper.vm.$nextTick()
    expect(original.name).toBe('服务任务')
    expect(original.config.implementation).toBe('fixture.Delegate')
    await wrapper.findAll('button').find(button => button.text() === '确定').trigger('click')
    expect(wrapper.emitted('save')[0]).toEqual([{
      name: '新名称',
      config: { implementationType: 'class', implementation: 'fixture.NewDelegate' },
    }, 'service_1'])
    wrapper.unmount()
  })

  it('取消不保存，readonly 不提供确认且禁止提交草稿', async () => {
    const wrapper = mountDrawer()
    await wrapper.findAll('button').find(button => button.text() === '取消').trigger('click')
    expect(wrapper.emitted('save')).toBeUndefined()
    expect(wrapper.emitted('update:visible')[0]).toEqual([false])
    wrapper.unmount()
    const readonly = mountDrawer({ readonly: true })
    expect(readonly.text()).not.toContain('确定')
    expect(readonly.vm.commitDraft()).toBe(false)
    expect(readonly.emitted('save')).toBeUndefined()
    readonly.unmount()
  })
})
