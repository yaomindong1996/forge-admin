/**
 * 文件相关工具函数
 */

import { managedFetch } from '@/composables/useGlobalLoading'
import { useAuthStore, useUserStore } from '@/store'
import { downloadScope } from '@/stores/system/recentDownloadsUtils'
import { generateUUID } from './common'
import { createDownloadObserver, receiveDownloadBlob } from './download-events'
import { request } from './http'
import { getLocalStorage, setLocalStorage } from './storage'

const FILE_URL_CACHE_KEY = 'FILE_URL_CACHE'
const FILE_URL_CACHE_EXPIRE = 43200
const fileUrlMemoryCache = new Map()

function getRequestPrefix() {
  return import.meta.env.VITE_REQUEST_PREFIX || ''
}

function normalizeFileAccessUrl(url) {
  if (!url)
    return ''

  if (url.startsWith('http://')
    || url.startsWith('https://')
    || url.startsWith('data:')
    || url.startsWith('blob:')) {
    return url
  }

  const prefix = getRequestPrefix()
  // 本地存储的 /api/file/url 接口会返回 /api/file/download/{fileId} 这种相对路径。
  // 浏览器直接渲染图片时必须补上前端代理前缀，否则会请求到当前站点根路径导致图片不可见。
  if (url.startsWith('/api/file/') && prefix) {
    return `${prefix}${url}`
  }

  return url
}

function isDirectFilePath(value) {
  const prefix = getRequestPrefix()
  return value.startsWith('http://')
    || value.startsWith('https://')
    || value.startsWith('data:')
    || value.startsWith('blob:')
    || value.startsWith('/api/file/')
    || (prefix && value.startsWith(`${prefix}/api/file/`))
}

function isInternalFileUrl(url) {
  const prefix = getRequestPrefix()
  return url.startsWith('/api/file/')
    || url.includes('/api/file/')
    || (prefix && url.startsWith(`${prefix}/api/file/`))
}

function getAuthHeaders() {
  try {
    const authStore = useAuthStore()
    const headers = {
      'X-Timestamp': Date.now().toString(),
      'X-Nonce': generateUUID(),
    }
    if (authStore.accessToken) {
      headers.Authorization = `Bearer ${authStore.accessToken}`
    }
    return headers
  }
  catch {
    return {
      'X-Timestamp': Date.now().toString(),
      'X-Nonce': generateUUID(),
    }
  }
}

/**
 * 获取文件访问 URL
 * @param {string | object} fileData - 文件ID字符串或文件元数据对象
 *   - 如果是字符串: fileId（推荐）或 filePath
 *   - 如果是对象: { fileId, filePath, accessUrl }
 * @returns {string} 完整的文件访问 URL
 */
export function getFileUrl(fileData) {
  if (!fileData)
    return ''

  // 如果是对象，优先使用 accessUrl，然后是 fileId，最后是 filePath
  if (typeof fileData === 'object') {
    if (fileData.accessUrl) {
      return normalizeFileAccessUrl(fileData.accessUrl)
    }
    if (fileData.fileId) {
      return getFileDownloadUrl(fileData.fileId)
    }
    if (fileData.filePath) {
      // filePath 无法直接访问，需要通过 fileId
      console.warn('文件对象缺少 fileId，无法生成访问URL:', fileData)
      return ''
    }
    return ''
  }

  // 如果是字符串，统一使用下载接口
  return buildFileUrl(fileData)
}

/**
 * 构建文件访问 URL（内部方法）
 * @param {string} idOrPath - fileId 或 filePath
 * @returns {string} 完整的文件访问 URL
 */
function buildFileUrl(idOrPath) {
  if (!idOrPath)
    return ''

  // 如果已经是完整的 URL（http:// 或 https://），直接返回
  if (idOrPath.startsWith('http://') || idOrPath.startsWith('https://')) {
    return idOrPath
  }

  const prefix = getRequestPrefix()

  if (idOrPath.startsWith('/api/file/') || (prefix && idOrPath.startsWith(`${prefix}/api/file/`))) {
    return normalizeFileAccessUrl(idOrPath)
  }

  // 统一使用下载接口访问文件
  // 注意：这里假设传入的是 fileId
  // 如果传入的是 filePath（包含 /），也会尝试作为 fileId 使用，可能会失败
  return `${prefix}/api/file/download/${idOrPath}`
}

/**
 * 获取文件下载 URL
 * @param {string} fileId - 文件ID
 * @returns {string} 文件下载 URL
 */
export function getFileDownloadUrl(fileId) {
  if (!fileId)
    return ''

  const prefix = getRequestPrefix()
  return `${prefix}/api/file/download/${fileId}`
}

function getFileUrlCacheStore() {
  return getLocalStorage(FILE_URL_CACHE_KEY) || {}
}

function setFileUrlCacheStore(store) {
  setLocalStorage(FILE_URL_CACHE_KEY, store)
}

function isExpiredCache(item) {
  return !item || !item.url || item.expiresAt <= Date.now()
}

function cleanupFileUrlCacheStore(store) {
  const nextStore = {}
  Object.keys(store || {}).forEach((key) => {
    const item = store[key]
    if (!isExpiredCache(item)) {
      nextStore[key] = item
    }
  })
  return nextStore
}

export function getCachedFileAccessUrl(fileId) {
  const memoryItem = fileUrlMemoryCache.get(fileId)
  if (!isExpiredCache(memoryItem)) {
    return normalizeFileAccessUrl(memoryItem.url)
  }

  const store = cleanupFileUrlCacheStore(getFileUrlCacheStore())
  const cacheItem = store[fileId]
  setFileUrlCacheStore(store)
  if (isExpiredCache(cacheItem)) {
    return ''
  }

  fileUrlMemoryCache.set(fileId, cacheItem)
  return normalizeFileAccessUrl(cacheItem.url)
}

export function removeCachedFileAccessUrl(fileId) {
  fileUrlMemoryCache.delete(fileId)
  const store = cleanupFileUrlCacheStore(getFileUrlCacheStore())
  delete store[fileId]
  setFileUrlCacheStore(store)
}

export async function resolveFileAccessUrl(fileData, expires = FILE_URL_CACHE_EXPIRE, forceRefresh = false) {
  if (!fileData)
    return ''

  if (typeof fileData === 'object' && fileData.accessUrl) {
    return normalizeFileAccessUrl(fileData.accessUrl)
  }

  const rawValue = typeof fileData === 'object' ? fileData.fileId || fileData.filePath : fileData
  if (!rawValue)
    return ''

  if (isDirectFilePath(rawValue)) {
    return normalizeFileAccessUrl(rawValue)
  }

  if (forceRefresh) {
    // 图片加载失败时会走强制刷新，先清掉旧的临时访问地址，再向后端重新取。
    removeCachedFileAccessUrl(rawValue)
  }

  const cachedUrl = getCachedFileAccessUrl(rawValue)
  if (cachedUrl) {
    return cachedUrl
  }

  const result = await request({
    url: `/api/file/url/${rawValue}`,
    method: 'get',
    params: { expires },
    needTip: false,
    encrypt: false,
    skipGlobalLoading: true,
  })
  if (result.code !== 200 || !result.data) {
    throw new Error(result.msg || result.message || '文件访问地址获取失败')
  }

  const accessUrl = normalizeFileAccessUrl(result.data)
  const cacheItem = {
    url: accessUrl,
    expiresAt: Date.now() + Math.max(expires - 60, 60) * 1000,
  }

  fileUrlMemoryCache.set(rawValue, cacheItem)
  const store = cleanupFileUrlCacheStore(getFileUrlCacheStore())
  store[rawValue] = cacheItem
  setFileUrlCacheStore(store)

  return accessUrl
}

const renderableBlobUrlCache = new Map()

function renderableCacheKey(fileData) {
  if (!fileData)
    return ''
  if (typeof fileData === 'object')
    return String(fileData.fileId || fileData.filePath || fileData.accessUrl || '')
  return String(fileData)
}

/**
 * 解析成可直接渲染的文件地址
 * - 外部直链、data/blob 直接返回
 * - 内部文件接口会先拉 blob 再返回 blob URL
 */
export async function resolveRenderableFileUrl(fileData, expires = FILE_URL_CACHE_EXPIRE, forceRefresh = false) {
  const cacheKey = renderableCacheKey(fileData)
  if (!forceRefresh && cacheKey && renderableBlobUrlCache.has(cacheKey))
    return renderableBlobUrlCache.get(cacheKey)

  const url = await resolveFileAccessUrl(fileData, expires, forceRefresh)
  if (!url)
    return ''

  if (url.startsWith('data:') || url.startsWith('blob:')) {
    return url
  }

  if (!isInternalFileUrl(url)) {
    return url
  }

  const response = await managedFetch(url, {
    headers: getAuthHeaders(),
  }, {
    // Preview/thumbnail loads should stay silent; explicit downloadFile() still shows loading.
    skipGlobalLoading: true,
  })
  if (!response.ok) {
    throw new Error('文件加载失败')
  }

  const blob = await response.blob()
  const blobUrl = URL.createObjectURL(blob)
  if (cacheKey) {
    const previous = renderableBlobUrlCache.get(cacheKey)
    if (previous && previous !== blobUrl)
      URL.revokeObjectURL(previous)
    renderableBlobUrlCache.set(cacheKey, blobUrl)
  }
  return blobUrl
}

function triggerDownloadLink(url, filename) {
  const link = document.createElement('a')
  link.href = url
  link.download = filename || ''
  link.rel = 'noopener'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

function getDownloadFilename(response, fallbackName) {
  const contentDisposition = response.headers.get('Content-Disposition')
  if (!contentDisposition) {
    return fallbackName
  }
  const utf8Match = contentDisposition.match(/filename\*=utf-8''([^;]+)/i)
  if (utf8Match?.[1]) {
    return decodeURIComponent(utf8Match[1])
  }
  const normalMatch = contentDisposition.match(/filename="?([^";]+)"?/i)
  if (normalMatch?.[1]) {
    return decodeURIComponent(normalMatch[1])
  }
  return fallbackName
}

function resolveDownloadFileId(fileData) {
  if (!fileData || (typeof fileData !== 'string' && typeof fileData !== 'object'))
    return ''
  if (typeof fileData === 'object')
    return fileData.fileId ? String(fileData.fileId) : ''
  if (isDirectFilePath(fileData))
    return ''
  return String(fileData)
}

function notifyDownloadRecorded() {
  window.dispatchEvent(new CustomEvent('forge-download-recorded'))
}

/** 对象存储直链不经过下载接口，补记一笔；失败不影响文件本身。 */
async function reportExternalDownload(fileId, fileName) {
  if (!fileId)
    return
  try {
    await request.post('/system/file/download-log', {
      fileId,
      fileName: fileName || '未命名文件',
    }, { needTip: false, skipGlobalLoading: true })
    notifyDownloadRecorded()
  }
  catch {
    // 记录接口未就绪时不打断直链下载
  }
}

async function saveAuthenticatedDownload(url, filename, observer) {
  const response = await fetch(url, {
    headers: {
      ...getAuthHeaders(),
      'X-Forge-Download-Intent': '1',
    },
  })
  if (!response.ok) {
    const errorText = await response.text().catch(() => '')
    const error = new Error(errorText || '下载失败')
    error.allowDirectFallback = response.status >= 500
    throw error
  }
  const contentType = response.headers.get('Content-Type') || ''
  if (contentType.toLowerCase().includes('application/json') && !response.headers.get('Content-Disposition')) {
    // 统一异常可能以 HTTP 200 返回 JSON，不能把鉴权/业务错误保存成文件。
    const error = new Error('文件下载失败，请重试或检查文件权限')
    error.allowDirectFallback = false
    throw error
  }

  const blob = await receiveDownloadBlob(response, observer?.progress)
  assertDownloadContext(observer)
  const blobUrl = URL.createObjectURL(blob)
  triggerDownloadLink(blobUrl, getDownloadFilename(response, filename))
  notifyDownloadRecorded()
  setTimeout(() => URL.revokeObjectURL(blobUrl), 100)
  return blobUrl
}

/**
 * 下载文件。有 fileId 时走系统下载接口，避免对象存储直链绕过下载记录。
 */
export async function downloadFile(fileData, filename, expires = FILE_URL_CACHE_EXPIRE, forceRefresh = false) {
  const fileId = resolveDownloadFileId(fileData)
  const observer = fileId ? observeFileDownload(fileId, filename) : null
  try {
    const result = await downloadFileContent(fileData, filename, expires, forceRefresh, observer)
    observer?.saved()
    return result
  }
  catch (error) {
    observer?.failed()
    throw error
  }
}

function observeFileDownload(fileId, fileName) {
  try {
    const auth = useAuthStore()
    const user = useUserStore()
    const token = auth.accessToken
    const scope = downloadScope(user.tenantId, user.userId)
    return createDownloadObserver({
      scope,
      fileId,
      fileName,
      // 捕获会话而非把凭据写入事件；旧会话结束后的响应不能进入新账号面板。
      isCurrent: () => auth.accessToken === token && downloadScope(user.tenantId, user.userId) === scope,
    })
  }
  catch {
    // 进度是附加能力，未挂载 Pinia 的公共文件工具调用仍保持原有行为。
    return null
  }
}

function assertDownloadContext(observer) {
  if (observer && !observer.isCurrent())
    throw new Error('登录身份已改变，请重新下载')
}

async function downloadFileContent(fileData, filename, expires, forceRefresh, observer) {
  const fileId = resolveDownloadFileId(fileData)
  if (fileId) {
    try {
      return await saveAuthenticatedDownload(getFileDownloadUrl(fileId), filename, observer)
    }
    catch (error) {
      assertDownloadContext(observer)
      // 不能在权限拒绝后借缓存的旧签名地址绕过服务端下载校验。
      if (error.allowDirectFallback === false)
        throw error
      const fallbackUrl = await resolveFileAccessUrl(fileData, expires, forceRefresh)
      assertDownloadContext(observer)
      if (!fallbackUrl || isInternalFileUrl(fallbackUrl))
        throw error
      triggerDownloadLink(fallbackUrl, filename)
      // 已交给浏览器保存；审计补记慢/失败不能把前端一直留在“接收中”。
      void reportExternalDownload(fileId, filename)
      return fallbackUrl
    }
  }

  const url = await resolveFileAccessUrl(fileData, expires, forceRefresh)
  if (!url)
    throw new Error('文件地址不存在')

  if (url.startsWith('data:') || url.startsWith('blob:') || !isInternalFileUrl(url)) {
    triggerDownloadLink(url, filename)
    return url
  }

  return saveAuthenticatedDownload(url, filename, observer)
}

/**
 * 从 axios blob 响应触发浏览器下载。
 * 注意：不能在 click 后立刻 revokeObjectURL，否则大文件/慢磁盘下会出现「点好几次才下得下来」。
 *
 * @param {object|Blob} response axios rawResponse 或 Blob
 * @param {string} fallbackName 无 Content-Disposition 时的文件名
 * @param {{ revokeDelayMs?: number }} [options]
 * @returns {Promise<string>} 解析出的文件名
 */
export async function downloadAxiosBlobResponse(response, fallbackName = 'download.bin', options = {}) {
  const revokeDelayMs = Number.isFinite(options.revokeDelayMs) ? options.revokeDelayMs : 60_000
  let blob = response?.data instanceof Blob ? response.data : response
  if (!(blob instanceof Blob) && blob && typeof blob === 'object') {
    blob = new Blob([JSON.stringify(blob, null, 2)], { type: 'application/json;charset=UTF-8' })
  }
  if (!(blob instanceof Blob))
    throw new TypeError('下载响应不是文件流')

  await assertBlobIsNotBusinessError(blob)

  const headers = response?.headers
  const disposition = (typeof headers?.get === 'function'
    ? (headers.get('content-disposition') || headers.get('Content-Disposition'))
    : null)
    || headers?.['content-disposition']
    || headers?.['Content-Disposition']
    || ''
  const utf8Match = String(disposition).match(/filename\*=(?:UTF-8|utf-8)''([^;]+)/i)
  const normalMatch = String(disposition).match(/filename="?([^";]+)"?/i)
  const fileName = utf8Match?.[1]
    ? decodeURIComponent(utf8Match[1])
    : (normalMatch?.[1] ? decodeURIComponent(normalMatch[1]) : fallbackName)

  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  link.rel = 'noopener'
  // 保持在文档树中直到浏览器接管下载，减少偶发 click 无效
  document.body.appendChild(link)
  link.click()
  window.setTimeout(() => {
    link.remove()
    URL.revokeObjectURL(url)
  }, Math.max(1000, revokeDelayMs))
  return fileName
}

/** 部分接口失败时仍以 octet-stream 返回 JSON 错误体，避免保存成假文件。 */
async function assertBlobIsNotBusinessError(blob) {
  if (!blob || blob.size <= 0 || blob.size > 8192)
    return
  let text = ''
  try {
    text = await readBlobAsText(blob)
  }
  catch {
    return
  }
  if (!String(text || '').trimStart().startsWith('{'))
    return
  let payload
  try {
    payload = JSON.parse(text)
  }
  catch {
    return
  }
  if (!payload || typeof payload !== 'object' || Array.isArray(payload))
    return
  const code = payload.code
  if (code === undefined || code === null || code === 200 || code === 0 || code === '200' || code === '0')
    return
  throw new Error(payload.message || '导出失败')
}

function readBlobAsText(blob) {
  if (typeof blob?.text === 'function')
    return blob.text()
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result || ''))
    reader.onerror = () => reject(reader.error || new Error('读取下载内容失败'))
    reader.readAsText(blob)
  })
}

/**
 * 获取图片预览 URL（带缩略图参数）
 * @param {string} filePath - 文件路径
 * @param {object} options - 选项
 * @param {number} options.width - 宽度
 * @param {number} options.height - 高度
 * @param {string} options.mode - 缩放模式: 'fit' | 'fill' | 'crop'
 * @returns {string} 图片预览 URL
 */
export function getImageUrl(filePath, options = {}) {
  const baseUrl = getFileUrl(filePath)

  if (!baseUrl || !options || Object.keys(options).length === 0) {
    return baseUrl
  }

  const params = new URLSearchParams()
  if (options.width)
    params.append('width', options.width)
  if (options.height)
    params.append('height', options.height)
  if (options.mode)
    params.append('mode', options.mode)

  const queryString = params.toString()
  return queryString ? `${baseUrl}?${queryString}` : baseUrl
}
