import { beforeEach, describe, expect, it, vi } from 'vitest'
import { cancelPluginTask, confirmPluginTask, reviewPluginTask, uploadPluginPackage } from '@/api/system/pluginTask'
import { request } from '@/utils/request'

vi.mock('@/utils/request', () => ({ request: { get: vi.fn(), post: vi.fn() } }))
describe('plugin workbench protocol', () => {
  beforeEach(() => vi.clearAllMocks())
  it('uploads multipart with stable id, never JSON encryption or caller identity/path', () => {
    const file = new File(['zip'], 'demo.zip')
    uploadPluginPackage(file, 'id')
    const [url, form, config] = request.post.mock.calls[0]
    expect(url).toBe('/system/plugin-task/upload')
    expect([...form.keys()]).toEqual(['file', 'requestId'])
    expect(form.get('requestId')).toBe('id')
    expect(config.encrypt).toBe(false)
  })
  it('confirms and cancels exact digest and revision with encoded task id', () => {
    const task = { id: 'a/b', revision: 2, sha256: 'hash', tenantId: 99, command: 'not-sent' }
    confirmPluginTask(task)
    cancelPluginTask(task)
    expect(request.post.mock.calls[0]).toEqual([
      '/system/plugin-task/a%2Fb/confirm',
      { revision: 2, sha256: 'hash' },
      { needTip: false },
    ])
    expect(request.post.mock.calls[1][0]).toBe('/system/plugin-task/a%2Fb/cancel')
  })
  it('review uses encrypted JSON with fixed fields, never caller identity or commands', () => {
    const command = {
      taskId: 'a/b',
      requestId: 'id',
      revision: 3,
      sha256: 'zip',
      resultSha256: 'report',
      decision: 'close_task',
      executorStopped: true,
      notDeployed: true,
      note: '核查说明',
      artifactsReviewed: null,
      migrationsReviewed: null,
      tenantId: 99,
      command: 'not-sent',
    }
    reviewPluginTask(command)
    const [url, body, config] = request.post.mock.calls[0]
    expect(url).toBe('/system/plugin-task/a%2Fb/review')
    expect(body).not.toHaveProperty('tenantId')
    expect(body).not.toHaveProperty('command')
    expect(body.requestId).toBe('id')
    expect(config).toEqual({ needTip: false })
  })
})
