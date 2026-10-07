import { request } from '@/utils/request'

export function getHelloPluginInfo() {
  return request.get('/plugin/hello/info', { needTip: false })
}
