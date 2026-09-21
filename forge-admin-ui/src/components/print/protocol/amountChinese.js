import { PrintError } from './types'

const DIGITS = '零壹贰叁肆伍陆柒捌玖'
const SMALL_UNITS = ['', '拾', '佰', '仟']
const GROUP_UNITS = ['', '万', '亿', '兆']

function parseAmount(value) {
  const match = /^(-?)(\d{1,24})(?:\.(\d{1,8}))?$/.exec(String(value).trim())
  if (!match) {
    throw new PrintError('INVALID_NUMBER', '金额大写只接受普通十进制数')
  }
  const fraction = (match[3] || '').padEnd(2, '0').slice(0, 2)
  return { negative: match[1] === '-', integer: match[2].replace(/^0+(?=\d)/, ''), jiao: fraction[0], fen: fraction[1] }
}

function groupToChinese(digits) {
  let text = ''
  let zero = false
  for (let i = 0; i < digits.length; i++) {
    const n = Number(digits[i])
    const unit = SMALL_UNITS[digits.length - i - 1]
    if (n === 0) {
      zero = text.length > 0
      continue
    }
    if (zero) {
      text += '零'
      zero = false
    }
    text += DIGITS[n] + unit
  }
  return text
}

function integerToChinese(raw) {
  if (raw === '0') {
    return ''
  }
  const padded = raw.padStart(Math.ceil(raw.length / 4) * 4, '0')
  const groups = padded.match(/.{4}/g) || []
  let text = ''
  let pendingZero = false
  groups.forEach((group, index) => {
    const body = groupToChinese(group)
    const unit = GROUP_UNITS[groups.length - index - 1]
    if (!body) {
      pendingZero = text.length > 0
      return
    }
    if (pendingZero) {
      text += '零'
      pendingZero = false
    }
    text += body + unit
  })
  return text.replace(/零+/g, '零').replace(/零+$/, '')
}

/** Convert a yuan decimal to RMB uppercase. 100 → 壹佰元整. */
export function amountToChinese(value) {
  const { negative, integer, jiao, fen } = parseAmount(value)
  const head = integerToChinese(integer)
  let text = `${head || '零'}元`
  if (jiao === '0' && fen === '0') {
    text += '整'
  }
  else {
    if (jiao !== '0') {
      text += `${DIGITS[Number(jiao)]}角`
    }
    else if (fen !== '0' && head) {
      text += '零'
    }
    if (fen !== '0') {
      text += `${DIGITS[Number(fen)]}分`
    }
  }
  return `${negative ? '负' : ''}${text}`
}
