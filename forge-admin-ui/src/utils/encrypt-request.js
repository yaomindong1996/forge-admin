import { request } from './http'
import { canParseToJson } from './common'
import { EncryptTool } from './encryptTool'

/**
 * 发送加密POST请求
 * @param {string} url - 请求地址
 * @param {Object} data - 请求数据
 * @param {Object} options - axios配置选项
 * @returns {Promise} 解密后的响应数据
 */
export function postEncrypt(url, data = {}, options = {}) {
  let encryptData = EncryptTool.encrypt("RSA", encodeURIComponent(JSON.stringify(data)))
  return new Promise((resolve, reject) => {
    request.post(url, {
      encryptData,
      ...options
    }).then(res => {
      let decryptData = decodeURIComponent(EncryptTool.decrypt("RSA", res).replace(/\+/g, '%20'))

      if (canParseToJson(decryptData)) {
        resolve(JSON.parse(decryptData))
      } else {
        resolve(decryptData)
      }
    }).catch(err => {
      reject(err)
    })
  })
}

/**
 * 创建加密请求实例
 * @param {Object} options - axios实例配置
 * @returns {Object} 包含加密请求方法的对象
 */
export function createEncryptRequest(options = {}) {
  const service = request // 使用现有的request实例

  return {
    /**
     * 发送加密POST请求
     * @param {string} url - 请求地址
     * @param {Object} data - 请求数据
     * @param {Object} config - axios配置选项
     * @returns {Promise} 解密后的响应数据
     */
    postEncrypt(url, data = {}, config = {}) {
      return postEncrypt(url, data, { ...options, ...config })
    }
  }
}

// 默认导出加密请求实例
export default createEncryptRequest()
