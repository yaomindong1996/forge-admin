import { onBeforeUnmount, ref, watch } from 'vue'
import { modelListByProvider } from '@/api/ai/model-routing'
import { imageRecordId } from '@/stores/ai/image-job-utils'

// 模型读取与提交分离；响应只属于发起时的账号上下文。
export function useImageModelOptions(allowed, contextVersion) {
  const modelOptions = ref([])
  const modelError = ref('')
  let alive = true
  onBeforeUnmount(() => {
    alive = false
  })

  async function loadModels() {
    modelOptions.value = []
    modelError.value = ''
    if (!allowed.value) {
      return
    }
    const version = contextVersion()
    try {
      const response = await modelListByProvider()
      if (response?.code !== 200 || !Array.isArray(response.data)) {
        throw new Error('图片模型响应无效')
      }
      if (alive && version === contextVersion()) {
        modelOptions.value = response.data
          .filter(model => ['image_generation', 'image'].includes(model.modelType) && imageRecordId(model.id))
          .map(model => ({ label: model.modelName || model.modelId, value: imageRecordId(model.id) }))
      }
    }
    catch {
      if (alive && version === contextVersion()) {
        modelError.value = '加载图片模型失败，请刷新页面后重试'
      }
    }
  }
  watch([allowed, contextVersion], loadModels, { immediate: true })
  return { modelOptions, modelError }
}
