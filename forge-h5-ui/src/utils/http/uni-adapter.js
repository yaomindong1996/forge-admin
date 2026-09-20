import { AxiosError } from 'axios'

export function resolvePlatformRequestBaseURL(prefix = '', platformBaseURL = '') {
  const normalizedPrefix = String(prefix || '').trim()
  const normalizedBaseURL = String(platformBaseURL || '').trim()
  return normalizedBaseURL ? resolveRequestUrl(normalizedBaseURL, normalizedPrefix) : normalizedPrefix
}

export function resolveRequestUrl(baseURL = '', requestURL = '') {
  const request = String(requestURL || '').trim()
  if (/^https?:\/\//i.test(request)) return request
  const base = String(baseURL || '').trim()
  if (!base) return request
  return `${base.replace(/\/+$/, '')}/${request.replace(/^\/+/, '')}`
}

export function createUniRequestAdapter() {
  return config => new Promise((resolve, reject) => {
    const requestURL = appendParams(resolveRequestUrl(config.baseURL, config.url), config.params, config.paramsSerializer)
    if (!/^https?:\/\//i.test(requestURL)) {
      reject(createNetworkError(
        '小程序请求地址必须是绝对地址，请配置 VITE_MP_API_BASE_URL',
        config,
        'ERR_INVALID_URL',
      ))
      return
    }

    let settled = false
    const requestTask = uni.request({
      url: requestURL,
      method: String(config.method || 'get').toUpperCase(),
      data: normalizeRequestData(config.data),
      header: normalizeHeaders(config.headers),
      timeout: Number(config.timeout) || 12000,
      dataType: config.responseType === 'arraybuffer' ? undefined : 'json',
      responseType: config.responseType === 'arraybuffer' ? 'arraybuffer' : 'text',
      withCredentials: config.withCredentials === true,
      success: (result) => {
        if (settled) return
        settled = true
        const response = {
          data: result.data,
          status: result.statusCode,
          statusText: String(result.errMsg || ''),
          headers: normalizeResponseHeaders(result.header),
          config,
          request: requestTask,
        }
        const validateStatus = config.validateStatus || (status => status >= 200 && status < 300)
        if (!response.status || validateStatus(response.status)) resolve(response)
        else reject(new AxiosError(
          `Request failed with status code ${response.status}`,
          response.status >= 500 ? AxiosError.ERR_BAD_RESPONSE : AxiosError.ERR_BAD_REQUEST,
          config,
          requestTask,
          response,
        ))
      },
      fail: (error) => {
        if (settled) return
        settled = true
        reject(createNetworkError(error?.errMsg || '小程序网络请求失败', config, AxiosError.ERR_NETWORK, requestTask))
      },
    })

    const abort = () => {
      if (settled) return
      settled = true
      requestTask?.abort?.()
      reject(createNetworkError('请求已取消', config, AxiosError.ERR_CANCELED, requestTask))
    }
    if (config.signal) {
      if (config.signal.aborted) abort()
      else config.signal.addEventListener('abort', abort, { once: true })
    }
    config.cancelToken?.subscribe?.(abort)
  })
}

function appendParams(url, params, paramsSerializer) {
  if (!params || typeof params !== 'object') return url
  const serialized = typeof paramsSerializer === 'function'
    ? paramsSerializer(params)
    : paramsSerializer?.serialize
      ? paramsSerializer.serialize(params)
      : serializeParams(params)
  if (!serialized) return url
  return `${url}${url.includes('?') ? '&' : '?'}${serialized}`
}

function serializeParams(params) {
  const values = []
  Object.entries(params).forEach(([key, rawValue]) => {
    if (rawValue === undefined || rawValue === null) return
    const list = Array.isArray(rawValue) ? rawValue : [rawValue]
    list.forEach(value => values.push(`${encodeURIComponent(key)}=${encodeURIComponent(normalizeParamValue(value))}`))
  })
  return values.join('&')
}

function normalizeParamValue(value) {
  if (value instanceof Date) return value.toISOString()
  if (value && typeof value === 'object') return JSON.stringify(value)
  return String(value)
}

function normalizeRequestData(value) {
  if (typeof value !== 'string') return value
  try { return JSON.parse(value) }
  catch { return value }
}

function normalizeHeaders(headers) {
  const source = typeof headers?.toJSON === 'function' ? headers.toJSON() : headers || {}
  return Object.fromEntries(Object.entries(source).filter(([, value]) => value !== undefined && value !== null))
}

function normalizeResponseHeaders(headers = {}) {
  return Object.fromEntries(Object.entries(headers).map(([key, value]) => [String(key).toLowerCase(), value]))
}

function createNetworkError(message, config, code, request) {
  return new AxiosError(message, code, config, request)
}
