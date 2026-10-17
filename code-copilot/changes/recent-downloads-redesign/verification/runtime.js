import { reactive } from 'vue'

// 仅隔离验证使用的合成身份。正式构建不引用本文件，也不绕过生产鉴权。
const auth = reactive({ accessToken: 'local-fixture-only' })
const user = reactive({ userId: 'fixture-user', tenantId: 'fixture-tenant', isAdmin: false })
export const useAuthStore = () => auth
export const useUserStore = () => user
export const isNullOrUndef = value => value === null || value === undefined

async function send(url, options = {}) {
  const params = new URLSearchParams(options.params || {})
  const response = await fetch(params.size ? `${url}?${params}` : url, {
    method: options.method || 'GET',
  })
  if (!response.ok)
    throw new Error('隔离夹具错误')
  return response.json()
}
export const request = Object.assign(
  options => send(options.url, options),
  { get: send, post: url => send(url, { method: 'POST' }) },
)
export async function setFixtureMode(mode) {
  await fetch(`/__fixture/mode?value=${mode}`)
}
