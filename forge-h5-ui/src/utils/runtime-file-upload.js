import { resolvePlatformRequestBaseURL, resolveRequestUrl } from './http/uni-adapter.js'

export async function uploadRuntimeFile(options = {}) {
  const action = resolveUploadAction(options.action)
  const headers = buildAuthHeaders(options.authStore)
  let payload

  // #ifdef H5
  payload = await uploadWithFetch(action, headers, options)
  // #endif

  // #ifndef H5
  payload = await uploadWithUni(action, headers, options)
  // #endif

  return normalizeUploadResult(payload, options.fileName)
}

function resolveUploadAction(action = '') {
  if (action) return String(action)
  const prefix = String(import.meta.env.VITE_REQUEST_PREFIX || '').replace(/\/$/, '')
  let platformBaseURL = ''
  // #ifndef H5
  platformBaseURL = import.meta.env.VITE_MP_API_BASE_URL || ''
  // #endif
  const requestBaseURL = resolvePlatformRequestBaseURL(prefix, platformBaseURL)
  return resolveRequestUrl(requestBaseURL, '/api/file/upload')
}

function buildAuthHeaders(authStore) {
  const headers = {
    'X-Timestamp': Date.now().toString(),
    'X-Nonce': createNonce(),
  }
  if (authStore?.accessToken)
    headers.Authorization = `${authStore.tokenType || 'Bearer'} ${authStore.accessToken}`
  return headers
}

async function uploadWithFetch(action, headers, options) {
  let file = options.file
  if (!file && options.filePath) {
    const response = await fetch(options.filePath)
    file = await response.blob()
  }
  if (!file) throw new Error('没有可上传的文件')
  const formData = new FormData()
  formData.append('file', file, options.fileName || file.name || `upload-${Date.now()}`)
  appendFormData(formData, options)
  const response = await fetch(action, { method: 'POST', headers, body: formData })
  const result = await response.json().catch(() => null)
  if (!response.ok) throw new Error(result?.message || result?.msg || '文件上传失败')
  return result
}

function uploadWithUni(action, headers, options) {
  const filePath = String(options.filePath || options.file?.path || options.file?.tempFilePath || '')
  if (!filePath) return Promise.reject(new Error('没有可上传的临时文件'))
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: action,
      filePath,
      name: 'file',
      header: headers,
      formData: buildPlainFormData(options),
      success: (response) => {
        const result = parseUploadResponse(response.data)
        if (response.statusCode >= 200 && response.statusCode < 300) resolve(result)
        else reject(new Error(result?.message || result?.msg || '文件上传失败'))
      },
      fail: error => reject(new Error(error?.errMsg || '文件上传失败')),
    })
  })
}

function appendFormData(formData, options) {
  Object.entries(buildPlainFormData(options)).forEach(([key, value]) => formData.append(key, value))
}

function buildPlainFormData(options) {
  return {
    businessType: String(options.businessType || 'lowcode_attachment'),
    isPrivate: String(options.isPrivate !== false),
    ...(options.businessId ? { businessId: String(options.businessId) } : {}),
    ...(options.formData && typeof options.formData === 'object' ? options.formData : {}),
  }
}

function normalizeUploadResult(result, fallbackName = '') {
  if (!(result?.code === 200 || result?.respCode === '0000'))
    throw new Error(result?.message || result?.msg || '文件上传失败')
  const data = result?.data || {}
  const id = data.fileId || data.id || data.filePath
  if (!id) throw new Error('附件服务未返回文件标识')
  return {
    id: String(id),
    name: data.originalName || data.fileName || fallbackName || String(id),
    data,
  }
}

function parseUploadResponse(value) {
  if (value && typeof value === 'object') return value
  try { return JSON.parse(String(value || '{}')) }
  catch { return {} }
}

function createNonce() {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (char) => {
    const random = Math.random() * 16 | 0
    const value = char === 'x' ? random : (random & 0x3 | 0x8)
    return value.toString(16)
  })
}
