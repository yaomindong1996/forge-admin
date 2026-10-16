import { defineStore } from 'pinia'
import { onScopeDispose, reactive, ref, watch } from 'vue'
import { useAiImagePermission } from '@/composables/ai/useAiImagePermission'
import { useAuthStore } from '@/store/modules/auth'
import { useUserStore } from '@/store/modules/user'
import { ImageJobQueries } from './ImageJobQueries'

// 页面与工具卡共享内存状态，计时器归查询生命周期；不持久化凭据。
export const useAiImageJobsStore = defineStore('ai-image-jobs', () => {
  const jobs = reactive(new Map())
  const contextVersion = ref(0)
  const auth = useAuthStore()
  const user = useUserStore()
  const { canQuery } = useAiImagePermission()
  const queries = new ImageJobQueries(jobs, () => canQuery.value)

  function reset() {
    queries.reset()
    contextVersion.value += 1
  }
  watch([() => auth.accessToken, () => user.userId, () => user.tenantId, canQuery], reset, { flush: 'sync' })
  onScopeDispose(reset)

  return {
    jobs,
    contextVersion,
    subscribe: queries.subscribe.bind(queries),
    resume: queries.resume.bind(queries),
  }
})
