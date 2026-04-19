/**
 * AI Form 组件导出入口
 */

import AiCrudPage from './AiCrudPage.vue'
import AiCustomSelect from './AiCustomSelect.vue'
import AiForm from './AiForm.vue'
import AiFormItem from './AiFormItem.vue'
import AiSearch from './AiSearch.vue'
import AiTable from './AiTable.vue'
import AiTableFilter from './AiTableFilter.vue'
import AiToolbarAction from './AiToolbarAction.vue'
import { createField, FIELD_TYPES, FieldFactory } from './config.js'
import * as SchemaHelper from './schemaHelper.js'

// 导出组件
export {
  AiCrudPage,
  AiCustomSelect,
  AiForm,
  AiFormItem,
  AiSearch,
  AiTable,
  AiTableFilter,
  AiToolbarAction,
}

// 导出配置
export { createField, FIELD_TYPES, FieldFactory }

// 导出工具函数
export { SchemaHelper }

// 默认导出
export default AiForm
