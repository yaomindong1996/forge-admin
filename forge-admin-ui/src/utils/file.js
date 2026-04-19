/**
 * 文件相关工具函数
 */

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
      return fileData.accessUrl
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

  const prefix = import.meta.env.VITE_REQUEST_PREFIX || ''

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

  const prefix = import.meta.env.VITE_REQUEST_PREFIX || ''
  return `${prefix}/api/file/download/${fileId}`
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
