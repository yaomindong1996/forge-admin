import { beforeEach, expect, it, vi } from 'vitest'
import * as api from '@/api/print'
import { request } from '@/utils/request'

vi.mock('@/utils/request', () => ({ request: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() } }))
vi.mock('@/utils/file', () => ({ getFileUrl: id => `/api/file/download/${id}` }))
beforeEach(() => vi.clearAllMocks())
it('管理与运行请求启用加密并保持长整型标识，不发送客户端版本号', () => {
  api.updatePrintTemplate('9007199254740993', { expectedRevision: 3, schemaJson: '{}' })
  expect(request.put).toHaveBeenCalledWith('/print/templates/9007199254740993', expect.any(Object), expect.objectContaining({ encrypt: true }))
  const record = { source: {}, recordId: 'synthetic' }
  api.preparePrint(record, '7')
  expect(request.post).toHaveBeenCalledWith('/print/prepare', { record, templateId: '7' }, expect.objectContaining({ encrypt: true }))
  api.printTemplates({ applicationId: '2', pageNum: 1, pageSize: 20 })
  expect(request.get).toHaveBeenCalledWith('/print/templates/page', expect.objectContaining({ params: { applicationId: '2', pageNum: 1, pageSize: 20 }, encrypt: true }))
  api.deletePrintTemplate('7', 4)
  expect(request.delete).toHaveBeenCalledWith('/print/templates/7', expect.objectContaining({ params: { expectedRevision: 4 }, encrypt: true }))
})
it('文件请求使用鉴权 HTTP 客户端及可取消二进制下载', () => {
  const controller = new AbortController()
  api.loadPrintFile('synthetic_1', { signal: controller.signal })
  expect(request.get).toHaveBeenCalledWith('/api/file/download/synthetic_1', expect.objectContaining({ baseURL: '', responseType: 'blob', signal: controller.signal }))
  expect(() => api.loadPrintFile('https://external.invalid')).toThrow()
  expect(() => api.printTemplate('../7')).toThrow()
})
