import { mount } from '@vue/test-utils'
import { NCheckbox } from 'naive-ui'
import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'
import { usePluginReviewStore } from '@/stores/plugin/reviewStore'
import PluginTaskReview from '../components/PluginTaskReview.vue'

let allowed = true
vi.mock('../usePluginPermission', () => ({ usePluginPermission: () => () => allowed }))
vi.mock('@/composables/useDict', () => ({ useDict: () => ({ dict: {} }) }))
function task() {
  return {
    id: 'task',
    status: 'built',
    revision: 3,
    sha256: 'a'.repeat(64),
    execution: { resultSha256: 'b'.repeat(64), result: { success: true }, leaseExpired: false },
    reviews: [],
  }
}
const stubs = {
  NButton: { props: ['disabled'], template: '<button :disabled="disabled"><slot /></button>' },
  NAlert: { props: ['title'], template: '<div><div>{{ title }}</div><slot /></div>' },
  NCheckbox: {
    props: ['checked', 'disabled'],
    emits: ['update:checked'],
    template: '<label><input type="checkbox" :checked="checked" :disabled="disabled"'
      + ' @change="$emit(\'update:checked\', $event.target.checked)" /><slot /></label>',
  },
  NInput: {
    props: ['value', 'disabled'],
    emits: ['update:value'],
    template: '<textarea :value="value" :disabled="disabled"'
      + ' @input="$emit(\'update:value\', $event.target.value)" />',
  },
  NSpace: { template: '<div><slot /></div>' },
  DictTag: { props: ['value'], template: '<span>{{ value }}</span>' },
}
const wrappers = []
function setup(value = task()) {
  const wrapper = mount(PluginTaskReview, { props: { task: value }, global: { stubs } })
  wrappers.push(wrapper)
  return wrapper
}
function button(wrapper, label) {
  return wrapper.findAll('button').find(item => item.text().includes(label))
}
describe('人工核查表单', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    allowed = true
  })
  afterEach(() => wrappers.splice(0).forEach(wrapper => wrapper.unmount()))
  it('active builds and unprivileged users have no review actions, expired builds can close only', () => {
    const current = task()
    current.status = 'building'
    expect(setup(current).findAll('button')).toHaveLength(0)
    current.execution.leaseExpired = true
    expect(setup(current).findAll('button').map(item => item.text())).toEqual(['核查并关闭任务'])
    allowed = false
    expect(setup().findAll('button')).toHaveLength(0)
  })
  it('approval needs all confirmations and note, freezes payload for exact retry', async () => {
    const wrapper = setup()
    await button(wrapper, '审查构建').trigger('click')
    expect(button(wrapper, '确认审查通过').element.disabled).toBe(true)
    for (const check of wrapper.findAllComponents(NCheckbox)) check.vm.$emit('update:checked', true)
    await nextTick()
    await wrapper.find('textarea').setValue('已核对本次产物、权限及迁移恢复方案')
    expect(wrapper.findAllComponents(NCheckbox).map(check => check.props('checked'))).toEqual([true, true, true, true])
    expect(button(wrapper, '确认审查通过').element.disabled).toBe(false)
    await button(wrapper, '确认审查通过').trigger('click')
    const store = usePluginReviewStore()
    const pending = store.pending
    expect(pending.resultSha256).toBe('b'.repeat(64))
    expect(pending.decision).toBe('approve_build')
    expect(wrapper.find('textarea').element.disabled).toBe(true)
    await button(wrapper, '重试同一核查请求').trigger('click')
    expect(store.pending).toBe(pending)
    expect(wrapper.emitted('review')).toHaveLength(2)
    await wrapper.setProps({ busy: true })
    expect(button(wrapper, '重试同一核查请求').element.disabled).toBe(true)
  })
  it('manual closure escapes history and cannot overwrite another task request', async () => {
    const value = task()
    value.reviews = [{ id: 'review', decision: 'close_task', reviewedBy: 9, note: '<img src=x onerror=alert(1)>' }]
    const wrapper = setup(value)
    expect(wrapper.find('img').exists()).toBe(false)
    expect(wrapper.text()).toContain('<img src=x onerror=alert(1)>')
    const store = usePluginReviewStore()
    store.prepare({ ...value, id: 'other' }, 'close_task', {
      note: '另一个核查请求',
      executorStopped: true,
      notDeployed: true,
    })
    await button(wrapper, '核查并关闭任务').trigger('click')
    expect(wrapper.text()).toContain('返回上一任务')
    expect(store.pending.taskId).toBe('other')
  })
  it('opening another view restores same pending checks, but refreshed new revision clears the request', async () => {
    const value = task()
    const store = usePluginReviewStore()
    store.prepare(value, 'close_task', {
      executorStopped: true,
      notDeployed: true,
      note: '完整核查说明已停止执行器',
    })
    const wrapper = setup(value)
    expect(wrapper.find('textarea').element.value).toContain('完整核查说明')
    expect(wrapper.find('textarea').element.disabled).toBe(true)
    store.reconcile({ ...value, revision: 4 })
    await wrapper.setProps({ task: { ...value, status: 'closed', revision: 4 } })
    expect(store.pending).toBeNull()
    expect(wrapper.find('textarea').exists()).toBe(false)
  })
})
