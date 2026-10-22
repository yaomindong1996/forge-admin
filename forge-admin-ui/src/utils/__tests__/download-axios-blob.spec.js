import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { downloadAxiosBlobResponse } from '../file'

describe('downloadAxiosBlobResponse', () => {
  let scheduled

  beforeEach(() => {
    scheduled = []
    const NativeURL = URL
    vi.stubGlobal('URL', class extends NativeURL {
      static createObjectURL = vi.fn(() => 'blob:axios-download')
      static revokeObjectURL = vi.fn()
    })
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})
    vi.spyOn(window, 'setTimeout').mockImplementation((fn, ms) => {
      scheduled.push({ fn, ms })
      return scheduled.length
    })
  })

  afterEach(() => {
    vi.restoreAllMocks()
    vi.unstubAllGlobals()
    document.body.innerHTML = ''
  })

  it('解析 Content-Disposition 文件名并延迟 revoke，避免立刻回收导致下载失败', async () => {
    const blob = new Blob(['{"app":1}'], { type: 'application/json' })
    const fileName = await downloadAxiosBlobResponse({
      data: blob,
      headers: {
        'content-disposition': "attachment; filename*=UTF-8''demo.forge-app.json",
      },
    }, 'fallback.json', { revokeDelayMs: 5000 })

    expect(fileName).toBe('demo.forge-app.json')
    expect(URL.createObjectURL).toHaveBeenCalledWith(blob)
    expect(HTMLAnchorElement.prototype.click).toHaveBeenCalledTimes(1)
    expect(URL.revokeObjectURL).not.toHaveBeenCalled()
    expect(scheduled).toHaveLength(1)
    expect(scheduled[0].ms).toBe(5000)

    scheduled[0].fn()
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:axios-download')
  })

  it('业务错误 JSON blob 不触发下载', async () => {
    const blob = new Blob([JSON.stringify({ code: 500, message: '导出超时' })], {
      type: 'application/json',
    })
    await expect(downloadAxiosBlobResponse({ data: blob, headers: {} }, 'x.json'))
      .rejects
      .toThrow('导出超时')
    expect(URL.createObjectURL).not.toHaveBeenCalled()
    expect(scheduled).toHaveLength(0)
  })

  it('无 Content-Disposition 时使用 fallback 文件名', async () => {
    const blob = new Blob(['ok'], { type: 'application/octet-stream' })
    const fileName = await downloadAxiosBlobResponse(
      { data: blob, headers: {} },
      'application.forge-app.json',
      { revokeDelayMs: 1000 },
    )
    expect(fileName).toBe('application.forge-app.json')
    const anchor = document.body.querySelector('a[download]')
    expect(anchor?.getAttribute('download')).toBe('application.forge-app.json')
    expect(scheduled[0].ms).toBe(1000)
  })
})
