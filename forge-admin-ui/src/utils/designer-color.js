/**
 * 设计器样式面板颜色互转：画布可能存 hex / rgb / hsl，右侧 color/hex 控件只认 #RRGGBB。
 */

function clampByte(n) {
  return Math.max(0, Math.min(255, Math.round(Number(n) || 0)))
}

function toHexPair(n) {
  return clampByte(n).toString(16).padStart(2, '0').toUpperCase()
}

function expandShortHex(hex) {
  return hex.split('').map(ch => `${ch}${ch}`).join('')
}

/** h∈[0,360) s,l∈[0,1] → {r,g,b} 0–255 */
function hslToRgb(h, s, l) {
  const hue = ((Number(h) % 360) + 360) % 360
  const sat = Math.max(0, Math.min(1, Number(s)))
  const light = Math.max(0, Math.min(1, Number(l)))
  if (sat === 0) {
    const v = clampByte(light * 255)
    return { r: v, g: v, b: v }
  }
  const q = light < 0.5 ? light * (1 + sat) : light + sat - light * sat
  const p = 2 * light - q
  const hk = hue / 360
  const channel = (t) => {
    let x = t
    if (x < 0)
      x += 1
    if (x > 1)
      x -= 1
    if (x < 1 / 6)
      return p + (q - p) * 6 * x
    if (x < 1 / 2)
      return q
    if (x < 2 / 3)
      return p + (q - p) * (2 / 3 - x) * 6
    return p
  }
  return {
    r: clampByte(channel(hk + 1 / 3) * 255),
    g: clampByte(channel(hk) * 255),
    b: clampByte(channel(hk - 1 / 3) * 255),
  }
}

function parseAlphaToken(token) {
  if (token == null || token === '')
    return 1
  const text = String(token).trim()
  if (text.endsWith('%'))
    return Math.max(0, Math.min(1, Number.parseFloat(text) / 100))
  return Math.max(0, Math.min(1, Number(text)))
}

function parseRgbColor(text) {
  const match = text.match(/^rgba?\(\s*([\d.]+)\s*[,/\s]+\s*([\d.]+)\s*[,/\s]+\s*([\d.]+)(?:\s*[,/]\s*([\d.]+%?))?\s*\)$/i)
  if (!match)
    return null
  if (parseAlphaToken(match[4]) === 0)
    return 'transparent'
  return `${toHexPair(match[1])}${toHexPair(match[2])}${toHexPair(match[3])}`
}

function parseHslColor(text) {
  // hsl(0 78% 92%) / hsl(0, 78%, 92%) / hsla(...)
  const match = text.match(/^hsla?\(\s*([\d.]+)(?:deg)?\s*[,/\s]+\s*([\d.]+)%\s*[,/\s]+\s*([\d.]+)%(?:\s*[,/]\s*([\d.]+%?))?\s*\)$/i)
  if (!match)
    return null
  if (parseAlphaToken(match[4]) === 0)
    return 'transparent'
  const { r, g, b } = hslToRgb(match[1], Number(match[2]) / 100, Number(match[3]) / 100)
  return `${toHexPair(r)}${toHexPair(g)}${toHexPair(b)}`
}

/**
 * 任意 CSS 颜色 → 6 位大写 hex（无 #）；transparent/无法解析时返回 fallback。
 * @param {unknown} value
 * @param {string} fallback 无 # 的 hex，如 'E4E4E7' 或 ''
 */
export function colorToHexInput(value, fallback = '') {
  const text = String(value ?? '').trim()
  if (!text || /^(transparent|none)$/i.test(text))
    return fallback

  const hexMatch = text.match(/^#?([0-9a-f]{3}|[0-9a-f]{6}|[0-9a-f]{8})$/i)
  if (hexMatch) {
    let hex = hexMatch[1]
    if (hex.length === 3)
      hex = expandShortHex(hex)
    else if (hex.length === 8) {
      if (Number.parseInt(hex.slice(6, 8), 16) === 0)
        return fallback
      hex = hex.slice(0, 6)
    }
    return hex.toUpperCase()
  }

  const rgb = parseRgbColor(text)
  if (rgb === 'transparent')
    return fallback
  if (rgb)
    return rgb

  const hsl = parseHslColor(text)
  if (hsl === 'transparent')
    return fallback
  if (hsl)
    return hsl

  // 浏览器兜底：var(--x) / 命名色等
  if (typeof document !== 'undefined') {
    try {
      const el = document.createElement('div')
      el.style.color = text
      document.body.appendChild(el)
      const computed = getComputedStyle(el).color
      document.body.removeChild(el)
      const fromComputed = parseRgbColor(computed)
      if (fromComputed && fromComputed !== 'transparent')
        return fromComputed
    }
    catch {
      // ignore
    }
  }

  return fallback
}

/** hex 输入（可无 #）→ #rrggbb；非法则 fallback */
export function hexInputToColor(value, fallback = '#ffffff') {
  const text = String(value || '').trim().replace(/^#/, '')
  if (!/^[0-9a-f]{3}(?:[0-9a-f]{3})?$/i.test(text))
    return fallback
  if (text.length === 3)
    return `#${expandShortHex(text)}`
  return `#${text}`
}
