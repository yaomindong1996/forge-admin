import { computed, onActivated, onBeforeUnmount, onDeactivated, ref, toValue, watch } from 'vue'
import { imageRecordId } from '@/stores/ai/image-job-utils'
import { useAiImageJobsStore } from '@/stores/ai/imageJobsStore'
import { useAiImagePermission } from './useAiImagePermission'

export function useImageJob(recordId) {
  const store = useAiImageJobsStore()
  const { canQuery } = useAiImagePermission()
  const active = ref(true)
  const subscribeError = ref('')
  const id = computed(() => imageRecordId(toValue(recordId)))
  const job = computed(() => store.jobs.get(id.value))
  let unsubscribe = () => {}

  const stopWatch = watch([id, canQuery, active, () => store.contextVersion], () => {
    unsubscribe()
    subscribeError.value = id.value ? '' : '图片任务标识无效，请刷新后重试'
    if (!active.value) {
      return
    }
    try {
      unsubscribe = store.subscribe(id.value)
    }
    catch (error) {
      subscribeError.value = error.message
    }
  }, { immediate: true })

  onActivated(() => {
    active.value = true
  })
  onDeactivated(() => {
    active.value = false
    unsubscribe()
  })
  onBeforeUnmount(() => {
    stopWatch()
    unsubscribe()
  })

  return { job, canQuery, subscribeError, resume: () => store.resume(id.value) }
}
