import { post } from '@/api/http'
import { encryptPassword } from '@/utils/rsa'

export interface LoginRequest {
  username: string
  password: string
  authType?: string
  userClient?: string
  appId?: string
  encrypted?: boolean
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

/**
 * 用户登录（密码使用 RSA 加密）
 */
export const loginApi = async (data: LoginRequest): Promise<LoginResponse> => {
  // RSA 加密密码
  const encryptedPwd = await encryptPassword(data.password)

  return post('/goview-api/auth/login', {
    username: data.username,
    password: encryptedPwd,
    authType: data.authType || 'password',
    userClient: data.userClient || 'forge_report',
    appId: data.appId || 'forge_report',
    appSecret: 'forage_pc123',
    encrypted: true,
  }) as unknown as Promise<LoginResponse>
}

/**
 * 用户登出
 */
export const logoutApi = (): Promise<any> => {
  return post('/goview-api/auth/logout') as Promise<any>
}
