<template>
  <BusinessObjectDesignerShell
    :active-panel="activePanel"
    :designer="designer"
    :loading="loading"
    :dirty="dirty"
    :saving="saving"
    :publishing="publishing"
    :publish-disabled="publishDisabled"
    :preview-disabled="!objectId"
    :show-advanced="canAdvanced"
    :closure-steps="closureSteps"
    @save="handleSave"
    @preview="handlePreview"
    @publish="handlePublish"
    @back="handleBack"
    @refresh="loadDesigner"
    @open-runtime="openRuntime"
    @open-trigger="openTriggerConfig"
    @open-function-market="functionMarketVisible = true"
    @update:active-panel="handlePanelSwitch"
  >
    <section v-if="activePanel === 'basic'" class="basic-panel">
      <div class="panel-head">
        <div>
          <h2>基本信息</h2>
          <p>维护业务单元的名称、说明、显示字段和启停状态。</p>
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
      :object-code="draft.objectCode || objectCode"
      :fields="draft.fields"
      :relations="draft.relations"
      :form-designer-schema="draft.formDesignerSchema"
      :developer-mode="developerMode"
      @updated="handleFieldsUpdated"
      @dirty-change="handleDirtyChange"
      @add-to-form="handleAddFieldToForm"
    />

    <section v-else-if="activePanel === 'form'" class="form-detail-panel">
      <n-tabs v-model:value="formDetailTab" type="line">
        <n-tab-pane name="form" tab="表单设计">
          <BusinessFormDesigner
            ref="formDesignerRef"
            v-model="draft.pageSchema"
            v-model:form-designer-schema="draft.formDesignerSchema"
            :object-id="objectId"
            :object-code="draft.objectCode"
            :object-name="draft.objectName"
            :model-schema="draft.modelSchema"
            :fields="draft.fields"
            :relations="draft.relations"
            @saved="handleLayoutSaved"
            @fields-updated="handleFieldsUpdated"
            @dirty-change="handleDirtyChange"
            @create-field="handlePanelSwitch('fields')"
            @open-relations="handlePanelSwitch('relations')"
          />
        </n-tab-pane>
        <n-tab-pane name="detail" tab="详情设置">
          <BusinessDetailDesigner
            ref="detailDesignerRef"
            v-model="draft.pageSchema"
            v-model:view-schema="draft.viewSchema"
            :object-id="objectId"
            :model-schema="draft.modelSchema"
            :fields="draft.fields"
            :relations="draft.relations"
            @saved="handleLayoutSaved"
            @dirty-change="handleDirtyChange"
            @open-form="formDetailTab = 'form'"
            @open-relations="handlePanelSwitch('relations')"
          />
        </n-tab-pane>
      </n-tabs>
    </section>

    <BusinessListDesigner
      v-else-if="activePanel === 'list'"
      ref="listDesignerRef"
      v-model="draft.pageSchema"
      v-model:view-schema="draft.viewSchema"
      :object-id="objectId"
      :model-schema="draft.modelSchema"
      :fields="draft.fields"
      :form-options="runtimeFormOptions"
      @saved="handleLayoutSaved"
      @dirty-change="handleDirtyChange"
    />

    <BusinessRelationDesigner
      v-else-if="activePanel === 'relations'"
      ref="relationDesignerRef"
      v-model:linkage-schema="draft.linkageSchema"
      :object-id="objectId"
      :suite-code="draft.suiteCode"
      :object-code="draft.objectCode"
      :object-name="draft.objectName"
      :fields="draft.fields"
      @updated="handleRelationsUpdated"
      @fields-updated="handleFieldsUpdated"
      @dirty-change="handleDirtyChange"
    />

    <BusinessDocumentPanel
      v-else-if="activePanel === 'document'"
      ref="documentPanelRef"
      :object-id="objectId"
      :suite-code="draft.suiteCode"
      :object-code="draft.objectCode"
      :object-name="draft.objectName"
      :fields="draft.fields"
      :initial-config="draft.documentConfig"
      @saved="handleDocumentSaved"
      @configure-flow="handlePanelSwitch('automation')"
      @dirty-change="handleDirtyChange"
    />

    <BusinessFlowBindingPanel
      v-else-if="activePanel === 'automation'"
      ref="flowBindingPanelRef"
      :object-code="draft.objectCode"
      :fields="draft.fields"
      @saved="handleFlowSaved"
      @dirty-change="handleDirtyChange"
    />

    <BusinessActionDesigner
      v-else-if="activePanel === 'actions'"
      ref="actionDesignerRef"
      :object-id="objectId"
      :fields="draft.fields"
      :form-options="runtimeFormOptions"
      @updated="handleActionsUpdated"
      @dirty-change="handleDirtyChange"
    />

    <BusinessPermissionFlowPanel
      v-else-if="activePanel === 'permission'"
      ref="permissionFlowRef"
      v-model:model-schema="draft.modelSchema"
      v-model:page-schema="draft.pageSchema"
      :fields="draft.fields"
      :object-name="draft.objectName"
      @dirty-change="handleDirtyChange"
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

  <FormulaFunctionMarket v-model:show="functionMarketVisible" />
</template>

<script setup>
import { useMessage } from 'naive-ui'
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import {
  businessObjectDesigner,
  businessObjectList,
  businessObjectPublishCheck,
  businessObjectRuntimeInfo,
  publishBusinessObject,
  saveBusinessObjectDesigner,
} from '@/api/business-app'
import { cloneSchema } from '@/components/lowcode-builder/model/model-schema'
import { useTabStore, useUserStore } from '@/store'
import { getDefaultPageTitle } from '@/utils/page-title'
import BusinessActionDesigner from './components/designer/BusinessActionDesigner.vue'
import BusinessAdvancedConfig from './components/designer/BusinessAdvancedConfig.vue'
import BusinessDetailDesigner from './components/designer/BusinessDetailDesigner.vue'
import BusinessDocumentPanel from './components/designer/BusinessDocumentPanel.vue'
import BusinessFieldManager from './components/designer/BusinessFieldManager.vue'
import BusinessFlowBindingPanel from './components/designer/BusinessFlowBindingPanel.vue'
import BusinessFormDesigner from './components/designer/BusinessFormDesigner.vue'
import BusinessListDesigner from './components/designer/BusinessListDesigner.vue'
import BusinessObjectDesignerShell from './components/designer/BusinessObjectDesignerShell.vue'
import BusinessPermissionFlowPanel from './components/designer/BusinessPermissionFlowPanel.vue'
import BusinessPublishChecklist from './components/designer/BusinessPublishChecklist.vue'
import BusinessRelationDesigner from './components/designer/BusinessRelationDesigner.vue'
import { renameFormDesignerFieldRefs } from './components/designer/form-first/fieldReferenceUtils'
import { normalizeMultiFormDesignerSchema } from './components/designer/form-first/formDesignerSchema'
import { renameViewSchemaFieldRefs, sanitizeViewSchemaFieldRefs } from './components/designer/form-first/viewSchema'
import FormulaFunctionMarket from './components/designer/formula/FormulaFunctionMarket.vue'

const props = defineProps({
  embedded: {
    type: Boolean,
    default: false,
  },
  embeddedObjectCode: {
    type: String,
    default: '',
  },
  embeddedObjectId: {
    type: [Number, String],
    default: null,
  },
  embeddedSuiteCode: {
    type: String,
    default: '',
  },
  initialPanel: {
    type: String,
    default: 'form',
  },
  initialDetailTab: {
    type: String,
    default: 'form',
  },
})

const emit = defineEmits(['close', 'saved'])

const route = useRoute()
const router = useRouter()
const message = useMessage()
const tabStore = useTabStore()
const userStore = useUserStore()

const loading = ref(false)
const saving = ref(false)
const publishing = ref(false)
const dirty = ref(false)
const designerDraftDirty = ref(false)
const ready = ref(false)
const activePanel = ref(resolveInitialPanel())
const formDetailTab = ref(resolveInitialDetailTab())
const developerMode = ref(false)
const designer = ref(null)
const runtimeInfo = ref(null)
const publishCheckState = ref(null)
const functionMarketVisible = ref(false)
const fieldManagerRef = ref(null)
const formDesignerRef = ref(null)
const listDesignerRef = ref(null)
const detailDesignerRef = ref(null)
const relationDesignerRef = ref(null)
const documentPanelRef = ref(null)
const flowBindingPanelRef = ref(null)
const actionDesignerRef = ref(null)
const permissionFlowRef = ref(null)
const publishChecklistRef = ref(null)
const draft = reactive(createEmptyDraft())

const objectCode = computed(() => props.embedded ? props.embeddedObjectCode : route.params.objectCode)
const suiteCode = computed(() => (props.embedded ? props.embeddedSuiteCode : route.query.suiteCode) || draft.suiteCode)
const objectId = computed(() => designer.value?.objectId || draft.objectId)
const pageTitle = computed(() => `${draft.objectName || objectCode.value || '业务单元'}设计`)
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
const closureSteps = computed(() => {
  const documentConfig = draft.documentConfig || {}
  const mainFlow = documentConfig.mainFlowSummary || {}
  const startMode = normalizeStartMode(mainFlow.startMode)
  const triggerRequired = startMode === 'TRIGGER' || startMode === 'BOTH'
  const hasTriggerGap = hasPublishItem('DOCUMENT_TRIGGER_MISSING')
  return [
    step('document', '单据设置', 'document', Boolean(documentConfig.documentEnabled && documentConfig.statusField)),
    step('flow', '主流程', 'automation', Boolean(mainFlow.configured)),
    step('start', '发起方式', 'automation', Boolean(startMode), !startMode && Boolean(mainFlow.configured)),
    step('trigger', '自动化触发器', 'trigger', triggerRequired && !hasTriggerGap, triggerRequired && hasTriggerGap),
    step('publish', '发布检查', 'publish', publishCheckState.value?.publishable === true, publishCheckState.value?.publishable === false),
    step('runtime', '试运行', 'runtime', Boolean(runtimeInfo.value?.canOpen), Boolean(runtimeInfo.value && !runtimeInfo.value.canOpen)),
  ]
})
const fieldOptions = computed(() => {
  const modelFields = draft.modelSchema?.fields || []
  const source = modelFields.length ? modelFields : draft.fields.map(toPageField)
  return source
    .filter(field => field.field && !isInactiveField(field))
    .map(field => ({
      label: `${field.label || field.field}（${field.field}）`,
      value: field.field,
    }))
})
const runtimeFormOptions = computed(() => {
  const schema = normalizeMultiFormDesignerSchema(draft.formDesignerSchema || {})
  return (schema.forms || [])
    .filter(form => form?.formKey)
    .map(form => ({
      label: `${form.formName || form.formKey}${form.formKey === schema.defaultFormKey ? '（默认）' : ''}`,
      value: form.formKey,
    }))
})

function resolveInitialPanel() {
  const panel = props.embedded ? props.initialPanel : route.query.panel
  if (panel === 'flow')
    return 'automation'
  return panel === 'detail' ? 'form' : panel || 'form'
}

function resolveInitialDetailTab() {
  if (!props.embedded && route.query.panel === 'detail')
    return 'detail'
  return props.embedded ? props.initialDetailTab || 'form' : route.query.detailTab || 'form'
}

function normalizeStartMode(value) {
  const normalized = String(value || '').toUpperCase()
  if (['MANUAL_AND_TRIGGER', 'MANUAL_TRIGGER'].includes(normalized))
    return 'BOTH'
  if (['AUTO', 'AUTOMATIC'].includes(normalized))
    return 'TRIGGER'
  return normalized
}

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
  if (props.embedded)
    return
  if (!title)
    return
  route.meta.title = title
  document.title = `${title} | ${getDefaultPageTitle()}`
  tabStore.updateTabTitle(route.fullPath, title)
}, { immediate: true })

watch(activePanel, (panel) => {
  if (props.embedded)
    return
  router.replace({
    path: route.path,
    query: {
      ...route.query,
      panel,
      ...(panel === 'form' ? { detailTab: formDetailTab.value } : {}),
    },
  })
})

watch(formDetailTab, (tab) => {
  if (props.embedded)
    return
  if (activePanel.value !== 'form')
    return
  router.replace({
    path: route.path,
    query: {
      ...route.query,
      panel: 'form',
      detailTab: tab,
    },
  })
})

watch(canAdvanced, (value) => {
  if (!value && activePanel.value === 'advanced')
    activePanel.value = 'form'
}, { immediate: true })

async function loadDesigner() {
  loading.value = true
  ready.value = false
  try {
    const object = await resolveBusinessObject()
    if (!object?.id) {
      message.warning('未找到业务单元')
      return
    }
    const res = await businessObjectDesigner(object.id)
    designer.value = res.data || null
    Object.assign(draft, createDraftFromDesigner(designer.value))
    if (object.id)
      await loadRuntimeInfo(object.id)
    await nextTick()
    dirty.value = false
    designerDraftDirty.value = false
    ready.value = true
  }
  finally {
    loading.value = false
  }
}

async function resolveBusinessObject() {
  const queryObjectId = props.embedded ? props.embeddedObjectId : route.query.objectId
  if (queryObjectId)
    return { id: Array.isArray(queryObjectId) ? queryObjectId[0] : queryObjectId }
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
  if (saving.value)
    return
  saving.value = true
  await waitForSaveLoadingPaint()
  try {
    if (activePanel.value === 'fields') {
      await fieldManagerRef.value?.saveSelectedField?.()
      return
    }
    if (activePanel.value === 'form') {
      if (formDetailTab.value === 'detail')
        await detailDesignerRef.value?.saveLayout?.()
      else
        await formDesignerRef.value?.saveLayout?.()
      await loadDesigner()
      return
    }
    if (activePanel.value === 'list') {
      await listDesignerRef.value?.saveLayout?.()
      await loadDesigner()
      return
    }
    if (activePanel.value === 'detail') {
      await detailDesignerRef.value?.saveLayout?.()
      await loadDesigner()
      return
    }
    if (activePanel.value === 'relations') {
      await relationDesignerRef.value?.saveRelations?.()
      await loadDesigner()
      return
    }
    if (activePanel.value === 'document') {
      await persistPendingDesignerDraft()
      await documentPanelRef.value?.saveConfig?.()
      await loadDesigner()
      return
    }
    if (activePanel.value === 'automation') {
      await persistPendingDesignerDraft()
      await flowBindingPanelRef.value?.saveConfig?.()
      await loadDesigner()
      return
    }
    if (activePanel.value === 'actions') {
      await persistPendingDesignerDraft()
      await actionDesignerRef.value?.saveActions?.()
      await loadDesigner()
      return
    }
    if (activePanel.value === 'permission') {
      permissionFlowRef.value?.saveConfig?.()
    }
    await saveDesignerDraft(true, { manageSaving: false })
  }
  finally {
    saving.value = false
  }
}

async function saveDesignerDraft(showMessage = true, options = {}) {
  if (!objectId.value)
    return
  const reload = options.reload !== false
  const manageSaving = options.manageSaving !== false
  if (manageSaving)
    saving.value = true
  try {
    await saveBusinessObjectDesigner(objectId.value, buildDesignerPayload())
    dirty.value = false
    designerDraftDirty.value = false
    if (showMessage)
      message.success('设计器已保存')
    if (reload)
      await loadDesigner()
    emit('saved')
  }
  finally {
    if (manageSaving)
      saving.value = false
  }
}

async function waitForSaveLoadingPaint() {
  await nextTick()
  await new Promise(resolve => setTimeout(resolve, 0))
}

async function handlePanelSwitch(panel) {
  if (!panel || panel === activePanel.value)
    return
  await syncActiveFormDraft()
  await syncActiveListDraft()
  activePanel.value = panel
}

async function syncActiveFormDraft() {
  if (activePanel.value !== 'form' || formDetailTab.value !== 'form')
    return
  await nextTick()
  const result = formDesignerRef.value?.syncDesignerDraft?.()
  if (result?.dirty)
    designerDraftDirty.value = true
  await nextTick()
}

async function syncActiveListDraft() {
  if (activePanel.value !== 'list')
    return
  await nextTick()
  const result = listDesignerRef.value?.syncDesignerDraft?.()
  if (result?.dirty)
    designerDraftDirty.value = true
  await nextTick()
}

async function persistPendingDesignerDraft() {
  if (!designerDraftDirty.value)
    return
  await saveDesignerDraft(false, { reload: false })
}

function handlePreview() {
  if (activePanel.value === 'form') {
    window.dispatchEvent(new CustomEvent('forge-form-designer:preview-current-form'))
    return
  }
  if (activePanel.value === 'list') {
    window.dispatchEvent(new CustomEvent('forge-list-designer:preview-current-list'))
    return
  }
  message.info('当前面板暂未接入本地预览')
}

async function handlePublish(options = {}) {
  const publishOptions = options && typeof options === 'object' && !('target' in options) ? options : {}
  if (!objectId.value)
    return
  await syncActiveFormDraft()
  await syncActiveListDraft()
  await persistPendingDesignerDraft()
  if (dirty.value) {
    await handleSave()
  }
  const latestCheck = await refreshPublishCheckState()
  if (latestCheck?.publishable === false) {
    message.warning(formatPublishBlockMessage(latestCheck))
    activePanel.value = 'publish'
    await nextTick()
    await publishChecklistRef.value?.refresh?.()
    return
  }
  if (hasTableSyncIssue(latestCheck) && !publishOptions.syncTable) {
    message.warning('发布前请在发布检查中确认同步数据表结构')
    activePanel.value = 'publish'
    await nextTick()
    await publishChecklistRef.value?.refresh?.()
    return
  }
  publishing.value = true
  try {
    const res = await publishBusinessObject(objectId.value, {
      publishMode: 'PUBLISH',
      syncTable: !!publishOptions.syncTable,
      force: false,
      modelSchema: cloneSchema(draft.modelSchema || {}),
      pageSchema: cloneSchema(draft.pageSchema || {}),
      publishOptions: {},
    })
    message.success(res.data ? `业务单元已发布，版本 ${res.data}` : '业务单元已发布')
    await loadRuntimeInfo()
    await loadDesigner()
    emit('saved')
    await nextTick()
    if (activePanel.value === 'publish')
      await publishChecklistRef.value?.refresh?.()
  }
  finally {
    publishing.value = false
  }
}

async function refreshPublishCheckState() {
  if (!objectId.value)
    return null
  const res = await businessObjectPublishCheck(objectId.value)
  publishCheckState.value = res.data || null
  return publishCheckState.value
}

function formatPublishBlockMessage(check = {}) {
  const blocks = Array.isArray(check.blockItems) ? check.blockItems : []
  if (!blocks.length)
    return '发布检查存在阻断项，请先修复'
  const summary = blocks
    .slice(0, 3)
    .map(item => item?.zoneKey || item?.fieldCode ? `${item?.title || item?.itemCode}（${item.zoneKey || item.fieldCode}）` : (item?.title || item?.itemCode))
    .filter(Boolean)
    .join('；')
  const remain = blocks.length > 3 ? `；另有 ${blocks.length - 3} 项` : ''
  return `发布检查存在 ${blocks.length} 个阻断项：${summary}${remain}`
}

async function handleBack() {
  if (props.embedded) {
    if (dirty.value) {
      const confirmed = await confirmLeave()
      if (!confirmed)
        return
    }
    emit('close')
    return
  }
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

function openTriggerConfig() {
  const code = draft.objectCode || objectCode.value
  if (!code) {
    message.warning('缺少业务单元编码，无法打开触发器配置')
    return
  }
  if (props.embedded) {
    emit('close')
    return
  }
  router.push({
    path: '/app-center/trigger',
    query: {
      objectCode: code,
      returnTo: route.fullPath,
    },
  })
}

async function handleFieldsUpdated(fields, options = {}) {
  draft.fields = cloneSchema(fields || [])
  syncDraftModelFields(draft.fields)
  if (options?.fieldRename)
    applyFieldRename(options.fieldRename)
  draft.viewSchema = sanitizeViewSchemaFieldRefs(draft.viewSchema || {}, draft.fields)
  if (options?.reloadDesigner) {
    await loadDesigner()
    return
  }
  if (options?.persisted === false) {
    dirty.value = true
    designerDraftDirty.value = true
  }
  else {
    dirty.value = false
    designerDraftDirty.value = false
  }
}

async function handleAddFieldToForm(field) {
  if (!field)
    return
  activePanel.value = 'form'
  formDetailTab.value = 'form'
  await nextTick()
  formDesignerRef.value?.appendFieldToForm?.(toPageField(field))
}

function hasTableSyncIssue(result) {
  const items = Array.isArray(result?.items) ? result.items : []
  return items.some(item =>
    item?.fixAction === 'SYNC_TABLE' || item?.itemCode === 'TABLE_MISSING' || item?.itemCode === 'TABLE_COLUMN_MISSING')
}

function handleLayoutSaved(pageSchema) {
  draft.pageSchema = cloneSchema(pageSchema || draft.pageSchema)
  dirty.value = false
  designerDraftDirty.value = false
  emit('saved')
}

function applyFieldRename(rename = {}) {
  const oldFieldCode = String(rename.oldFieldCode || '').trim()
  const newFieldCode = String(rename.newFieldCode || '').trim()
  if (!oldFieldCode || !newFieldCode || oldFieldCode === newFieldCode)
    return
  const normalizedRename = {
    ...rename,
    oldFieldCode,
    newFieldCode,
  }
  draft.formDesignerSchema = renameFormDesignerFieldRefs(draft.formDesignerSchema || {}, normalizedRename)
  draft.viewSchema = renameViewSchemaFieldRefs(draft.viewSchema || {}, normalizedRename)
  draft.pageSchema = renameFieldRefsInValue(draft.pageSchema || {}, normalizedRename)
  if (draft.displayField === oldFieldCode)
    draft.displayField = newFieldCode
}

function handleRelationsUpdated(relations) {
  draft.relations = cloneSchema(relations || [])
}

async function handleDocumentSaved() {
  await loadDesigner()
  const mainFlow = draft.documentConfig?.mainFlowSummary || {}
  if (draft.documentConfig?.documentEnabled && !mainFlow.configured)
    activePanel.value = 'automation'
}

async function handleFlowSaved(binding = {}) {
  await loadDesigner()
  const startMode = String(binding.startMode || draft.documentConfig?.mainFlowSummary?.startMode || '').toUpperCase()
  if (startMode === 'TRIGGER' || startMode === 'BOTH') {
    openTriggerConfig()
    return
  }
  activePanel.value = 'publish'
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

function hasPublishItem(itemCode) {
  const items = Array.isArray(publishCheckState.value?.items) ? publishCheckState.value.items : []
  return items.some(item => item?.itemCode === itemCode && item?.level !== 'PASS')
}

function step(key, label, panel, done, warn = false) {
  const status = done ? 'done' : warn ? 'warn' : 'todo'
  const labels = {
    done: '完成',
    warn: '待处理',
    todo: '未完成',
  }
  return {
    key,
    label,
    panel,
    status,
    statusLabel: labels[status],
  }
}

async function handleFixTarget(panel) {
  const targetPanel = panel === 'flow' ? 'automation' : panel
  if (targetPanel === 'trigger') {
    openTriggerConfig()
    return
  }
  if (panel === 'advanced' && !canAdvanced.value) {
    message.warning('该修复入口需要高级配置权限')
    return
  }
  if (panel === 'detail') {
    await handlePanelSwitch('form')
    formDetailTab.value = 'detail'
    return
  }
  await handlePanelSwitch(targetPanel || 'form')
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
  if (value && activePanel.value === 'form' && formDetailTab.value === 'form')
    designerDraftDirty.value = true
  if (value && activePanel.value === 'list')
    designerDraftDirty.value = true
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
  const viewSchema = sanitizeViewSchemaFieldRefs(draft.viewSchema || {}, draft.fields)
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
    formDesignerSchema: cloneSchema(draft.formDesignerSchema || {}),
    viewSchema: cloneSchema(viewSchema),
    linkageSchema: cloneSchema(draft.linkageSchema || {}),
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
    formDesignerSchema: null,
    viewSchema: null,
    linkageSchema: null,
    fields: [],
    relations: [],
    documentConfig: null,
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
    formDesignerSchema: cloneSchema(value?.formDesignerSchema || null),
    viewSchema: cloneSchema(value?.viewSchema || null),
    linkageSchema: cloneSchema(value?.linkageSchema || null),
    fields: cloneSchema(value?.fields || []),
    relations: cloneSchema(value?.relations || []),
    documentConfig: cloneSchema(value?.documentConfig || null),
    designerOptions: cloneSchema(value?.designerOptions || {}),
  }
}

function toFieldPayload(field = {}) {
  return {
    fieldName: field.fieldName || field.label || field.fieldCode || field.field,
    fieldCode: field.fieldCode || field.field,
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
    fieldBinding: cloneSchema(field.fieldBinding || {}),
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
    fieldStatus: field.fieldStatus,
    basicProps: cloneSchema(field.basicProps || {}),
    advancedProps: cloneSchema(field.advancedProps || {}),
  }
}

function isInactiveField(field = {}) {
  const status = String(field.fieldStatus || '').toUpperCase()
  return status === 'DISABLED' || status === 'HIDDEN'
}

function syncDraftModelFields(fields = []) {
  const modelFields = draft.modelSchema?.fields || []
  const existingMap = new Map(modelFields.map(field => [field.field, field]))
  draft.modelSchema = {
    ...(draft.modelSchema || {}),
    fields: fields.map((field) => {
      const fieldCode = field.fieldCode || field.field
      return {
        ...(existingMap.get(fieldCode) || {}),
        field: fieldCode,
        columnName: field.columnName,
        label: field.fieldName || field.label || fieldCode,
        dataType: field.dataType,
        length: field.length,
        precision: field.precision,
        required: field.required,
        defaultValue: field.defaultValue,
        searchable: field.searchable,
        listVisible: field.listVisible,
        formVisible: field.formVisible,
        componentType: field.componentType,
        queryType: field.queryType,
        dictType: field.dictType,
        sensitiveType: field.sensitiveType,
        encryptAlgorithm: field.encryptAlgorithm,
        sortable: field.sortable,
        systemField: field.systemField,
        readonly: field.readonly,
        width: field.width,
        remark: field.remark,
        businessFieldType: field.fieldType,
        fieldStatus: field.fieldStatus,
        importable: field.importable,
        exportable: field.exportable,
        referenceObjectCode: field.referenceObjectCode,
        referenceDisplayField: field.referenceDisplayField,
        sortOrder: field.sortOrder,
        basicProps: cloneSchema(field.basicProps || {}),
        advancedProps: cloneSchema(field.advancedProps || {}),
      }
    }),
  }
}

const FIELD_REF_KEYS = new Set([
  'field',
  'fieldCode',
  'fieldRef',
  'queryField',
  'sourceField',
  'targetField',
  'displayField',
])

function renameFieldRefsInValue(value, rename = {}) {
  return renameFieldRefsInClonedValue(cloneSchema(value || {}), rename)
}

function renameFieldRefsInClonedValue(value, rename = {}) {
  if (Array.isArray(value)) {
    return value.map((item) => {
      if (item === rename.oldFieldCode)
        return rename.newFieldCode
      if (rename.oldColumnName && item === rename.oldColumnName)
        return rename.newColumnName || item
      return renameFieldRefsInClonedValue(item, rename)
    })
  }
  if (!value || typeof value !== 'object')
    return value
  if (value.fieldSettings && typeof value.fieldSettings === 'object' && !Array.isArray(value.fieldSettings)) {
    if (Object.prototype.hasOwnProperty.call(value.fieldSettings, rename.oldFieldCode)) {
      const oldSetting = value.fieldSettings[rename.oldFieldCode]
      delete value.fieldSettings[rename.oldFieldCode]
      value.fieldSettings[rename.newFieldCode] = oldSetting
    }
  }
  Object.keys(value).forEach((key) => {
    const item = value[key]
    if (FIELD_REF_KEYS.has(key) && item === rename.oldFieldCode) {
      value[key] = rename.newFieldCode
      return
    }
    if (key === 'columnName' && rename.oldColumnName && item === rename.oldColumnName) {
      value[key] = rename.newColumnName || item
      return
    }
    value[key] = renameFieldRefsInClonedValue(item, rename)
  })
  return value
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
.placeholder-panel,
.form-detail-panel {
  min-height: calc(100vh - 106px);
}

.basic-panel,
.placeholder-panel {
  padding: 20px;
}

.form-detail-panel :deep(.n-tabs-nav) {
  padding: 0 16px;
}

.form-detail-panel :deep(.n-tab-pane) {
  padding: 0;
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
