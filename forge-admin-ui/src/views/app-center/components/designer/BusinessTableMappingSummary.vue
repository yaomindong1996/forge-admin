<template>
  <section class="table-mapping" :class="{ compact: !expanded }">
    <header class="mapping-header">
      <div class="mapping-anchor">
        <span class="database-mark">
          <n-icon><ServerOutline /></n-icon>
        </span>
        <div>
          <strong>{{ mapping?.tableName || '尚未确定物理表' }}</strong>
          <p>
            <span>{{ mapping?.datasourceName || mapping?.datasourceCode || '平台主库' }}</span>
            <code v-if="mapping?.datasourceCode">{{ mapping.datasourceCode }}</code>
            <span>{{ mapping?.tableMode === 'EXISTING' ? '绑定已有表' : '由设计创建表' }}</span>
          </p>
        </div>
      </div>

      <div class="mapping-status">
        <span class="sync-state" :class="syncTone(mapping?.syncStatus)">
          <i />
          {{ syncLabel(mapping?.syncStatus) }}
        </span>
        <span v-if="!expanded">{{ mapping?.sharedApplicationCount || 0 }} 个应用使用</span>
        <span v-if="!expanded && mapping?.lastSyncTime">最近同步 {{ mapping.lastSyncTime }}</span>
        <template v-if="expanded">
          <n-button v-if="canAlignImportedFields" size="small" secondary @click="confirmAlignImportedFields">
            <template #icon>
              <n-icon><SwapHorizontalOutline /></n-icon>
            </template>
            按数据库校准字段
          </n-button>
          <n-button size="small" secondary @click="openIndexDesigner">
            <template #icon>
              <n-icon><KeyOutline /></n-icon>
            </template>
            配置索引
          </n-button>
          <n-button size="small" secondary :loading="previewing" @click="previewDiff">
            <template #icon>
              <n-icon><EyeOutline /></n-icon>
            </template>
            预览数据库变更
          </n-button>
          <n-button
            type="primary"
            size="small"
            :disabled="mapping?.readonly || mapping?.allowDdl === false"
            :loading="syncing"
            @click="confirmSync"
          >
            <template #icon>
              <n-icon><SyncOutline /></n-icon>
            </template>
            确认同步数据库
          </n-button>
        </template>
        <n-button v-else size="small" secondary @click="emit('openStructure')">
          <template #icon>
            <n-icon><GridOutline /></n-icon>
          </template>
          查看数据结构
        </n-button>
      </div>
    </header>

    <template v-if="expanded">
      <n-alert v-if="mapping?.sharedApplicationCount > 1" type="warning" :bordered="false">
        当前对象被 {{ mapping.sharedApplicationCount }} 个应用共同使用，字段结构变更会影响所有引用应用。
      </n-alert>
      <n-alert v-if="mapping?.allowDdl === false || mapping?.readonly" type="info" :bordered="false">
        当前运行数据源禁止在线 DDL 或为只读模式；可以预览并导出迁移脚本，由数据库管理员审核执行。
      </n-alert>
    </template>

    <n-modal v-model:show="previewVisible" preset="card" title="数据库变更预览" class="ddl-preview-modal">
      <div class="ddl-summary">
        <span>设计版本 v{{ mapping?.designVersion || 0 }}</span>
        <span>{{ preview?.ddlStatements?.length || 0 }} 条变更语句</span>
        <span :class="onlineExecutable ? 'safe' : 'manual'">
          {{ onlineSyncLabel }}
        </span>
      </div>
      <n-alert v-for="warning in preview?.warnings || []" :key="warning" type="warning" :bordered="false">
        {{ warning }}
      </n-alert>
      <div v-if="preview?.ddlStatements?.length" class="ddl-list">
        <pre v-for="(ddl, index) in preview.ddlStatements" :key="`${index}-${ddl}`"><code>{{ ddl }}</code></pre>
      </div>
      <n-empty v-else size="small" description="数据库结构与当前设计一致" />
      <template #footer>
        <n-space justify="space-between">
          <n-button :disabled="!preview?.ddlStatements?.length" @click="exportScript">
            导出迁移脚本
          </n-button>
          <n-space>
            <n-button @click="previewVisible = false">
              关闭
            </n-button>
            <n-button
              v-if="canForceExecute"
              type="warning"
              secondary
              :disabled="mapping?.readonly || mapping?.allowDdl === false"
              :loading="syncing"
              @click="confirmForceSync"
            >
              强制执行全部
            </n-button>
            <n-button
              type="primary"
              :disabled="!onlineExecutable"
              :loading="syncing"
              @click="confirmSync"
            >
              确认在线同步
            </n-button>
          </n-space>
        </n-space>
      </template>
    </n-modal>

    <n-modal v-model:show="indexVisible" preset="card" title="显式索引配置" class="index-config-modal">
      <n-alert type="info" :bordered="false">
        只有这里明确保存的索引才会进入数据库变更预览；查询字段、关联字段和系统字段不会自动创建二级索引。
      </n-alert>
      <n-alert v-if="legacyAutoIndexCount" type="warning" :bordered="false">
        已忽略 {{ legacyAutoIndexCount }} 条旧版自动索引配置。需要保留时，请在下方重新显式添加。
      </n-alert>
      <div v-if="localIndexes.length" class="index-config-list">
        <div v-for="(item, index) in localIndexes" :key="item.clientKey" class="index-config-row">
          <n-input
            v-model:value="item.indexName"
            size="small"
            placeholder="索引名称，例如 idx_order_no"
            @blur="normalizeLocalIndexName(item)"
          />
          <n-select
            v-model:value="item.indexType"
            size="small"
            :options="indexTypeOptions"
          />
          <n-select
            v-model:value="item.fields"
            multiple
            filterable
            size="small"
            :options="indexFieldOptions"
            placeholder="选择一个或多个字段"
          />
          <n-input v-model:value="item.remark" size="small" placeholder="索引用途说明" />
          <n-button text size="small" class="text-error" @click="removeIndex(index)">
            删除
          </n-button>
        </div>
      </div>
      <n-empty v-else size="small" description="暂无显式索引，不会自动创建二级索引" />
      <template #footer>
        <n-space justify="space-between">
          <n-button secondary @click="addIndex">
            添加索引
          </n-button>
          <n-space>
            <n-button @click="indexVisible = false">
              取消
            </n-button>
            <n-button type="primary" @click="applyIndexes">
              应用配置
            </n-button>
          </n-space>
        </n-space>
      </template>
    </n-modal>
  </section>
</template>

<script setup>
import {
  EyeOutline,
  GridOutline,
  KeyOutline,
  ServerOutline,
  SwapHorizontalOutline,
  SyncOutline,
} from '@vicons/ionicons5'
import { useDialog, useMessage } from 'naive-ui'
import { computed, onMounted, ref, watch } from 'vue'
import {
  businessObjectTableMapping,
  previewBusinessObjectDatabaseDiff,
  syncBusinessObjectDatabase,
} from '@/api/business-application'
import { cloneSchema } from '@/components/lowcode-builder/model/model-schema'

const props = defineProps({
  objectId: {
    type: [Number, String],
    default: null,
  },
  expanded: {
    type: Boolean,
    default: false,
  },
  modelSchema: {
    type: Object,
    default: () => ({}),
  },
})

const emit = defineEmits([
  'openStructure',
  'synced',
  'loaded',
  'update:modelSchema',
  'dirtyChange',
  'applyIndexes',
  'applyModelSchema',
])
const dialog = useDialog()
const message = useMessage()
const loading = ref(false)
const previewing = ref(false)
const syncing = ref(false)
const previewVisible = ref(false)
const indexVisible = ref(false)
const mapping = ref(null)
const preview = ref(null)
const localIndexes = ref([])
let indexSequence = 0

const indexTypeOptions = [
  { label: '普通索引', value: 'NORMAL' },
  { label: '唯一索引', value: 'UNIQUE' },
]
const indexableDataTypes = new Set([
  'varchar',
  'char',
  'int',
  'bigint',
  'decimal',
  'date',
  'datetime',
  'time',
  'tinyint',
])

const indexFieldOptions = computed(() => (props.modelSchema?.fields || [])
  .filter(field => field
    && field.field
    && !field.systemField
    && indexableDataTypes.has(String(field.dataType || '').toLowerCase())
    && String(field.fieldStatus || 'ENABLED').toUpperCase() !== 'HIDDEN')
  .map(field => ({
    label: `${field.label || field.field}（${field.columnName || field.field}）`,
    value: field.field,
  })))

const legacyAutoIndexCount = computed(() => (props.modelSchema?.indexes || [])
  .filter(index => index?.auto === true)
  .length)

const canAlignImportedFields = computed(() => {
  if (mapping.value?.tableMode !== 'EXISTING')
    return false
  return (mapping.value?.fields || []).some((field) => {
    if (field?.systemField || !field?.fieldCode || !field?.databaseType)
      return false
    const required = field.required === true
    const databaseRequired = field.databaseNullable === false
    return field.syncStatus === 'TYPE_MISMATCH' || required !== databaseRequired
  })
})

/** 有可在线执行的安全变更即可点同步（加列/建表/兼容扩容）；高风险项可并存，同步后仍提示导出。 */
const onlineExecutable = computed(() => {
  if (!preview.value?.ddlStatements?.length)
    return false
  if (mapping.value?.readonly || mapping.value?.allowDdl === false)
    return false
  if (preview.value.executable !== true)
    return false
  if (typeof preview.value.onlineSafeDdlCount === 'number')
    return preview.value.onlineSafeDdlCount > 0
  return preview.value.ddlStatements.some(isSafeOnlineDdl)
})

const onlineSyncLabel = computed(() => {
  const safe = Number(preview.value?.onlineSafeDdlCount)
  const manual = Number(preview.value?.manualReviewDdlCount)
  if (Number.isFinite(safe) && Number.isFinite(manual)) {
    if (safe > 0 && manual > 0)
      return `可在线同步 ${safe} 条，另有 ${manual} 条可强制执行`
    if (safe > 0)
      return '可在线执行追加式 / 必填开关 / 兼容扩容'
    return '含高风险变更，可强制执行或导出审核'
  }
  return onlineExecutable.value ? '可在线执行追加式变更' : '仅预览 / 人工审核'
})

/** 预览里存在高风险 DDL，且数据源允许在线 DDL 时展示强制执行。 */
const canForceExecute = computed(() => {
  if (!preview.value?.ddlStatements?.length)
    return false
  if (mapping.value?.readonly || mapping.value?.allowDdl === false)
    return false
  if (preview.value.executable !== true)
    return false
  const manual = Number(preview.value.manualReviewDdlCount)
  if (Number.isFinite(manual))
    return manual > 0
  return !preview.value.ddlStatements.every(isSafeOnlineDdl)
})

watch(() => props.objectId, loadMapping)
// 展开「数据结构」时刷新映射，避免设计版本在保存草稿后过期导致预览/同步失败
watch(() => props.expanded, (expanded) => {
  if (expanded)
    loadMapping()
})
onMounted(loadMapping)

async function loadMapping() {
  if (!props.objectId)
    return
  loading.value = true
  try {
    const response = await businessObjectTableMapping(props.objectId)
    mapping.value = response.data || null
    emit('loaded', mapping.value)
  }
  finally {
    loading.value = false
  }
}

async function previewDiff() {
  if (!props.objectId)
    return
  previewing.value = true
  try {
    // 预览前强制刷新，防止本地 mapping.designVersion 与服务端不一致
    await loadMapping()
    if (mapping.value?.designVersion === undefined)
      return
    const response = await previewBusinessObjectDatabaseDiff(props.objectId, mapping.value.designVersion)
    preview.value = response.data || null
    previewVisible.value = true
  }
  finally {
    previewing.value = false
  }
}

async function confirmSync() {
  if (!preview.value)
    await previewDiff()
  if (!onlineExecutable.value) {
    message.warning(canForceExecute.value
      ? '当前差异均为高风险变更，请使用「强制执行全部」或导出脚本'
      : '当前差异不能在线执行，请导出迁移脚本后人工审核')
    return
  }
  const safe = Number(preview.value?.onlineSafeDdlCount)
  const manual = Number(preview.value?.manualReviewDdlCount)
  const hasPartial = Number.isFinite(safe) && Number.isFinite(manual) && safe > 0 && manual > 0
  dialog.warning({
    title: '确认同步数据库结构',
    content: hasPartial
      ? `将按设计版本 v${mapping.value.designVersion} 在线执行 ${safe} 条安全 DDL（加列/建表/必填开关/兼容扩容）；另有 ${manual} 条高风险变更不会自动执行，可再点「强制执行全部」。`
      : `将按设计版本 v${mapping.value.designVersion} 执行 ${safe > 0 ? safe : preview.value.ddlStatements.length} 条受控安全 DDL。保存设计草稿本身不会执行这些变更。`,
    positiveText: '确认同步',
    negativeText: '取消',
    onPositiveClick: () => executeSync(false),
  })
}

async function confirmForceSync() {
  if (!preview.value)
    await previewDiff()
  if (!canForceExecute.value) {
    message.warning('当前没有需要强制执行的高风险变更')
    return
  }
  const total = preview.value.ddlStatements.length
  const manual = Number(preview.value?.manualReviewDdlCount)
  dialog.error({
    title: '强制执行全部 DDL',
    content: `将按设计版本 v${mapping.value.designVersion} 执行预览中的全部 ${total} 条语句`
      + (Number.isFinite(manual) && manual > 0 ? `（含 ${manual} 条高风险变更，如改类型族、缩短长度）` : '')
      + '。可能导致数据截断或执行失败，请确认已备份或可接受风险。仍禁止 DROP/TRUNCATE。',
    positiveText: '确认强制执行',
    negativeText: '取消',
    onPositiveClick: () => executeSync(true),
  })
}

async function executeSync(forceExecute = false) {
  syncing.value = true
  try {
    await syncBusinessObjectDatabase(props.objectId, mapping.value.designVersion, { forceExecute })
    if (forceExecute)
      message.success('已强制执行预览中的全部 DDL')
    else {
      const manual = Number(preview.value?.manualReviewDdlCount)
      if (Number.isFinite(manual) && manual > 0)
        message.warning(`已同步安全变更；仍有 ${manual} 条高风险 DDL，可强制执行或导出脚本`)
      else
        message.success('数据库结构同步成功')
    }
    previewVisible.value = false
    preview.value = null
    await loadMapping()
    emit('synced')
  }
  finally {
    syncing.value = false
  }
}

function openIndexDesigner() {
  localIndexes.value = (props.modelSchema?.indexes || [])
    .filter(index => index && index.auto !== true)
    .map(index => ({
      clientKey: `index_${Date.now()}_${++indexSequence}`,
      indexName: String(index.indexName || ''),
      indexType: index.indexType === 'UNIQUE' || index.unique === true ? 'UNIQUE' : 'NORMAL',
      fields: Array.isArray(index.fields) ? [...index.fields] : [],
      remark: String(index.remark || ''),
    }))
  indexVisible.value = true
}

function addIndex() {
  localIndexes.value.push({
    clientKey: `index_${Date.now()}_${++indexSequence}`,
    indexName: '',
    indexType: 'NORMAL',
    fields: [],
    remark: '',
  })
}

function removeIndex(index) {
  localIndexes.value.splice(index, 1)
}

function normalizeLocalIndexName(item) {
  item.indexName = normalizeIndexName(item.indexName, item.indexType)
}

function applyIndexes() {
  const normalized = localIndexes.value.map(item => ({
    indexName: normalizeIndexName(item.indexName, item.indexType),
    indexType: item.indexType === 'UNIQUE' ? 'UNIQUE' : 'NORMAL',
    fields: Array.from(new Set(item.fields || [])),
    unique: item.indexType === 'UNIQUE',
    auto: false,
    remark: String(item.remark || '').trim(),
  }))
  if (normalized.some(item => !item.indexName)) {
    message.warning('请为每个索引填写索引名称')
    return
  }
  if (normalized.some(item => !item.fields.length)) {
    message.warning('请为每个索引至少选择一个字段')
    return
  }
  if (new Set(normalized.map(item => item.indexName)).size !== normalized.length) {
    message.warning('索引名称不能重复')
    return
  }
  const nextSchema = cloneSchema(props.modelSchema || {})
  nextSchema.indexes = normalized
  emit('update:modelSchema', nextSchema)
  emit('dirtyChange', true)
  emit('applyIndexes', nextSchema)
  indexVisible.value = false
}

function normalizeIndexName(value, indexType = 'NORMAL') {
  let normalized = String(value || '')
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9_]/g, '_')
    .replace(/_+/g, '_')
    .replace(/^_+|_+$/g, '')
  if (!normalized)
    return ''
  if (!normalized.startsWith('idx_') && !normalized.startsWith('uk_'))
    normalized = `${indexType === 'UNIQUE' ? 'uk' : 'idx'}_${normalized}`
  return normalized.slice(0, 64)
}

function confirmAlignImportedFields() {
  dialog.warning({
    title: '按数据库结构校准字段',
    content: '只会把已有映射字段的物理类型、长度、精度和必填状态恢复为当前数据库列定义，不修改字段名称、页面控件、显示规则或数据库结构。',
    positiveText: '确认校准',
    negativeText: '取消',
    onPositiveClick: alignImportedFields,
  })
}

function alignImportedFields() {
  const databaseFields = new Map()
  for (const field of mapping.value?.fields || []) {
    if (field?.fieldCode && field?.databaseType)
      databaseFields.set(`field:${field.fieldCode}`, field)
    if (field?.columnName && field?.databaseType)
      databaseFields.set(`column:${String(field.columnName).toLowerCase()}`, field)
  }
  const nextSchema = cloneSchema(props.modelSchema || {})
  nextSchema.fields = (nextSchema.fields || []).map((field) => {
    if (!field || field.systemField)
      return field
    const databaseField = databaseFields.get(`field:${field.field}`)
      || databaseFields.get(`column:${String(field.columnName || '').toLowerCase()}`)
    if (!databaseField)
      return field
    const type = parseDatabaseType(databaseField.databaseType)
    return {
      ...field,
      dataType: type.dataType,
      length: type.length,
      precision: type.precision,
      required: databaseField.databaseNullable === false,
      businessFieldType: inferBusinessFieldType(type.dataType, field.businessFieldType),
    }
  })
  emit('update:modelSchema', nextSchema)
  emit('dirtyChange', true)
  emit('applyModelSchema', nextSchema)
}

function parseDatabaseType(value) {
  const normalized = String(value || '').trim().toLowerCase().replace(/\s+/g, '')
  const match = normalized.match(/^([a-z0-9_]+)(?:\((\d+)(?:,(\d+))?\))?/)
  const baseType = match?.[1] || 'varchar'
  const dataType = normalizeDatabaseDataType(baseType)
  return {
    dataType,
    length: match?.[2] ? Number(match[2]) : null,
    precision: dataType === 'decimal' ? Number(match?.[3] || 0) : null,
  }
}

function normalizeDatabaseDataType(value) {
  if (['numeric', 'number', 'double', 'float'].includes(value))
    return 'decimal'
  if (['integer', 'smallint', 'mediumint'].includes(value))
    return 'int'
  if (['timestamp'].includes(value))
    return 'datetime'
  if (['varchar2', 'nvarchar', 'nvarchar2'].includes(value))
    return 'varchar'
  return value
}

function inferBusinessFieldType(dataType, fallback) {
  const mapping = {
    decimal: 'MONEY',
    int: 'NUMBER',
    bigint: 'NUMBER',
    tinyint: 'SWITCH',
    date: 'DATE',
    datetime: 'DATETIME',
    text: 'MULTILINE',
    longtext: 'MULTILINE',
  }
  return mapping[dataType] || fallback || 'TEXT'
}

function exportScript() {
  const statements = preview.value?.ddlStatements || []
  if (!statements.length)
    return
  const header = `-- Forge 业务对象数据库差异\n-- table: ${mapping.value?.tableName || '-'}\n-- design version: ${mapping.value?.designVersion || 0}\n\n`
  const content = `${header}${statements.map(statement => `${statement.replace(/;?$/, ';')}\n`).join('\n')}`
  const blob = new Blob([content], { type: 'text/sql;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `${mapping.value?.tableName || 'database-diff'}-v${mapping.value?.designVersion || 0}.sql`
  link.click()
  URL.revokeObjectURL(url)
}

function isSafeOnlineDdl(statement) {
  const ddl = String(statement || '').trim().replace(/\s+/g, ' ').toUpperCase()
  if (ddl.startsWith('CREATE TABLE '))
    return true
  if (ddl.startsWith('CREATE INDEX ') || ddl.startsWith('CREATE UNIQUE INDEX '))
    return true
  return ddl.startsWith('ALTER TABLE ') && (
    ddl.includes(' ADD COLUMN ')
    || ddl.includes(' ADD (')
    || ddl.includes(' ADD KEY ')
    || ddl.includes(' ADD UNIQUE KEY ')
  )
}

function syncLabel(status) {
  const labels = {
    IN_SYNC: '数据库结构已同步',
    PARTIAL: '部分同步，字段调整待确认',
    OUT_OF_SYNC: '存在未同步变更',
    TABLE_MISSING: '物理表尚未创建',
    CHECK_FAILED: '数据库结构检查失败',
  }
  return labels[status] || '等待结构检查'
}

function syncTone(status) {
  if (status === 'IN_SYNC')
    return 'is-ready'
  if (status === 'CHECK_FAILED' || status === 'FAILED')
    return 'is-error'
  return 'is-warning'
}

defineExpose({ refresh: loadMapping })
</script>

<style scoped>
.table-mapping {
  display: grid;
  gap: 8px;
  flex: 0 0 auto;
  margin: 0;
  padding: 8px 12px;
  border: 0;
  border-bottom: 1px solid var(--border-light);
  border-radius: 0;
  background: var(--bg-primary, #fff);
  color: var(--text-primary, #1f2328);
}

.table-mapping.compact {
  margin: 0;
  padding: 8px 12px;
}

.mapping-header,
.mapping-anchor,
.mapping-status,
.mapping-toolbar {
  display: flex;
  align-items: center;
}

.mapping-header,
.mapping-toolbar {
  justify-content: space-between;
  gap: 16px;
}

.mapping-anchor {
  min-width: 0;
  gap: 10px;
}

.database-mark {
  display: inline-flex;
  flex: 0 0 32px;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: 1px solid var(--border-light);
  border-radius: 5px;
  color: var(--n-primary-color);
}

.mapping-anchor > div,
.mapping-toolbar > div {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 3px;
}

.mapping-anchor strong,
.mapping-toolbar strong {
  overflow: hidden;
  color: var(--text-primary);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mapping-anchor p {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 0;
  color: var(--text-tertiary);
  font-size: 11px;
}

.mapping-anchor code {
  color: var(--text-tertiary);
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
}

.mapping-status {
  flex: 0 0 auto;
  gap: 12px;
  color: var(--text-tertiary);
  font-size: 11px;
}

.sync-state {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: var(--text-secondary);
}

.sync-state i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #bf8700;
}

.sync-state.is-ready i {
  background: #2da44e;
}

.sync-state.is-error i {
  background: #cf222e;
}

.mapping-toolbar {
  display: none;
}

.ddl-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 12px;
  color: var(--text-tertiary);
  font-size: 12px;
}

.ddl-summary .safe {
  color: #1a7f37;
}

.ddl-summary .manual {
  color: #9a6700;
}

.ddl-list {
  display: grid;
  gap: 8px;
  max-height: 420px;
  margin-top: 12px;
  overflow: auto;
}

.ddl-list pre {
  overflow: auto;
  margin: 0;
  padding: 10px 12px;
  border: 1px solid var(--border-light);
  border-radius: 5px;
  background: var(--n-code-color, #f6f8fa);
  font-size: 11px;
  line-height: 1.55;
  white-space: pre-wrap;
}

.index-config-list {
  display: grid;
  gap: 10px;
  margin-top: 14px;
}

.index-config-row {
  display: grid;
  grid-template-columns: minmax(150px, 0.8fr) 112px minmax(220px, 1.4fr) minmax(150px, 0.8fr) auto;
  gap: 10px;
  align-items: center;
}

:global(.ddl-preview-modal) {
  width: min(880px, calc(100vw - 32px));
}

:global(.index-config-modal) {
  width: min(1040px, calc(100vw - 32px));
}

@media (max-width: 900px) {
  .mapping-header,
  .mapping-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .mapping-status {
    flex-wrap: wrap;
  }

  .index-config-row {
    grid-template-columns: 1fr;
  }
}
</style>
