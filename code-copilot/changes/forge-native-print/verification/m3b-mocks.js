import { useDictStore } from '@/stores/system/dictStore'
import { computed, reactive } from 'vue'
export const user = reactive({ getDataPermission: ['print:template:view', 'print:template:manage', 'print:template:publish', 'print:execute'] })
export const useUserStore = () => user
const all = {
  sys_print_design_status: [{ value: 'DRAFT', label: '草稿' }, { value: 'PUBLISHED', label: '已发布' }, { value: 'CHANGED', label: '发布后修改' }],
  sys_print_source_type: [{ value: 'LOWCODE', label: '低代码表单' }, { value: 'CODE', label: '代码业务表单' }],
  sys_normal_disable: [{ value: '1', label: '正常' }, { value: '0', label: '停用' }],
  sys_print_scene: [{ value: 'LIST', label: '列表' }, { value: 'DETAIL', label: '详情' }, { value: 'FLOW_TODO', label: '待办' }, { value: 'FLOW_DONE', label: '已办' }, { value: 'FLOW_STARTED', label: '我发起' }],
}
export const useDict = () => ({ dict: computed(() => all) })

export async function getDictData(type) { const data = all[type] || []; useDictStore().dictCache.set(type, data); return data }
