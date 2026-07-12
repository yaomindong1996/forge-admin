import { nextTick } from 'vue'
import { attachRequestGlobalLoading, finishRequestGlobalLoading } from '@/composables/useGlobalLoading'
import { useAuthStore, usePermissionStore, useTabStore, useTenantStore } from '@/store'
import { cryptoConfig, decryptResponse, encryptRequest, matchPath, shouldEncrypt } from '@/utils/crypto'
import { getSessionKey, initKeyExchange, resetKeyExchange } from '@/utils/crypto/key-exchange'
import { getTenantPageBaseTitle } from '@/utils/page-title'
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

function normalizeRequestData(data) {
  if (!data)
    return {}
  if (typeof data === 'object')
    return data
  if (typeof data !== 'string')
    return {}
  try {
    const parsed = JSON.parse(data)
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {}
  }
  catch {
    return {}
  }
}

function resolveSelectorObjectCode(config = {}) {
  const data = normalizeRequestData(config.data)
  const params = config.params && typeof config.params === 'object' ? config.params : {}
  return [
    data.objectCode,
    data.businessObjectCode,
    data.targetObjectCode,
    data.targetEntityCode,
    data.candidateObjectCode,
    data.referenceObjectCode,
    data.refObjectCode,
    data.sourceObjectCode,
    data.targetCode,
    params.objectCode,
    params.businessObjectCode,
    params.targetObjectCode,
    params.targetEntityCode,
    params.candidateObjectCode,
    params.referenceObjectCode,
    params.refObjectCode,
    params.sourceObjectCode,
    params.targetCode,
  ].map(value => String(value ?? '').trim()).find(Boolean) || ''
}

function assertBusinessSelectorRequest(config = {}) {
  const url = String(config.url || '')
  if (!url.includes('/ai/business/selector/query') && !url.includes('ai/business/selector/query'))
    return
  if (resolveSelectorObjectCode(config))
    return
  const error = {
    code: 'BUSINESS_SELECTOR_OBJECT_CODE_MISSING',
    message: '选择器缺少业务对象编码，已在前端阻止请求',
    config: {
      url: config.url,
      method: config.method,
      data: normalizeRequestData(config.data),
      params: config.params || {},
    },
    stack: new Error('Business selector request missing objectCode').stack,
  }
  console.error('[BusinessRecordSelector] 阻止缺少业务对象编码的接口请求', error)
  throw error
}

function normalizePageTitle(title = '') {
  const text = String(title || '').trim()
  if (!text)
    return ''
  return text.split('|')[0]?.trim() || text
}

function normalizeRoutePath(path) {
  const value = String(path || '').trim()
  const [pathWithoutHash] = value.split('#')
  const [pathname] = pathWithoutHash.split('?')
  const normalized = String(pathname || '').replace(/\/+/g, '/')
  if (!normalized || normalized === '/')
    return normalized
  return normalized.startsWith('/') ? normalized : `/${normalized}`
}

function escapeRegExp(value) {
  return String(value).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

function isSameRoutePath(menuPath, targetPath) {
  const normalizedMenuPath = normalizeRoutePath(menuPath)
  const normalizedTargetPath = normalizeRoutePath(targetPath)
  if (!normalizedMenuPath || !normalizedTargetPath)
    return false
  if (normalizedMenuPath === normalizedTargetPath)
    return true
  if (!normalizedMenuPath.includes(':'))
    return false
  const pattern = normalizedMenuPath
    .split('/')
    .map(segment => segment.startsWith(':') ? '[^/]+' : escapeRegExp(segment))
    .join('/')
  return new RegExp(`^${pattern}$`).test(normalizedTargetPath)
}

function resolveCurrentPagePath() {
  if (typeof window === 'undefined')
    return ''
  const hashPath = window.location.hash?.replace(/^#/, '')
  if (hashPath)
    return sanitizePageAuditPath(hashPath)
  return sanitizePageAuditPath(`${window.location.pathname || ''}${window.location.search || ''}`)
}

function sanitizePageAuditPath(pagePath) {
  const [pathname, queryString = ''] = String(pagePath || '').split('?', 2)
  if (!queryString)
    return pathname
  const sensitiveKeys = new Set([
    'state',
    'code',
    'code_verifier',
    'code_challenge',
    'client_secret',
    'access_token',
    'token',
  ])
  try {
    const params = new URLSearchParams(queryString)
    for (const key of sensitiveKeys) {
      if (params.has(key))
        params.set(key, '[REDACTED]')
    }
    const sanitized = params.toString()
    return sanitized ? `${pathname}?${sanitized}` : pathname
  }
  catch {
    return pathname
  }
}

function resolvePageQueryParam(pagePath, names = []) {
  const queryString = String(pagePath || '').split('?')[1] || ''
  if (!queryString)
    return ''
  try {
    const params = new URLSearchParams(queryString)
    return names.map(name => params.get(name)).find(Boolean) || ''
  }
  catch {
    return ''
  }
}

function findTitleFromAllMenus(allMenus, targetPath, menuKey) {
  if (!Array.isArray(allMenus))
    return ''
  if (menuKey !== undefined && menuKey !== null && menuKey !== '') {
    const menu = allMenus.find(item => String(item.key || item.id) === String(menuKey))
    if (menu)
      return menu.label || menu.name || menu.meta?.title || ''
  }
  const found = allMenus.find(menu => isSameRoutePath(menu.path, targetPath))
  return found?.label || found?.name || found?.meta?.title || ''
}

function resolveTitleFromStores(pagePath) {
  try {
    const permissionStore = usePermissionStore()
    const tabStore = useTabStore()
    const normalizedPath = normalizeRoutePath(pagePath)
    const matchedTab = tabStore.tabs?.find(tab => tab.path === pagePath || normalizeRoutePath(tab.path) === normalizedPath)
    if (matchedTab?.title && matchedTab.title !== '业务页面')
      return matchedTab.title
    const menuKey = resolvePageQueryParam(pagePath, ['menuKey', 'menuResourceId'])
    return findTitleFromAllMenus(permissionStore.allMenus, normalizedPath, menuKey)
  }
  catch {
    return ''
  }
}

function resolveBasePageTitle() {
  try {
    return getTenantPageBaseTitle(useTenantStore())
  }
  catch {
    return ''
  }
}

function resolveTitleFromDocument() {
  const pageTitle = normalizePageTitle(document.title)
  const baseTitle = normalizePageTitle(resolveBasePageTitle())
  if (!pageTitle || (baseTitle && pageTitle === baseTitle))
    return ''
  return pageTitle
}

function resolvePageAuditTitle(pagePath) {
  return resolveTitleFromStores(pagePath) || resolveTitleFromDocument()
}

function resolvePageAuditHeaders() {
  if (typeof window === 'undefined')
    return {}
  const pagePath = resolveCurrentPagePath()
  const pageTitle = resolvePageAuditTitle(pagePath)
  const headers = {}
  if (pagePath)
    headers['X-Page-Path'] = pagePath.slice(0, 500)
  if (pageTitle)
    headers['X-Page-Title'] = encodeURIComponent(pageTitle.slice(0, 200))
  return headers
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
    finishRequestGlobalLoading(response?.config)

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

        try {
          const retryResult = await retryReadonlyCryptoRequest(response?.config, axiosInstance)
          if (retryResult)
            return retryResult
        }
        catch (retryError) {
          return Promise.reject(retryError)
        }

        // 提示用户
        const message = '安全会话已过期，请重新操作'
        await nextTick()
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
    await nextTick()
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
    finishRequestGlobalLoading(error?.config || error?.response?.config)

    // 1. 处理已经在 resResolve 中标记了 skipErrorHandler 的错误（解密错误等）
    if (error?.skipErrorHandler) {
      return Promise.reject(error)
    }

    await nextTick()

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

function isReadonlyCryptoRequest(config = {}) {
  const method = String(config.method || 'get').toLowerCase()
  return ['get', 'head', 'options'].includes(method)
}

function shouldRetryReadonlyCryptoRequest(config = {}) {
  return config
    && !config.__cryptoRetry
    && config.encrypt !== false
    && isReadonlyCryptoRequest(config)
    && shouldEncrypt(config.url || '')
}

async function retryReadonlyCryptoRequest(config, axiosInstance) {
  if (!shouldRetryReadonlyCryptoRequest(config))
    return null

  const authStore = useAuthStore()
  const exchanged = await initKeyExchange(axiosInstance, authStore.accessToken)
  if (!exchanged || !getSessionKey())
    return null

  return axiosInstance({
    ...config,
    headers: { ...(config.headers || {}) },
    __cryptoRetry: true,
  })
}

async function ensureEncryptionSession(config, axiosInstance, authStore) {
  if (!shouldEnsureEncryptionSession(config))
    return

  const exchanged = await initKeyExchange(axiosInstance, authStore.accessToken)
  if (!exchanged || !getSessionKey()) {
    const error = createEncryptSessionError(config)
    throw error
  }
}

/**
 * 请求拦截器
 */
async function reqResolve(config, axiosInstance) {
  config = attachRequestGlobalLoading(config)

  try {
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

    if (config.pageAudit !== false)
      Object.assign(config.headers, resolvePageAuditHeaders())

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

    assertBusinessSelectorRequest(config)

    // 加密处理
    config = encryptRequest(config)

    return config
  }
  catch (error) {
    finishRequestGlobalLoading(config)
    if (error?.code === 'ENCRYPT_KEY_MISSING') {
      await nextTick()
      window.$message?.error(error.message)
    }
    throw error
  }
}

function reqReject(error) {
  finishRequestGlobalLoading(error?.config)
  return Promise.reject(error)
}
