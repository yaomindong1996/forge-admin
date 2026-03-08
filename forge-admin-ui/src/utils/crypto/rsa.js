/**
 * RSA 加密工具
 * 使用 jsencrypt-ext 进行 RSA 加密
 */
import JSEncrypt from 'jsencrypt-ext'

/**
 * RSA 加密
 * @param {string} data 要加密的数据
 * @param {string} publicKey RSA公钥（Base64格式）
 * @returns {string} 加密后的数据（Base64格式）
 */
export function rsaEncrypt(data, publicKey) {
  const encrypt = new JSEncrypt()
  encrypt.setPublicKey(publicKey)
  const encrypted = encrypt.encrypt(data)
  if (!encrypted) {
    throw new Error('RSA 加密失败')
  }
  return encrypted
}

/**
 * RSA 解密（通常只在后端使用）
 * @param {string} data 加密的数据（Base64格式）
 * @param {string} privateKey RSA私钥（Base64格式）
 * @returns {string} 解密后的数据
 */
export function rsaDecrypt(data, privateKey) {
  const decrypt = new JSEncrypt()
  decrypt.setPrivateKey(privateKey)
  const decrypted = decrypt.decrypt(data)
  if (!decrypted) {
    throw new Error('RSA 解密失败')
  }
  return decrypted
}
