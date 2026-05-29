<template>
  <BusinessObjectDesignerShell
    v-model:active-panel="activePanel"
    :designer="designer"
    :loading="loading"
    :dirty="dirty"
    :saving="saving"
    :publishing="publishing"
    :publish-disabled="publishDisabled"
    :preview-disabled="!objectId"
    :show-advanced="canAdvanced"
    @save="handleSave"
    @preview="handlePreview"
    @publish="handlePublish"
    @back="handleBack"
    @refresh="loadDesigner"
    @open-runtime="openRuntime"
  >
    <section v-if="activePanel === 'basic'" class="basic-panel">
      <div class="panel-head">
        <div>
          <h2>基本信息</h2>
          <p>维护业务对象的名称、说明、显示字段和启停状态。</p>
        </div>
      </div>
      <n-form label-placement="top" :show-feedback="false" class="basic-form">
        <n-grid :cols="2" :x-gap="16" :y-gap="4" responsive="screen">
          <n-form-item-gi label="对象名称">
            <n-input v-model:value="draft.objectName" placeholder="例如：客户" @update:value="markDirty" />
          </n-form-item-gi>
          <n-form-item-gi label="显示字段">
            <n-select
              v-model:value="draft.displayField"
              :options="fieldOptions"
              clearable
              filterable
              placeholder="选择运行态列表和关联回显字段"
              @update:value="markDirty"
            />
          </n-form-item-gi>
          <n-form-item-gi label="对象图标">
            <n-input v-model:value="draft.icon" placeholder="例如：i-carbon-user" @update:value="markDirty" />
          </n-form-item-gi>
          <n-form-item-gi label="启用状态">
            <n-switch
              :value="draft.status !== 0"
              @update:value="updateStatus"
            />
          </n-form-item-gi>
          <n-form-item-gi :span="2" label="对象说明">
            <n-input
              v-model:value="draft.description"
              type="textarea"
              :rows="4"
              placeholder="描述对象的业务含义和使用场景"
              @update:value="markDirty"
            />
          </n-form-item-gi>
        </n-grid>
      </n-form>
    </section>

    <BusinessFieldManager
      v-else-if="activePanel === 'fields'"
      ref="fieldManagerRef"
      :object-id="objectId"
      :fields="draft.fields"
      :developer-mode="developerMode"
      @updated="handleFieldsUpdated"
      @dirty-change="handleDirtyChange"
    />

    <BusinessFormDesigner
      v-else-if="activePanel === 'form'"
      ref="formDesignerRef"
      v-model="draft.pageSchema"
      :object-id="objectId"
      :model-schema="draft.modelSchema"
      :fields="draft.fields"
      @saved="handleLayoutSaved"
      @dirty-change="handleDirtyChange"
      @create-field="activePanel = 'fields'"
    />

    <BusinessListDesigner
      v-else-if="activePanel === 'list'"
      ref="listDesignerRef"
      v-model="draft.pageSchema"
      :object-id="objectId"
      :model-schema="draft.modelSchema"
      :fields="draft.fields"
      @saved="handleLayoutSaved"
      @dirty-change="handleDirtyChange"
    />

    <BusinessDetailDesigner
      v-else-if="activePanel === 'detail'"
      ref="detailDesignerRef"
      v-model="draft.pageSchema"
      :object-id="objectId"
      :model-schema="draft.modelSchema"
      :fields="draft.fields"
      :relations="draft.relations"
      @saved="handleLayoutSaved"
      @dirty-change="handleDirtyChange"
    />

    <BusinessRelationDesigner
      v-else-if="activePanel === 'relations'"
      ref="relationDesignerRef"
      :object-id="objectId"
      :suite-code="draft.suiteCode"
      :object-code="draft.objectCode"
      :object-name="draft.objectName"
      :fields="draft.fields"
      @updated="handleRelationsUpdated"
      @dirty-change="handleDirtyChange"
    />

    <BusinessActionDesigner
      v-else-if="activePanel === 'actions'"
      ref="actionDesignerRef"
      :object-id="objectId"
      @updated="handleActionsUpdated"
      @dirty-change="handleDirtyChange"
    />

    <BusinessPermissionFlowPanel
      v-else-if="activePanel === 'permission'"
      ref="permissionFlowRef"
      :object-id="objectId"
      :object-code="draft.objectCode"
    />

    <BusinessPublishChecklist
      v-else-if="activePanel === 'publish'"
      ref="publishChecklistRef"
      :object-id="objectId"
      :runtime-info="runtimeInfo"
      :publishing="publishing"
      @check-updated="handlePublishCheckUpdated"
      @fix="handleFixTarget"
      @publish="handlePublish"
      @open-runtime="openRuntime"
      @rolled-back="loadDesigner"
    />

    <BusinessAdvancedConfig
      v-else-if="activePanel === 'advanced'"
      v-model:developer-mode="developerMode"
      :draft="draft"
      :can-advanced="canAdvanced"
      @open-developer="openDeveloperPath"
    />
  </BusinessObjectDesignerShell>
</template>

<script setup>
import { useMessage } from 'naive-ui'
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import {
  businessObjectDesigner,
  businessObjectList,
  businessObjectRuntimeInfo,
  previewBusinessObjectLayout,
  publishBusinessObject,
  saveBusinessObjectDesigner,
} from '@/api/business-app'
import { cloneSchema } from '@/components/lowcode-builder/model/model-schema'
import { useTabStore, useUserStore } from '@/store'
import BusinessActionDesigner from './components/designer/BusinessActionDesigner.vue'
import BusinessAdvancedConfig from './components/designer/BusinessAdvancedConfig.vue'
import BusinessDetailDesigner from './components/designer/BusinessDetailDesigner.vue'
import BusinessFieldManager from './components/designer/BusinessFieldManager.vue'
import BusinessFormDesigner from './components/designer/BusinessFormDesigner.vue'
import BusinessListDesigner from './components/designer/BusinessListDesigner.vue'
import BusinessObjectDesignerShell from './components/designer/BusinessObjectDesignerShell.vue'
import BusinessPermissionFlowPanel from './components/designer/BusinessPermissionFlowPanel.vue'
import BusinessPublishChecklist from './components/designer/BusinessPublishChecklist.vue'
import BusinessRelationDesigner from './components/designer/BusinessRelationDesigner.vue'

const route = useRoute()
const router = useRouter()
const message = useMessage()
const tabStore = useTabStore()
const userStore = useUserStore()

const loading = ref(false)
const saving = ref(false)
const publishing = ref(false)
const dirty = ref(false)
const ready = ref(false)
const activePanel = ref(route.query.panel || 'fields')
const developerMode = ref(false)
const designer = ref(null)
const runtimeInfo = ref(null)
const publishCheckState = ref(null)
const fieldManagerRef = ref(null)
const formDesignerRef = ref(null)
const listDesignerRef = ref(null)
const detailDesignerRef = ref(null)
const relationDesignerRef = ref(null)
const actionDesignerRef = ref(null)
const permissionFlowRef = ref(null)
const publishChecklistRef = ref(null)
const draft = reactive(createEmptyDraft())

const objectCode = computed(() => route.params.objectCode)
const suiteCode = computed(() => route.query.suiteCode || draft.suiteCode)
const objectId = computed(() => designer.value?.objectId || draft.objectId)
const pageTitle = computed(() => `${draft.objectName || objectCode.value || '业务对象'}设计`)
const canAdvanced = computed(() => {
  return userStore.isAdmin
    || hasPermission(userStore.permissions, 'ai:businessObject:advanced')
    || hasPermission(userStore.apiPermissions, 'ai:businessObject:advanced')
    || hasPermission(userStore.getDataPermission, 'ai:businessObject:advanced')
})
const publishDisabled = computed(() => {
  if (!objectId.value)
    return true
  return publishCheckState.value?.publishable === false
})
const fieldOptions = computed(() => {
  const modelFields = draft.modelSchema?.fields || []
  const source = modelFields.length ? modelFields : draft.fields.map(toPageField)
  return source
    .filter(field => field.field)
    .map(field => ({
      label: `${field.label || field.field}（${field.field}）`,
      value: field.field,
    }))
})
onMounted(() => {
  loadDesigner()
  window.addEventListener('beforeunload', handleBeforeUnload)
})

onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload)
})

onBeforeRouteLeave(() => {
  if (!dirty.value)
    return true
  return confirmLeave()
})

watch(pageTitle, (title) => {
  if (!title)
    return
  route.meta.title = title
  document.title = `${title} | ${import.meta.env.VITE_TITLE}`
  tabStore.updateTabTitle(route.fullPath, title)
}, { immediate: true })

watch(activePanel, (panel) => {
  router.replace({
    path: route.path,
    query: {
      ...route.query,
      panel,
    },
  })
})

watch(canAdvanced, (value) => {
  if (!value && activePanel.value === 'advanced')
    activePanel.value = 'fields'
}, { immediate: true })

async function loadDesigner() {
  loading.value = true
  ready.value = false
  try {
    const object = await resolveBusinessObject()
    if (!object?.id) {
      message.warning('未找到业务对象')
      return
    }
    const res = await businessObjectDesigner(object.id)
    designer.value = res.data || null
    Object.assign(draft, createDraftFromDesigner(designer.value))
    if (object.id)
      await loadRuntimeInfo(object.id)
    await nextTick()
    dirty.value = false
    ready.value = true
  }
  finally {
    loading.value = false
  }
}

async function resolveBusinessObject() {
  if (route.query.objectId)
    return { id: Array.isArray(route.query.objectId) ? route.query.objectId[0] : route.query.objectId }
  const res = await businessObjectList({
    suiteCode: suiteCode.value,
    objectCode: objectCode.value,
  })
  return (res.data || [])[0] || null
}

async function loadRuntimeInfo(id = objectId.value) {
  if (!id)
    return
  const res = await businessObjectRuntimeInfo(id)
  runtimeInfo.value = res.data || null
}

async function handleSave() {
  if (activePanel.value === 'fields') {
    await fieldManagerRef.value?.saveSelectedField?.()
    return
  }
  if (activePanel.value === 'form') {
    await formDesignerRef.value?.saveLayout?.()
    await saveDesignerDraft(false)
    return
  }
  if (activePanel.value === 'list') {
    await listDesignerRef.value?.saveLayout?.()
    await saveDesignerDraft(false)
    return
  }
  if (activePanel.value === 'detail') {
    await detailDesignerRef.value?.saveLayout?.()
    await saveDesignerDraft(false)
    return
  }
  if (activePanel.value === 'relations') {
    await relationDesignerRef.value?.saveRelations?.()
    await loadDesigner()
    return
  }
  if (activePanel.value === 'actions') {
    await actionDesignerRef.value?.saveActions?.()
    await loadDesigner()
    return
  }
  await saveDesignerDraft(true)
}

async function saveDesignerDraft(showMessage = true) {
  if (!objectId.value)
    return
  saving.value = true
  try {
    await saveBusinessObjectDesigner(objectId.value, buildDesignerPayload())
    dirty.value = false
    if (showMessage)
      message.success('设计器已保存')
    await loadDesigner()
  }
  finally {
    saving.value = false
  }
}

async function handlePreview() {
  if (!objectId.value)
    return
  const layoutKey = activePanel.value === 'list' ? 'list' : activePanel.value === 'detail' ? 'detail' : 'form'
  const res = await previewBusinessObjectLayout(objectId.value, {
    layoutKey,
    layoutName: layoutKey === 'list' ? '列表布局预览' : layoutKey === 'detail' ? '详情布局预览' : '表单布局预览',
    layoutType: draft.pageSchema?.layoutType,
    pageSchema: cloneSchema(draft.pageSchema || {}),
    zones: draft.pageSchema?.zones || [],
    settings: {},
  })
  if (res.data) {
    message.success('运行配置预览已生成，可进入发布检查继续确认')
    activePanel.value = 'publish'
  }
}

async function handlePublish() {
  if (!objectId.value)
    return
  if (publishCheckState.value?.publishable === false) {
    message.warning('发布检查存在阻断项，请先修复')
    activePanel.value = 'publish'
    return
  }
  if (dirty.value) {
    await handleSave()
  }
  publishing.value = true
  try {
    const res = await publishBusinessObject(objectId.value, {
      publishMode: 'PUBLISH',
      syncTable: false,
      force: false,
      modelSchema: cloneSchema(draft.modelSchema || {}),
      pageSchema: cloneSchema(draft.pageSchema || {}),
      publishOptions: {},
    })
    message.success(res.data ? `业务对象已发布，版本 ${res.data}` : '业务对象已发布')
    await loadRuntimeInfo()
    await loadDesigner()
    await publishChecklistRef.value?.refresh?.()
  }
  finally {
    publishing.value = false
  }
}

function handleBack() {
  if (route.query.returnTo) {
    router.push(String(route.query.returnTo))
    return
  }
  if (suiteCode.value)
    router.push(`/app-center/suite/${suiteCode.value}`)
  else
    router.push(`/app-center/object/${objectCode.value}`)
}

function openRuntime() {
  if (!runtimeInfo.value?.canOpen) {
    message.warning(runtimeInfo.value?.message || '应用暂不可打开')
    return
  }
  router.push(runtimeInfo.value.routePath)
}

function handleFieldsUpdated(fields) {
  draft.fields = cloneSchema(fields || [])
  dirty.value = false
  loadDesigner()
}

function handleLayoutSaved(pageSchema) {
  draft.pageSchema = cloneSchema(pageSchema || draft.pageSchema)
  dirty.value = false
}

function handleRelationsUpdated(relations) {
  draft.relations = cloneSchema(relations || [])
}

function handleActionsUpdated(actions) {
  draft.designerOptions = {
    ...(draft.designerOptions || {}),
    actions: cloneSchema(actions || []),
  }
}

function handlePublishCheckUpdated(check) {
  publishCheckState.value = check || null
}

function handleFixTarget(panel) {
  if (panel === 'advanced' && !canAdvanced.value) {
    message.warning('该修复入口需要高级配置权限')
    return
  }
  activePanel.value = panel || 'fields'
}

function openDeveloperPath(path) {
  if (!path)
    return
  router.push(path)
}

function handleDirtyChange(value) {
  if (!ready.value)
    return
  dirty.value = !!value
}

function markDirty() {
  if (ready.value)
    dirty.value = true
}

function updateStatus(value) {
  draft.status = value ? 1 : 0
  markDirty()
}

function buildDesignerPayload() {
  return {
    objectId: objectId.value,
    objectName: draft.objectName,
    description: draft.description,
    icon: draft.icon,
    displayField: draft.displayField,
    status: draft.status,
    designStatus: draft.designStatus,
    modelSchema: cloneSchema(draft.modelSchema || {}),
    pageSchema: cloneSchema(draft.pageSchema || {}),
    fields: draft.fields.map(toFieldPayload),
    relations: cloneSchema(draft.relations || []),
    designerOptions: cloneSchema(draft.designerOptions || {}),
  }
}

function createEmptyDraft() {
  return {
    objectId: null,
    appId: null,
    suiteCode: '',
    suiteName: '',
    objectCode: '',
    objectName: '',
    configKey: '',
    displayField: '',
    icon: '',
    description: '',
    status: 1,
    designStatus: 'DRAFT',
    publishStatus: 'UNPUBLISHED',
    lastPublishVersion: null,
    modelSchema: null,
    pageSchema: null,
    fields: [],
    relations: [],
    designerOptions: {},
  }
}

function createDraftFromDesigner(value = {}) {
  return {
    ...createEmptyDraft(),
    ...cloneSchema(value || {}),
    status: value?.status ?? 1,
    modelSchema: cloneSchema(value?.modelSchema || {}),
    pageSchema: cloneSchema(value?.pageSchema || null),
    fields: cloneSchema(value?.fields || []),
    relations: cloneSchema(value?.relations || []),
    designerOptions: cloneSchema(value?.designerOptions || {}),
  }
}

function toFieldPayload(field = {}) {
  return {
    fieldName: field.fieldName,
    fieldCode: field.fieldCode,
    columnName: field.columnName,
    fieldType: field.fieldType,
    dataType: field.dataType,
    length: field.length,
    precision: field.precision,
    required: field.required,
    defaultValue: field.defaultValue,
    searchable: field.searchable,
    listVisible: field.listVisible,
    formVisible: field.formVisible,
    importable: field.importable,
    exportable: field.exportable,
    componentType: field.componentType,
    queryType: field.queryType,
    dictType: field.dictType,
    sensitiveType: field.sensitiveType,
    encryptAlgorithm: field.encryptAlgorithm,
    sortable: field.sortable,
    systemField: field.systemField,
    readonly: field.readonly,
    fieldStatus: field.fieldStatus,
    referenceObjectCode: field.referenceObjectCode,
    referenceDisplayField: field.referenceDisplayField,
    placeholder: field.basicProps?.placeholder || '',
    remark: field.remark,
    sortOrder: field.sortOrder,
    basicProps: cloneSchema(field.basicProps || {}),
    advancedProps: cloneSchema(field.advancedProps || {}),
  }
}

function toPageField(field = {}) {
  return {
    ...field,
    field: field.field || field.fieldCode,
    label: field.label || field.fieldName || field.fieldCode,
    columnName: field.columnName,
    dataType: field.dataType,
    componentType: field.componentType,
    dictType: field.dictType,
    required: field.required,
    systemField: field.systemField,
    readonly: field.readonly,
    searchable: field.searchable,
    listVisible: field.listVisible,
    formVisible: field.formVisible,
  }
}

function handleBeforeUnload(event) {
  if (!dirty.value)
    return
  event.preventDefault()
  event.returnValue = ''
}

function confirmLeave() {
  return new Promise((resolve) => {
    if (!window.$dialog) {
      resolve(false)
      return
    }
    window.$dialog.warning({
      title: '未保存变更',
      content: '当前设计器有未保存变更，确认离开吗？',
      positiveText: '离开',
      negativeText: '取消',
      onPositiveClick: () => resolve(true),
      onNegativeClick: () => resolve(false),
      onClose: () => resolve(false),
    })
  })
}

function hasPermission(source, permission) {
  if (!Array.isArray(source))
    return false
  return source.includes(permission) || source.includes('**') || source.includes('*:*:*')
}
</script>

<style scoped>
.basic-panel,
.placeholder-panel {
  min-height: calc(100vh - 106px);
  padding: 20px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.panel-head h2 {
  margin: 0;
  color: #111827;
  font-size: 18px;
  letter-spacing: 0;
}

.panel-head p {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 13px;
}

.basic-form {
  max-width: 920px;
}

.advanced-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.advanced-grid div {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 12px;
}

.advanced-grid span {
  display: block;
  color: #64748b;
  font-size: 12px;
}

.advanced-grid code {
  display: block;
  overflow: hidden;
  margin-top: 6px;
  color: #111827;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
