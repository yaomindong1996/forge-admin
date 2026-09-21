import { amountToChinese } from './amountChinese'
import { PrintError } from './types'

function decimal(value, scale, cents = false) {
  if (typeof value === 'number' && (!Number.isFinite(value) || Math.abs(value) > Number.MAX_SAFE_INTEGER || (cents && !Number.isInteger(value)))) {
    throw new PrintError('UNSAFE_NUMBER', '金额和大整数须使用无损十进制文本')
  }
  const match = /^(-?)(\d{1,100})(?:\.(\d{1,30}))?$/.exec(String(value))
  if (!match || (cents && match[3])) {
    throw new PrintError('INVALID_NUMBER', cents ? '金额须为整数分' : '数值须为普通十进制数')
  }
  const fraction = cents ? '' : (match[3] || '')
  let amount = BigInt(match[2] + fraction)
  const sourceScale = cents ? 2 : fraction.length
  if (sourceScale > scale) {
    const divisor = 10n ** BigInt(sourceScale - scale)
    amount = (amount + divisor / 2n) / divisor
  }
  else {
    amount *= 10n ** BigInt(scale - sourceScale)
  }
  const digits = amount.toString().padStart(scale + 1, '0')
  return `${match[1] && amount !== 0n ? '-' : ''}${scale ? `${digits.slice(0, -scale)}.${digits.slice(-scale)}` : digits}`
}

function date(value, pattern) {
  const match = /^(\d{4})-(\d{2})-(\d{2})(?:[T ](\d{2}):(\d{2})(?::(\d{2})(?:\.\d{1,9})?)?)?$/.exec(String(value))
  if (!match) {
    throw new PrintError('INVALID_DATE', '日期须为服务端本地日期或时间')
  }
  const [, year, month, day, hour = '00', minute = '00', second = '00'] = match
  const maxDay = new Date(Date.UTC(Number(year), Number(month), 0)).getUTCDate()
  if (+month < 1 || +month > 12 || +day < 1 || +day > maxDay || +hour > 23 || +minute > 59 || +second > 59) {
    throw new PrintError('INVALID_DATE', '日期值无效')
  }
  const text = `${year}-${month}-${day} ${hour}:${minute}:${second}`
  return text.slice(0, pattern === 'YYYY-MM-DD HH:mm:ss' ? 19 : pattern === 'YYYY-MM-DD HH:mm' ? 16 : 10)
}

/** Formatting produces plain text only. Vue text nodes perform output escaping. */
export function formatValue(value, format = {}) {
  if (value === null || value === undefined || value === '') {
    return format.emptyText ?? ''
  }
  if (!['string', 'number', 'boolean'].includes(typeof value)) {
    throw new PrintError('EXPECTED_SCALAR', '集合或对象不能直接转为打印文本')
  }
  if (typeof value === 'number' && (!Number.isFinite(value) || (Number.isInteger(value) && !Number.isSafeInteger(value)))) {
    throw new PrintError('UNSAFE_NUMBER', '大整数须由服务端作为文本传输')
  }
  switch (format.type ?? 'TEXT') {
    case 'TEXT':
      return String(value)
    case 'MONEY':
      return decimal(value, 2, true)
    case 'MONEY_UPPER':
      return amountToChinese(decimal(value, 2, true))
    case 'NUMBER':
      return decimal(value, format.scale ?? 2)
    case 'DATE':
      return date(value, format.datePattern)
    case 'BOOLEAN':
      if (typeof value !== 'boolean') {
        throw new PrintError('INVALID_BOOLEAN', '布尔格式仅接受 true 或 false')
      }
      return value ? (format.trueText ?? '是') : (format.falseText ?? '否')
    default:
      throw new PrintError('UNKNOWN_FORMAT', '不支持此字段格式')
  }
}
