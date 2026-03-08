import { sm4 } from 'sm-crypto'

/**
 * SM4 加密
 * @param {string} plainText 明文
 * @param {string} key 密钥（16字节hex字符串）
 * @returns {string} 密文（Base64编码）
 */
export function sm4Encrypt(plainText, key) {
  if (!plainText) return plainText
  
  // sm-crypto 的 sm4.encrypt 返回 hex 字符串
  const hexCipherText = sm4.encrypt(plainText, key)
  // 转换为 Base64
  return hexToBase64(hexCipherText)
}

/**
 * SM4 解密
 * @param {string} cipherText 密文（Base64编码）
 * @param {string} key 密钥（16字节hex字符串）
 * @returns {string} 明文
 */
export function sm4Decrypt(cipherText, key) {
  if (!cipherText) return cipherText
  
  // 将 Base64 转换为 hex
  const hexCipherText = base64ToHex(cipherText)
  // sm-crypto 的 sm4.decrypt 接受 hex 字符串
  return sm4.decrypt(hexCipherText, key)
}

/**
 * Hex 转 Base64
 */
function hexToBase64(hexString) {
  const bytes = []
  for (let i = 0; i < hexString.length; i += 2) {
    bytes.push(parseInt(hexString.substr(i, 2), 16))
  }
  return btoa(String.fromCharCode.apply(null, bytes))
}

/**
 * Base64 转 Hex
 */
function base64ToHex(base64String) {
  const raw = atob(base64String)
  let result = ''
  for (let i = 0; i < raw.length; i++) {
    const hex = raw.charCodeAt(i).toString(16)
    result += (hex.length === 2 ? hex : '0' + hex)
  }
  return result
}

/**
 * Base64 解码密钥为 Hex
 * @param {string} base64Key Base64编码的密钥
 * @returns {string} Hex格式密钥
 */
export function decodeKeyToHex(base64Key) {
  return base64ToHex(base64Key)
}
