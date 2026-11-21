export function inferModelType(modelId) {
  if (!modelId)
    return 'chat'
  const lower = modelId.toLowerCase()
  if (lower.includes('embedding') || lower.includes('embed'))
    return 'embedding'
  if (lower.includes('rerank') || lower.includes('re-rank') || lower.includes('cross-encoder'))
    return 'rerank'
  if (lower.includes('t2v') || lower.includes('i2v')
    || lower.includes('video-gen') || lower.includes('videogen')
    || lower.includes('maku')) {
    return 'video_generation'
  }
  if (lower.includes('t2i') || lower.includes('i2i')
    || lower.includes('dall-e') || lower.includes('dalle')
    || lower.includes('imagen') || lower.includes('flux')
    || lower.includes('midjourney') || lower.includes('stable-diffusion')
    || lower.includes('sdxl') || lower.includes('cogview')
    || lower.includes('wanx')) {
    return 'image_generation'
  }
  if (lower.includes('video'))
    return 'video_understanding'
  if (lower.includes('whisper') || lower.includes('asr')
    || lower.includes('speech-to-text') || lower.includes('paraformer')
    || lower.includes('sensevoice')) {
    return 'asr'
  }
  if (lower.includes('tts') || lower.includes('speech-to-speech')
    || lower.includes('speech-synthesis') || lower.includes('cosyvoice')
    || lower.includes('sambert')) {
    return 'tts'
  }
  if (lower.includes('audio'))
    return 'audio_understanding'
  if (lower.includes('vl') || lower.includes('vision'))
    return 'vision'
  return 'chat'
}

export function createProviderForm() {
  return {
    providerName: '',
    providerType: null,
    adapterCode: 'openai_compatible',
    logo: '',
    baseUrl: '',
    apiKey: '',
    isDefault: '0',
    status: '0',
    remark: '',
  }
}

export function createModelForm(providerId = null) {
  return {
    providerId,
    modelType: null,
    modelId: '',
    modelName: '',
    icon: '',
    maxTokens: null,
    contextWindow: null,
    inputPricePerMillionCent: null,
    outputPricePerMillionCent: null,
    capabilityCodes: [],
    sortOrder: 0,
    isDefault: '0',
    status: '0',
    description: '',
  }
}

export function providerInitial(provider) {
  return (provider?.providerName || '?').trim().charAt(0).toUpperCase()
}

export function providerModelCount(provider) {
  if (Array.isArray(provider?.models))
    return provider.models.length
  if (!provider?.models)
    return 0
  try {
    const models = JSON.parse(provider.models)
    return Array.isArray(models) ? models.length : 0
  }
  catch {
    return 0
  }
}

export function formatProviderTime(value) {
  if (!value)
    return '—'
  return String(value).replace('T', ' ').slice(0, 16)
}

export function modelCapabilityPresentation(model) {
  if (model.modelType === 'embedding')
    return { icon: 'i-lucide:layers-3', tone: 'knowledge', usage: '用于知识向量化' }
  if (model.modelType === 'rerank')
    return { icon: 'i-lucide:list-filter', tone: 'ranking', usage: '用于候选重排' }
  if (['vision', 'video_understanding', 'audio_understanding'].includes(model.modelType))
    return { icon: 'i-lucide:scan-eye', tone: 'knowledge', usage: '用于多模态理解' }
  if (['asr', 'tts'].includes(model.modelType))
    return { icon: 'i-lucide:audio-lines', tone: 'knowledge', usage: '用于语音处理' }
  return {
    icon: 'i-lucide:messages-square',
    tone: 'dialogue',
    usage: model.maxTokens ? `输出上限 ${Number(model.maxTokens).toLocaleString()}` : '按服务端默认输出上限',
  }
}
