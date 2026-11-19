import { request } from '@/utils/request'

export function listTenantPackageOptions() {
  return request.get('/system/tenant-package/options')
}

export function getTenantPackageDetail(id) {
  return request.get(`/system/tenant-package/${id}`)
}

export function pageTenantPackages(params) {
  return request.get('/system/tenant-package/page', { params })
}
