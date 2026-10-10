export function taskResponse(response) {
  if (response?.code !== 200 || !response.data)
    throw new Error(response?.message || '插件任务响应无效')
  return response.data
}

export function createPluginRequestId() {
  const bytes = crypto.getRandomValues(new Uint8Array(16))
  bytes[6] = (bytes[6] & 15) | 64
  bytes[8] = (bytes[8] & 63) | 128
  const hex = [...bytes].map(value => value.toString(16).padStart(2, '0')).join('')
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`
}

export function pluginTime(value) {
  if (!value)
    return '—'
  return String(value).replace('T', ' ').replace(/\.\d+Z?$/, '').replace(/Z$/, '')
}
