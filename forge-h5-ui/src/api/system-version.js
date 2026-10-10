import { request } from '@/utils'

export function getSystemVersion() {
  return request({ url: '/system/version', method: 'get', needTip: false, timeout: 3000 })
}
