import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const srcDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..')

// 审批详情页拆分后的源码集合；顺序与页面模板自上而下一致，跨文件断言依赖该顺序。
export const TODO_DETAIL_FILES = Object.freeze([
  'pages/todo-detail.vue',
  'components/flow/FlowBusinessFormPanel.vue',
  'components/flow/TodoSignRelations.vue',
  'components/flow/TodoActionBar.vue',
  'components/flow/TodoRemindBar.vue',
  'components/flow/TodoMoreActionSheet.vue',
  'components/flow/TodoDelegateSheet.vue',
  'components/flow/TodoSignSheet.vue',
  'components/flow/FlowUserPicker.vue',
  'composables/flow/useFlowBusinessForm.js',
  'composables/flow/useTodoDetailLoader.js',
  'composables/flow/useTodoTaskActions.js',
  'composables/flow/useTodoSignActions.js',
  'composables/flow/useFlowRemind.js',
  'store/modules/todoDetail.js',
])

export function readTodoDetailSource() {
  return TODO_DETAIL_FILES.map(file => fs.readFileSync(path.join(srcDir, file), 'utf8')).join('\n')
}

export function readTodoDetailStyles() {
  return [fs.readFileSync(path.join(srcDir, 'pages/styles/todo-detail.scss'), 'utf8'), readTodoDetailSource()].join('\n')
}
