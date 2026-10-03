<template>
  <!-- 独立配置面板，共享状态由 Pinia 提供 -->
  <section class="dataset-edit-panel dataset-edit-panel--params" data-step-section="2">
    <div class="panel-section-head panel-section-head--inline">
      <h3>查询参数定义</h3>
      <n-popover trigger="click" placement="bottom-end" :width="320">
        <template #trigger>
          <button class="panel-inline-indicator panel-inline-indicator--button" type="button">
            参数预览
          </button>
        </template>
        <div class="param-preview-popover">
          <div v-if="getParamPreviewRows(formData).length" class="param-preview-list">
            <div
              v-for="(param, index) in getParamPreviewRows(formData)"
              :key="`${param.paramName || param.label || param.fieldName}-${index}`"
              class="param-preview-row"
            >
              <strong>{{ param.paramName || '-' }}</strong>
              <span>{{ getParamPreviewDescription(param, formData.datasetType) }}</span>
            </div>
          </div>
          <n-empty v-else size="small" description="暂无查询参数" />
        </div>
      </n-popover>
    </div>
    <DatasetParamSchemaEditor
      :model-value="formData.paramSchemaJson || []"
      :readonly="isFormReadOnly"
      :dataset-type="formData.datasetType"
      :connection-id="formData.connectionId"
      :table-name="formData.tableName"
      :sql-text="formData.sqlText"
      @update:model-value="value => updateDatasetFormField(formData, 'paramSchemaJson', value, updateValue)"
    />
  </section>
</template>

<script>
import { useDatasetWorkspaceContext } from '@/stores/data/datasetWorkspaceStore'
import { datasetLocalComponents } from '../datasetLocalComponents'

export default {
  components: { ...datasetLocalComponents },
  props: { formData: { type: Object, required: true }, updateValue: { type: Function, required: true } },
  setup() { return useDatasetWorkspaceContext() },
}
</script>

<style scoped src="../dataset-editor.css"></style>
