import { request } from '@/utils/request'

const encrypted = { encrypt: true, needTip: false }
const params = value => ({ ...encrypted, params: value })

function id(value) {
  if (!/^[1-9]\d*$/.test(String(value)))
    throw new Error('内容资产标识无效')
  return encodeURIComponent(String(value))
}

export const contentAssetPage = query => request.get('/system/content-assets/page', params(query))
export const contentAssetDetail = value => request.get(`/system/content-assets/${id(value)}`, encrypted)
export const installContentAsset = dto => request.post('/system/content-assets', dto, encrypted)
export const removeContentAsset = value => request.delete(`/system/content-assets/${id(value)}`, encrypted)
