<template>
  <div class="preview-pane">
    <div class="preview-toolbar">
      <div>
        <div class="preview-title">
          {{ modelSchema.businessName || '业务应用预览' }}
        </div>
        <div class="preview-desc">
          {{ modelSchema.tableName }} · {{ visibleColumns.length }} 个列表字段
        </div>
      </div>
      <n-space size="small">
        <n-button :loading="loading" @click="refreshPreview">
          后端校验
        </n-button>
        <n-button
          :loading="runtimeLoading"
          :disabled="!isPublished"
          type="primary"
          secondary
          @click="toggleRuntimePreview"
        >
          {{ showRuntimePreview ? '配置预览' : '真实运行预览' }}
        </n-button>
        <n-button :disabled="!isPublished" @click="openRuntimePage">
          打开运行页
        </n-button>
      </n-space>
    </div>

    <n-alert v-if="errorMsg" type="error" :bordered="false" class="preview-alert">
      {{ errorMsg }}
    </n-alert>
    <n-alert v-if="runtimeError" type="error" :bordered="false" class="preview-alert">
      {{ runtimeError }}
    </n-alert>
    <n-alert v-if="!isPublished" type="info" :bordered="false" class="preview-alert">
      当前展示的是草稿配置预览。保存并发布后，可在这里切换到真实运行预览，加载数据库数据并验证新增、编辑、删除等交互动作。
    </n-alert>

    <div v-if="showRuntimePreview" class="runtime-crud-preview">
      <div class="runtime-preview-head">
        <div>
          <div class="band-title">
            真实运行态预览
          </div>
          <p>当前区域连接已发布运行接口，页面动作会按正式应用配置执行。</p>
        </div>
        <n-tag type="success" :bordered="false">
          已发布
        </n-tag>
      </div>
      <AiCrudPage v-bind="runtimeCrudProps" />
    </div>

    <div v-else class="preview-surface">
      <div class="preview-band runtime-surface">
        <div class="band-title">
          {{ pageSchema.layoutType === 'tree-crud' ? '左树右表运行态' : '列表页面运行态' }}
        </div>
        <div class="runtime-layout" :class="{ tree: pageSchema.layoutType === 'tree-crud' }">
          <aside v-if="pageSchema.layoutType === 'tree-crud'" class="tree-preview">
            <div class="tree-title">
              {{ tableZone?.props?.treeConfig?.treeTitle || `${modelSchema.businessName || '业务'}树` }}
            </div>
            <div class="tree-node active">
              全部
            </div>
            <div class="tree-node">
              示例节点一
            </div>
            <div class="tree-node">
              示例节点二
            </div>
          </aside>
          <div class="runtime-main">
            <div v-if="searchFields.length" class="query-set-preview">
              <div class="search-grid">
                <n-form-item
                  v-for="field in searchFields"
                  :key="field.field"
                  :label="field.label"
                  :show-feedback="false"
                >
                  <n-select v-if="field.dictType || ['dictSelect', 'userSelect'].includes(field.componentType)" disabled placeholder="请选择" />
                  <n-tree-select
                    v-else-if="['treeSelect', 'orgTreeSelect', 'regionTreeSelect'].includes(field.componentType)"
                    disabled
                    placeholder="请选择"
                  />
                  <n-date-picker
                    v-else-if="['daterange', 'datetimerange'].includes(resolveSearchPreviewType(field))"
                    disabled
                    :type="resolveSearchPreviewType(field)"
                    style="width: 100%"
                  />
                  <div v-else-if="resolveSearchPreviewType(field) === 'timerange'" class="preview-time-range">
                    <n-time-picker disabled style="width: 100%" />
                    <span>至</span>
                    <n-time-picker disabled style="width: 100%" />
                  </div>
                  <n-date-picker
                    v-else-if="['date', 'datetime'].includes(resolveSearchPreviewType(field))"
                    disabled
                    :type="resolveSearchPreviewType(field)"
                    style="width: 100%"
                  />
                  <n-time-picker
                    v-else-if="resolveSearchPreviewType(field) === 'time'"
                    disabled
                    style="width: 100%"
                  />
                  <n-input
                    v-else-if="field.componentType === 'number' || ['int', 'bigint', 'decimal'].includes(field.dataType)"
                    disabled
                    :placeholder="resolveSampleValue(field)"
                    style="width: 100%"
                  />
                  <n-input v-else disabled :placeholder="`请输入${field.label}`" />
                </n-form-item>
              </div>
              <div class="query-actions">
                <n-button size="small" type="primary" disabled>
                  查询
                </n-button>
                <n-button size="small" disabled>
                  重置
                </n-button>
                <n-button size="small" text type="primary" disabled>
                  条件收起
                </n-button>
                <n-button v-if="tableZone?.props?.enableCustomQuery !== false" size="small" text disabled>
                  自定义查询
                </n-button>
              </div>
            </div>
            <div class="table-actions">
              <n-space>
                <n-button size="small" type="primary" disabled>
                  新增
                </n-button>
                <n-button v-if="tableZone?.props?.showExport" size="small" disabled>
                  数据导出
                </n-button>
                <n-button v-if="tableZone?.props?.showImport" size="small" disabled>
                  批量导入
                </n-button>
                <n-button
                  v-for="action in customToolbarActions"
                  :key="action.key"
                  size="small"
                  :type="action.type === 'default' ? undefined : action.type"
                  disabled
                >
                  {{ action.label }}
                </n-button>
              </n-space>
            </div>
            <n-data-table
              :columns="tableColumns"
              :data="sampleRows"
              :bordered="false"
              size="small"
            />
          </div>
        </div>
      </div>

      <div v-if="formFields.length || childFieldGroups.length" class="preview-band">
        <div class="band-title">
          表单与详情
        </div>
        <div class="form-grid" :style="formPreviewGridStyle">
          <n-form-item
            v-for="field in formFields"
            :key="field.field"
            :label="field.label"
            :required="field.required"
            :style="resolveFormPreviewItemStyle(field)"
          >
            <n-input v-if="['input', 'textarea'].includes(field.componentType)" disabled :placeholder="`请输入${field.label}`" />
            <n-input
              v-else-if="field.componentType === 'number'"
              disabled
              :placeholder="resolveSampleValue(field)"
              style="width: 100%"
            />
            <n-select v-else-if="field.dictType || ['select', 'radio', 'checkbox', 'dictSelect', 'userSelect'].includes(field.componentType)" disabled placeholder="请选择" />
            <n-tree-select v-else-if="['treeSelect', 'orgTreeSelect', 'regionTreeSelect'].includes(field.componentType)" disabled placeholder="请选择" />
            <n-cascader v-else-if="field.componentType === 'cascader'" disabled placeholder="请选择" />
            <n-date-picker v-else-if="['date', 'datetime'].includes(field.componentType)" disabled style="width: 100%" />
            <n-upload v-else-if="['fileUpload', 'imageUpload', 'upload'].includes(field.componentType)" disabled>
              <n-button size="small" disabled>
                {{ field.componentType === 'imageUpload' ? '选择图片' : '选择文件' }}
              </n-button>
            </n-upload>
            <n-input v-else disabled :placeholder="field.componentType" />
          </n-form-item>
        </div>
        <div v-if="childFieldGroups.length" class="child-preview-groups">
          <div v-for="group in childFieldGroups" :key="group.key" class="child-preview-group">
            <div class="child-preview-title">
              {{ group.name }}
            </div>
            <div class="child-preview-table">
              <span v-for="field in group.fields" :key="field.field">
                {{ field.label || field.field }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, h, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { crudConfigRender } from '@/api/ai'
import { lowcodePreview } from '@/api/lowcode-crud'
import AiCrudPage from '@/components/ai-form/AiCrudPage.vue'
import DictTag from '@/components/DictTag.vue'
import {
  buildPageDesignModelSchema,
  isPageFieldVisible,
  resolveFieldRefsFromFormCreateRules,
} from '@/components/lowcode-builder/page/page-schema'
import { getDictData } from '@/composables/useDict'

const props = defineProps({
  appId: {
    type: [String, Number],
    default: null,
  },
  draft: {
    type: Object,
    required: true,
  },
})

const router = useRouter()
const loading = ref(false)
const errorMsg = ref('')
const runtimeLoading = ref(false)
const runtimeError = ref('')
const runtimeConfig = ref(null)
const previewMode = ref('draft')
const dictCache = ref({})

const pageSchema = computed(() => props.draft.pageSchema || { zones: [] })
const modelSchema = computed(() => {
  return buildPageDesignModelSchema(props.draft.modelSchema || { fields: [] }, pageSchema.value.modelRefs || [])
})
const tableZone = computed(() => pageSchema.value.zones?.find(zone => zone.zoneKey === 'table'))
const editZone = computed(() => pageSchema.value.zones?.find(zone => zone.zoneKey === 'edit'))
const isPublished = computed(() => props.draft.publishStatus === 'PUBLISHED')
const showRuntimePreview = computed(() => Boolean(previewMode.value === 'runtime' && runtimeConfig.value))

const fieldMap = computed(() => new Map((modelSchema.value.fields || []).map(field => [field.field, field])))

const searchFields = computed(() => resolveFields('search', field => field.searchable))
const visibleColumns = computed(() => resolveFields('table', field => field.listVisible !== false))
const editFields = computed(() => resolveFields('edit', field => field.formVisible !== false))
const formPreviewGridCols = computed(() => resolveFormPreviewGridCols())
const formPreviewGridStyle = computed(() => ({
  gridTemplateColumns: `repeat(${formPreviewGridCols.value}, minmax(0, 1fr))`,
}))
const primaryModelCode = computed(() => pageSchema.value.primaryModelCode
  || pageSchema.value.modelRefs?.find(ref => ref.primary)?.modelCode
  || props.draft.modelSchema?.object?.code
  || '')
const formFields = computed(() => editFields.value.filter(field => isPrimaryFormField(field)))
const childFieldGroups = computed(() => {
  if (pageSchema.value.layoutType !== 'master-detail-crud')
    return []
  const groupMap = new Map()
  editFields.value.filter(field => !isPrimaryFormField(field)).forEach((field) => {
    const key = field.modelCode || 'children'
    if (!groupMap.has(key)) {
      groupMap.set(key, {
        key,
        name: field.modelName || key,
        fields: [],
      })
    }
    groupMap.get(key).fields.push(field)
  })
  return Array.from(groupMap.values())
})
const customToolbarActions = computed(() => resolveCustomActions('toolbar'))
const customRowActions = computed(() => resolveCustomActions('row'))

const tableColumns = computed(() => visibleColumns.value.map(field => ({
  key: field.field,
  title: field.label || field.field,
  minWidth: field.width || 140,
})).concat({
  key: 'actions',
  title: '操作',
  width: Math.max(196, (3 + customRowActions.value.length) * 62),
  fixed: 'right',
  render: row => renderDraftActions(row),
}))

const sampleRows = computed(() => {
  return Array.from({ length: 3 }).map((_, index) => {
    const row = { id: index + 1, actions: '编辑 / 删除' }
    visibleColumns.value.forEach((field) => {
      row[field.field] = resolveSampleValue(field, index)
    })
    return row
  })
})

const runtimeCrudProps = computed(() => buildRuntimeCrudProps(runtimeConfig.value))

watch(
  () => [props.draft.configKey, props.draft.publishStatus],
  () => {
    runtimeConfig.value = null
    runtimeError.value = ''
    previewMode.value = 'draft'
  },
)

function resolveFields(zoneKey, fallback) {
  const zone = pageSchema.value.zones?.find(item => item.zoneKey === zoneKey)
  const canvasRefs = resolveCanvasRefs(zone, zoneKey)
  if (['edit', 'detail'].includes(zoneKey) && canvasRefs.length) {
    return canvasRefs.map(ref => fieldMap.value.get(ref)).filter(field => field && isPageFieldVisible(field, zoneKey))
  }
  const formCreateRefs = zoneKey === 'edit'
    ? resolveFieldRefsFromFormCreateRules(zone?.props?.formCreateRule, new Set(fieldMap.value.keys()))
    : []
  if (formCreateRefs.length)
    return formCreateRefs.map(ref => fieldMap.value.get(ref)).filter(field => field && isPageFieldVisible(field, zoneKey))
  if (zone?.fieldRefs?.length) {
    return uniqueRefs(zone.fieldRefs).map(ref => fieldMap.value.get(ref)).filter(field => field && isPageFieldVisible(field, zoneKey))
  }
  if (canvasRefs.length) {
    return canvasRefs.map(ref => fieldMap.value.get(ref)).filter(field => field && isPageFieldVisible(field, zoneKey))
  }
  return (modelSchema.value.fields || []).filter(field => fallback(field) && isPageFieldVisible(field, zoneKey))
}

function isPrimaryFormField(field) {
  if (pageSchema.value.layoutType !== 'master-detail-crud')
    return true
  return !field.modelCode || field.modelCode === primaryModelCode.value
}

function resolveCanvasRefs(zone, zoneKey) {
  const items = sortCanvasItemsByPosition(zone?.props?.canvas?.items || [])
  const primary = zoneKey === 'table'
    ? items.find(item => item.componentKey === 'data-table')
    : zoneKey === 'search'
      ? items.find(item => item.componentKey === 'query-set')
      : null
  if (primary) {
    const refs = uniqueRefs(primary.fieldRefs || primary.props?.fieldRefs || [])
    if (refs.length)
      return refs
  }
  const refs = []
  items.forEach((item) => {
    if (item.fieldRef)
      refs.push(item.fieldRef)
    refs.push(...(item.fieldRefs || item.props?.fieldRefs || []))
  })
  return uniqueRefs(refs)
}

function uniqueRefs(refs) {
  return Array.from(new Set((refs || []).filter(Boolean)))
}

function sortCanvasItemsByPosition(items = []) {
  return [...items].sort((a, b) => {
    const rowA = Math.round(Number(a.y || 0) / 16)
    const rowB = Math.round(Number(b.y || 0) / 16)
    if (rowA !== rowB)
      return rowA - rowB
    const xDiff = Number(a.x || 0) - Number(b.x || 0)
    if (xDiff !== 0)
      return xDiff
    return Number(a.zIndex || 0) - Number(b.zIndex || 0)
  })
}

function resolveFormPreviewGridCols() {
  const configured = Number(editZone.value?.props?.editGridCols || 0)
  if (configured > 0)
    return Math.max(1, Math.min(3, configured))
  const items = [...(editZone.value?.props?.canvas?.items || [])]
    .filter(item => item.fieldRef)
    .sort((a, b) => Number(a.x || 0) - Number(b.x || 0))
  const columns = []
  items.forEach((item) => {
    const x = Number(item.x || 0)
    if (!columns.some(colX => Math.abs(colX - x) < 80))
      columns.push(x)
  })
  return Math.max(1, Math.min(3, columns.length || 1))
}

function resolveFormPreviewItemStyle(field) {
  const item = (editZone.value?.props?.canvas?.items || []).find(canvasItem => canvasItem.fieldRef === field.field)
  if (!item)
    return null
  const canvasWidth = Number(editZone.value?.props?.canvas?.width || 1040)
  const colWidth = Math.max(1, (canvasWidth - 64) / Math.max(1, formPreviewGridCols.value))
  const span = Math.max(1, Math.min(formPreviewGridCols.value, Math.round(Number(item.w || 280) / colWidth) || 1))
  return { gridColumn: `span ${span}` }
}

function resolveSampleValue(field, index = 0) {
  if (!field)
    return '示例值'
  if (field.dictType)
    return '字典值'
  if (field.componentType === 'switch' || field.dataType === 'tinyint')
    return index % 2 === 0 ? '是' : '否'
  if (field.componentType === 'number' || ['int', 'bigint', 'decimal'].includes(field.dataType))
    return field.dataType === 'decimal' ? `${128 + index}.00` : String(128 + index)
  if (field.componentType === 'date' || field.dataType === 'date')
    return '2026-05-20'
  if (field.componentType === 'datetime' || field.dataType === 'datetime')
    return '2026-05-20 09:30:00'
  return field.label ? `${field.label}示例${index + 1}` : `示例${index + 1}`
}

function resolveCustomActions(position) {
  return (tableZone.value?.props?.customActions || [])
    .filter(action => (action.position || 'toolbar') === position)
}

function resolveSearchPreviewType(field) {
  const setting = pageSchema.value.zones?.find(item => item.zoneKey === 'search')?.props?.fieldSettings?.[field.field]
    || {}
  const queryType = setting.queryType || field.queryType
  const componentType = field.componentType || field.dataType
  if (queryType === 'between') {
    if (componentType === 'datetime')
      return 'datetimerange'
    if (componentType === 'date')
      return 'daterange'
    if (componentType === 'time')
      return 'timerange'
  }
  if (componentType === 'datetime')
    return 'datetime'
  if (componentType === 'date')
    return 'date'
  if (componentType === 'time')
    return 'time'
  return 'input'
}

function renderDraftActions(_row) {
  const actions = [
    { key: 'edit', label: '编辑', type: 'primary' },
    { key: 'detail', label: '查看详情', type: 'info' },
    { key: 'delete', label: '删除', type: 'error' },
    ...customRowActions.value,
  ]
  return h('div', { class: 'draft-action-column' }, actions.map((action, index) => [
    index > 0 ? h('span', { class: 'draft-action-divider' }, ' | ') : null,
    h('span', { class: ['draft-action-link', `type-${action.type || 'default'}`] }, action.label),
  ]).flat().filter(Boolean))
}

async function refreshPreview() {
  if (!props.appId) {
    window.$message?.info('新应用保存草稿后可执行后端预览校验')
    return
  }
  loading.value = true
  errorMsg.value = ''
  try {
    await lowcodePreview(props.appId, props.draft)
    window.$message?.success('草稿配置校验通过')
  }
  catch (e) {
    errorMsg.value = e?.message || '预览失败'
  }
  finally {
    loading.value = false
  }
}

async function toggleRuntimePreview() {
  if (showRuntimePreview.value) {
    previewMode.value = 'draft'
    return
  }
  await loadRuntimePreview()
}

async function loadRuntimePreview() {
  if (!isPublished.value) {
    window.$message?.warning('应用发布后才能加载真实运行态')
    return
  }
  if (!props.draft.configKey) {
    window.$message?.warning('缺少应用编码，无法加载运行态')
    return
  }
  runtimeLoading.value = true
  runtimeError.value = ''
  try {
    const res = await crudConfigRender(props.draft.configKey)
    runtimeConfig.value = res.data
    await preloadDicts(runtimeConfig.value)
    previewMode.value = 'runtime'
  }
  catch (e) {
    runtimeError.value = e?.message || '真实运行态预览加载失败'
  }
  finally {
    runtimeLoading.value = false
  }
}

function openRuntimePage() {
  if (!isPublished.value || !props.draft.configKey)
    return
  const route = router.resolve(`/ai/crud-page/${props.draft.configKey}`)
  window.open(route.href, '_blank')
}

function buildRuntimeCrudProps(cfg) {
  if (!cfg)
    return {}
  const options = cfg.options || {}
  const fieldMetaMap = buildRuntimeFieldMetaMap(cfg.modelSchema)
  return {
    searchSchema: transformFields(cfg.searchSchema),
    columns: transformColumns(cfg.columnsSchema, cfg.transConfig),
    editSchema: transformEditFields(cfg.editSchema, options.editFormLayout, fieldMetaMap),
    childrenConfig: transformChildrenConfig(options.masterDetailConfig?.children || []),
    apiConfig: cfg.apiConfig || {},
    options,
    rowKey: cfg.rowKey || 'id',
    formOpenMode: options.formOpenMode || cfg.formOpenMode || options.modalType || cfg.modalType || 'modal',
    tabWorkspace: options.tabWorkspace || cfg.tabWorkspace || {},
    modalType: options.modalType || cfg.modalType || 'modal',
    modalWidth: options.modalWidth || cfg.modalWidth || '800px',
    editGridCols: options.editGridCols || cfg.editGridCols || 1,
    editLabelWidth: options.editLabelWidth || cfg.editLabelWidth || 'auto',
    editLabelPlacement: options.editLabelPlacement || cfg.editLabelPlacement || 'left',
    editLabelAlign: options.editLabelAlign || cfg.editLabelAlign || 'right',
    editSize: options.editSize || cfg.editSize || 'medium',
    editShowFeedback: options.editShowFeedback ?? cfg.editShowFeedback ?? true,
    editFormClass: options.editFormClass || cfg.editFormClass || '',
    editFormStyle: options.editFormStyle || cfg.editFormStyle,
    formAssets: options.formAssets || cfg.formAssets || [],
    editXGap: normalizeNumberOption(options.editXGap ?? cfg.editXGap, 16),
    editYGap: normalizeNumberOption(options.editYGap ?? cfg.editYGap, 16),
    loadDetailOnEdit: options.loadDetailOnEdit ?? cfg.loadDetailOnEdit ?? true,
    searchGridCols: options.searchGridCols || cfg.searchGridCols || 4,
    hideBatchDelete: !!options.hideBatchDelete,
    showImport: !!options.showImport,
    showExport: !!options.showExport,
    importApi: extractApiUrl(cfg.apiConfig?.import),
    exportApi: cfg.apiConfig?.export || '',
    importTemplateUrl: extractApiUrl(cfg.apiConfig?.importTemplate),
    enableCustomQuery: options.enableCustomQuery !== false,
    customQueryConfigKey: cfg.configKey,
    toolbarActions: options.toolbarActions || [],
  }
}

function transformColumns(columns, transConfig) {
  const transMap = {}
  const transTypeMap = {}
  if (transConfig && typeof transConfig === 'object') {
    for (const [field, conf] of Object.entries(transConfig)) {
      transMap[field] = conf.targetField || `${field}Name`
      if (conf.type)
        transTypeMap[field] = conf.type
    }
  }
  return (columns || []).map((col) => {
    const key = col.prop || col.key || col.dataIndex
    const nextCol = { ...col, prop: key }
    if (['actions', 'action'].includes(key) && Array.isArray(col.actions)) {
      nextCol.actions = ensureDetailRowAction(col.actions)
      nextCol.width = Math.max(Number(col.width) || 0, nextCol.actions.length * 58, 180)
    }
    if (col.render && typeof col.render === 'object') {
      const renderType = col.render.type
      if (renderType === 'dictTag') {
        nextCol.render = row => h(DictTag, {
          dictType: col.render.dictType,
          value: row[key],
          size: 'small',
        })
        return nextCol
      }
      if (renderType === 'orgName' || renderType === 'userName' || renderType === 'regionName') {
        const targetField = col.render.targetField || `${key}Name`
        nextCol.render = row => row[targetField] ?? row[key] ?? '-'
        return nextCol
      }
      if (renderType === 'imageUpload') {
        nextCol.render = (row) => {
          const value = row[key]
          if (!value)
            return '-'
          return h('span', { style: 'color: #2563eb' }, value)
        }
        return nextCol
      }
      if (renderType === 'fileUpload') {
        const targetField = col.render.targetField || `${key}Name`
        nextCol.render = row => row[targetField] ?? row[key] ?? '-'
        return nextCol
      }
    }
    if (transMap[key]) {
      const targetField = transMap[key]
      nextCol.render = row => row[targetField] ?? row[key]
      return nextCol
    }
    return nextCol
  })
}

function ensureDetailRowAction(actions = []) {
  if (actions.some(action => action?.key === 'detail'))
    return actions
  const next = [...actions]
  const editIndex = next.findIndex(action => action?.key === 'edit')
  const detailAction = { key: 'detail', label: '查看详情', type: 'info', position: 'row' }
  if (editIndex >= 0) {
    next.splice(editIndex + 1, 0, detailAction)
    return next
  }
  next.unshift(detailAction)
  return next
}

function transformFields(fields, fieldMetaMap = new Map()) {
  return (fields || []).map((field) => {
    const nextField = { ...field }
    applyRuntimeFieldMeta(nextField, fieldMetaMap.get(field.field || field.fieldCode))
    if (field.dictType && ['select', 'radio', 'checkbox'].includes(field.type)) {
      nextField.props = {
        ...(nextField.props || {}),
        options: dictCache.value[field.dictType] || [],
      }
    }
    const timeProps = resolveDateTimeProps(field.type)
    if (timeProps) {
      nextField.props = {
        ...(nextField.props || {}),
        ...timeProps,
      }
    }
    return nextField
  })
}

function transformEditFields(fields = [], layout = [], fieldMetaMap = new Map()) {
  const transformedFields = transformFields(fields, fieldMetaMap)
  if (!Array.isArray(layout) || !layout.length)
    return transformedFields

  const fieldMap = new Map(transformedFields.map(field => [field.field, field]))
  const usedFields = new Set()
  const nodes = layout
    .map(node => hydrateRuntimeLayoutNode(node, fieldMap, usedFields))
    .filter(Boolean)

  transformedFields.forEach((field) => {
    if (field.field && !usedFields.has(field.field))
      nodes.push(field)
  })
  return nodes
}

function buildRuntimeFieldMetaMap(modelSchema = {}) {
  const result = new Map()
  const fields = Array.isArray(modelSchema?.fields) ? modelSchema.fields : []
  fields.forEach((field) => {
    const fieldCode = field?.field || field?.fieldCode
    if (fieldCode)
      result.set(fieldCode, field)
  })
  return result
}

function applyRuntimeFieldMeta(field, meta = {}) {
  if (!field || !meta)
    return
  if (!field.dataType && meta.dataType)
    field.dataType = meta.dataType
  if (!field.fieldDataType && meta.dataType)
    field.fieldDataType = meta.dataType
  if (!field.componentType && meta.componentType)
    field.componentType = meta.componentType
  const formulaConfig = field.formulaConfig || meta.formulaConfig
  if (!hasRuntimeFormulaConfig(formulaConfig))
    return
  field.formulaConfig = formulaConfig
  field.required = false
  field.disabled = true
  field.readonly = true
  if (Array.isArray(field.rules))
    field.rules = field.rules.filter(rule => !rule?.required)
  field.props = {
    ...(field.props || {}),
    disabled: true,
    readonly: true,
  }
}

function hasRuntimeFormulaConfig(formulaConfig) {
  if (!formulaConfig)
    return false
  if (typeof formulaConfig === 'string')
    return formulaConfig.trim().length > 0
  return typeof formulaConfig === 'object' && Object.keys(formulaConfig).length > 0
}

function hydrateRuntimeLayoutNode(node = {}, fieldMap, usedFields) {
  if (!node || typeof node !== 'object')
    return null
  const nodeType = resolveRuntimeLayoutNodeType(node)
  if (node.nodeType === 'field') {
    const field = fieldMap.get(node.field)
    if (!field)
      return null
    usedFields.add(node.field)
    return {
      ...field,
      nodeType: 'field',
      key: node.key || field.field,
      span: node.span || field.span,
      gridStyle: node.gridStyle || field.gridStyle,
    }
  }

  const children = (node.children || [])
    .map(child => hydrateRuntimeLayoutNode(child, fieldMap, usedFields))
    .filter(Boolean)
  if (!children.length && !isStandaloneRuntimeLayoutNode({ ...node, nodeType }))
    return null
  return {
    ...node,
    nodeType,
    children,
  }
}

function resolveRuntimeLayoutNodeType(node = {}) {
  if (isGroupTitleRuntimeLayoutNode(node))
    return 'groupTitle'
  if (isLegacyGroupTitleRuntimeLayoutNode(node))
    return 'groupTitle'
  if (isSectionTitleRuntimeLayoutNode(node))
    return 'divider'
  if (isActionRuntimeLayoutNode(node))
    return node.componentKey || node.type || node.nodeType
  return node.nodeType
}

function isGroupTitleRuntimeLayoutNode(node = {}) {
  return ['title', 'fcTitle', 'sectionTitle', 'groupTitle', 'groupHeader', 'GroupHeader', 'titleBlock', 'section']
    .includes(node.componentKey || node.type || node.nodeType)
}

function isSectionTitleRuntimeLayoutNode(node = {}) {
  return ['divider', 'elDivider', 'AiFormSectionTitle', 'aiFormSectionTitle', 'formSectionTitle', 'FormSectionTitle']
    .includes(node.componentKey || node.type || node.nodeType)
}

function isLegacyGroupTitleRuntimeLayoutNode(node = {}) {
  const props = node.props || {}
  return node.nodeType === 'divider'
    && !node.componentKey
    && Object.prototype.hasOwnProperty.call(props, 'description')
    && !Object.prototype.hasOwnProperty.call(props, 'title')
}

function isStandaloneRuntimeLayoutNode(node = {}) {
  return isSectionTitleRuntimeLayoutNode(node) || isGroupTitleRuntimeLayoutNode(node) || isActionRuntimeLayoutNode(node)
}

function isActionRuntimeLayoutNode(node = {}) {
  return ['button', 'table', 'tableGrid', 'AiCrudPage', 'aiCrudPage', 'crud', 'crudBlock']
    .includes(node.componentKey || node.type || node.nodeType)
}

function resolveDateTimeProps(type) {
  switch (String(type || '').toLowerCase()) {
    case 'date':
    case 'daterange':
      return { format: 'yyyy-MM-dd', valueFormat: 'yyyy-MM-dd' }
    case 'datetime':
    case 'datetimerange':
      return { format: 'yyyy-MM-dd HH:mm:ss', valueFormat: 'yyyy-MM-dd HH:mm:ss' }
    case 'time':
    case 'timerange':
      return { format: 'HH:mm:ss', valueFormat: 'HH:mm:ss' }
    default:
      return null
  }
}

function transformChildrenConfig(children = []) {
  return (children || []).map(child => ({
    ...child,
    fields: transformFields(child.fields || []),
  }))
}

async function preloadDicts(cfg) {
  const types = new Set()
  ;(cfg?.columnsSchema || []).forEach((col) => {
    if (col.render?.dictType)
      types.add(col.render.dictType)
  })
  ;[...(cfg?.searchSchema || []), ...(cfg?.editSchema || [])].forEach((field) => {
    if (field.dictType)
      types.add(field.dictType)
  })
  ;(cfg?.options?.masterDetailConfig?.children || []).forEach((child) => {
    ;(child.fields || []).forEach((field) => {
      if (field.dictType)
        types.add(field.dictType)
    })
  })
  for (const type of types) {
    if (!dictCache.value[type]) {
      try {
        dictCache.value[type] = await getDictData(type)
      }
      catch (e) {
        console.warn(`[lowcode-preview] 加载字典 ${type} 失败`, e)
      }
    }
  }
}

function extractApiUrl(apiConfigValue) {
  if (!apiConfigValue)
    return ''
  const parts = String(apiConfigValue).split('@')
  return parts.length > 1 ? parts.slice(1).join('@') : apiConfigValue
}

function normalizeNumberOption(value, fallback) {
  const number = Number(value)
  return Number.isFinite(number) ? number : fallback
}
</script>

<style scoped>
.preview-pane {
  display: grid;
  gap: 14px;
}

.preview-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.preview-title {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.preview-desc {
  margin-top: 2px;
  font-size: 12px;
  color: #64748b;
}

.preview-alert {
  border-radius: 8px;
}

.preview-surface {
  display: grid;
  gap: 14px;
}

.runtime-crud-preview {
  display: grid;
  gap: 12px;
  min-height: 620px;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  background: #fff;
  padding: 14px;
}

.runtime-preview-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #eef2f7;
}

.runtime-preview-head p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
}

.preview-band {
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.runtime-surface {
  padding: 0;
  overflow: hidden;
}

.runtime-surface .band-title {
  margin: 0;
  padding: 14px 16px;
  border-bottom: 1px solid #eef2f7;
}

.runtime-layout {
  display: grid;
  gap: 0;
}

.runtime-layout.tree {
  grid-template-columns: 240px minmax(0, 1fr);
}

.tree-preview {
  padding: 14px;
  border-right: 1px solid #eef2f7;
  background: #f8fafc;
}

.tree-title {
  margin-bottom: 10px;
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
}

.tree-node {
  min-height: 32px;
  display: flex;
  align-items: center;
  padding: 0 10px;
  border-radius: 6px;
  color: #475569;
  font-size: 12px;
}

.tree-node.active {
  background: #eff6ff;
  color: #1d4ed8;
  font-weight: 700;
}

.runtime-main {
  padding: 14px;
}

.query-set-preview {
  margin-bottom: 12px;
  padding: 14px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f8fafc;
}

.query-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.preview-time-range {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  gap: 8px;
  align-items: center;
}

.preview-time-range span {
  color: #64748b;
  font-size: 12px;
}

.band-title {
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 12px;
}

.table-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.table-actions .band-title {
  margin-bottom: 0;
}

.draft-action-column {
  white-space: nowrap;
}

.draft-action-link {
  color: #2563eb;
  cursor: default;
  font-size: 12px;
}

.draft-action-link.type-error,
.draft-action-link.type-danger {
  color: #dc2626;
}

.draft-action-link.type-warning {
  color: #d97706;
}

.draft-action-link.type-info {
  color: #475569;
}

.draft-action-link.type-success {
  color: #16a34a;
}

.draft-action-divider {
  color: #cbd5e1;
}

.search-grid,
.form-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.child-preview-groups {
  display: grid;
  gap: 10px;
  margin-top: 14px;
  border-top: 1px solid #eef2f7;
  padding-top: 12px;
}

.child-preview-group {
  display: grid;
  gap: 8px;
}

.child-preview-title {
  color: #0f172a;
  font-size: 13px;
  font-weight: 700;
}

.child-preview-table {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #f8fafc;
  padding: 10px;
}

.child-preview-table span {
  border: 1px solid #dbe3ee;
  border-radius: 6px;
  background: #fff;
  color: #475569;
  font-size: 12px;
  padding: 5px 8px;
}

@media (max-width: 1180px) {
  .runtime-layout.tree {
    grid-template-columns: 1fr;
  }

  .tree-preview {
    border-right: 0;
    border-bottom: 1px solid #eef2f7;
  }

  .search-grid,
  .form-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
