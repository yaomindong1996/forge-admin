/** 签名协议的时间是 UTC epoch 秒；统一转为用户本地时间，不直接显示 ISO 字符串。 */
export function licenseTime(value) {
  if (value === null || value === undefined || value === '')
    return '—'
  const date = typeof value === 'number' ? new Date(value * 1000) : new Date(value)
  if (!Number.isFinite(date.getTime()))
    return '—'
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  }).format(date)
}

export function licenseUsePeriod(terms) {
  if (!terms)
    return '—'
  const end = terms.validUntil === null ? '永久使用' : `${licenseTime(terms.validUntil)} 截止`
  return `${licenseTime(terms.notBefore)} 起 · ${end}`
}
