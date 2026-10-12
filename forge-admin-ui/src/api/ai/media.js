import { request } from '@/utils/request'

export function imageGenerate(data) {
  return request.post('/ai/image-generate', data)
}

export function imageGeneratePage(params, config = {}) {
  return request.get('/ai/image-generate/page', { ...config, params })
}

export function imageGenerateGetResult(id, config = {}) {
  return request.get(`/ai/image-generate/${encodeURIComponent(id)}`, config)
}

export function voiceAsr(formData) {
  return request.post('/ai/voice/asr', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function voiceTts(data) {
  return request.post('/ai/voice/tts', data)
}
