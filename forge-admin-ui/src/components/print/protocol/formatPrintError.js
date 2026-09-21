/**
 * Prefer protocol issue details from RespInfo.data over the generic wrapper message.
 */
export function formatPrintApiError(error, fallback = '请求失败') {
  const issues = extractPrintIssues(error)
  if (issues.length) {
    const first = issues[0]
    const detail = [first.message || first.code, first.path].filter(Boolean).join(' · ')
    if (issues.length === 1)
      return detail ? `打印模板校验失败：${detail}` : '打印模板校验失败'
    return `打印模板校验失败：${detail} 等 ${issues.length} 项`
  }
  return error?.message || fallback
}

function extractPrintIssues(error) {
  const candidates = [
    error?.error?.data,
    error?.data,
    error?.detail?.responseData?.data,
    error?.issues,
  ]
  for (const value of candidates) {
    if (!Array.isArray(value) || !value.length)
      continue
    if (value.every(item => item && typeof item === 'object' && ('message' in item || 'code' in item || 'path' in item)))
      return value
  }
  return []
}
