import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { usePluginArtifactStore } from '@/stores/plugin/artifactStore'
import PluginArtifactRegistration from '../components/PluginArtifactRegistration.vue'
import { parseArtifactMetadata } from '../pluginArtifactUtils'
import { artifactMetadata, artifactTask } from './artifactFixtures'

let allowed = true
vi.mock('../usePluginPermission', () => ({ usePluginPermission: () => () => allowed }))
const stubs = {
  NButton: { props: ['disabled'], template: '<button :disabled="disabled"><slot /></button>' },
  NAlert: { template: '<div><slot /></div>' },
  NInput: {
    props: ['value', 'disabled'],
    emits: ['update:value'],
    template: '<textarea :disabled="disabled" :value="value"'
      + ' @input="$emit(\'update:value\', $event.target.value)" />',
  },
  NCheckbox: {
    props: ['checked', 'disabled'],
    emits: ['update:checked'],
    template: '<label><input type="checkbox" :checked="checked" :disabled="disabled"'
      + ' @change="$emit(\'update:checked\', $event.target.checked)" /><slot /></label>',
  },
  NUpload: { template: '<div><slot /></div>' },
  NDescriptions: { template: '<div><slot /></div>' },
  NDescriptionsItem: { props: ['label'], template: '<div>{{ label }}<slot /></div>' },
  NSpace: { template: '<div><slot /></div>' },
}
const button = (wrapper, label) => wrapper.findAll('button').find(item => item.text().includes(label))

describe('候选制品人工登记', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    allowed = true
  })
  it('strict import rejects duplicate/unknown keys and all changed report bindings', () => {
    const task = artifactTask()
    const text = JSON.stringify(artifactMetadata(task))
    expect(parseArtifactMetadata(text, task).taskId).toBe(task.id)
    const invalid = [
      text.replace('{', '{"revision":3,'),
      text.replace('{', '{"tenantId":99,'),
      text.replace('"revision":3', '"revision":"3"'),
      text.replace('"artifactCount":1', '"artifactCount":2'),
      `${text}{}`,
      ' '.repeat(65537),
    ]
    for (const value of invalid) {
      expect(() => parseArtifactMetadata(value, task)).toThrow()
    }
  })
  it('freezes exact retry, clears only matched audit, then task switching resets drafts', () => {
    const task = artifactTask()
    const store = usePluginArtifactStore()
    store.select(task)
    store.draft.text = JSON.stringify(artifactMetadata(task))
    store.draft.note = '已经核查本地所有制品且尚未部署'
    store.draft.localVerified = true
    store.draft.notDeployed = true
    store.prepare(task)
    const frozen = store.pending
    expect(Object.isFrozen(frozen)).toBe(true)
    store.draft.note = 'changed'
    store.prepare(task)
    expect(store.pending).toBe(frozen)
    store.select({ ...task, status: 'closed', revision: 4 })
    expect(store.pending).toBe(frozen)
    store.select({ ...task, artifacts: [{ requestId: frozen.requestId }] })
    expect(store.pending).toBeNull()
    store.draft.text = 'draft'
    store.select({ ...task, id: 'other' })
    expect(store.draft.text).toBe('')
  })
  it('requires preview/checks/note, freezes retries and hides actions from unprivileged users', async () => {
    const task = artifactTask()
    const wrapper = mount(PluginArtifactRegistration, { props: { task }, global: { stubs } })
    await button(wrapper, '登记候选制品').trigger('click')
    const store = usePluginArtifactStore()
    store.draft.text = JSON.stringify(artifactMetadata(task))
    store.preview(task)
    store.draft.note = '已经核查本地所有制品且尚未部署'
    await wrapper.vm.$nextTick()
    expect(button(wrapper, '确认登记').element.disabled).toBe(true)
    store.draft.localVerified = true
    store.draft.notDeployed = true
    await wrapper.vm.$nextTick()
    await button(wrapper, '确认登记').trigger('click')
    expect(wrapper.emitted('register')).toHaveLength(1)
    expect(wrapper.find('textarea').element.disabled).toBe(true)
    const pending = store.pending
    await button(wrapper, '重试同一').trigger('click')
    expect(store.pending).toBe(pending)
    await wrapper.setProps({ busy: true })
    expect(button(wrapper, '重试同一').element.disabled).toBe(true)
    wrapper.unmount()
    allowed = false
    const readonly = mount(PluginArtifactRegistration, { props: { task }, global: { stubs } })
    expect(readonly.findAll('button')).toHaveLength(0)
    readonly.unmount()
  })
  it('stale historical registration is escaped and never displayed as deployed or downloadable', () => {
    const task = artifactTask()
    task.status = 'closed'
    task.artifacts = [{
      id: 'record',
      requestId: 'old',
      metadata: artifactMetadata(task),
      registeredBy: 9,
      registeredTime: '2026-10-08T12:00:00',
      note: '<img src=x onerror=alert(1)>',
      currentApprovalMatches: false,
    }]
    const wrapper = mount(PluginArtifactRegistration, { props: { task }, global: { stubs } })
    expect(wrapper.find('img').exists()).toBe(false)
    expect(wrapper.text()).toContain('当前审批不匹配')
    expect(wrapper.text()).toContain('未部署')
    expect(wrapper.find('a').exists()).toBe(false)
    expect(wrapper.findAll('button')).toHaveLength(0)
    wrapper.unmount()
  })
})
