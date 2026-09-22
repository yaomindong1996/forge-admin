/**
 * 打印模块随机 ID。优先 Web Crypto，避免裸用 crypto.randomUUID 在
 * HTTP / 旧环境 / 被 Node polyfill 覆盖时抛错。
 */
export function createPrintRandomToken() {
  const uuid = globalThis.crypto?.randomUUID?.()
  if (typeof uuid === 'string' && uuid)
    return uuid.replaceAll('-', '')
  if (typeof globalThis.crypto?.getRandomValues === 'function') {
    const bytes = new Uint8Array(16)
    globalThis.crypto.getRandomValues(bytes)
    return Array.from(bytes, byte => byte.toString(16).padStart(2, '0')).join('')
  }
  return `${Date.now().toString(16)}${Math.random().toString(16).slice(2)}${Math.random().toString(16).slice(2)}`
    .replace(/\./g, '')
    .slice(0, 32)
}

export function newPrintId() {
  return `p_${createPrintRandomToken()}`
}

export function newPrintTemplateCode() {
  return `print_${createPrintRandomToken()}`
}
