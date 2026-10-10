import { onScopeDispose, ref } from 'vue'

/** 只接受最后一次请求的结果，关闭面板/销毁页面后不再回填旧响应。 */
export function useLatestPluginRequest(loader, clearOnLoad = false) {
  const data = ref(null)
  const loading = ref(false)
  const error = ref('')
  let sequence = 0

  async function run(params) {
    const current = ++sequence
    loading.value = true
    error.value = ''
    if (clearOnLoad)
      data.value = null
    try {
      const result = await loader(params)
      if (current === sequence)
        data.value = result
    }
    catch (cause) {
      if (current !== sequence)
        return
      data.value = null
      error.value = cause?.message || '请求失败，请重试'
    }
    finally {
      if (current === sequence)
        loading.value = false
    }
  }

  function cancel() {
    ++sequence
    data.value = null
    loading.value = false
    error.value = ''
  }

  onScopeDispose(cancel)
  return { data, loading, error, run, cancel }
}
