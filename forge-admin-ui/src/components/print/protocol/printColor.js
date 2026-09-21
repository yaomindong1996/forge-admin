const COLOR_KEYS = ['color', 'backgroundColor', 'borderColor']

function hexPair(n) {
  return Math.max(0, Math.min(255, Math.round(Number(n)))).toString(16).padStart(2, '0')
}

function expandShortHex(raw) {
  return `#${raw[1]}${raw[1]}${raw[2]}${raw[2]}${raw[3]}${raw[3]}`.toLowerCase()
}

function alphaIsZero(hex) {
  return Number.parseInt(hex, 16) === 0
}

/** Convert picker CSS colors to protocol `#rrggbb` or `transparent`. */
export function toPrintColor(value) {
  if (value == null || typeof value !== 'string')
    return value
  const raw = value.trim()
  if (!raw || raw.toLowerCase() === 'none' || raw.toLowerCase() === 'transparent')
    return 'transparent'
  if (/^#[\da-f]{3}$/i.test(raw))
    return expandShortHex(raw)
  if (/^#[\da-f]{4}$/i.test(raw))
    return alphaIsZero(raw[4]) ? 'transparent' : expandShortHex(raw)
  if (/^#[\da-f]{6}$/i.test(raw))
    return raw.toLowerCase()
  if (/^#[\da-f]{8}$/i.test(raw))
    return alphaIsZero(raw.slice(7, 9)) ? 'transparent' : `#${raw.slice(1, 7)}`.toLowerCase()
  const rgb = raw.match(/^rgba?\(\s*([\d.]+)[\s,/]+([\d.]+)[\s,/]+([\d.]+)(?:[\s,/]+([\d.]+%?))?/i)
  if (rgb) {
    if (rgb[4] != null) {
      const token = rgb[4]
      const alpha = String(token).endsWith('%') ? Number.parseFloat(token) / 100 : Number(token)
      if (alpha === 0)
        return 'transparent'
    }
    return `#${hexPair(rgb[1])}${hexPair(rgb[2])}${hexPair(rgb[3])}`
  }
  return raw
}

export function isPrintColor(value) {
  if (typeof value !== 'string' || !value.trim())
    return false
  const next = toPrintColor(value)
  return next === 'transparent' || /^#[\da-f]{6}$/i.test(next)
}

export function normalizeStyleColors(style) {
  if (!style || typeof style !== 'object' || Array.isArray(style))
    return style
  const next = { ...style }
  for (const key of COLOR_KEYS) {
    if (typeof next[key] === 'string')
      next[key] = toPrintColor(next[key])
  }
  return next
}

export function sanitizePrintColors(node) {
  if (!node || typeof node !== 'object')
    return node
  if (Array.isArray(node)) {
    node.forEach(sanitizePrintColors)
    return node
  }
  for (const [key, value] of Object.entries(node)) {
    if (COLOR_KEYS.includes(key) && typeof value === 'string')
      node[key] = toPrintColor(value)
    else
      sanitizePrintColors(value)
  }
  return node
}
