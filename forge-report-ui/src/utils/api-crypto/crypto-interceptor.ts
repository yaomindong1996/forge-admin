import type { AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { cryptoConfig, shouldEncrypt } from './crypto-config'
import { decodeKeyToHex, sm4Decrypt, sm4Encrypt } from './sm4'

type EncryptedPayload = {
  data?: string
  algorithm?: string
}

function isKeyValid(): boolean {
  return Boolean(cryptoConfig.secretKey)
}

function encrypt(data: string, algorithm = cryptoConfig.algorithm): string {
  if (!cryptoConfig.secretKey) throw new Error('加密密钥未设置')
  if (algorithm === 'SM4') {
    return sm4Encrypt(data, decodeKeyToHex(cryptoConfig.secretKey))
  }
  throw new Error(`不支持的加密算法: ${algorithm}`)
}

function decrypt(data: string, algorithm = cryptoConfig.algorithm): string {
  if (!cryptoConfig.secretKey) throw new Error('解密密钥未设置')
  if (algorithm === 'SM4') {
    return sm4Decrypt(data, decodeKeyToHex(cryptoConfig.secretKey))
  }
  throw new Error(`不支持的加密算法: ${algorithm}`)
}

export function isEncryptedPayload(payload: any): payload is Required<EncryptedPayload> {
  return Boolean(payload && typeof payload.data === 'string' && typeof payload.algorithm === 'string')
}

export function decryptPayload<T = any>(payload: Required<EncryptedPayload>): T {
  if (!isKeyValid()) {
    throw new Error('解密密钥未设置')
  }

  try {
    const decryptedData = decrypt(payload.data, payload.algorithm)
    return JSON.parse(decryptedData) as T
  } catch (error: any) {
    const decryptError = new Error('DECRYPT_ERROR') as Error & { originalError?: unknown }
    decryptError.originalError = error
    throw decryptError
  }
}

export function encryptRequest(config: InternalAxiosRequestConfig): InternalAxiosRequestConfig {
  const customConfig = config as InternalAxiosRequestConfig & { encrypt?: boolean }
  config.headers = config.headers || {}

  if (customConfig.encrypt === false || !shouldEncrypt(config.url || '')) {
    return config
  }

  if (!isKeyValid()) {
    console.warn('[Crypto] 加密密钥未设置，跳过请求加密')
    return config
  }

  if (config.data && typeof config.data === 'object') {
    const encryptedData = encrypt(JSON.stringify(config.data))
    config.data = {
      data: encryptedData,
      algorithm: cryptoConfig.algorithm
    }
  }

  return config
}

export function decryptResponse(response: AxiosResponse): AxiosResponse {
  const payload = response.data as EncryptedPayload
  if (!isEncryptedPayload(payload)) {
    return response
  }

  response.data = decryptPayload(payload)
  return response
}
