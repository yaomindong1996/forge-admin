import { defineStore } from 'pinia'
import { reactive, shallowRef, toRaw, toRefs } from 'vue'

// 配置面板共享原有查询/表单上下文，不另建一套数据源或保存协议。
export const useDatasetWorkspaceStore = defineStore('data-dataset-workspace', () => {
  const context = shallowRef(null)
  function bind(api) {
    context.value = reactive(api)
  }
  function release(api) {
    // 只清理自己的上下文，避免路由切换时旧实例清掉新实例。
    if (context.value && toRaw(context.value) === api)
      context.value = null
  }
  return { context, bind, release }
})

export function useDatasetWorkspaceContext() {
  const store = useDatasetWorkspaceStore()
  if (!store.context)
    throw new Error('数据集工作区尚未初始化')
  // 保留 ref 与方法，避免 spread reactive 对象后丢失响应式。
  return toRefs(store.context)
}
