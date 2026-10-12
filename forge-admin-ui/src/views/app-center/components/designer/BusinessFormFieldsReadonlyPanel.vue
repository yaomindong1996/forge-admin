<template>
  <div class="readonly-fields-panel">
    <header class="readonly-fields-head">
      <div>
        <h3>表单字段</h3>
        <p>字段由代码 Provider 提供，编辑请联系开发人员。节点权限在业务流程配置中维护。</p>
      </div>
      <n-button size="small" secondary :loading="loading" @click="loadData">
        刷新
      </n-button>
    </header>

    <n-spin :show="loading">
      <div class="readonly-fields-body">
        <n-alert v-if="errorMessage" type="error" :bordered="false" class="readonly-alert">
          {{ errorMessage }}
        </n-alert>
        <n-alert v-else-if="warnings.length" type="warning" :bordered="false" class="readonly-alert">
          {{ warnings.join('；') }}
        </n-alert>

        <div class="readonly-grid">
          <aside class="asset-column">
            <div class="section-title">
              <strong>字段资产</strong>
              <span>{{ formAssets.length }} 个</span>
            </div>
            <button
              v-for="asset in formAssets"
              :key="asset.assetKey"
              type="button"
              class="asset-card"
              :class="{ active: asset.assetKey === selectedAssetKey }"
              @click="selectedAssetKey = asset.assetKey"
            >
              <span class="asset-card-title">{{ asset.formName || asset.formKey }}</span>
              <span class="asset-card-meta">
                <n-tag size="small" :type="sourceTagType(asset.sourceType)" :bordered="false">
                  {{ sourceLabel(asset.sourceType) }}
                </n-tag>
                <em>{{ asset.fieldCount || asset.fields.length }} 个字段</em>
              </span>
              <span class="asset-card-key">{{ asset.formKey || asset.providerKey || '-' }}</span>
            </button>
            <n-empty v-if="!formAssets.length && !loading" size="small" description="暂无字段资产" />
          </aside>

          <main class="field-column">
            <div class="field-toolbar">
              <div>
                <strong>{{ selectedAsset?.formName || '字段目录' }}</strong>
                <span>{{ objectName || objectCode }}</span>
              </div>
              <n-select
                v-model:value="selectedNodeKey"
                :options="nodeOptions"
                clearable
                filterable
                size="small"
                placeholder="选择流程节点查看字段权限"
                class="node-select"
              />
            </div>

            <div class="field-table-wrap">
              <n-table v-if="currentFields.length" size="small" :single-line="false">
                <thead>
                  <tr>
                    <th>字段编码</th>
                    <th>字段名</th>
                    <th>类型</th>
                    <th>字段必填</th>
                    <th>系统字段</th>
                    <th>{{ selectedNodeLabel }}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="field in currentFields" :key="field.fieldCode">
                    <td>
                      <code>{{ field.fieldCode }}</code>
                    </td>
                    <td>
                      <div class="field-name-cell">
                        <strong>{{ field.label }}</strong>
                        <span v-if="field.description">{{ field.description }}</span>
                      </div>
                    </td>
                    <td>{{ typeLabel(field) }}</td>
                    <td>
                      <n-tag size="small" :type="field.required ? 'warning' : 'default'" :bordered="false">
                        {{ field.required ? '是' : '否' }}
                      </n-tag>
                    </td>
                    <td>
                      <n-space size="small">
                        <n-tag size="small" :type="field.internal ? 'error' : 'default'" :bordered="false">
                          {{ field.internal ? '内部' : '业务' }}
                        </n-tag>
                        <n-tag size="small" :type="field.systemField ? 'info' : 'default'" :bordered="false">
                          {{ field.systemField ? '系统' : '普通' }}
                        </n-tag>
                      </n-space>
                    </td>
                    <td>
                      <n-space size="small">
                        <n-tag size="small" :type="permissionFor(field).visible ? 'success' : 'error'" :bordered="false">
                          {{ permissionFor(field).visible ? '可见' : '隐藏' }}
                        </n-tag>
                        <n-tag size="small" :type="permissionFor(field).editable ? 'info' : 'default'" :bordered="false">
                          {{ permissionFor(field).editable ? '可编辑' : '只读' }}
                        </n-tag>
                        <n-tag size="small" :type="permissionFor(field).required ? 'warning' : 'default'" :bordered="false">
                          {{ permissionFor(field).required ? '本节点必填' : '非必填' }}
                        </n-tag>
                      </n-space>
                    </td>
                  </tr>
                </tbody>
              </n-table>
              <n-empty v-else size="small" description="当前资产没有可展示字段" />
            </div>
          </main>
        </div>
      </div>
    </n-spin>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { businessFlowBinding, businessFlowFormAssets } from '@/api/business-app'

const props = defineProps({
  objectCode: {
    type: String,
    default: '',
  },
  objectName: {
    type: String,
    default: '',
  },
})

const loading = ref(false)
const errorMessage = ref('')
const warnings = ref([])
const formAssets = ref([])
const nodeForms = ref([])
const selectedAssetKey = ref('')
const selectedNodeKey = ref('')

const selectedAsset = computed(() => formAssets.value.find(asset => asset.assetKey === selectedAssetKey.value) || null)
const currentFields = computed(() => selectedAsset.value?.fields || [])
const selectedNode = computed(() => nodeForms.value.find(node => node.taskDefKey === selectedNodeKey.value) || null)
const selectedNodeLabel = computed(() => selectedNode.value?.taskName || '节点权限')
const nodeOptions = computed(() => nodeForms.value.map(node => ({
  label: node.taskName ? `${node.taskName}（${node.taskDefKey}）` : node.taskDefKey,
  value: node.taskDefKey,
})))
const permissionMap = computed(() => {
  const map = new Map()
  const permissions = Array.isArray(selectedNode.value?.fieldPermissions) ? selectedNode.value.fieldPermissions : []
  permissions.forEach((permission) => {
    const code = normalizeText(permission.fieldCode || permission.field || permission.code)
    if (code)
      map.set(code, permission)
  })
  return map
})

onMounted(loadData)

watch(() => props.objectCode, () => {
  loadData()
})

async function loadData() {
  if (!props.objectCode) {
    formAssets.value = []
    nodeForms.value = []
    return
  }
  loading.value = true
  errorMessage.value = ''
  try {
    const [assetRes, bindingRes] = await Promise.all([
      businessFlowFormAssets(props.objectCode, { includeInternal: true }),
      businessFlowBinding(props.objectCode),
    ])
    const assetData = assetRes.data || {}
    warnings.value = Array.isArray(assetData.warnings) ? assetData.warnings : []
    formAssets.value = normalizeAssets(assetData.formAssets || [])
    const binding = bindingRes.data || {}
    nodeForms.value = normalizeNodeForms(binding.nodeForms || [])
    selectedAssetKey.value = keepOrFirst(selectedAssetKey.value, formAssets.value, 'assetKey')
    selectedNodeKey.value = keepOrFirst(selectedNodeKey.value, nodeForms.value, 'taskDefKey')
  }
  catch (error) {
    errorMessage.value = error?.message || '字段目录加载失败'
    formAssets.value = []
    nodeForms.value = []
  }
  finally {
    loading.value = false
  }
}

function normalizeAssets(list = []) {
  return (Array.isArray(list) ? list : [])
    .map((asset, index) => {
      const fields = normalizeFields(asset.fields || asset.fieldCatalog || [])
      const assetKey = [
        normalizeText(asset.formMode || asset.type || 'FORM'),
        normalizeText(asset.providerKey || asset.sourceType || 'asset'),
        normalizeText(asset.formKey || asset.formName || index),
      ].join(':')
      return {
        ...asset,
        assetKey,
        fields,
        fieldCount: Number(asset.fieldCount || fields.length || 0),
      }
    })
    .filter(asset => asset.formKey || asset.formName || asset.fields.length)
}

function normalizeFields(list = []) {
  return (Array.isArray(list) ? list : [])
    .map((field) => {
      const fieldCode = normalizeText(field.fieldCode || field.field || field.code || field.name)
      if (!fieldCode)
        return null
      return {
        ...field,
        fieldCode,
        label: normalizeText(field.label || field.fieldName || field.fieldLabel || fieldCode),
        dataType: normalizeText(field.dataType || field.fieldType || field.type),
        componentType: normalizeText(field.componentType || field.type),
        required: readBoolean(field.required),
        internal: readBoolean(field.internal),
        systemField: readBoolean(field.systemField),
        description: normalizeText(field.description || field.remark),
      }
    })
    .filter(Boolean)
}

function normalizeNodeForms(list = []) {
  return (Array.isArray(list) ? list : [])
    .map((node) => {
      const taskDefKey = normalizeText(node.taskDefKey || node.id)
      if (!taskDefKey)
        return null
      return {
        ...node,
        taskDefKey,
        taskName: normalizeText(node.taskName || node.name || taskDefKey),
        fieldPermissions: Array.isArray(node.fieldPermissions) ? node.fieldPermissions : [],
      }
    })
    .filter(Boolean)
}

function permissionFor(field) {
  const permission = permissionMap.value.get(field.fieldCode)
  const visible = permission
    ? readBoolean(permission.visible ?? permission.readable, true)
    : true
  const editable = visible && (permission
    ? readBoolean(permission.editable ?? permission.writable, false)
    : false)
  return {
    visible,
    editable,
    required: editable && (permission ? readBoolean(permission.required, false) : false),
  }
}

function keepOrFirst(current, list, key) {
  if (current && list.some(item => item[key] === current))
    return current
  return list[0]?.[key] || ''
}

function sourceLabel(sourceType) {
  if (sourceType === 'codeProvider')
    return '代码 Provider'
  if (sourceType === 'businessObject')
    return '业务对象'
  if (sourceType === 'external')
    return '外部地址'
  return '表单资产'
}

function sourceTagType(sourceType) {
  if (sourceType === 'codeProvider')
    return 'info'
  if (sourceType === 'businessObject')
    return 'success'
  return 'default'
}

function typeLabel(field) {
  const type = field.componentType || field.dataType || '-'
  const labels = {
    input: '文本',
    textarea: '多行文本',
    inputNumber: '数字',
    number: '数字',
    date: '日期',
    datetime: '日期时间',
    select: '下拉',
    dictSelect: '字典',
    fileUpload: '文件',
    imageUpload: '图片',
  }
  return labels[type] || type
}

function normalizeText(value) {
  return String(value || '').trim()
}

function readBoolean(value, fallback = false) {
  if (value === undefined || value === null || value === '')
    return fallback
  if (typeof value === 'boolean')
    return value
  if (typeof value === 'number')
    return value !== 0
  const text = String(value).trim().toLowerCase()
  if (['true', '1', 'yes', 'y'].includes(text))
    return true
  if (['false', '0', 'no', 'n'].includes(text))
    return false
  return fallback
}
</script>

<style scoped>
.readonly-fields-panel {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  min-height: calc(100vh - 106px);
  background: var(--bg-secondary);
}

.readonly-fields-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid var(--border-light);
  background: var(--bg-primary);
  padding: 14px 16px;
}

.readonly-fields-head h3 {
  margin: 0;
  color: var(--text-primary);
  font-size: 15px;
}

.readonly-fields-head p {
  margin: 4px 0 0;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.readonly-fields-body {
  min-height: 0;
  padding: 14px;
}

.readonly-alert {
  margin-bottom: 12px;
}

.readonly-grid {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 12px;
  min-height: 0;
}

.asset-column,
.field-column {
  min-width: 0;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: var(--bg-primary);
}

.asset-column {
  display: grid;
  align-content: start;
  gap: 8px;
  padding: 12px;
}

.section-title,
.field-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.section-title strong,
.field-toolbar strong {
  display: block;
  color: var(--text-primary);
  font-size: 13px;
}

.section-title span,
.field-toolbar span {
  display: block;
  margin-top: 2px;
  color: var(--text-secondary);
  font-size: 12px;
}

.asset-card {
  display: grid;
  gap: 7px;
  width: 100%;
  cursor: pointer;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: var(--bg-primary);
  padding: 10px;
  text-align: left;
}

.asset-card:hover {
  border-color: #bfdbfe;
  background: #f8fbff;
}

.asset-card.active {
  border-color: #2563eb;
  background: #eff6ff;
}

.asset-card-title,
.asset-card-key {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.asset-card-title {
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 700;
}

.asset-card-meta {
  display: flex;
  align-items: center;
  gap: 6px;
}

.asset-card-meta em {
  color: var(--text-secondary);
  font-size: 12px;
  font-style: normal;
}

.asset-card-key {
  color: var(--text-secondary);
  font-size: 12px;
}

.field-column {
  overflow: hidden;
}

.field-toolbar {
  border-bottom: 1px solid var(--border-light);
  padding: 12px;
}

.node-select {
  width: min(360px, 42vw);
}

.field-table-wrap {
  overflow: auto;
  padding: 12px;
}

.field-table-wrap code {
  color: var(--text-primary);
  font-size: 12px;
}

.field-name-cell {
  display: grid;
  gap: 2px;
}

.field-name-cell strong {
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 650;
}

.field-name-cell span {
  color: var(--text-secondary);
  font-size: 12px;
}

@media (max-width: 1100px) {
  .readonly-grid {
    grid-template-columns: 1fr;
  }

  .node-select {
    width: 100%;
  }

  .field-toolbar {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
