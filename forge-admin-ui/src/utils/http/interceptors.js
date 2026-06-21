import { useAuthStore } from '@/store'
import { cryptoConfig, decryptResponse, encryptRequest, matchPath, shouldEncrypt } from '@/utils/crypto'
import { getSessionKey, initKeyExchange, resetKeyExchange } from '@/utils/crypto/key-exchange'
import { isAuthErrorCode, resolveResError, shouldSilenceAuthError } from './helpers'

// 生成 UUID
function generateUUID() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = Math.random() * 16 | 0
    const v = c === 'x' ? r : (r & 0x3 | 0x8)
    return v.toString(16)
  })
}

function getContentType(headers, data) {
  return headers?.get?.('content-type')
    || headers?.['content-type']
    || headers?.['Content-Type']
    || data?.type
    || ''
}

function isBlobData(data) {
  return typeof Blob !== 'undefined' && data instanceof Blob
}

function isArrayBufferData(data) {
  return typeof ArrayBuffer !== 'undefined' && data instanceof ArrayBuffer
}

function isBinaryData(data) {
  return isBlobData(data) || isArrayBufferData(data)
}

function hasBusinessCode(data) {
  return data && typeof data === 'object' && Object.prototype.hasOwnProperty.call(data, 'code')
}

function buildErrorDetail(config, payload = {}, fallbackError) {
  return {
    code: payload.code,
    message: payload.message,
    rawMessage: payload.rawMessage,
    method: config?.method ? String(config.method).toUpperCase() : '',
    url: config?.url || '',
    traceId: config?.headers?.traceId || config?.headers?.TraceId || '',
    status: payload.status,
    responseData: payload.responseData,
    error: fallbackError,
  }
}

async function parseBlobJsonResponse(response) {
  const { data, headers } = response
  const contentType = getContentType(headers, data)
  if (!isBlobData(data) || !contentType.includes('json')) {
    return response
  }

  try {
    const text = await data.text()
    if (!text) {
      return response
    }

    return {
      ...response,
      data: JSON.parse(text),
    }
  }
  catch (error) {
    console.warn('JSON Blob 响应解析失败:', error)
    return response
  }
}

export function setupInterceptors(axiosInstance) {
  const SUCCESS_CODES = [0, 200]

  /**
   * 响应成功拦截器
   */
  async function resResolve(response) {
    // 先进行解密处理
    try {
      response = decryptResponse(response)
      response = await parseBlobJsonResponse(response)
      response = decryptResponse(response)
    }
    catch (error) {
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
    const contentType = getContentType(headers, data)

    // 二进制响应（下载、图片、附件等）不走 RespInfo code 判断
    if (isBinaryData(data)) {
      return Promise.resolve(config?.rawResponse ? response : data)
    }

    // 非 JSON 响应直接返回
    if (!contentType.includes('json')) {
      return Promise.resolve(data ?? response)
    }

    // 兼容少数非 RespInfo JSON 响应
    if (!hasBusinessCode(data)) {
      return Promise.resolve(data ?? response)
    }

    // 成功响应 (code === 200 或在 SUCCESS_CODES 中)
    if (data && (data.code === 200 || SUCCESS_CODES.includes(data?.code))) {
      return Promise.resolve(data)
    }

    // 业务错误响应
    const code = data?.code ?? status
    const message = data?.message ?? statusText
    const needTip = config?.needTip !== false
    const detail = buildErrorDetail(config, {
      code,
      message,
      rawMessage: message,
      status,
      responseData: data,
    }, data ?? response)
    const finalMessage = resolveResError(code, message, needTip, detail)
    if (isAuthErrorCode(code) && shouldSilenceAuthError()) {
      return Promise.resolve({ code, data: null, message: finalMessage, silentAuthError: true })
    }
    return Promise.reject({ code, message: finalMessage, error: data ?? response, detail })
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
        const detail = buildErrorDetail(error?.config, {
          code,
          message,
          rawMessage: error?.message,
          responseData: error?.error,
        }, error?.error)
        const finalMessage = resolveResError(code, message, needTip, detail)
        return Promise.reject({ code, message: finalMessage, error: error.error, detail })
      }

      // 网络错误或其他错误
      const code = error?.code || 'NETWORK_ERROR'
      const message = error?.message || '网络连接失败，请检查您的网络'
      const needTip = error?.config?.needTip !== false
      const detail = buildErrorDetail(error?.config, {
        code,
        message,
        rawMessage: error?.message,
      }, error)
      resolveResError(code, message, needTip, detail)
      return Promise.reject({ code, message, error, detail })
    }

    // 3. 处理HTTP错误响应
    const { data, status, config } = error.response
    const code = data?.code ?? status
    const message = data?.message ?? error.message
    const needTip = config?.needTip !== false
    // 调用统一错误处理
    const detail = buildErrorDetail(config, {
      code,
      message,
      rawMessage: message,
      status,
      responseData: data,
    }, error.response?.data || error.response)
    const finalMessage = resolveResError(code, message, needTip, detail)
    if (isAuthErrorCode(code) && shouldSilenceAuthError()) {
      return Promise.resolve({ code, data: null, message: finalMessage, silentAuthError: true })
    }
    return Promise.reject({
      code,
      message: finalMessage,
      error: error.response?.data || error.response,
      detail,
    })
  }
  axiosInstance.interceptors.request.use(config => reqResolve(config, axiosInstance), reqReject)
  axiosInstance.interceptors.response.use(resResolve, resReject)
}

function shouldEnsureEncryptionSession(config) {
  return config?.encrypt === true
    && cryptoConfig.enabled !== false
    && shouldEncrypt(config.url || '')
    && !getSessionKey()
}

function createEncryptSessionError(config) {
  const message = '安全会话初始化失败，已阻止明文请求，请重试'
  const error = new Error(message)
  error.code = 'ENCRYPT_KEY_MISSING'
  error.config = config
  return error
}

async function ensureEncryptionSession(config, axiosInstance, authStore) {
  if (!shouldEnsureEncryptionSession(config))
    return

  const exchanged = await initKeyExchange(axiosInstance, authStore.accessToken)
  if (!exchanged || !getSessionKey()) {
    const error = createEncryptSessionError(config)
    window.$message?.error(error.message)
    throw error
  }
}

/**
 * 请求拦截器
 */
async function reqResolve(config, axiosInstance) {
  // 获取认证存储实例
  const authStore = useAuthStore()

  // 设置默认headers
  config.headers = config.headers || {}

  // 生成traceid: 时间戳+5位随机数
  const timestamp = Date.now()
  const random = Math.floor(10000 + Math.random() * 90000)
  config.headers.traceId = `${timestamp}${random}`

  // 添加认证token
  if (authStore.accessToken) {
    config.headers.Authorization = `Bearer ${authStore.accessToken}`
  }

  // 添加防重放参数
  const enableReplay = cryptoConfig?.enableReplay !== false
  if (enableReplay && config.replay !== false) {
    const url = config.url || ''
    const path = url.split('?')[0]
    const excludePaths = cryptoConfig?.replayExcludePaths || ['/auth/captcha', '/auth/captcha/**', '/auth/loginConfig', '/crypto/public-key']

    let excluded = false
    for (const pattern of excludePaths) {
      if (matchPath(path, pattern)) {
        excluded = true
        break
      }
    }

    if (!excluded) {
      config.headers['X-Timestamp'] = Date.now().toString()
      config.headers['X-Nonce'] = generateUUID()
    }
  }

  // 显式加密接口必须先完成会话密钥协商，禁止在无密钥时降级成明文请求
  await ensureEncryptionSession(config, axiosInstance, authStore)

  // 加密处理
  config = encryptRequest(config)

  return config
}

function reqReject(error) {
  return Promise.reject(error)
}
