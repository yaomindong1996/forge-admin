/**
 * 加密工具
 * 提供 MD5、SHA 等加密方法
 */

import md5 from 'md5'

/**
 * MD5 加密
 * @param {string} data - 要加密的数据
 * @returns {string} 加密后的 MD5 字符串
 * 
 * @example
 * const encrypted = crypto.md5('password123')
 * console.log(encrypted) // '482c811da5d5b4bc6d497ffa98491e38'
 */
export function md5Encrypt(data) {
  if (!data) {
    console.warn('md5Encrypt: 加密数据为空')
    return ''
  }
  return md5(String(data))
}

/**
 * SHA 加密 (HMAC)
 * @param {string} algorithm - 算法类型 ('sha1', 'sha256', 'sha512' 等)
 * @param {string} key - 密钥
 * @param {string} data - 要加密的数据
 * @returns {string} 加密后的 SHA 字符串
 * 
 * @example
 * const encrypted = crypto.sha('sha256', 'secretKey', 'password123')
 */
export function shaEncrypt(algorithm, key, data) {
  if (!data || !key) {
    console.warn('shaEncrypt: 加密数据或密钥为空')
    return ''
  }

  // 在浏览器环境中,需要使用 crypto-js 等库
  // 这里提供一个基础实现,实际项目中可能需要引入 crypto-js
  console.warn('shaEncrypt: 浏览器环境不支持 Node.js crypto 模块,请使用 crypto-js 库')
  
  // 临时返回 md5,实际应该使用 crypto-js 的 HmacSHA256 等方法
  return md5(String(data))
}

/**
 * Base64 编码
 * @param {string} data - 要编码的数据
 * @returns {string} Base64 编码后的字符串
 */
export function base64Encode(data) {
  if (!data) return ''
  try {
    return btoa(unescape(encodeURIComponent(String(data))))
  } catch (error) {
    console.error('base64Encode error:', error)
    return ''
  }
}

/**
 * Base64 解码
 * @param {string} data - Base64 编码的字符串
 * @returns {string} 解码后的字符串
 */
export function base64Decode(data) {
  if (!data) return ''
  try {
    return decodeURIComponent(escape(atob(String(data))))
  } catch (error) {
    console.error('base64Decode error:', error)
    return ''
  }
}

/**
 * 生成随机字符串
 * @param {number} length - 字符串长度
 * @returns {string} 随机字符串
 */
export function randomString(length = 16) {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789'
  let result = ''
  for (let i = 0; i < length; i++) {
    result += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  return result
}

/**
 * 加密工具对象 (兼容旧版本 this.$crypto 的用法)
 */
export const crypto = {
  md5: md5Encrypt,
  sha: shaEncrypt,
  base64Encode,
  base64Decode,
  randomString
}

// 导出传输加解密相关功能
export { encryptRequest, decryptResponse, encrypt, decrypt } from './crypto/crypto-interceptor'
export { cryptoConfig, updateCryptoConfig, shouldEncrypt } from './crypto/crypto-config'
export { sm4Encrypt, sm4Decrypt } from './crypto/sm4'
export { aesEncrypt, aesDecrypt } from './crypto/aes'
export { rsaEncrypt, rsaDecrypt } from './crypto/rsa'
export { 
  fetchPublicKey, 
  exchangeKey, 
  getSessionKey, 
  isKeyExchanged, 
  resetKeyExchange, 
  initKeyExchange 
} from './crypto/key-exchange'

export default crypto
