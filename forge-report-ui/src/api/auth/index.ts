import { get, post } from '@/api/http'
import { loadRuntimeCryptoConfig } from '@/utils/api-crypto/crypto-config'
import { encryptPassword } from '@/utils/rsa'

export interface LoginRequest {
  username: string
  password: string
  authType?: string
  userClient?: string
  appId?: string
  /** 多工作区账号二次登录时必传 */
  tenantId?: number | string
}

export interface LoginTenantOption {
  tenantId: number | string
  tenantName?: string
  systemName?: string
  browserTitle?: string
}

export interface LoginResult {
  accessToken: string
  tokenType: string
  expiresIn: number
}

export interface LoginResponse {
  code: number
  msg: string
  data: LoginResult
}

export interface SsoExchangeRequest {
  ticket: string
}

export interface LoginConfigResponse {
  code: number
  msg?: string
  message?: string
  data?: {
    enablePasswordEncryption?: boolean
  }
}

export const getLoginConfigApi = (userClient = 'forge_report'): Promise<LoginConfigResponse> => {
  return get('/forge-report-api/auth/loginConfig', { userClient }) as unknown as Promise<LoginConfigResponse>
}

/**
 * 用户登录。密码 RSA 策略由服务端登录配置决定。
 */
export const loginApi = async (data: LoginRequest): Promise<LoginResponse> => {
  await loadRuntimeCryptoConfig()
  const userClient = data.userClient || 'forge_report'
  const loginConfig = await getLoginConfigApi(userClient)
  const passwordEncryptionEnabled = loginConfig?.data?.enablePasswordEncryption !== false
  const submittedPassword = await encryptPassword(data.password, passwordEncryptionEnabled)

  return post('/forge-report-api/auth/login', {
    username: data.username,
    password: submittedPassword,
    authType: data.authType || 'password',
    userClient,
    appId: data.appId || 'forge_report',
    ...(data.tenantId != null && data.tenantId !== '' ? { tenantId: data.tenantId } : {}),
  }) as unknown as Promise<LoginResponse>
}

/**
 * 用户登出
 */
export const logoutApi = (): Promise<any> => {
  return post('/forge-report-api/auth/logout') as Promise<any>
}

/**
 * SSO 票据交换。必须先跟随后端 /crypto/config，避免加密开关不一致导致 ticket 读空。
 */
export const ssoExchangeApi = async (data: SsoExchangeRequest): Promise<LoginResponse> => {
  await loadRuntimeCryptoConfig()
  return post('/forge-report-api/auth/sso/exchange', data) as unknown as Promise<LoginResponse>
}

/**
 * 获取当前用户信息
 */
export const getUserInfoApi = () => {
  return get('/forge-report-api/auth/userInfo') as unknown as Promise<{ code: number; data: any; msg: string }>
}

/**
 * 获取当前用户菜单树
 */
export const getUserMenuApi = () => {
  return get('/forge-report-api/auth/current/menu') as unknown as Promise<{ code: number; data: any; msg: string }>
}

/**
 * 获取当前用户权限标识列表
 */
export const getUserPermissionsApi = () => {
  return get('/forge-report-api/auth/current/permissions') as unknown as Promise<{ code: number; data: string[]; msg: string }>
}
