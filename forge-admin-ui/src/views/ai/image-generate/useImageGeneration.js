import { onBeforeUnmount, ref, watch } from 'vue'
import { imageGenerate } from '@/api/ai/media'
import { useAiImagePermission } from '@/composables/ai/useAiImagePermission'
import { imageRecordId } from '@/stores/ai/image-job-utils'
import { useAiImageJobsStore } from '@/stores/ai/imageJobsStore'
import { useImageModelOptions } from './useImageModelOptions'

const emptyForm = () => ({ prompt: '', negativePrompt: '', size: '1024x1024', modelId: null })

export function useImageGeneration(message) {
  const store = useAiImageJobsStore()
  const { canQuery, canHistory } = useAiImagePermission()
  const form = ref(emptyForm())
  const { modelOptions, modelError } = useImageModelOptions(canQuery, () => store.contextVersion)
  const submitting = ref(false)
  const recordId = ref('')
  const refreshKey = ref(0)
  let alive = true
  let submitVersion = 0

  watch(() => store.contextVersion, () => {
    submitVersion += 1
    form.value = emptyForm()
    recordId.value = ''
    submitting.value = false
  })
  onBeforeUnmount(() => {
    alive = false
  })

  async function generate() {
    if (submitting.value || !canQuery.value) {
      return
    }
    if (!form.value.prompt.trim() || !imageRecordId(form.value.modelId)) {
      message.warning('请填写提示词并选择图片模型')
      return
    }
    const version = ++submitVersion
    submitting.value = true
    try {
      const response = await imageGenerate({ ...form.value, prompt: form.value.prompt.trim() })
      if (!alive || version !== submitVersion) {
        return
      }
      const id = imageRecordId(response?.data)
      if (response?.code !== 200 || !id) {
        throw new Error('任务提交响应无效')
      }
      recordId.value = id
      refreshKey.value += 1
    }
    catch {
      if (alive && version === submitVersion) {
        message.error('图片任务提交未确认，请查看生成历史；不会自动重复提交')
      }
    }
    finally {
      if (version === submitVersion) {
        submitting.value = false
      }
    }
  }

  return { form, modelOptions, modelError, submitting, recordId, refreshKey, canQuery, canHistory, generate }
}
