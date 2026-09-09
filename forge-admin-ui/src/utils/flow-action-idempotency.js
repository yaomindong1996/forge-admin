/** 为流程写操作生成可重试的幂等凭证。 */
export async function createFlowActionCredentials(action, taskId, payload = {}) {
  const normalizedAction = String(action || 'action').trim() || 'action'
  const normalizedTaskId = String(taskId || '').trim()
  const raw = `${normalizedAction}|${normalizedTaskId}|${JSON.stringify(payload)}`
  let digest
  if (globalThis.crypto?.subtle && globalThis.TextEncoder) {
    const buffer = await globalThis.crypto.subtle.digest('SHA-256', new TextEncoder().encode(raw))
    digest = Array.from(new Uint8Array(buffer), byte => byte.toString(16).padStart(2, '0')).join('')
  }
  else {
    let hash = 2166136261
    for (let index = 0; index < raw.length; index += 1) {
      hash ^= raw.charCodeAt(index)
      hash = Math.imul(hash, 16777619)
    }
    digest = `fnv1a:${(hash >>> 0).toString(16).padStart(8, '0')}:${raw.length}`
  }
  const nonce = globalThis.crypto?.randomUUID?.() || `${Date.now()}-${Math.random().toString(16).slice(2)}`
  return {
    idempotencyKey: `flow:${normalizedAction}:${normalizedTaskId}:${nonce}`.slice(0, 128),
    requestDigest: digest.slice(0, 71),
  }
}
