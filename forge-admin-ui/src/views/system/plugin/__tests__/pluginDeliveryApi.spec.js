import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  createDelivery,
  listDeliveries,
  listDeliveryCandidates,
  listDeliveryTargets,
  reconcileDelivery,
} from '@/api/system/pluginDelivery'
import { request } from '@/utils/request'

vi.mock('@/utils/request', () => ({ request: { get: vi.fn(), post: vi.fn() } }))
describe('独立交付API固定字段协议', () => {
  beforeEach(() => vi.clearAllMocks())
  it('loads real target/candidate/audit endpoints', () => {
    listDeliveryTargets()
    listDeliveryCandidates()
    listDeliveries()
    expect(request.get.mock.calls.map(row => row[0])).toEqual([
      '/system/plugin-delivery/targets',
      '/system/plugin-delivery/candidates',
      '/system/plugin-delivery/list',
    ])
  })
  it('creation cannot forward caller identity, path, URL, credentials or shell', () => {
    const allowed = {
      requestId: 'request',
      taskId: 'task',
      targetId: 'test',
      action: 'publish',
      releaseId: 'release',
      note: '已核查本次发布候选',
      backupReference: null,
      migrationsReviewed: false,
      backwardCompatible: false,
    }
    createDelivery({ ...allowed, tenantId: 99, token: 'not-sent', path: '/forbidden', url: 'https://example.invalid', command: 'never-execute' })
    expect(request.post.mock.calls[0]).toEqual(['/system/plugin-delivery/add', allowed, { needTip: false }])
  })
  it('manual close sends only fixed confirmation and encoded task identity', () => {
    reconcileDelivery('a/b', { executorStopped: true, note: '已停止并核查', tenantId: 99, status: 'succeeded' })
    expect(request.post.mock.calls[0]).toEqual([
      '/system/plugin-delivery/a%2Fb/reconcile',
      { executorStopped: true, note: '已停止并核查' },
      { needTip: false },
    ])
  })
})
