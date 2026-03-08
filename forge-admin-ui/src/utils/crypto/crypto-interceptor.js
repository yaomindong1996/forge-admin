import { cryptoConfig, shouldEncrypt } from './crypto-config'
import { sm4Encrypt, sm4Decrypt, decodeKeyToHex } from './sm4'
import { aesEncrypt, aesDecrypt } from './aes'

/**
 * 检查密钥是否有效
 * @returns {boolean}
 */
function isKeyValid() {
  const key = cryptoConfig.secretKey
  // 密钥必须存在且长度大于0
  return key && key.length > 0
}

/**
 * 加密数据
 * @param {string} data 数据
 * @param {string} algorithm 算法
 * @returns {string} 加密后的数据
 */
function encrypt(data, algorithm = cryptoConfig.algorithm) {
  const key = cryptoConfig.secretKey
  if (!key) {
    throw new Error('加密密钥未设置')
  }
  
  if (algorithm === 'SM4') {
    const hexKey = decodeKeyToHex(key)
    return sm4Encrypt(data, hexKey)
  } else if (algorithm === 'AES') {
    return aesEncrypt(data, key)
  }
  
  throw new Error(`不支持的加密算法: ${algorithm}`)
}

/**
 * 解密数据
 * @param {string} data 加密数据
 * @param {string} algorithm 算法
 * @returns {string} 解密后的数据
 */
function decrypt(data, algorithm = cryptoConfig.algorithm) {
  const key = cryptoConfig.secretKey
  if (!key) {
    throw new Error('解密密钥未设置')
  }
  
  if (algorithm === 'SM4') {
    const hexKey = decodeKeyToHex(key)
    return sm4Decrypt(data, hexKey)
  } else if (algorithm === 'AES') {
    return aesDecrypt(data, key)
  }
  
  throw new Error(`不支持的加密算法: ${algorithm}`)
}

/**
 * 请求加密拦截器
 * 注意：防重放参数已在 interceptors.js 中处理，此函数仅负责请求体加密
 * @param {object} config axios 请求配置
 * @returns {object} 处理后的配置
 */
export function encryptRequest(config) {
  config.headers = config.headers || {}
  
  // 检查是否需要加密
  if (config.encrypt === false || !shouldEncrypt(config.url)) {
    return config
  }
  
  // 检查密钥是否有效
  if (!isKeyValid()) {
    console.warn('加密密钥未设置，跳过请求加密')
    return config
  }
  
  // 加密请求体
  if (config.data && typeof config.data === 'object') {
    try {
      const jsonData = JSON.stringify(config.data)
      const encryptedData = encrypt(jsonData)
      config.data = {
        data: encryptedData,
        algorithm: cryptoConfig.algorithm
      }
    } catch (e) {
      console.error('请求加密失败:', e)
    }
  }
  
  return config
}

/**
 * 响应解密拦截器
 * @param {object} response axios 响应
 * @returns {object} 处理后的响应
 */
export function decryptResponse(response) {
  // 检查是否是加密响应
  if (response.data && response.data.data && response.data.algorithm) {
    // 检查密钥是否有效
    if (!isKeyValid()) {
      console.warn('解密密钥未设置，跳过响应解密')
      return response
    }
    
    try {
      const decryptedData = decrypt(response.data.data, response.data.algorithm)
      response.data = JSON.parse(decryptedData)
    } catch (e) {
      console.error('响应解密失败:', e)
      
      // 如果是填充错误，通常是密钥过期或不正确
      if (e.message?.includes('padding') || e.message?.includes('key')) {
        // 这里抛出一个特定错误，由 interceptors.js 捕获处理
        const decryptError = new Error('DECRYPT_ERROR')
        decryptError.originalError = e
        throw decryptError
      }
      
      return response
    }
  }
  
  return response
}

export { encrypt, decrypt }
