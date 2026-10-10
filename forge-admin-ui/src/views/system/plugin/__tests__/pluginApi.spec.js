import { describe, expect, it, vi } from 'vitest'
import { getRuntimePlugin, listRuntimePlugins } from '@/api/system/plugin'

const get = vi.hoisted(() => vi.fn())
vi.mock('@/utils/request', () => ({ request: { get } }))

describe('插件中心 API 协议', () => {
  it('查询通过统一客户端发送固定分页参数，不发写请求', async () => {
    const params = { pageNum: 2, pageSize: 15, keyword: '系统', origin: 'builtin' }
    get.mockResolvedValueOnce({ code: 200, data: { records: [] } })
    await listRuntimePlugins(params)
    expect(get).toHaveBeenLastCalledWith('/system/plugin/page', { params, needTip: false })
  })

  it('详情标识编码后仍留在单一路由段，避免越界查询路径', async () => {
    await getRuntimePlugin('../hello?x=1')
    expect(get).toHaveBeenLastCalledWith('/system/plugin/..%2Fhello%3Fx%3D1', { needTip: false })
  })
})
