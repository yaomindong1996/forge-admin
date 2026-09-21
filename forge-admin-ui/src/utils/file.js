/**
 * 文件相关工具函数
 */

import { managedFetch } from '@/composables/useGlobalLoading'
import { useAuthStore } from '@/store'
import { generateUUID } from './common'
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

/**
 * 下载文件
 */
export async function downloadFile(fileData, filename, expires = FILE_URL_CACHE_EXPIRE, forceRefresh = false) {
  const url = await resolveFileAccessUrl(fileData, expires, forceRefresh)
  if (!url) {
    throw new Error('文件地址不存在')
  }

  if (url.startsWith('data:') || url.startsWith('blob:') || !isInternalFileUrl(url)) {
    triggerDownloadLink(url, filename)
    return url
  }

  const response = await fetch(url, {
    headers: getAuthHeaders(),
  })
  if (!response.ok) {
    const errorText = await response.text().catch(() => '')
    throw new Error(errorText || '下载失败')
  }

  const blob = await response.blob()
  const blobUrl = URL.createObjectURL(blob)
  triggerDownloadLink(blobUrl, getDownloadFilename(response, filename))
  setTimeout(() => URL.revokeObjectURL(blobUrl), 100)
  return blobUrl
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
