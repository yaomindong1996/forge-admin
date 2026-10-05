import { request } from '@/utils/request'

export function listTenantUsers(tenantId, params) {
  return request.get(`/system/tenant/${tenantId}/users`, { params })
}

export function removeTenantUser(tenantId, userId) {
  return request.post(`/system/tenant/${tenantId}/users/${userId}/remove`)
}

export function listTenantBusinessDatasources() {
  return request.get('/generator/datasource/enabled', { params: { usageScope: 'TENANT_BUSINESS' } })
}

export function removeTenant(id) {
  return request.post('/system/tenant/remove', null, { params: { id } })
}
