const HINT_PATHS = ['main.name', 'main.title', 'main.code', 'main.number', 'main.no', 'main.billNo', 'main.orderNo']
const TOKEN = /\{(timestamp|template)\}|\{\{([^{}]+)\}\}/g
const PATH = /^[a-z_$][\w$]*(?:\.[a-z_$][\w$]*)*$/i
const DANGEROUS = new Set(['__proto__', 'prototype', 'constructor'])
const CONTROL_MAX = 31

export function isExportFileNamePattern(value) {
  if (typeof value !== 'string' || !value || value.length > 120 || hasForbiddenChars(value))
    return false
  let valid = true
  const rest = value.replace(/\{\{([^{}]+)\}\}|\{(timestamp|template)\}/g, (full, path) => {
    if (path && !isSafePath(path))
      valid = false
    return ''
  })
  return valid && !/[{}]/.test(rest)
}

export function buildPrintExportFileName({ pattern, templateName = '', context = {}, now = new Date() } = {}) {
  const timestamp = formatStamp(now)
  const template = sanitizeSegment(templateName) || '打印'
  const source = String(pattern || '').trim()
  const hasStamp = source.includes('{timestamp}')
  let resolved = source && isExportFileNamePattern(source)
    ? interpolate(source, { context, template, timestamp })
    : [template, businessHint(context)].filter(Boolean).join('-')
  if (!hasStamp)
    resolved = `${resolved}-${timestamp}`
  return `${sanitizeFilename(resolved) || `${template}-${timestamp}`}.pdf`
}

function interpolate(pattern, { context, template, timestamp }) {
  return pattern.replace(TOKEN, (_, token, path) => {
    if (token === 'timestamp')
      return timestamp
    if (token === 'template')
      return template
    return sanitizeSegment(readHint(context, path))
  })
}

function businessHint(context) {
  for (const path of HINT_PATHS) {
    const value = sanitizeSegment(readHint(context, path))
    if (value)
      return value
  }
  return ''
}

function isSafePath(path) {
  return typeof path === 'string' && path.length <= 300 && PATH.test(path) && path.split('.').every(segment => !DANGEROUS.has(segment))
}

function readHint(context, path) {
  if (!isSafePath(path))
    return ''
  return path.split('.').reduce((current, key) => {
    if (!current || typeof current !== 'object' || !Object.hasOwn(current, key))
      return null
    const value = current[key]
    return value == null ? null : value
  }, context) ?? ''
}

function formatStamp(date) {
  const pad = value => String(value).padStart(2, '0')
  return `${date.getFullYear()}${pad(date.getMonth() + 1)}${pad(date.getDate())}${pad(date.getHours())}${pad(date.getMinutes())}${pad(date.getSeconds())}`
}

function hasForbiddenChars(value) {
  for (const char of value) {
    const code = char.charCodeAt(0)
    if (code <= CONTROL_MAX || char === '\\' || char === '/')
      return true
  }
  return false
}

function sanitizeSegment(value) {
  return [...String(value || '')]
    .filter(char => char.charCodeAt(0) > CONTROL_MAX && char !== '\\' && char !== '/')
    .join('')
    .replace(/[<>:"|?*]+/g, '')
    .replace(/\s+/g, ' ')
    .trim()
}

function sanitizeFilename(value) {
  return sanitizeSegment(value).replace(/\.pdf$/i, '').replace(/[.\s]+$/g, '').slice(0, 120)
}
