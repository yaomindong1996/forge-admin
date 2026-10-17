import { describe, expect, it, vi } from 'vitest'
import { createDownloadObserver, DOWNLOAD_EVENT, receiveDownloadBlob } from '../download-events'

function streamResponse(headers = {}) {
  const reader = {
    read: vi.fn().mockResolvedValueOnce({ done: false, value: new Uint8Array([1, 2, 3]) }).mockResolvedValueOnce({ done: true }),
    releaseLock: vi.fn(),
  }
  return { response: { headers: new Headers(headers), body: { getReader: () => reader } }, reader }
}

describe('文件接收技术状态', () => {
  it('已知长度按真实字节报告进度，结束后释放流', async () => {
    const { response, reader } = streamResponse({ 'Content-Length': '6' })
    const progress = vi.fn()
    const blob = await receiveDownloadBlob(response, progress)
    expect(blob.size).toBe(3)
    expect(progress).toHaveBeenLastCalledWith({ loaded: 3, total: 6 })
    expect(reader.releaseLock).toHaveBeenCalledTimes(1)
  })
  it.each([{}, { 'Content-Length': '100', 'Content-Encoding': 'gzip' }])(
    '未知/压缩长度 %s 不伪造百分比分母',
    async (headers) => {
      const { response } = streamResponse(headers)
      const progress = vi.fn()
      await receiveDownloadBlob(response, progress)
      expect(progress).toHaveBeenLastCalledWith({ loaded: 3, total: null })
    },
  )
  it('无流接口回退 Blob；网络中断抛错并释放 reader', async () => {
    const blob = new Blob(['test'])
    await expect(receiveDownloadBlob({ blob: () => blob }, vi.fn())).resolves.toBe(blob)
    const { response, reader } = streamResponse()
    reader.read.mockReset().mockRejectedValueOnce(new Error('断流'))
    await expect(receiveDownloadBlob(response, vi.fn())).rejects.toThrow('断流')
    expect(reader.releaseLock).toHaveBeenCalledTimes(1)
  })
  it('会话过期后不再广播；事件不带凭据或错误正文', () => {
    const events = []
    const listener = event => events.push(event.detail)
    window.addEventListener(DOWNLOAD_EVENT, listener)
    let current = true
    const observer = createDownloadObserver({ scope: 'test-scope', isCurrent: () => current, fileId: 'file-1', fileName: '报表.xlsx' })
    observer.progress({ loaded: 30, total: 100 })
    current = false
    observer.failed()
    observer.saved()
    window.removeEventListener(DOWNLOAD_EVENT, listener)
    expect(events).toHaveLength(2)
    expect(events[1]).toMatchObject({ fileId: 'file-1', loaded: 30 })
    expect(JSON.stringify(events)).not.toMatch(/token|Authorization|https?:/)
  })
})
