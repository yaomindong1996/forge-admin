/**
 * 复制文本到剪贴板
 * @param {string} text - 要复制的文本
 * @param {function} onSuccess - 复制成功回调
 * @param {function} onError - 复制失败回调
 * @returns {Promise<boolean>}
 */
export async function copyToClipboard(text, onSuccess, onError) {
  try {
    // 优先使用现代 Clipboard API
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(text)
      onSuccess?.()
      return true
    }
    
    // 降级方案：使用 execCommand
    const textArea = document.createElement('textarea')
    textArea.value = text
    textArea.style.position = 'fixed'
    textArea.style.left = '-999999px'
    textArea.style.top = '-999999px'
    document.body.appendChild(textArea)
    textArea.focus()
    textArea.select()
    
    const successful = document.execCommand('copy')
    textArea.remove()
    
    if (successful) {
      onSuccess?.()
      return true
    } else {
      throw new Error('复制失败')
    }
  } catch (err) {
    console.error('复制到剪贴板失败:', err)
    onError?.(err)
    return false
  }
}

/**
 * 全局复制方法（带消息提示）
 * @param {string} text - 要复制的文本
 * @param {string} successMsg - 成功提示文字
 * @param {string} errorMsg - 失败提示文字
 * @returns {Promise<boolean>}
 */
export async function copy(text, successMsg = '复制成功', errorMsg = '复制失败') {
  const result = await copyToClipboard(
    text,
    () => {
      if (window.$message) {
        window.$message.success(successMsg)
      }
    },
    (err) => {
      if (window.$message) {
        window.$message.error(errorMsg)
      }
      console.error(err)
    }
  )
  return result
}
