import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { effectScope } from 'vue'
import { confirmPluginTask, getPluginTask, reviewPluginTask, uploadPluginPackage } from '@/api/system/pluginTask'
import { usePluginReviewStore } from '@/stores/plugin/reviewStore'
import { usePluginTaskActions } from '../usePluginTaskActions'

vi.mock('@/api/system/pluginTask', () => ({
  getPluginTask: vi.fn(),
  uploadPluginPackage: vi.fn(),
  confirmPluginTask: vi.fn(),
  cancelPluginTask: vi.fn(),
  reviewPluginTask: vi.fn(),
}))
const response = status => ({ code: 200, data: { id: 'task', status, revision: 0, sha256: 'hash' } })
const scopes = []
function setup() {
  const scope = effectScope()
  scopes.push(scope)
  const refresh = vi.fn().mockResolvedValue()
  return { state: scope.run(() => usePluginTaskActions(refresh)), refresh, scope }
}
describe('workbench actions', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    setActivePinia(createPinia())
  })
  afterEach(() => scopes.splice(0).forEach(scope => scope.stop()))
  it('keeps one upload id for retry after an uncertain network failure', async () => {
    const { state, refresh } = setup()
    uploadPluginPackage.mockRejectedValueOnce(new Error('网络失败')).mockResolvedValueOnce(response('await_confirmation'))
    await state.upload(new File(['zip'], 'demo.zip'))
    expect(state.pendingUpload.value).not.toBeNull()
    await state.retryUpload()
    expect(uploadPluginPackage.mock.calls[0][1]).toBe(uploadPluginPackage.mock.calls[1][1])
    expect(state.task.value.status).toBe('await_confirmation')
    expect(state.pendingUpload.value).toBeNull()
    expect(refresh).toHaveBeenCalledTimes(1)
  })
  it('invalid file is rejected locally without a request', async () => {
    const { state } = setup()
    await state.upload(new File(['bad'], 'bad.txt'))
    expect(state.actionError.value).toContain('ZIP')
    expect(uploadPluginPackage).not.toHaveBeenCalled()
  })
  it('one in-flight confirmation, cannot close or switch tasks until it finishes', async () => {
    const { state } = setup()
    getPluginTask.mockResolvedValue(response('await_confirmation'))
    await state.open('task')
    let resolve
    confirmPluginTask.mockReturnValue(new Promise((done) => {
      resolve = done
    }))
    const pending = state.confirm()
    state.confirm()
    state.close()
    state.open('other')
    expect(confirmPluginTask).toHaveBeenCalledTimes(1)
    expect(state.visible.value).toBe(true)
    resolve(response('queued'))
    await pending
    expect(state.task.value.status).toBe('queued')
  })
  it('disposed page never reopens after a delayed upload', async () => {
    const { state, scope, refresh } = setup()
    let resolve
    uploadPluginPackage.mockReturnValue(new Promise((done) => {
      resolve = done
    }))
    const pending = state.upload(new File(['zip'], 'demo.zip'))
    scope.stop()
    resolve(response('await_confirmation'))
    await pending
    expect(state.visible.value).toBe(false)
    expect(refresh).not.toHaveBeenCalled()
  })
  it('uncertain review retry retains exact frozen request and refresh reconciles a changed revision', async () => {
    const { state } = setup()
    getPluginTask.mockResolvedValue(response('built'))
    await state.open('task')
    const store = usePluginReviewStore()
    store.prepare(state.task.value, 'close_task', {
      executorStopped: true,
      notDeployed: true,
      note: '完整核查说明已停止执行器',
    })
    const command = store.pending
    reviewPluginTask.mockRejectedValueOnce(new Error('结果不确定')).mockResolvedValueOnce(response('closed'))
    await state.review()
    expect(store.pending).toBe(command)
    await state.review()
    expect(reviewPluginTask.mock.calls[0][0]).toBe(reviewPluginTask.mock.calls[1][0])
    expect(store.pending).toBeNull()
    expect(state.task.value.status).toBe('closed')
    store.prepare(state.task.value, 'close_task', {
      executorStopped: true,
      notDeployed: true,
      note: '完整核查说明已停止执行器',
    })
    getPluginTask.mockResolvedValue({ code: 200, data: { ...state.task.value, revision: 1 } })
    await state.refreshDetail()
    expect(store.pending).toBeNull()
  })
  it('late disposed review response cannot clear a new page draft', async () => {
    const { state, scope } = setup()
    getPluginTask.mockResolvedValue(response('built'))
    await state.open('task')
    const store = usePluginReviewStore()
    store.prepare(state.task.value, 'close_task', {
      executorStopped: true,
      notDeployed: true,
      note: '完整核查说明已停止执行器',
    })
    let finish
    reviewPluginTask.mockReturnValue(new Promise((resolve) => {
      finish = resolve
    }))
    const pending = state.review()
    scope.stop()
    store.prepare({ ...state.task.value, id: 'next' }, 'close_task', {
      executorStopped: true,
      notDeployed: true,
      note: '下一个页面独立核查的说明',
    })
    finish(response('closed'))
    await pending
    expect(store.pending.taskId).toBe('next')
  })
})
