import { request } from '@/utils/request'

/** 只查询当前后端的启动快照，无文件路径、身份参数或写接口。 */
export function getRuntimeLicenseStatus() {
  return request.get('/system/plugin/runtime-license/status', { needTip: false })
}
