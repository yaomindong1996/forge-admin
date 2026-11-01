import { request } from '@/utils'
import { parseOptionApi } from '../aiFormItemUtils'

export async function submitQuickCreateApi(api, payload) {
  const { method, url } = parseOptionApi(api)
  const res = await request({
    method,
    url,
    params: method === 'get' ? payload : undefined,
    data: method === 'get' ? undefined : payload,
  })
  if (res?.code !== undefined && res.code !== 200)
    throw new Error(res.msg || res.message || '保存失败')
  return res
}
