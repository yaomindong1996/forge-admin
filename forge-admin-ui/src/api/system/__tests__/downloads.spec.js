import { beforeEach, describe, expect, it, vi } from 'vitest'
import { request } from '@/utils/http'
import { getRecentDownloads } from '../downloads'

vi.mock('@/utils/http', () => ({ request: { get: vi.fn() } }))
beforeEach(() => vi.clearAllMocks())
describe('下载记录真实 API 契约', () => {
  it('分页参数符合后端，范围不接受前端用户或租户参数，且不触发全局加载', async () => {
    request.get.mockResolvedValue({ code: 200, data: { records: [], total: 0 } })
    await getRecentDownloads(2)
    expect(request.get).toHaveBeenCalledWith('/system/file/download-log/page', {
      params: { pageNum: 2, pageSize: 20 },
      needTip: false,
      skipGlobalLoading: true,
    })
  })
  it.each([{ code: 500 }, { code: 200, data: {} }, { code: 403 }])('非法响应不能当空记录 %s', async (response) => {
    request.get.mockResolvedValue(response)
    await expect(getRecentDownloads()).rejects.toThrow('下载记录加载失败')
  })
})
