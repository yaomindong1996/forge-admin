const MAX_LONG_ID = '9223372036854775807'
const IMAGE_STATUSES = new Set(['pending', 'generating', 'success', 'failed'])

// 不接收已经丢精度的 Number，也不把任务 ID 或任意 endpoint 当作文件地址。
export function imageRecordId(value) {
  if (typeof value === 'number' && !Number.isSafeInteger(value)) {
    return ''
  }
  const id = String(value ?? '')
  if (!/^[1-9]\d{0,18}$/.test(id) || (id.length === 19 && id > MAX_LONG_ID)) {
    return ''
  }
  return id
}

export function isImageJobTerminal(result) {
  return result?.status === 'success' || result?.status === 'failed'
}

export function imageJobResult(value, expectedId) {
  if (!value || imageRecordId(value.id) !== expectedId || !IMAGE_STATUSES.has(value.status)) {
    throw new Error('图片任务响应无效，请稍后重新查询')
  }
  const resultFileId = value.status === 'success' ? imageRecordId(value.resultFileId) : ''
  if (value.status === 'success' && !resultFileId) {
    throw new Error('图片任务缺少有效结果文件，请稍后重新查询')
  }
  return {
    id: expectedId,
    status: value.status,
    resultFileId,
    errorMsg: value.status === 'failed' ? String(value.errorMsg || '图片生成失败').slice(0, 1000) : '',
  }
}

export function imageToolTask(toolCall) {
  if (toolCall?.name !== 'image_generate' || !['done', 'success'].includes(toolCall.status)) {
    return null
  }
  try {
    const raw = toolCall.result
    if (typeof raw === 'string' && raw.length > 8192) {
      return null
    }
    const value = typeof raw === 'string' ? JSON.parse(raw) : raw
    const recordId = imageRecordId(value?.recordId)
    if (!recordId || value.status !== 'pending') {
      return null
    }
    if (value.resultEndpoint && value.resultEndpoint !== `/ai/image-generate/${recordId}`) {
      return null
    }
    return { recordId }
  }
  catch {
    return null
  }
}
