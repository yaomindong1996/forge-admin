/**
 * RSA 加密工具
 * 用于登录时对密码进行 RSA 加密
 */
import JSEncrypt from 'jsencrypt-ext'
import axiosInstance from '@/api/axios'

// 缓存公钥
let cachedPublicKey: string | null = null

/**
 * 将裸 Base64 公钥包装为 PEM 格式（jsencrypt 需要 PEM）
 */
function wrapPublicKeyPem(base64Key: string): string {
  // 如果已经是 PEM 格式，直接返回
  if (base64Key.includes('-----BEGIN')) return base64Key
  // 每 64 字符换行
  const lines = base64Key.match(/.{1,64}/g)?.join('\n') || base64Key
  return `-----BEGIN PUBLIC KEY-----\n${lines}\n-----END PUBLIC KEY-----`
}

/**
 * 从后端获取 RSA 公钥
 */
export async function fetchPublicKey(forceRefresh = false): Promise<string> {
  if (cachedPublicKey && !forceRefresh) {
    return cachedPublicKey
  }
  try {
    const res: any = await axiosInstance.get('/goview-api/crypto/public-key')
    const rawKey = res?.data?.publicKey || res?.publicKey
    if (!rawKey) throw new Error('获取公钥失败')
    // 包装为 PEM 格式
    cachedPublicKey = wrapPublicKeyPem(rawKey)
    return cachedPublicKey
  } catch (error) {
    console.error('[RSA] 获取公钥失败:', error)
    throw error
  }
}

/**
 * RSA 加密（jsencrypt 返回 Base64 密文）
 */
export function rsaEncrypt(data: string, publicKey: string): string {
  const encrypt = new JSEncrypt()
  encrypt.setPublicKey(publicKey)
  const result = encrypt.encrypt(data)
  if (!result) throw new Error('RSA 加密失败')
  return result as string
}

/**
 * 加密密码（先获取公钥再加密，返回 Base64 密文）
 */
export async function encryptPassword(password: string): Promise<string> {
  try {
    const publicKey = await fetchPublicKey()
    return rsaEncrypt(password, publicKey)
  } catch (error) {
    console.warn('[RSA] 密码加密失败，使用明文（降级方案）:', error)
    return password
  }
}

/**
 * 清除缓存的公钥（登出时调用）
 */
export function clearPublicKeyCache(): void {
  cachedPublicKey = null
}