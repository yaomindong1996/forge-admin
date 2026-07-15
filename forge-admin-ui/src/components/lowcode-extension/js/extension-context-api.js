const MAX_SCRIPT_BYTES = 128 * 1024
const DEFAULT_MAX_OUTPUT_BYTES = 64 * 1024
const SAFE_EFFECT_TYPES = new Set(['SET_FIELD', 'SHOW_MESSAGE', 'TRIGGER_ACTION'])
const RESERVED_KEYS = new Set(['__proto__', 'prototype', 'constructor'])
const SENSITIVE_KEY_PATTERN = /token|secret|password|cookie|authorization|api[_-]?key|session/i

const forbiddenPatterns = [
  ['window', /\bwindow\b/],
  ['document', /\bdocument\b/],
  ['globalThis', /\bglobalThis\b/],
  ['self', /\bself\b/],
  ['location', /\blocation\b/],
  ['navigator', /\bnavigator\b/],
  ['cookie', /\bcookie\b/],
  ['localStorage', /\blocalStorage\b/],
  ['sessionStorage', /\bsessionStorage\b/],
  ['indexedDB', /\bindexedDB\b/],
  ['caches', /\bcaches\b/],
  ['fetch', /\bfetch\s*\(/],
  ['XMLHttpRequest', /\bXMLHttpRequest\b/],
  ['WebSocket', /\bWebSocket\b/],
  ['WebTransport', /\bWebTransport\b/],
  ['EventSource', /\bEventSource\b/],
  ['BroadcastChannel', /\bBroadcastChannel\b/],
  ['MessageChannel', /\bMessageChannel\b/],
  ['Worker', /\b(?:Shared)?Worker\b/],
  ['importScripts', /\bimportScripts\b/],
  ['postMessage', /\bpostMessage\b/],
  ['dynamic import', /\bimport\s*\(/],
  ['eval', /\beval\s*\(/],
  ['Function', /\b(?:new\s+)?Function\s*\(/],
  ['constructor', /\bconstructor\b/],
  ['__proto__', /__proto__/],
  ['prototype', /\.prototype\b/],
  ['setTimeout', /\bsetTimeout\b/],
  ['setInterval', /\bsetInterval\b/],
  ['WebAssembly', /\bWebAssembly\b/],
  ['SharedArrayBuffer', /\bSharedArrayBuffer\b/],
  ['Atomics', /\bAtomics\b/],
]

export function validateClientScript(source) {
  const script = String(source || '')
  if (!script.trim())
    throw new Error('客户端脚本不能为空')
  if (new TextEncoder().encode(script).byteLength > MAX_SCRIPT_BYTES)
    throw new Error('客户端脚本超过128KB限制')
  if (script.includes('`'))
    throw new Error('客户端脚本禁止使用模板字符串')
  if (/\\(?:u[\da-f]{4}|x[\da-f]{2})/i.test(script))
    throw new Error('客户端脚本禁止使用转义标识符')

  const code = stripCommentsAndStrings(script)
  for (const [name, pattern] of forbiddenPatterns) {
    if (pattern.test(code))
      throw new Error(`客户端脚本禁止访问 ${name}`)
  }
  return { valid: true }
}

export function sanitizeExtensionContext(context = {}, allowedFields = []) {
  const record = isPlainObject(context.record) ? context.record : {}
  const safeFields = normalizeCodes(allowedFields).filter(field => !SENSITIVE_KEY_PATTERN.test(field))
  const fields = Object.create(null)

  for (const field of safeFields) {
    if (!Object.prototype.hasOwnProperty.call(record, field))
      continue
    fields[field] = cloneSafeValue(record[field])
  }

  return {
    recordId: cloneSafeValue(record.id ?? context.recordId ?? null),
    fields: { ...fields },
    allowedFields: safeFields,
    allowedActions: normalizeCodes(context.allowedActions || []),
  }
}

export function validateSandboxResult(result, maxOutputBytes = DEFAULT_MAX_OUTPUT_BYTES) {
  if (!isPlainObject(result) || !Array.isArray(result.effects))
    throw new Error('沙箱结果协议不正确')

  for (const effect of result.effects) {
    if (!isPlainObject(effect) || !SAFE_EFFECT_TYPES.has(effect.type))
      throw new Error('沙箱结果协议包含未授权 effect')
    assertNoReservedKeys(effect)
  }

  const serialized = JSON.stringify(result)
  if (new TextEncoder().encode(serialized).byteLength > maxOutputBytes)
    throw new Error('沙箱输出超过大小限制')
  return cloneSafeValue(result)
}

function stripCommentsAndStrings(source) {
  let result = ''
  let mode = 'code'
  let quote = ''

  for (let index = 0; index < source.length; index += 1) {
    const current = source[index]
    const next = source[index + 1]

    if (mode === 'line-comment') {
      if (current === '\n') {
        mode = 'code'
        result += '\n'
      }
      else {
        result += ' '
      }
      continue
    }

    if (mode === 'block-comment') {
      if (current === '*' && next === '/') {
        result += '  '
        index += 1
        mode = 'code'
      }
      else {
        result += ' '
      }
      continue
    }

    if (mode === 'string') {
      if (current === '\\') {
        result += '  '
        index += 1
      }
      else if (current === quote) {
        result += ' '
        mode = 'code'
      }
      else {
        result += ' '
      }
      continue
    }

    if (current === '/' && next === '/') {
      result += '  '
      index += 1
      mode = 'line-comment'
    }
    else if (current === '/' && next === '*') {
      result += '  '
      index += 1
      mode = 'block-comment'
    }
    else if (current === '\'' || current === '"' || current === '`') {
      result += ' '
      mode = 'string'
      quote = current
    }
    else {
      result += current
    }
  }

  return result
}

function normalizeCodes(values) {
  if (!Array.isArray(values))
    return []
  return [...new Set(values
    .map(value => String(value || '').trim())
    .filter(value => /^[a-z]\w{0,63}$/i.test(value))
    .filter(value => !RESERVED_KEYS.has(value)))]
}

function cloneSafeValue(value, depth = 0) {
  if (depth > 8)
    throw new Error('扩展上下文嵌套过深')
  if (value == null || typeof value === 'string' || typeof value === 'boolean')
    return value
  if (typeof value === 'number')
    return Number.isFinite(value) ? value : null
  if (Array.isArray(value))
    return value.slice(0, 200).map(item => cloneSafeValue(item, depth + 1))
  if (!isPlainObject(value))
    return null

  const result = Object.create(null)
  for (const [key, item] of Object.entries(value).slice(0, 200)) {
    if (RESERVED_KEYS.has(key) || SENSITIVE_KEY_PATTERN.test(key))
      continue
    result[key] = cloneSafeValue(item, depth + 1)
  }
  return { ...result }
}

function assertNoReservedKeys(value, depth = 0) {
  if (depth > 8)
    throw new Error('沙箱结果协议嵌套过深')
  if (Array.isArray(value)) {
    value.forEach(item => assertNoReservedKeys(item, depth + 1))
    return
  }
  if (!isPlainObject(value))
    return
  for (const [key, item] of Object.entries(value)) {
    if (RESERVED_KEYS.has(key))
      throw new Error('沙箱结果协议包含保留字段')
    assertNoReservedKeys(item, depth + 1)
  }
}

function isPlainObject(value) {
  if (value == null || Object.prototype.toString.call(value) !== '[object Object]')
    return false
  const prototype = Object.getPrototypeOf(value)
  return prototype === Object.prototype || prototype === null
}
