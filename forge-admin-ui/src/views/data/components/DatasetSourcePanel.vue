<template>
  <!-- 独立配置面板，共享状态由 Pinia 提供 -->
  <section class="dataset-edit-panel dataset-edit-panel--basic" data-step-section="1">
    <div class="panel-section-head">
      <h3>基础信息</h3>
    </div>

    <div class="dataset-form-grid">
      <label class="dataset-field dataset-field--required">
        <span>数据集名称</span>
        <NInput
          :value="formData.datasetName"
          :disabled="isFormReadOnly"
          maxlength="64"
          show-count
          placeholder="请输入数据集名称"
          @update:value="value => updateDatasetFormField(formData, 'datasetName', value, updateValue)"
        />
      </label>
      <label class="dataset-field dataset-field--required">
        <span>数据集编码</span>
        <NInput
          :value="formData.datasetCode"
          :disabled="isFormReadOnly"
          placeholder="请输入数据集编码"
          @update:value="value => updateDatasetFormField(formData, 'datasetCode', value, updateValue)"
        />
      </label>
      <label class="dataset-field">
        <span>所属目录</span>
        <n-tree-select
          :value="formData.categoryId"
          :options="categoryTreeSelectOptions"
          :disabled="isFormReadOnly"
          clearable
          placeholder="请选择业务分类"
          @update:value="value => updateDatasetFormField(formData, 'categoryId', value, updateValue)"
        />
      </label>
      <label class="dataset-field dataset-field--required">
        <span>数据源</span>
        <div class="data-source-select">
          <i class="data-source-status-dot" />
          <NSelect
            :value="formData.connectionId"
            :options="connectionOptions"
            :disabled="isFormReadOnly"
            filterable
            clearable
            placeholder="请选择数据连接"
            @update:value="value => handleConnectionChange(value, formData, updateValue)"
          />
        </div>
      </label>
      <div class="dataset-field dataset-field--required">
        <span>数据集类型</span>
        <div class="dataset-type-segment" :class="{ 'is-disabled': isFormReadOnly }">
          <button
            v-for="option in datasetTypeOptions"
            :key="option.value"
            class="dataset-type-option"
            :class="{ 'is-active': formData.datasetType === option.value }"
            type="button"
            :disabled="isFormReadOnly"
            @mousedown.stop.prevent
            @click.stop.prevent="handleDatasetTypeChange(option.value, formData, updateValue)"
          >
            {{ option.label }}
          </button>
        </div>
      </div>
      <label class="dataset-field">
        <span>可用状态</span>
        <n-radio-group
          :value="formData.status"
          :disabled="isFormReadOnly"
          @update:value="value => updateDatasetFormField(formData, 'status', value, updateValue)"
        >
          <n-radio-button
            v-for="option in statusOptions"
            :key="option.value"
            :value="option.value"
          >
            {{ option.label }}
          </n-radio-button>
        </n-radio-group>
      </label>
      <label v-if="formData.datasetType === 'TABLE'" class="dataset-field dataset-field--wide dataset-field--required">
        <span>数据表</span>
        <NSelect
          :value="formData.tableName"
          :options="tableOptions"
          :loading="tableLoading"
          :disabled="isFormReadOnly"
          filterable
          clearable
          placeholder="请先选择数据连接，再选择数据表"
          @update:value="value => handleTableNameChange(value, formData, updateValue)"
        />
      </label>
      <label class="dataset-field dataset-field--wide">
        <span>描述</span>
        <NInput
          :value="formData.description"
          type="textarea"
          :disabled="isFormReadOnly"
          :autosize="{ minRows: 3, maxRows: 5 }"
          maxlength="200"
          show-count
          placeholder="请输入数据集描述"
          @update:value="value => updateDatasetFormField(formData, 'description', value, updateValue)"
        />
      </label>
      <div class="dataset-field dataset-field--wide">
        <span>标签</span>
        <div class="dataset-tag-list">
          <span
            v-for="tag in getDatasetTagLabels(formData)"
            :key="tag"
            class="dataset-soft-tag"
          >
            {{ tag }}
          </span>
        </div>
      </div>
    </div>
  </section>

  <section
    class="dataset-edit-panel dataset-edit-panel--sql"
    :class="{ 'is-table-mode': formData.datasetType !== 'SQL' }"
  >
    <div class="panel-section-head">
      <h3>{{ formData.datasetType === 'SQL' ? 'SQL编辑器 + 预览结果' : '来源表结构' }}</h3>
    </div>

    <div v-if="formData.datasetType === 'SQL'" class="sql-workbench">
      <div class="sql-editor-shell">
        <SqlEditor
          class="dataset-sql-editor"
          :value="formData.sqlText"
          :readonly="isFormReadOnly"
          theme="light"
          show-fullscreen
          placeholder="SELECT order_id, order_time, customer_name, amount, status FROM orders WHERE order_time >= :start_time AND order_time < :end_time AND status = :status LIMIT :limit"
          @update:value="value => updateDatasetFormField(formData, 'sqlText', value, updateValue)"
        />
      </div>
      <div class="sql-preview-shell">
        <div class="sql-workbench-toolbar">
          <span>预览结果（前5行）</span>
          <n-button
            size="small"
            :loading="sqlPreviewLoading"
            :disabled="isFormReadOnly"
            @click="handlePreviewSql(formData, false)"
          >
            刷新预览
          </n-button>
        </div>
        <n-data-table
          v-if="sqlPreviewColumns.length"
          size="small"
          :columns="sqlPreviewColumns"
          :data="sqlPreviewRows"
          :loading="sqlPreviewLoading"
          :pagination="false"
          :scroll-x="sqlPreviewScrollX"
          max-height="274px"
        />
        <n-empty
          v-else
          class="sql-preview-empty"
          description="暂无预览数据，点击预览SQL获取前5行"
          size="small"
        />
        <div class="sql-preview-note">
          预览仅返回前 5 行，不代表完整查询结果
        </div>
      </div>
    </div>

    <div v-else class="table-source-panel">
      <div class="table-source-summary">
        <div class="table-source-card">
          <span>数据连接</span>
          <strong>{{ formData.connectionId ? getConnectionName(formData.connectionId) : '待选择' }}</strong>
        </div>
        <div class="table-source-card">
          <span>来源数据表</span>
          <strong>{{ formData.tableName || '待选择' }}</strong>
        </div>
        <div class="table-source-card">
          <span>字段来源</span>
          <strong>{{ getRowScopeFieldSourceLabel(formData) }}</strong>
        </div>
      </div>

      <div class="table-source-fields">
        <div class="table-source-fields__head">
          <span>字段结构</span>
          <small>{{ getRowScopeFieldOptions(formData).length }} 个字段</small>
        </div>
        <div v-if="getRowScopeFieldOptions(formData).length" class="table-source-field-list">
          <span
            v-for="field in getRowScopeFieldOptions(formData).slice(0, 18)"
            :key="field.value"
            class="table-source-field-chip"
          >
            {{ field.label }}
          </span>
          <span
            v-if="getRowScopeFieldOptions(formData).length > 18"
            class="table-source-field-chip table-source-field-chip--more"
          >
            +{{ getRowScopeFieldOptions(formData).length - 18 }}
          </span>
        </div>
        <n-empty
          v-else
          size="small"
          description="暂无字段结构，请先选择数据表并刷新来源字段"
        />
      </div>

      <div class="table-source-actions">
        <n-button
          secondary
          :disabled="isFormReadOnly || !formData.connectionId || !formData.tableName"
          :loading="rowScopeTableFieldLoading"
          @click="loadRowScopeTableFields(formData, { force: true })"
        >
          刷新来源字段
        </n-button>
      </div>
    </div>
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
