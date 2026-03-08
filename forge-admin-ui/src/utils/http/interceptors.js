import { useAuthStore } from '@/store'
import { resolveResError } from './helpers'
import { encryptRequest, decryptResponse, cryptoConfig } from '@/utils/crypto'
import { resetKeyExchange } from '@/utils/crypto/key-exchange'

// 生成 UUID
function generateUUID() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
    const r = Math.random() * 16 | 0
    const v = c === 'x' ? r : (r & 0x3 | 0x8)
    return v.toString(16)
  })
}

export function setupInterceptors(axiosInstance) {
  const SUCCESS_CODES = [0, 200]

  /**
   * 响应成功拦截器
   */
  function resResolve(response) {
    // 先进行解密处理
    try {
      response = decryptResponse(response)
      if(response.data.code !== 200){
        const finalMessage = resolveResError(response.data.code, response.data.message, response.config.needTip)
        return Promise.reject({ code:response.data.code, message: finalMessage, error: response.data.message })
      }
    } catch (error) {
      if (error.message === 'DECRYPT_ERROR') {
        console.error('[Crypto] 检测到密钥已过期，正在重置...')
        // 清除过期密钥
        resetKeyExchange()

        // 提示用户
        const message = '安全会话已过期，请重新操作'
        window.$message?.error(message)

        // 抛出异常防止后续逻辑执行
        return Promise.reject({ code: 401, message, error, skipErrorHandler: true })
      }
      return Promise.reject({ code: 500, message: '解密数据失败', error, skipErrorHandler: true })
    }

    const { data, status, config, statusText, headers } = response

    // 处理JSON响应
    if (headers['content-type']?.includes('json')) {
      // 成功响应 (code === 200 或在 SUCCESS_CODES 中)
      if (data && (data.code === 200 || SUCCESS_CODES.includes(data?.code))) {
        return Promise.resolve(data)
      }

      // 业务错误响应 - 交给 resReject 统一处理
      const code = data?.code ?? status
      const message = data?.message ?? statusText
      const needTip = config?.needTip !== false

      // 标记为业务错误，传递给 resReject
      return Promise.reject({
        code,
        message,
        error: data ?? response,
        isBusinessError: true,
        needTip
      })
    }

    // 非JSON响应直接返回
    return Promise.resolve(data ?? response)
  }

  /**
   * 响应失败拦截器 - 统一错误处理入口
   */
  async function resReject(error) {
    // 1. 处理已经在 resResolve 中标记了 skipErrorHandler 的错误（解密错误等）
    if (error?.skipErrorHandler) {
      return Promise.reject(error)
    }

    // 2. 处理网络错误（没有 response）
    if (!error || !error.response) {
      // 如果是业务错误（从 resResolve 传来的）
      if (error?.isBusinessError) {
        const { code, message, needTip = true } = error
        const finalMessage = resolveResError(code, message, needTip)
        return Promise.reject({ code, message: finalMessage, error: error.error })
      }

      // 网络错误或其他错误
      const code = error?.code || 'NETWORK_ERROR'
      const message = error?.message || '网络连接失败，请检查您的网络'
      window.$message?.error(message)
      return Promise.reject({ code, message, error })
    }

    // 3. 处理HTTP错误响应
    const { data, status, config } = error.response
    const code = data?.code ?? status
    const message = data?.message ?? error.message
    const needTip = config?.needTip !== false
    // 调用统一错误处理
    const finalMessage = resolveResError(code, message, needTip)
    return Promise.reject({
      code,
      message: finalMessage,
      error: error.response?.data || error.response
    })
  }
  axiosInstance.interceptors.request.use(reqResolve, reqReject)
  axiosInstance.interceptors.response.use(resResolve, resReject)
}

/**
 * 请求拦截器
 */
function reqResolve(config) {
  // 获取认证存储实例
  const authStore = useAuthStore()

  // 设置默认headers
  config.headers = config.headers || {}

  // 生成traceid: 时间戳+5位随机数
  const timestamp = Date.now()
  const random = Math.floor(10000 + Math.random() * 90000)
  config.headers['traceId'] = `${timestamp}${random}`

  // 添加认证token
  if (authStore.accessToken) {
    config.headers['Authorization'] = `Bearer ${authStore.accessToken}`
  }

  // 添加防重放参数
  const enableReplay = cryptoConfig?.enableReplay !== false
  if (enableReplay && config.replay !== false) {
    const url = config.url || ''
    const path = url.split('?')[0]
    const excludePaths = cryptoConfig?.replayExcludePaths || ['/auth/captcha']

    let excluded = false
    for (const pattern of excludePaths) {
      if (path === pattern || path.endsWith(pattern)) {
        excluded = true
        break
      }
    }

    if (!excluded) {
      config.headers['X-Timestamp'] = Date.now().toString()
      config.headers['X-Nonce'] = generateUUID()
    }
  }

  // 加密处理
  config = encryptRequest(config)

  return config
}

function reqReject(error) {
  return Promise.reject(error)
}
