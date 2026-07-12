import { request } from '@/utils'

export function getMcpAuthorizationRequest(params) {
  return request.get('/ai/capability/oauth/authorization-request', {
    params,
    pageAudit: false,
    needTip: false,
  })
}

export function decideMcpAuthorization(data) {
  return request.post('/ai/capability/oauth/authorize', data, {
    pageAudit: false,
    needTip: false,
  })
}

export function revokeMcpUserToken(data) {
  return request.post('/ai/capability/oauth/token/revoke', data, {
    pageAudit: false,
    needTip: false,
  })
}
