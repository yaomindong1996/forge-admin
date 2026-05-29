<template>
  <n-modal
    v-model:show="visible"
    title="从数据源表导入模型"
    preset="card"
    style="width: min(1040px, calc(100vw - 28px))"
    :mask-closable="false"
  >
    <div class="model-import-modal">
      <section class="import-controls">
        <n-form label-placement="top" size="small" :show-feedback="false" class="import-form">
          <n-form-item label="数据源">
            <n-select
              v-model:value="selectedDatasourceId"
              filterable
              :loading="datasourceLoading"
              :options="datasourceOptions"
              placeholder="选择数据源"
              @update:value="handleDatasourceChange"
            />
          </n-form-item>
          <n-form-item label="数据表">
            <n-select
              v-model:value="selectedTableName"
              filterable
              clearable
              :loading="tableLoading"
              :options="tableOptions"
              placeholder="选择要导入的表"
              @update:value="syncDefaultModelMeta"
            />
          </n-form-item>
          <n-form-item label="模型编码">
            <n-input v-model:value="modelCode" placeholder="默认使用表名" />
          </n-form-item>
          <n-form-item label="模型名称">
            <n-input v-model:value="modelName" placeholder="默认使用表注释" />
          </n-form-item>
        </n-form>
        <div class="import-actions">
          <n-input
            v-model:value="keyword"
            clearable
            size="small"
            placeholder="搜索表名或表注释"
          />
          <n-button size="small" :loading="previewLoading" :disabled="!canPreview" @click="previewTable">
            预览字段
          </n-button>
        </div>
      </section>

      <section class="preview-panel">
        <div class="preview-head">
          <div>
            <strong>{{ previewSchema?.businessName || selectedTableName || '未选择数据表' }}</strong>
            <span>{{ previewSchema?.tableName || '先选择数据表并预览字段' }}</span>
          </div>
          <NTag :bordered="false">
            {{ previewFields.length }} 个业务字段
          </NTag>
        </div>
        <n-data-table
          v-if="previewFields.length"
          :columns="fieldColumns"
          :data="previewFields"
          :pagination="false"
          :bordered="false"
          size="small"
          max-height="360"
        />
        <n-empty v-else description="预览后将在这里展示可导入字段" class="preview-empty" />
      </section>
    </div>

    <template #footer>
      <n-space justify="end">
        <n-button @click="visible = false">
          取消
        </n-button>
        <n-button type="primary" :disabled="!previewSchema" @click="applyPreview">
          应用到模型设计
        </n-button>
      </n-space>
    </template>
  </n-modal>
</template>

<script setup>
import { NTag } from 'naive-ui'
import { computed, h, ref, watch } from 'vue'
import {
  genDatasourceEnabled,
  genDatasourceTables,
  lowcodePreviewDbTableModel,
} from '@/api/lowcode-crud'
import { normalizeObjectCode } from './model-schema'

const props = defineProps({
  show: {
    type: Boolean,
    default: false,
  },
  domainId: {
    type: [Number, String],
    default: null,
  },
})

const emit = defineEmits(['update:show', 'apply'])

const visible = ref(false)
const datasourceLoading = ref(false)
const tableLoading = ref(false)
const previewLoading = ref(false)
const datasources = ref([])
const tables = ref([])
const selectedDatasourceId = ref(null)
const selectedTableName = ref(null)
const keyword = ref('')
const modelCode = ref('')
const modelName = ref('')
const previewSchema = ref(null)

const datasourceOptions = computed(() => datasources.value.map(item => ({
  label: `${item.datasourceName}${item.isDefault === 1 ? '（默认）' : ''} / ${item.dbType || '-'}`,
  value: item.datasourceId,
})))

const filteredTables = computed(() => {
  const key = keyword.value.trim().toLowerCase()
  if (!key)
    return tables.value
  return tables.value.filter(item =>
    String(item.tableName || '').toLowerCase().includes(key)
    || String(item.tableComment || '').toLowerCase().includes(key),
  )
})

const tableOptions = computed(() => filteredTables.value.map(item => ({
  label: `${item.tableComment || item.tableName}（${item.tableName}）`,
  value: item.tableName,
})))

const previewFields = computed(() => previewSchema.value?.fields || [])
const canPreview = computed(() => Boolean(props.domainId && selectedDatasourceId.value && selectedTableName.value))

const fieldColumns = [
  { title: '字段名', key: 'field', width: 150, ellipsis: { tooltip: true } },
  { title: '列名', key: 'columnName', width: 170, ellipsis: { tooltip: true } },
  { title: '名称', key: 'label', width: 150, ellipsis: { tooltip: true } },
  { title: '类型', key: 'dataType', width: 100 },
  {
    title: '组件',
    key: 'componentType',
    width: 120,
    render(row) {
      return h(NTag, { size: 'small', bordered: false }, () => row.componentType || '-')
    },
  },
  {
    title: '可见性',
    key: 'visible',
    width: 180,
    render(row) {
      const tags = []
      if (row.searchable)
        tags.push('查询')
      if (row.listVisible)
        tags.push('列表')
      if (row.formVisible)
        tags.push('表单')
      return tags.length ? tags.join(' / ') : '-'
    },
  },
]

watch(() => props.show, async (value) => {
  visible.value = value
  if (value)
    await bootstrap()
})

watch(visible, (value) => {
  emit('update:show', value)
})

async function bootstrap() {
  previewSchema.value = null
  selectedTableName.value = null
  modelCode.value = ''
  modelName.value = ''
  keyword.value = ''
  if (!datasources.value.length)
    await loadDatasources()
  if (selectedDatasourceId.value)
    await loadTables(selectedDatasourceId.value)
}

async function loadDatasources() {
  datasourceLoading.value = true
  try {
    const res = await genDatasourceEnabled()
    datasources.value = res.data || []
    const current = datasources.value.find(item => item.datasourceId === selectedDatasourceId.value)
    const fallback = current || datasources.value.find(item => item.isDefault === 1) || datasources.value[0]
    selectedDatasourceId.value = fallback?.datasourceId || null
  }
  catch (e) {
    datasources.value = []
    window.$message?.error(e?.message || '加载数据源失败')
  }
  finally {
    datasourceLoading.value = false
  }
}

async function handleDatasourceChange(value) {
  selectedDatasourceId.value = value || null
  selectedTableName.value = null
  previewSchema.value = null
  tables.value = []
  if (value)
    await loadTables(value)
}

async function loadTables(datasourceId) {
  tableLoading.value = true
  try {
    const res = await genDatasourceTables(datasourceId)
    tables.value = res.data || []
  }
  catch (e) {
    tables.value = []
    window.$message?.error(e?.message || '加载数据表失败')
  }
  finally {
    tableLoading.value = false
  }
}

function syncDefaultModelMeta(tableName) {
  previewSchema.value = null
  const table = tables.value.find(item => item.tableName === tableName)
  if (!table)
    return
  if (!modelCode.value)
    modelCode.value = normalizeObjectCode(table.tableName || '')
  if (!modelName.value)
    modelName.value = table.tableComment || table.tableName || ''
}

async function previewTable() {
  if (!canPreview.value) {
    window.$message?.warning('请选择业务领域、数据源和数据表')
    return
  }
  previewLoading.value = true
  try {
    const res = await lowcodePreviewDbTableModel(buildRequest())
    previewSchema.value = res.data
    if (!modelCode.value)
      modelCode.value = normalizeObjectCode(previewSchema.value?.object?.code || previewSchema.value?.tableName || '')
    if (!modelName.value)
      modelName.value = previewSchema.value?.businessName || previewSchema.value?.object?.name || selectedTableName.value
  }
  catch (e) {
    previewSchema.value = null
    window.$message?.error(e?.message || '预览数据表失败')
  }
  finally {
    previewLoading.value = false
  }
}

function applyPreview() {
  if (!previewSchema.value)
    return
  emit('apply', {
    request: buildRequest(),
    modelSchema: previewSchema.value,
    modelCode: modelCode.value,
    modelName: modelName.value,
  })
  visible.value = false
}

function buildRequest() {
  return {
    datasourceId: selectedDatasourceId.value,
    domainId: props.domainId,
    tableName: selectedTableName.value,
    modelCode: modelCode.value || undefined,
    modelName: modelName.value || undefined,
    tenantEnabled: true,
    masterData: false,
  }
}
</script>

<style scoped>
.model-import-modal {
  display: grid;
  gap: 12px;
}

.import-controls,
.preview-panel {
  min-width: 0;
  border: 1px solid #d8dee8;
  border-radius: 8px;
  background: #fff;
  padding: 12px;
}

.import-form {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.import-form :deep(.n-form-item) {
  margin-bottom: 0;
}

.import-actions {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  margin-top: 10px;
}

.preview-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.preview-head > div {
  display: grid;
  min-width: 0;
  gap: 2px;
}

.preview-head strong,
.preview-head span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-head strong {
  color: #0f172a;
  font-size: 14px;
}

.preview-head span {
  color: #64748b;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
}

.preview-empty {
  padding: 44px 0;
}

@media (max-width: 900px) {
  .import-form {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .import-form,
  .import-actions {
    grid-template-columns: 1fr;
  }
}
</style>
