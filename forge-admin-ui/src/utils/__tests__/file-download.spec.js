import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { request } from '@/utils/http'
import { DOWNLOAD_EVENT } from '../download-events'
import { downloadFile } from '../file'

const identity = vi.hoisted(() => ({ auth: { accessToken: 'test-token' }, user: { userId: '1', tenantId: '1' } }))
vi.mock('@/store', () => ({ useAuthStore: () => identity.auth, useUserStore: () => identity.user }))
vi.mock('@/utils/http', () => ({ request: Object.assign(vi.fn(), { post: vi.fn() }) }))
vi.mock('@/composables/useGlobalLoading', () => ({ managedFetch: vi.fn() }))

describe('真实文件下载接入最近下载状态', () => {
  let events
  let listener
  beforeEach(() => {
    vi.useFakeTimers()
    vi.clearAllMocks()
    localStorage.clear()
    events = []
    listener = event => events.push(event.detail)
    window.addEventListener(DOWNLOAD_EVENT, listener)
    identity.auth.accessToken = 'test-token'
    const NativeURL = URL
    vi.stubGlobal('URL', class extends NativeURL {
      static createObjectURL = vi.fn(() => 'blob:test-download')
      static revokeObjectURL = vi.fn()
    })
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})
    vi.stubGlobal('fetch', vi.fn())
  })
  afterEach(() => {
    window.removeEventListener(DOWNLOAD_EVENT, listener)
    vi.runOnlyPendingTimers()
    vi.useRealTimers()
    vi.restoreAllMocks()
    vi.unstubAllGlobals()
  })

  it('复用鉴权头和下载意图，交给浏览器后发出成功事件', async () => {
    fetch.mockResolvedValue({ ok: true, headers: new Headers(), blob: async () => new Blob(['test']) })
    await downloadFile('test-file-success', '报表.xlsx')
    expect(fetch).toHaveBeenCalledWith(expect.stringContaining('/api/file/download/test-file-success'), {
      headers: expect.objectContaining({ 'Authorization': 'Bearer test-token', 'X-Forge-Download-Intent': '1' }),
    })
    expect(events.map(event => event.phase)).toEqual(['receiving', 'saved'])
    expect(URL.createObjectURL).toHaveBeenCalledTimes(1)
    vi.runOnlyPendingTimers()
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:test-download')
  })

  it('内部下载失败时可走既有 COS 直链兜底并补记记录', async () => {
    fetch.mockResolvedValue({ ok: false, status: 503, text: async () => 'network' })
    request.mockResolvedValue({ code: 200, data: 'https://files.example.test/download' })
    request.post.mockResolvedValue({ code: 200 })
    await downloadFile('test-file-fallback', '合同.pdf')
    expect(request.post).toHaveBeenCalledWith('/system/file/download-log', {
      fileId: 'test-file-fallback',
      fileName: '合同.pdf',
    }, { needTip: false, skipGlobalLoading: true })
    expect(events.at(-1).phase).toBe('saved')
    expect(JSON.stringify(events)).not.toContain('https://files')
  })

  it('直链补记的慢请求不阻塞浏览器保存与下载状态结束', async () => {
    fetch.mockResolvedValue({ ok: false, status: 503, text: async () => 'unavailable' })
    request.mockResolvedValue({ code: 200, data: 'https://files.example.test/slow-log' })
    request.post.mockReturnValue(new Promise(() => {}))
    await expect(downloadFile('test-slow-report', '报表.xlsx')).resolves.toContain('files.example.test')
    expect(events.at(-1).phase).toBe('saved')
  })

  it('失败保留原有异常给调用方，面板事件只发通用失败态', async () => {
    fetch.mockRejectedValue(new Error('network-address-secret'))
    request.mockResolvedValue({ code: 200, data: '/api/file/download/test-file-failed' })
    await expect(downloadFile('test-file-failed', '失败文件.pdf')).rejects.toThrow('network-address-secret')
    expect(events.at(-1).phase).toBe('failed')
    expect(JSON.stringify(events)).not.toContain('secret')
  })

  it.each([false, true])('鉴权拒绝/JSON 异常（%s）不保存错误文件，不使用旧签名地址绕过', async (json) => {
    fetch.mockResolvedValue({ ok: json, status: json ? 200 : 403, headers: new Headers(json ? { 'Content-Type': 'application/json' } : {}), text: async () => '无下载权限' })
    await expect(downloadFile(`test-denied-${json}`, '受限文件.pdf')).rejects.toThrow()
    expect(URL.createObjectURL).not.toHaveBeenCalled()
    expect(request).not.toHaveBeenCalled()
    expect(events.at(-1).phase).toBe('failed')
  })

  it('会话更换后，旧文件结束不再进入新会话面板', async () => {
    let resolveBlob
    fetch.mockResolvedValue({ ok: true, headers: new Headers(), blob: () => new Promise((resolve) => {
      resolveBlob = resolve
    }) })
    const pending = downloadFile('test-file-old-session', '旧会话文件.txt')
    await Promise.resolve()
    identity.auth.accessToken = 'new-test-token'
    resolveBlob(new Blob(['test']))
    await expect(pending).rejects.toThrow('登录身份已改变')
    expect(events.map(event => event.phase)).toEqual(['receiving'])
    expect(URL.createObjectURL).not.toHaveBeenCalled()
  })
})
