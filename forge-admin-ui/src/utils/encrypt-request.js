import { request } from './http'

/**
 * 发送加密POST请求（兼容新的会话密钥加密体系）
 * @param {string} url - 请求地址
 * @param {object} data - 请求数据
 * @param {object} options - axios配置选项
 * @returns {Promise} 解密后的响应数据
 */
export function postEncrypt(url, data = {}, options = {}) {
  // 标记请求需要加密，由拦截器统一处理加解密
  return request.post(url, data, {
    ...options,
    encrypt: true,
  })
}

/**
 * 创建加密请求实例
 * @param {object} options - axios实例配置
 * @returns {object} 包含加密请求方法的对象
 */
export function createEncryptRequest(options = {}) {
  const service = request // 使用现有的request实例

  return {
    /**
     * 发送加密POST请求
     * @param {string} url - 请求地址
     * @param {object} data - 请求数据
     * @param {object} config - axios配置选项
     * @returns {Promise} 解密后的响应数据
     */
    postEncrypt(url, data = {}, config = {}) {
      return postEncrypt(url, data, { ...options, ...config })
    },
  }
}

// 默认导出加密请求实例
export default createEncryptRequest()
