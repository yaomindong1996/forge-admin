import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import { defineComponent, h } from 'vue'
import FlowBusinessForm from '../FlowBusinessForm.vue'

const moduleLoads = vi.hoisted(() => {
  const deferred = () => {
    let resolve
    const promise = new Promise((value) => {
      resolve = value
    })
    return { promise, resolve }
  }

  return {
    leave: deferred(),
    agent: deferred(),
    home: deferred(),
    invalid: deferred(),
  }
})

vi.mock('@/views/leave/LeaveApproveForm.vue', async () => ({
  default: await moduleLoads.leave.promise,
}))

vi.mock('@/views/ai/components/AgentConfigForm.vue', async () => ({
  default: await moduleLoads.agent.promise,
}))

vi.mock('@/views/home/index.vue', async () => ({
  default: await moduleLoads.home.promise,
}))

vi.mock('@/views/workspace/index.vue', async () => ({
  default: await moduleLoads.invalid.promise,
}))

const STUBS = {
  NSpin: { template: '<div class="spin"><slot /></div>' },
  NResult: { template: '<div class="result"><slot /><slot name="footer" /></div>' },
  NButton: { template: '<button @click="$emit(\'click\')"><slot /></button>', emits: ['click'] },
  NEmpty: { props: ['description'], template: '<div class="empty">{{ description }}</div>' },
}

const leaveComponent = defineComponent({
  name: 'LeaveApprovalForm',
  setup: () => () => h('div', { class: 'leave-form' }, 'leave form'),
})

const agentComponent = defineComponent({
  name: 'AgentConfigForm',
  setup: () => () => h('div', { class: 'agent-form' }, 'agent form'),
})

const homeComponent = defineComponent({
  name: 'HomeForm',
  setup: () => () => h('div', { class: 'home-form' }, 'home form'),
})

function mountForm(formUrl) {
  return mount(FlowBusinessForm, {
    props: { formUrl },
    global: { stubs: STUBS },
  })
}

describe('flowBusinessForm', () => {
  it('表单地址快速切换时只渲染最新请求的组件', async () => {
    const wrapper = mountForm('/leave/LeaveApproveForm')

    await wrapper.setProps({ formUrl: '/ai/components/AgentConfigForm' })
    moduleLoads.leave.resolve(leaveComponent)
    await flushPromises()

    expect(wrapper.find('.leave-form').exists()).toBe(false)
    expect(wrapper.text()).toContain('加载业务表单中')

    moduleLoads.agent.resolve(agentComponent)
    await flushPromises()

    expect(wrapper.find('.agent-form').exists()).toBe(true)
    expect(wrapper.find('.leave-form').exists()).toBe(false)
    wrapper.unmount()
  })

  it('组件卸载后异步结果不会重新写入动态组件', async () => {
    const wrapper = mountForm('/home/index')
    wrapper.unmount()

    moduleLoads.home.resolve(homeComponent)
    await flushPromises()

    expect(wrapper.vm.formComponent).toBe(null)
  })

  it('找不到表单组件时保持空状态而不渲染空动态组件', async () => {
    const wrapper = mountForm('/flow/DoesNotExist')
    await flushPromises()

    expect(wrapper.find('.empty').exists()).toBe(true)
    expect(wrapper.findComponent({ name: 'component' }).exists()).toBe(false)
    wrapper.unmount()
  })

  it('模块没有导出 Vue 组件时展示失败状态而不渲染无效节点', async () => {
    const wrapper = mountForm('/workspace/index')
    moduleLoads.invalid.resolve(null)
    await flushPromises()

    expect(wrapper.vm.formComponent).toBe(null)
    expect(wrapper.vm.loadError).toContain('无法加载表单组件')
    expect(wrapper.find('.empty').exists()).toBe(false)
    wrapper.unmount()
  })
})
