import { request } from '@/utils/request'

export function listRuntimePlugins(params) {
  return request.get('/system/plugin/page', { params, needTip: false })
}

export function getRuntimePlugin(id) {
  return request.get(`/system/plugin/${encodeURIComponent(id)}`, { needTip: false })
}
