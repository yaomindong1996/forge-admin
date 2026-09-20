export function compactObject(source = {}) {
  return Object.fromEntries(
    Object.entries(source).filter(([, value]) => value !== undefined && value !== null && value !== ''),
  )
}

export function parseNestedJson(value, fallback = []) {
  if (!value || typeof value === 'object') return value || fallback
  try {
    const parsed = JSON.parse(value)
    return typeof parsed === 'string' ? parseNestedJson(parsed, fallback) : parsed
  }
  catch {
    return fallback
  }
}

export function resolveApiErrorMessage(error, fallback) {
  const message = error?.data?.message
    || error?.response?.data?.message
    || error?.error?.data?.message
    || error?.message
    || error?.msg
  return message && String(message).trim() ? String(message) : fallback
}
