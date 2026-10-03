<template>
  <div class="dataset-editor-page">
    <!-- 标题与主操作：保存仍交由 AiCrudPage 校验和提交 -->
    <header class="dataset-editor-header">
      <div class="dataset-editor-heading">
        <h2>{{ formData.datasetName || '新建数据集' }}</h2>
        <span>{{ formModeLabel }} · {{ getDatasetTypeLabel(formData.datasetType) }}</span>
      </div>
      <div class="dataset-editor-actions">
        <NButton @click="crudRef?.closeModal()">
          取消
        </NButton>
        <NButton
          v-if="formData.datasetType === 'SQL'"
          :loading="sqlPreviewLoading"
          @click="handlePreviewSql(formData, false)"
        >
          预览 SQL
        </NButton>
        <NButton type="primary" :disabled="isFormReadOnly" @click="crudRef?.submitForm()">
          保存
        </NButton>
      </div>
    </header>

    <!-- 分步配置降低初始密度，面板不销毁以保留输入与编辑器状态 -->
    <NTabs :value="currentStep" type="segment" :animated="false" @update:value="setEditorStep($event)">
      <NTabPane :name="1" tab="1. 基础与来源" display-directive="show">
        <DatasetSourcePanel :form-data="formData" :update-value="updateValue" />
      </NTabPane>
      <NTabPane :name="2" tab="2. 查询条件" display-directive="show">
        <DatasetQueryPanel :form-data="formData" :update-value="updateValue" />
      </NTabPane>
      <NTabPane :name="3" tab="3. 执行设置" display-directive="show">
        <DatasetRuntimePanel :form-data="formData" :update-value="updateValue" />
      </NTabPane>
      <NTabPane :name="4" tab="4. 权限控制" display-directive="show">
        <DatasetAccessPanel :form-data="formData" :update-value="updateValue" />
      </NTabPane>
    </NTabs>
    <footer class="dataset-editor-footer">
      <span>{{ currentStepMeta.description }}</span>
      <div>
        <NButton v-if="currentStep > 1" @click="goToPrevStep">
          上一步
        </NButton>
        <NButton v-if="currentStep < totalSteps" type="primary" secondary @click="goToNextStep(formData)">
          下一步
        </NButton>
      </div>
    </footer>
  </div>
</template>

<script>
import { NButton, NTabPane, NTabs } from 'naive-ui'
import { useDatasetWorkspaceContext } from '@/stores/data/datasetWorkspaceStore'
import DatasetAccessPanel from './DatasetAccessPanel.vue'
import DatasetQueryPanel from './DatasetQueryPanel.vue'
import DatasetRuntimePanel from './DatasetRuntimePanel.vue'
import DatasetSourcePanel from './DatasetSourcePanel.vue'

export default {
  components: { NTabs, NTabPane, NButton, DatasetSourcePanel, DatasetQueryPanel, DatasetRuntimePanel, DatasetAccessPanel },
  props: { formData: { type: Object, required: true }, updateValue: { type: Function, required: true } },
  setup() { return useDatasetWorkspaceContext() },
}
</script>

<style scoped src="../dataset-editor.css"></style>
