import CryptoJS from 'crypto-js'

/**
 * AES 加密
 * @param {string} plainText 明文
 * @param {string} key 密钥（Base64编码）
 * @returns {string} 密文（Base64编码）
 */
export function aesEncrypt(plainText, key) {
  if (!plainText)
    return plainText

  // 解析 Base64 密钥
  const keyBytes = CryptoJS.enc.Base64.parse(key)

  // 加密
  const encrypted = CryptoJS.AES.encrypt(plainText, keyBytes, {
    mode: CryptoJS.mode.ECB,
    padding: CryptoJS.pad.Pkcs7,
  })

  return encrypted.toString()
}

/**
 * AES 解密
 * @param {string} cipherText 密文（Base64编码）
 * @param {string} key 密钥（Base64编码）
 * @returns {string} 明文
 */
export function aesDecrypt(cipherText, key) {
  if (!cipherText)
    return cipherText

  // 解析 Base64 密钥
  const keyBytes = CryptoJS.enc.Base64.parse(key)

  // 解密
  const decrypted = CryptoJS.AES.decrypt(cipherText, keyBytes, {
    mode: CryptoJS.mode.ECB,
    padding: CryptoJS.pad.Pkcs7,
  })

  return decrypted.toString(CryptoJS.enc.Utf8)
}
