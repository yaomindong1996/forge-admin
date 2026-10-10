import { request } from '@/utils/request'

export function getSystemVersion() {
  return request.get('/system/version', { needTip: false, timeout: 3000 })
}
