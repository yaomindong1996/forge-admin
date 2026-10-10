import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { localBuildInfo, versionResponse, versionWarning } from '../../../../scripts/forge-shared/version-view.mjs'
import { getSystemVersion } from '@/api/system-version'

export const useSystemVersionStore = defineStore('system-version', () => {
  const visible = ref(false)
  const loading = ref(false)
  const error = ref('')
  const backend = ref(null)
  const local = ref(localBuildInfo())
  const warning = computed(() => versionWarning(local.value, backend.value))
  let sequence = 0

  async function refresh() {
    if (loading.value) return
    const request = ++sequence
    loading.value = true
    error.value = ''
    backend.value = null
    try {
      const result = versionResponse(await getSystemVersion())
      if (request === sequence) backend.value = result
    }
    catch {
      if (request === sequence) error.value = '暂时无法获取后端版本，请重试或联系管理员确认服务已升级。'
    }
    finally {
      if (request === sequence) loading.value = false
    }
  }

  function open() {
    visible.value = true
    return refresh()
  }

  function close() {
    ++sequence
    visible.value = false
    loading.value = false
    backend.value = null
    error.value = ''
  }

  return { visible, loading, error, backend, local, warning, open, close, refresh }
})
