import { onScopeDispose, ref, watch } from 'vue'

/** 仅在内存保留管理员诊断；同步清理防止账号切换后旧请求 ABA 回填。 */
export function createRuntimeLicenseState(api, authorize, identity) {
  const data = ref(null)
  const loading = ref(false)
  const error = ref('')
  let sequence = 0
  const clear = () => {
    ++sequence
    data.value = null
    error.value = ''
    loading.value = false
  }
  watch(() => [identity(), authorize()], clear, { flush: 'sync' })
  onScopeDispose(clear)
  async function load() {
    if (!authorize()) {
      clear()
      return
    }
    const current = ++sequence
    data.value = null
    error.value = ''
    loading.value = true
    try {
      const response = await api()
      if (current !== sequence || !authorize())
        return
      if (response?.code !== 200 || !response.data?.mode)
        throw new Error(response?.message || '加载运行时授权失败')
      data.value = response.data
    }
    catch (cause) {
      if (current === sequence)
        error.value = cause?.message || '加载运行时授权失败'
    }
    finally {
      if (current === sequence)
        loading.value = false
    }
  }
  return { data, loading, error, load, clear }
}
