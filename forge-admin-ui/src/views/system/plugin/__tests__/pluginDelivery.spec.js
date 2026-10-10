import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { usePluginDeliveryStore } from '@/stores/plugin/deliveryStore'
import PluginDeliveryForm from '../components/PluginDeliveryForm.vue'

const api = vi.hoisted(() => ({ listDeliveryTargets: vi.fn(), listDeliveryCandidates: vi.fn(), listDeliveries: vi.fn(), createDelivery: vi.fn(), reconcileDelivery: vi.fn() }))
vi.mock('@/api/system/pluginDelivery', () => api)
vi.mock('@/composables/useDict', () => ({ useDict: () => ({ dict: { value: { sys_plugin_delivery_action: [] } } }) }))
const success = data => ({ code: 200, data })
function seed(store) {
  store.targets = [{ id: 'test', name: '测试目标', repositoryId: 'local-test', previousReleaseId: 'old', currentReleaseId: 'current', activeTaskId: null }]
  store.candidates = [{ taskId: 'task', releaseId: 'old', repositoryId: 'local-test' }]
  Object.assign(store.draft, { targetId: 'test', releaseId: 'old', action: 'publish', note: '已核查制品及目标范围符合本次发布要求' })
}
describe('交付工作台真实API契约与状态', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.resetAllMocks()
    api.listDeliveryTargets.mockResolvedValue(success([]))
    api.listDeliveryCandidates.mockResolvedValue(success([]))
    api.listDeliveries.mockResolvedValue(success([]))
  })
  it('backup, target occupation, repository and confirmed restore version required', () => {
    const store = usePluginDeliveryStore()
    seed(store)
    expect(store.complete).toBe(true)
    store.draft.action = 'deploy'
    expect(store.complete).toBe(false)
    store.draft.backupReference = 'backup-01'
    store.draft.migrationsReviewed = true
    expect(store.complete).toBe(true)
    store.draft.action = 'restore'
    expect(store.complete).toBe(false)
    store.draft.backwardCompatible = true
    expect(store.complete).toBe(true)
    store.targets[0].unverifiedReleaseId = 'unknown'
    expect(store.complete).toBe(false)
    store.targets[0].currentReleaseId = 'old'
    expect(store.complete).toBe(true)
    store.draft.action = 'deploy'
    expect(store.complete).toBe(false)
    store.targets[0].activeTaskId = 'other'
    expect(store.complete).toBe(false)
  })
  it('uncertain creation freezes request and retries exact same content, not a second deployment', async () => {
    const store = usePluginDeliveryStore()
    seed(store)
    api.createDelivery.mockRejectedValueOnce(new Error('lost response'))
    await store.submit()
    const frozen = store.pending
    expect(Object.isFrozen(frozen)).toBe(true)
    store.draft.note = 'new note must not change an uncertain request'
    api.createDelivery.mockResolvedValueOnce(success({ ...frozen, id: 'created' }))
    await store.submit()
    expect(api.createDelivery.mock.calls[1][0]).toBe(frozen)
    expect(store.pending).toBeNull()
    expect(store.error).toBe('')
  })
  it('foreign or incomplete creation response never discards frozen retry', async () => {
    const store = usePluginDeliveryStore()
    seed(store)
    api.createDelivery.mockResolvedValueOnce(success({ id: 'foreign', targetId: 'other' }))
    await store.submit()
    expect(store.pending).not.toBeNull()
    expect(store.error).toContain('未确认')
  })
  it('a late old list cannot overwrite newer target state', async () => {
    const store = usePluginDeliveryStore()
    let finishOld
    api.listDeliveryTargets.mockImplementationOnce(() => new Promise((resolve) => {
      finishOld = resolve
    }))
    const old = store.load()
    api.listDeliveryTargets.mockResolvedValueOnce(success([{ id: 'new-target' }]))
    await store.load()
    finishOld(success([{ id: 'old-target' }]))
    await old
    expect(store.targets).toEqual([{ id: 'new-target' }])
    expect(store.loading).toBe(false)
  })
  it('malformed or stale lists never replace newer response; manual close requires confirmation', async () => {
    const store = usePluginDeliveryStore()
    api.listDeliveries.mockResolvedValueOnce(success({ incomplete: true }))
    await store.load()
    expect(store.error).toContain('失败')
    expect(await store.reconcile('task', { executorStopped: false, note: '执行器已经停止且核查了本次目标' })).toBe(false)
    expect(api.reconcileDelivery).not.toHaveBeenCalled()
    api.reconcileDelivery.mockResolvedValueOnce(success({ status: 'reconciled' }))
    expect(await store.reconcile('task', { executorStopped: true, note: '执行器已经停止且核查了本次目标' })).toBe(true)
  })
  it('actual Naive UI form renders independent confirmations, no executable path or credentials', async () => {
    const store = usePluginDeliveryStore()
    seed(store)
    const wrapper = mount(PluginDeliveryForm)
    expect(wrapper.text()).toContain('确认创建交付任务')
    store.draft.action = 'restore'
    await wrapper.vm.$nextTick()
    expect(wrapper.text()).toContain('向后兼容')
    expect(wrapper.findAll('input[type=password]')).toHaveLength(0)
    expect(wrapper.text()).not.toContain('shell')
    wrapper.unmount()
  })
})
