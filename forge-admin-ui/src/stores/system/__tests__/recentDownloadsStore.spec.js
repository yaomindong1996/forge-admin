import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { reactive } from 'vue'
import { getRecentDownloads } from '@/api/system/downloads'
import { DOWNLOAD_EVENT } from '@/utils/download-events'
import { downloadFile } from '@/utils/file'
import { useRecentDownloadsStore } from '../recentDownloadsStore'
import { downloadScope } from '../recentDownloadsUtils'

const identity = vi.hoisted(() => ({ auth: null, user: null }))
vi.mock('@/store/modules/auth', () => ({ useAuthStore: () => identity.auth }))
vi.mock('@/store/modules/user', () => ({ useUserStore: () => identity.user }))
vi.mock('@/api/system/downloads', () => ({ getRecentDownloads: vi.fn() }))
vi.mock('@/utils/file', () => ({ downloadFile: vi.fn() }))

const history = id => ({ id, fileId: `file-${id}`, fileName: '测试报表.xlsx', createTime: '2026-10-17T09:00:00' })
function deferred() {
  let resolve
  const promise = new Promise((done) => {
    resolve = done
  })
  return { promise, resolve }
}
function event(phase, rest = {}) {
  window.dispatchEvent(new CustomEvent(DOWNLOAD_EVENT, {
    detail: { id: 'task-1', fileId: 'file-1', fileName: '测试文件.pdf', startedAt: Date.now(), scope: downloadScope(identity.user.tenantId, identity.user.userId), phase, ...rest },
  }))
}

describe('真实最近下载状态', () => {
  let store
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    localStorage.clear()
    identity.auth = reactive({ accessToken: 'test-session' })
    identity.user = reactive({ userId: '9007199254740993', tenantId: '1', userType: 0 })
    getRecentDownloads.mockResolvedValue({ records: [], total: 0 })
    store = useRecentDownloadsStore()
  })
  afterEach(() => {
    store.$dispose()
  })

  it('不在创建时请求，并发刷新合并且只读取服务端分页', async () => {
    const pending = deferred()
    getRecentDownloads.mockReturnValueOnce(pending.promise)
    expect(getRecentDownloads).not.toHaveBeenCalled()
    const request = store.refresh()
    store.refresh()
    expect(getRecentDownloads).toHaveBeenCalledTimes(1)
    expect(getRecentDownloads).toHaveBeenCalledWith(1)
    pending.resolve({ records: [history('9007199254740995')], total: 21 })
    await request
    expect(store.groups.flatMap(group => group.rows)[0].id).toBe('9007199254740995')
    expect(store.hasMore).toBe(true)
  })

  it('加载更多保留原记录，失败不会伪造成空列表', async () => {
    getRecentDownloads.mockResolvedValueOnce({ records: [history('1')], total: 3 })
      .mockResolvedValueOnce({ records: [history('2')], total: 3 })
      .mockRejectedValueOnce(new Error('internal-url-secret'))
    await store.refresh()
    await store.refresh(true)
    expect(getRecentDownloads).toHaveBeenLastCalledWith(2)
    await store.refresh(true)
    expect(store.groups.flatMap(group => group.rows)).toHaveLength(2)
    expect(store.error).toContain('加载失败')
    expect(store.error).not.toContain('secret')
  })

  it('账号切换拒绝旧响应，清空旧记录、任务和面板', async () => {
    const pending = deferred()
    getRecentDownloads.mockReturnValueOnce(pending.promise)
    const request = store.refresh()
    event('receiving')
    identity.user.userId = '2'
    pending.resolve({ records: [history('old')], total: 1 })
    await request
    expect(store.open).toBe(false)
    expect(store.tasks).toEqual([])
    expect(store.groups).toEqual([])
  })

  it.each(['tenant', 'token'])('切换 %s 时清空当前可见数据，旧查询不能覆盖', async (kind) => {
    const pending = deferred()
    getRecentDownloads.mockReturnValueOnce(pending.promise)
    const request = store.refresh()
    if (kind === 'tenant')
      identity.user.tenantId = '2'
    else
      identity.auth.accessToken = 'new-test-session'
    getRecentDownloads.mockResolvedValueOnce({ records: [history('new')], total: 1 })
    await store.refresh()
    pending.resolve({ records: [history('old')], total: 1 })
    await request
    expect(store.groups.flatMap(group => group.rows).map(row => row.id)).toEqual(['new'])
  })

  it('本地清理只隐藏已加载 ID，保留实际下载任务且不删除后台', async () => {
    getRecentDownloads.mockResolvedValue({ records: [history('1')], total: 1 })
    await store.refresh()
    event('receiving', { loaded: 20, total: 100 })
    store.clear()
    await store.refresh()
    expect(store.groups.flatMap(group => group.rows)).toHaveLength(1)
    expect(store.activeCount).toBe(1)
    const persisted = Object.values(localStorage).join('')
    expect(persisted).toContain('"1"')
    expect(persisted).not.toContain('测试报表')
    identity.user.userId = 'other'
    await store.refresh()
    expect(store.groups.flatMap(group => group.rows)).toHaveLength(1)
  })

  it('拒绝其他账号事件，真实任务失败后提供重试，不让 Promise 泄漏', async () => {
    event('receiving', { scope: 'other-account' })
    expect(store.activeCount).toBe(0)
    event('receiving')
    event('failed')
    expect(store.activeCount).toBe(0)
    expect(store.tasks[0].phase).toBe('failed')
    downloadFile.mockRejectedValueOnce(new Error('网络错误'))
    await expect(store.download(store.tasks[0])).resolves.toBeUndefined()
    expect(downloadFile).toHaveBeenCalledWith('file-1', '测试文件.pdf')
  })

  it('重复点击同一文件和不能重放的导出不调用下载', async () => {
    event('receiving')
    await store.download({ fileId: 'file-1' })
    await store.download({ fileName: '无文件ID导出.xlsx' })
    expect(downloadFile).not.toHaveBeenCalled()
  })

  it('清理存储失败不阻塞下载，结束事件清理即时任务', () => {
    event('receiving')
    event('saved')
    expect(store.tasks).toEqual([])
    const storage = vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('quota')
    })
    expect(() => store.clear()).not.toThrow()
    storage.mockRestore()
  })

  it('最近记录与本地隐藏均有上限，避免长期使用后无界堆积', async () => {
    getRecentDownloads.mockResolvedValue({ records: Array.from({ length: 501 }, (_, index) => history(String(index))), total: 1000 })
    await store.refresh()
    expect(store.groups.flatMap(group => group.rows)).toHaveLength(500)
    expect(store.hasMore).toBe(false)
    expect(store.limitReached).toBe(true)
    store.clear()
    expect(store.groups).toEqual([])
  })
})
