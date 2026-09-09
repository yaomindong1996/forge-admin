<template>
  <BusinessObjectDesignerShell
    :embedded="embedded"
    :compact-embedded="embedded && activePanel === 'flow-app'"
    :active-panel="activePanel"
    :designer="designer"
    :loading="loading"
    :dirty="dirty"
    :confirm-dirty-switch="!fieldDraftDirty"
    :saving="saving"
    :publishing="publishing"
    :publish-disabled="publishDisabled"
    :preview-disabled="!objectId"
    :show-advanced="canAdvanced"
    :nav-panels="designerNavPanels"
    :show-preview="!isCodeAppDesigner"
    :show-publish="!isCodeAppDesigner"
    @save="handleSave"
    @preview="handlePreview"
    @publish="handlePublish"
    @back="handleBack"
    @refresh="loadDesigner"
    @open-runtime="openRuntime"
    @open-fields="handlePanelSwitch('fields')"
    @open-function-market="functionMarketVisible = true"
    @update:active-panel="handlePanelSwitch"
  >
    <BusinessTableMappingSummary
      v-if="objectId && !isCodeAppDesigner"
      ref="tableMappingSummaryRef"
      :object-id="objectId"
      :expanded="activePanel === 'fields' || activePanel === 'data-model'"
      :model-schema="draft.modelSchema"
      @loaded="tableMapping = $event"
      @open-structure="handlePanelSwitch('fields')"
      @update:model-schema="handleModelSchemaUpdated"
      @dirty-change="handleDirtyChange"
      @apply-indexes="handleModelSchemaApplied"
      @apply-model-schema="handleModelSchemaApplied"
    />

    <section v-if="activePanel === 'basic'" class="basic-panel">
      <div class="panel-head">
        <div>
          <h2>基本信息</h2>
          <p>维护业务单元的名称、说明、默认标题字段和启停状态。</p>
        </div>
      </div>
      <n-form label-placement="top" :show-feedback="false" class="basic-form">
        <n-grid :cols="2" :x-gap="16" :y-gap="4" responsive="screen">
          <n-form-item-gi label="对象名称">
            <n-input v-model:value="draft.objectName" placeholder="例如：客户" @update:value="markDirty" />
          </n-form-item-gi>
          <n-form-item-gi label="默认标题字段">
            <n-select
              v-model:value="draft.displayField"
              :options="fieldOptions"
              clearable
              filterable
              placeholder="关联关系未单独配置时使用"
              @update:value="markDirty"
            />
          </n-form-item-gi>
          <n-form-item-gi label="对象图标">
            <IconSelector v-model="draft.icon" @update:model-value="markDirty" />
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
      :table-mapping="tableMapping"
      @updated="handleFieldsUpdated"
      @dirty-change="handleFieldDirtyChange"
      @add-to-form="handleAddFieldToForm"
    />

    <section v-else-if="activePanel === 'data-model'" class="grouped-designer-panel">
      <n-tabs v-model:value="dataModelTab" type="line">
        <n-tab-pane name="relations" tab="对象关系">
          <BusinessRelationDesigner
            ref="relationDesignerRef"
            v-model:linkage-schema="draft.linkageSchema"
            :object-id="objectId"
            :suite-code="draft.suiteCode"
            :object-code="draft.objectCode"
            :object-name="draft.objectName"
            :fields="draft.fields"
            :designer-options="draft.designerOptions"
            :designer-actions="draft.designerOptions?.actions || []"
            model-only
            @updated="handleRelationsUpdated"
            @update:designer-actions="handleActionsUpdated"
            @fields-updated="handleFieldsUpdated"
            @dirty-change="handleDirtyChange"
          />
        </n-tab-pane>
        <n-tab-pane name="tree-model" tab="树形模型">
          <BusinessPermissionFlowPanel
            ref="treeModelRef"
            v-model:model-schema="draft.modelSchema"
            v-model:page-schema="draft.pageSchema"
            :fields="draft.fields"
            :object-name="draft.objectName"
            @dirty-change="handleDirtyChange"
          />
        </n-tab-pane>
      </n-tabs>
      <n-alert
        v-if="!embedded"
        class="process-migration-alert"
        type="info"
        title="流程与自动化配置已移至应用工作台"
        :bordered="false"
      >
        触发器、流程绑定和业务动作已统一为业务流程画布，请在应用工作台 → 业务流程中配置。
        <template #action>
          <n-button text type="primary" @click="openProcessWorkspace">
            前往应用工作台
          </n-button>
        </template>
      </n-alert>
      <ObjectProcessReadOnlyPanel
        :object-code="draft.objectCode || objectCode"
        :application-code="applicationCode"
      />
    </section>

    <BusinessFormDesigner
      v-else-if="activePanel === 'form'"
      ref="formDesignerRef"
      v-model="draft.pageSchema"
      v-model:form-designer-schema="draft.formDesignerSchema"
      v-model:view-schema="draft.viewSchema"
      v-model:linkage-schema="draft.linkageSchema"
      :object-id="objectId"
      :object-code="draft.objectCode"
      :object-name="draft.objectName"
      :application-code="applicationCode"
      :model-schema="draft.modelSchema"
      :fields="draft.fields"
      :relations="draft.relations"
      :actions="draft.designerOptions?.actions || []"
      :initial-property-tab="initialFormPropertyTab"
      :initial-canvas-view="initialFormCanvasView"
      @saved="handleLayoutSaved"
      @fields-updated="handleFieldsUpdated"
      @dirty-change="handleDirtyChange"
      @create-field="handlePanelSwitch('fields')"
      @open-relations="handlePanelSwitch('relations')"
    />

    <BusinessListDesigner
      v-else-if="activePanel === 'list'"
      ref="listDesignerRef"
      v-model="draft.pageSchema"
      v-model:view-schema="draft.viewSchema"
      :object-id="objectId"
      :model-schema="draft.modelSchema"
      :fields="draft.fields"
      :form-options="runtimeFormOptions"
      :designer-options="draft.designerOptions"
      :designer-actions="draft.designerOptions?.actions || []"
      @update:designer-actions="handleActionsUpdated"
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
      :designer-options="draft.designerOptions"
      :designer-actions="draft.designerOptions?.actions || []"
      model-only
      @updated="handleRelationsUpdated"
      @update:designer-actions="handleActionsUpdated"
      @fields-updated="handleFieldsUpdated"
      @dirty-change="handleDirtyChange"
    />

    <ObjectActionEditor
      v-else-if="activePanel === 'actions'"
      :actions="draft.designerOptions?.actions || []"
      :fields="draft.fields"
      :relations="draft.relations"
      :objects="workspaceObjects"
      :object-code="draft.objectCode"
      :config-key="draft.configKey"
      @save="handleActionsUpdated"
    />

    <BusinessFlowAppConfigPanel
      v-else-if="activePanel === 'flow-app'"
      ref="flowAppConfigRef"
      :object-id="objectId"
      :suite-code="draft.suiteCode"
      :object-code="draft.objectCode"
      :object-name="draft.objectName"
      :application-code="applicationCode"
      :fields="draft.fields"
      :initial-config="draft.documentConfig"
      :initial-section="embedded ? 'flow' : route.query.section"
      :code-app="isCodeAppDesigner"
      :compact="embedded"
      @saved="handleFlowAppSaved"
      @flow-context-change="emit('flowContextChange', $event)"
      @dirty-change="handleDirtyChange"
      @open-publish="handlePanelSwitch('publish')"
      @update-field-generation="handleFieldGenerationUpdate"
    />

    <BusinessPermissionFlowPanel
      v-else-if="activePanel === 'tree-model'"
      ref="treeModelRef"
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
      :fields="draft.fields"
      :runtime-info="runtimeInfo"
      :publishing="publishing"
      @check-updated="handlePublishCheckUpdated"
      @fix="handleFixTarget"
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
import { computed, defineAsyncComponent, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import {
  businessFlowAppConfig,
  businessObjectDesigner,
  businessObjectList,
  businessObjectPublishCheck,
  businessObjectRuntimeInfo,
  publishBusinessObject,
  saveBusinessFlowAppConfig,
  saveBusinessObjectDesigner,
} from '@/api/business-app'
import IconSelector from '@/components/IconSelector.vue'
import { cloneSchema, isSameSchema } from '@/components/lowcode-builder/model/model-schema'
import { createDefaultPageSchema } from '@/components/lowcode-builder/page/page-schema'
import { useTabStore, useUserStore } from '@/store'
import { getDefaultPageTitle } from '@/utils/page-title'
import BusinessObjectDesignerShell from './components/designer/BusinessObjectDesignerShell.vue'
import DesignerAsyncLoader from './components/designer/DesignerAsyncLoader.vue'
import { renameFormDesignerFieldRefs } from './components/designer/form-first/fieldReferenceUtils'
import { createDefaultFormDesignerSchema, normalizeMultiFormDesignerSchema } from './components/designer/form-first/formDesignerSchema'
import { createDefaultViewSchema, renameViewSchemaFieldRefs, sanitizeViewSchemaFieldRefs } from './components/designer/form-first/viewSchema'
import {
  pickBusinessObjectIdentity,
  resolveDataModelTab,
  resolveStandaloneObjectDesignerSection,
} from './components/designer/object-designer-navigation'
import { buildSeedTakeoverSummary, markSeedTakeoverAccepted, requiresSeedTakeoverConfirmation } from './components/designer/seed-takeover'

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
    default: 'fields',
  },
  initialDetailTab: {
    type: String,
    default: 'form',
  },
  initialFormPropertyTab: {
    type: String,
    default: 'basic',
  },
  embeddedNavPanels: {
    type: Array,
    default: () => [],
  },
})

const emit = defineEmits(['close', 'saved', 'dirtyChange', 'flowContextChange'])

const ObjectActionEditor = defineDesignerAsyncComponent(() => import('./components/designer/ObjectActionEditor.vue'))
const BusinessAdvancedConfig = defineDesignerAsyncComponent(() => import('./components/designer/BusinessAdvancedConfig.vue'))
const BusinessFieldManager = defineDesignerAsyncComponent(() => import('./components/designer/BusinessFieldManager.vue'))
const BusinessFlowAppConfigPanel = defineDesignerAsyncComponent(() => import('./components/designer/BusinessFlowAppConfigPanel.vue'))
const BusinessFormDesigner = defineDesignerAsyncComponent(() => import('./components/designer/BusinessFormDesigner.vue'))
const BusinessListDesigner = defineDesignerAsyncComponent(() => import('./components/designer/BusinessListDesigner.vue'))
const ObjectProcessReadOnlyPanel = defineDesignerAsyncComponent(() => import('./components/designer/ObjectProcessReadOnlyPanel.vue'))
const BusinessPermissionFlowPanel = defineDesignerAsyncComponent(() => import('./components/designer/BusinessPermissionFlowPanel.vue'))
const BusinessPublishChecklist = defineDesignerAsyncComponent(() => import('./components/designer/BusinessPublishChecklist.vue'))
const BusinessRelationDesigner = defineDesignerAsyncComponent(() => import('./components/designer/BusinessRelationDesigner.vue'))
const BusinessTableMappingSummary = defineDesignerAsyncComponent(() => import('./components/designer/BusinessTableMappingSummary.vue'))
const FormulaFunctionMarket = defineDesignerAsyncComponent(() => import('./components/designer/formula/FormulaFunctionMarket.vue'))

function defineDesignerAsyncComponent(loader) {
  return defineAsyncComponent({
    loader,
    loadingComponent: DesignerAsyncLoader,
    delay: 200,
    timeout: 60000,
  })
}

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
const fieldDraftDirty = ref(false)
const ready = ref(false)
const activePanel = ref(resolveInitialPanel())
const initialFormCanvasView = resolveInitialFormCanvasView()
const dataModelTab = ref(resolveDataModelTab(props.embedded ? props.initialPanel : route.query.modelTab || route.query.panel))
const developerMode = ref(false)
const designer = ref(null)
const runtimeInfo = ref(null)
const publishCheckState = ref(null)
const functionMarketVisible = ref(false)
const fieldManagerRef = ref(null)
const tableMappingSummaryRef = ref(null)
const tableMapping = ref(null)
const formDesignerRef = ref(null)
const listDesignerRef = ref(null)
const relationDesignerRef = ref(null)
const flowAppConfigRef = ref(null)
const treeModelRef = ref(null)
const publishChecklistRef = ref(null)
const draft = reactive(createEmptyDraft())

const objectCode = computed(() => props.embedded ? props.embeddedObjectCode : route.params.objectCode)
const suiteCode = computed(() => (props.embedded ? props.embeddedSuiteCode : route.query.suiteCode) || draft.suiteCode)
const objectId = computed(() => designer.value?.objectId || draft.objectId)
const applicationCode = computed(() => resolveApplicationCode())
const pageTitle = computed(() => `${draft.objectName || objectCode.value || '业务单元'}设计`)
const canAdvanced = computed(() => {
  return userStore.isAdmin
    || hasPermission(userStore.permissions, 'ai:businessObject:advanced')
    || hasPermission(userStore.apiPermissions, 'ai:businessObject:advanced')
    || hasPermission(userStore.getDataPermission, 'ai:businessObject:advanced')
})
const isCodeAppDesigner = computed(() => {
  return draft.designerOptions?.codeApp === true
    || designer.value?.options?.codeApp === true
    || (!objectId.value && (route.query.codeApp === '1' || route.query.appType === 'code'))
})
const usesLegacyObjectPanels = computed(() => props.embedded || isCodeAppDesigner.value)
const effectiveDesignerPanel = computed(() => {
  if (activePanel.value === 'data-model')
    return dataModelTab.value
  return activePanel.value
})
const publishDisabled = computed(() => {
  if (isCodeAppDesigner.value)
    return true
  if (!objectId.value)
    return true
  return publishCheckState.value?.publishable === false
})
// ObjectActionEditor 跨对象字段通过 businessObjectList 兜底加载，无需本地维护对象列表
const workspaceObjects = []
const designerNavPanels = computed(() => {
  if (props.embedded && props.embeddedNavPanels.length)
    return props.embeddedNavPanels
  return isCodeAppDesigner.value ? ['form', 'list', 'flow-app'] : []
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
  const normalized = normalizePanel(panel) || 'fields'
  const codeAppRoute = route.query.codeApp === '1' || route.query.appType === 'code'
  return props.embedded || codeAppRoute ? normalized : resolveStandaloneObjectDesignerSection(normalized)
}

function resolveApplicationCode() {
  const directCode = route.query.applicationCode
    || route.query.appCode
    || designer.value?.applicationCode
    || draft.designerOptions?.applicationCode
  if (directCode)
    return String(directCode)
  const returnTo = String(route.query.returnTo || '')
  return decodeURIComponent(returnTo.match(/\/app-center\/application\/([^/?]+)/)?.[1] || '')
}

function openProcessWorkspace() {
  if (!applicationCode.value) {
    router.push('/app-center')
    return
  }
  router.push({
    name: 'BusinessApplicationWorkspace',
    params: { applicationCode: applicationCode.value },
    query: { section: 'automation' },
  })
}

function normalizePanel(panel) {
  if (panel === 'permission')
    return 'tree-model'
  if (['trigger', 'automation-trigger', 'triggers'].includes(panel))
    return 'fields'
  if (['flow', 'document', 'automation', 'flow-app'].includes(panel))
    return 'flow-app'
  return panel === 'detail' ? 'form' : panel
}

function resolveInitialFormCanvasView() {
  const legacyView = props.embedded
    ? props.initialDetailTab
    : route.query.panel === 'detail' ? 'detail' : route.query.detailTab
  if (legacyView === 'detail')
    return 'detail'
  if (legacyView === 'sections')
    return 'sections'
  return 'layout'
}

onMounted(() => {
  loadDesigner()
  window.addEventListener('beforeunload', handleBeforeUnload)
})

onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload)
  if (!props.embedded)
    tabStore.setTabDirty(route.fullPath, false)
})

onBeforeRouteLeave(() => {
  if (tabStore.consumeDirtyNavigation(route.fullPath))
    return true
  if (!dirty.value)
    return true
  return confirmLeave()
})

watch(dirty, (value) => {
  if (props.embedded)
    emit('dirtyChange', value)
  else
    tabStore.setTabDirty(route.fullPath, value, '当前业务对象设计存在未保存的更改')
}, { immediate: true })

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
      ...(panel === 'data-model' ? { modelTab: dataModelTab.value } : {}),
    },
  })
})

watch(dataModelTab, (tab) => {
  if (props.embedded || activePanel.value !== 'data-model')
    return
  router.replace({ path: route.path, query: { ...route.query, panel: 'data-model', modelTab: tab } })
})

watch(canAdvanced, (value) => {
  if (!value && activePanel.value === 'advanced')
    activePanel.value = 'form'
}, { immediate: true })

async function loadDesigner() {
  loading.value = true
  ready.value = false
  try {
    if (isCodeAppRoute()) {
      await applyCodeAppDesignerDraft()
      await nextTick()
      dirty.value = false
      designerDraftDirty.value = false
      ready.value = true
      return
    }
    const object = await resolveBusinessObject()
    if (isCodeAppBusinessObject(object)) {
      await applyCodeAppDesignerDraft(object)
      await nextTick()
      dirty.value = false
      designerDraftDirty.value = false
      ready.value = true
      return
    }
    if (!object?.id) {
      if (shouldOpenCodeAppDesigner()) {
        await applyCodeAppDesignerDraft()
        await nextTick()
        dirty.value = false
        designerDraftDirty.value = false
        ready.value = true
        return
      }
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
  const identity = pickBusinessObjectIdentity({ queryObjectId })
  if (identity?.id)
    return identity
  return findBusinessObjectByCode()
}

async function findBusinessObjectByCode() {
  if (!objectCode.value)
    return null
  const res = await businessObjectList({
    suiteCode: suiteCode.value,
    objectCode: objectCode.value,
  })
  return (res.data || [])[0] || null
}

function isCodeAppRoute() {
  if (props.embedded)
    return false
  return route.query.codeApp === '1' || route.query.appType === 'code'
}

function shouldOpenCodeAppDesigner() {
  return ['form', 'list', 'flow-app'].includes(activePanel.value)
    && !props.embedded
    && !!objectCode.value
    && (isCodeAppRoute() || ['form', 'list', 'detail', 'actions', 'flow-app'].includes(String(route.query.panel || '')))
}

function isCodeAppBusinessObject(object = {}) {
  return hasCodeAppFlag(object?.options) || hasCodeAppFlag(object?.designerOptions)
}

function hasCodeAppFlag(value) {
  if (!value)
    return false
  if (typeof value === 'object')
    return value.codeApp === true
  if (typeof value !== 'string')
    return false
  const compactValue = value.replace(/\s/g, '')
  if (compactValue.includes('"codeApp":true'))
    return true
  try {
    return JSON.parse(value)?.codeApp === true
  }
  catch {
    return false
  }
}

async function applyCodeAppDesignerDraft(sourceObject = null) {
  const code = String(objectCode.value || '').trim()
  const res = await businessFlowAppConfig(code)
  const config = res.data || {}
  const name = String(sourceObject?.objectName || config.objectName || route.query.name || route.query.objectName || code || '代码应用')
  const metadata = resolveCodeAppMetadata(config)
  const providerCatalogAssets = collectCodeAppProviderCatalogAssets(config.formAssets?.providerCatalog || [])
  const providerAssets = hydrateCodeAppProviderAssets(
    normalizeCodeAppAssets(config.formAssets?.formAssets || []),
    providerCatalogAssets,
  )
  const metadataAssets = normalizeCodeAppAssets(metadata.formAssets || [])
  const baseProviderAssets = providerAssets.length ? providerAssets : providerCatalogAssets
  const formAssets = mergeCodeAppAssets(baseProviderAssets, metadataAssets, code, name)
  const providerFields = collectCodeAppAssetFields(baseProviderAssets.length ? baseProviderAssets : formAssets)
  const configuredFields = metadata.fields?.length ? metadata.fields : collectCodeAppAssetFields(metadataAssets)
  const fields = mergeCodeAppFields(providerFields, configuredFields)
  const modelSchema = createCodeAppModelSchema(code, name, fields)
  const pageSchema = createCodeAppPageSchema(metadata.pageSchema, modelSchema)
  const viewSchema = createCodeAppViewSchema(metadata.viewSchema, fields)
  const formDesignerSchema = createCodeAppFormDesignerSchema(metadata, formAssets, fields, code, name)
  const virtualDesigner = {
    objectId: null,
    objectCode: code,
    objectName: sourceObject?.objectName || config.objectName || name,
    suiteCode: sourceObject?.suiteCode || config.suiteCode || 'CODE_APP',
    suiteName: sourceObject?.suiteName || config.suiteName || '代码应用',
    designStatus: 'PUBLISHED',
    publishStatus: 'PUBLISHED',
    options: { codeApp: true },
    designerOptions: {
      codeApp: true,
      codeAppAssets: formAssets,
    },
    fields,
    modelSchema,
    pageSchema,
    formDesignerSchema,
    viewSchema,
    documentConfig: config.documentConfig || null,
  }
  designer.value = virtualDesigner
  runtimeInfo.value = null
  publishCheckState.value = null
  if (!['form', 'list', 'flow-app'].includes(activePanel.value))
    activePanel.value = 'form'
  Object.assign(draft, createDraftFromDesigner(virtualDesigner))
}

async function loadRuntimeInfo(id = objectId.value) {
  if (!id)
    return
  const res = await businessObjectRuntimeInfo(id)
  runtimeInfo.value = res.data || null
}

async function handleSave() {
  if (saving.value)
    return false
  saving.value = true
  await waitForSaveLoadingPaint()
  try {
    const currentPanel = effectiveDesignerPanel.value
    if (!await confirmSeedConfigurationTakeover())
      return false
    if (currentPanel === 'fields') {
      const saved = await fieldManagerRef.value?.saveSelectedField?.()
      if (saved === true)
        fieldDraftDirty.value = false
      return saved === true
    }
    if (isCodeAppDesigner.value && currentPanel === 'form') {
      await syncActiveFormDraft()
      await saveCodeAppDesignerDraft(true)
      return true
    }
    if (isCodeAppDesigner.value && currentPanel === 'list') {
      await syncActiveListDraft()
      await saveCodeAppDesignerDraft(true)
      return true
    }
    if (currentPanel === 'form') {
      await formDesignerRef.value?.saveLayout?.()
      await loadDesigner()
      return true
    }
    if (currentPanel === 'list') {
      await listDesignerRef.value?.saveLayout?.()
      await loadDesigner()
      return true
    }
    if (currentPanel === 'relations') {
      await relationDesignerRef.value?.saveRelations?.()
      await loadDesigner()
      return true
    }
    if (currentPanel === 'flow-app') {
      const codeAppMetadata = isCodeAppDesigner.value && designerDraftDirty.value
        ? buildCodeAppMetadataPayload()
        : null
      if (!isCodeAppDesigner.value)
        await persistPendingDesignerDraft()
      await flowAppConfigRef.value?.saveConfig?.({ codeAppMetadata })
      await loadDesigner()
      return true
    }
    if (currentPanel === 'tree-model') {
      treeModelRef.value?.saveConfig?.()
    }
    await saveDesignerDraft(true, { manageSaving: false })
    return true
  }
  finally {
    saving.value = false
  }
}

async function confirmSeedConfigurationTakeover() {
  if (!requiresSeedTakeoverConfirmation(designer.value || {}))
    return true
  const summary = buildSeedTakeoverSummary(designer.value || {}, draft)
  const confirmed = await requestSeedTakeoverConfirmation(summary)
  if (!confirmed)
    return false

  draft.designerOptions = markSeedTakeoverAccepted(draft.designerOptions || {})
  if (effectiveDesignerPanel.value === 'form')
    await syncActiveFormDraft()
  if (effectiveDesignerPanel.value === 'list')
    await syncActiveListDraft()
  await saveBusinessObjectDesigner(objectId.value, buildDesignerPayload())
  designer.value = {
    ...(designer.value || {}),
    designerOptions: cloneSchema(draft.designerOptions),
  }
  return true
}

function requestSeedTakeoverConfirmation(summary) {
  const content = `此配置由系统种子初始化。确认后，后续修改由设计器草稿接管；已发布版本保持不变，只有正式发布才会生成新快照。\n本次变化：${summary}`
  if (!window.$dialog)
    return Promise.resolve(false)
  return new Promise((resolve) => {
    window.$dialog.warning({
      title: '接管内置配置',
      content,
      positiveText: '确认接管并保存',
      negativeText: '取消',
      onPositiveClick: () => resolve(true),
      onNegativeClick: () => resolve(false),
      onClose: () => resolve(false),
    })
  })
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
  const rawPanel = String(panel || '').trim()
  const normalizedPanel = !usesLegacyObjectPanels.value && rawPanel === 'detail'
    ? 'detail'
    : normalizePanel(panel)
  if (!usesLegacyObjectPanels.value) {
    if (['relations', 'flow-app', 'tree-model'].includes(normalizedPanel))
      dataModelTab.value = normalizedPanel
  }
  const compatibilityPanel = ['publish', 'advanced'].includes(normalizedPanel)
  const nextPanel = usesLegacyObjectPanels.value || compatibilityPanel
    ? normalizedPanel
    : resolveStandaloneObjectDesignerSection(normalizedPanel)
  if (!nextPanel || nextPanel === activePanel.value)
    return
  if (!await saveFieldDraftBeforePanelSwitch())
    return
  await syncActiveFormDraft()
  await syncActiveListDraft()
  activePanel.value = nextPanel
}

async function saveFieldDraftBeforePanelSwitch() {
  if (activePanel.value !== 'fields' || !fieldDraftDirty.value)
    return true
  if (saving.value)
    return false
  saving.value = true
  await waitForSaveLoadingPaint()
  try {
    const saved = await fieldManagerRef.value?.saveSelectedField?.()
    if (saved !== true)
      return false
    fieldDraftDirty.value = false
    return true
  }
  finally {
    saving.value = false
  }
}

async function syncActiveFormDraft() {
  const panel = effectiveDesignerPanel.value
  if (!['form', 'detail'].includes(panel))
    return
  await nextTick()
  const result = formDesignerRef.value?.syncDesignerDraft?.()
  const changed = applyDesignerDraftSyncResult(result)
  if (result?.dirty || changed) {
    dirty.value = true
    designerDraftDirty.value = true
  }
  await nextTick()
}

async function syncActiveListDraft() {
  if (effectiveDesignerPanel.value !== 'list')
    return
  await nextTick()
  const result = listDesignerRef.value?.syncDesignerDraft?.()
  const changed = applyDesignerDraftSyncResult(result)
  if (result?.dirty || changed) {
    dirty.value = true
    designerDraftDirty.value = true
  }
  await nextTick()
}

function applyDesignerDraftSyncResult(result = {}) {
  if (!result || typeof result !== 'object')
    return false
  let changed = false
  let fieldsChanged = false

  if (hasOwn(result, 'fields') && Array.isArray(result.fields)) {
    const nextFields = cloneSchema(result.fields)
    fieldsChanged = !isSameSchema(nextFields, draft.fields)
    if (fieldsChanged) {
      draft.fields = nextFields
      changed = true
    }
  }

  if (hasOwn(result, 'modelSchema') && result.modelSchema) {
    const nextModelSchema = cloneSchema(result.modelSchema)
    if (!isSameSchema(nextModelSchema, draft.modelSchema)) {
      draft.modelSchema = nextModelSchema
      changed = true
    }
  }
  else if (fieldsChanged) {
    syncDraftModelFields(draft.fields)
  }

  if (hasOwn(result, 'pageSchema')) {
    const nextPageSchema = cloneSchema(result.pageSchema || null)
    if (!isSameSchema(nextPageSchema, draft.pageSchema)) {
      draft.pageSchema = nextPageSchema
      changed = true
    }
  }

  if (hasOwn(result, 'formDesignerSchema')) {
    const nextFormDesignerSchema = cloneSchema(result.formDesignerSchema || null)
    if (!isSameSchema(nextFormDesignerSchema, draft.formDesignerSchema)) {
      draft.formDesignerSchema = nextFormDesignerSchema
      changed = true
    }
  }

  if (hasOwn(result, 'viewSchema')) {
    const nextViewSchema = sanitizeViewSchemaFieldRefs(result.viewSchema || {}, draft.fields)
    if (!isSameSchema(nextViewSchema, draft.viewSchema)) {
      draft.viewSchema = cloneSchema(nextViewSchema)
      changed = true
    }
  }
  else if (fieldsChanged && draft.viewSchema) {
    const nextViewSchema = sanitizeViewSchemaFieldRefs(draft.viewSchema || {}, draft.fields)
    if (!isSameSchema(nextViewSchema, draft.viewSchema)) {
      draft.viewSchema = cloneSchema(nextViewSchema)
      changed = true
    }
  }

  return changed
}

function hasOwn(value, key) {
  return Object.prototype.hasOwnProperty.call(value, key)
}

async function persistPendingDesignerDraft() {
  if (!designerDraftDirty.value)
    return
  if (isCodeAppDesigner.value) {
    await saveCodeAppDesignerDraft(false, { reload: false })
    return
  }
  await saveDesignerDraft(false, { reload: false })
}

function handlePreview() {
  if (effectiveDesignerPanel.value === 'form') {
    window.dispatchEvent(new CustomEvent('forge-form-designer:preview-current-form'))
    return
  }
  if (effectiveDesignerPanel.value === 'list') {
    window.dispatchEvent(new CustomEvent('forge-list-designer:preview-current-list'))
    return
  }
  message.info('当前面板暂未接入本地预览')
}

async function handlePublish(options = {}) {
  const explicitOptions = options && typeof options === 'object' && !('target' in options) ? options : {}
  const checklistOptions = activePanel.value === 'publish'
    ? publishChecklistRef.value?.getPublishOptions?.() || {}
    : {}
  const publishOptions = { ...checklistOptions, ...explicitOptions }
  if (!objectId.value || publishing.value)
    return
  publishing.value = true
  try {
    window.$loading?.show?.('正在发布业务单元，请稍候...')
    await waitForSaveLoadingPaint()
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
    window.$loading?.close?.()
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
    .slice(0, 5)
    .map((item) => {
      const title = item?.title || item?.itemCode || ''
      const detail = item?.message || ''
      return detail ? `${title}：${detail}` : title
    })
    .filter(Boolean)
    .join('；')
  const remain = blocks.length > 5 ? `；另有 ${blocks.length - 5} 项` : ''
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

async function handleFieldsUpdated(fields, options = {}) {
  draft.fields = cloneSchema(fields || [])
  syncDraftModelFields(draft.fields)
  if (options?.fieldRename)
    applyFieldRename(options.fieldRename)
  draft.viewSchema = sanitizeViewSchemaFieldRefs(draft.viewSchema || {}, draft.fields)
  if (options?.reloadDesigner) {
    await loadDesigner()
    await tableMappingSummaryRef.value?.refresh?.()
    return
  }
  if (options?.persisted === false) {
    dirty.value = true
    designerDraftDirty.value = true
  }
  else {
    dirty.value = false
    designerDraftDirty.value = false
    await tableMappingSummaryRef.value?.refresh?.()
  }
}

async function handleAddFieldToForm(field) {
  if (!field)
    return
  activePanel.value = 'form'
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
  // 透传保存后的pageSchema，供宿主页同步页面区块的查询字段等快照。
  emit('saved', pageSchema ? cloneSchema(pageSchema) : undefined)
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

async function handleFlowAppSaved() {
  await loadDesigner()
  if (isCodeAppDesigner.value) {
    if (!['form', 'list', 'flow-app'].includes(activePanel.value))
      activePanel.value = 'flow-app'
    return
  }
  const mainFlow = draft.documentConfig?.mainFlowSummary || {}
  if (draft.documentConfig?.documentEnabled && !mainFlow.configured) {
    activePanel.value = usesLegacyObjectPanels.value ? 'flow-app' : 'data-model'
    dataModelTab.value = 'flow-app'
  }
  const startMode = String(mainFlow.startMode || '').toUpperCase()
  if (startMode === 'TRIGGER' || startMode === 'BOTH')
    message.info('自动化触发配置请前往应用工作台的业务流程画布维护')
  activePanel.value = 'publish'
}

function handleFieldGenerationUpdate(payload = {}) {
  const fieldCode = String(payload.fieldCode || '').trim()
  if (!fieldCode)
    return
  const generation = cloneSchema(payload.generation || { enabled: false })
  draft.fields = (draft.fields || []).map((field) => {
    const code = field.fieldCode || field.field
    if (code !== fieldCode)
      return field
    const enabled = generation.enabled === true
    return {
      ...field,
      readonly: enabled ? true : field.readonly,
      required: enabled ? false : field.required,
      formVisible: enabled ? false : field.formVisible,
      basicProps: {
        ...(field.basicProps || {}),
        generation,
      },
      advancedProps: {
        ...(field.advancedProps || {}),
        generation,
      },
    }
  })
  syncDraftModelFields(draft.fields)
  draft.formDesignerSchema = updateFormDesignerFieldGeneration(draft.formDesignerSchema || {}, fieldCode, generation)
  dirty.value = true
  designerDraftDirty.value = true
}

function handleActionsUpdated(actions) {
  draft.designerOptions = {
    ...(draft.designerOptions || {}),
    actions: cloneSchema(actions || []),
  }
  if (ready.value && ['list', 'relations', 'actions'].includes(effectiveDesignerPanel.value)) {
    dirty.value = true
    designerDraftDirty.value = true
  }
}

function handlePublishCheckUpdated(check) {
  publishCheckState.value = check || null
}

async function handleFixTarget(panel) {
  const targetPanel = normalizePanel(panel)
  if (panel === 'advanced' && !canAdvanced.value) {
    message.warning('该修复入口需要高级配置权限')
    return
  }
  if (panel === 'detail') {
    await handlePanelSwitch('form')
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
  const panel = effectiveDesignerPanel.value
  if (value && ['form', 'detail'].includes(panel))
    designerDraftDirty.value = true
  if (value && ['list', 'relations', 'actions'].includes(panel))
    designerDraftDirty.value = true
}

function handleFieldDirtyChange(value) {
  if (!ready.value)
    return
  fieldDraftDirty.value = !!value
  if (value) {
    dirty.value = true
    return
  }
  if (!designerDraftDirty.value)
    dirty.value = false
}

function handleModelSchemaUpdated(modelSchema) {
  draft.modelSchema = cloneSchema(modelSchema || {})
  dirty.value = true
  designerDraftDirty.value = true
}

async function handleModelSchemaApplied(modelSchema) {
  handleModelSchemaUpdated(modelSchema)
  await saveDesignerDraft(true)
  await tableMappingSummaryRef.value?.refresh?.()
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

async function saveCodeAppDesignerDraft(showMessage = true, options = {}) {
  const code = draft.objectCode || objectCode.value
  if (!code)
    return
  const reload = options.reload !== false
  const metadata = buildCodeAppMetadataPayload()
  await saveBusinessFlowAppConfig(code, {
    documentConfig: null,
    flowBinding: null,
    options: {
      codeAppMetadata: metadata,
    },
  })
  dirty.value = false
  designerDraftDirty.value = false
  if (showMessage)
    message.success('代码应用设计已保存')
  emit('saved')
  if (reload)
    await loadDesigner()
}

function buildCodeAppMetadataPayload() {
  const baseFields = normalizeCodeAppFields(draft.fields || [])
  const assets = normalizeCodeAppAssets(draft.designerOptions?.codeAppAssets || [])
  const fallbackAsset = {
    formKey: `${draft.objectCode || objectCode.value || 'code_app'}_form`,
    formName: `${draft.objectName || '代码业务'}表单`,
    formMode: 'BUSINESS_CODE_FORM',
    type: 'BUSINESS_CODE_FORM',
    providerKey: '',
    formUrl: '',
  }
  const sourceAssets = assets.length ? assets : [fallbackAsset]
  const formDesignerSchema = cloneSchema(
    draft.formDesignerSchema || createCodeAppFormDesignerSchema({}, sourceAssets, baseFields, draft.objectCode, draft.objectName),
  )
  const fields = applyCodeAppFormVisibility(baseFields, formDesignerSchema)
  const formAssets = (assets.length ? assets : [fallbackAsset]).map(asset => ({
    ...asset,
    objectCode: draft.objectCode || objectCode.value,
    objectName: draft.objectName,
    fields,
    fieldCatalog: fields,
    fieldCount: fields.length,
  }))
  return {
    objectCode: draft.objectCode || objectCode.value,
    objectName: draft.objectName,
    formAssets,
    fields,
    formDesignerSchema,
    pageSchema: cloneSchema(draft.pageSchema || {}),
    viewSchema: cloneSchema(createCodeAppViewSchema(draft.viewSchema, fields)),
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

function resolveCodeAppMetadata(config = {}) {
  return cloneSchema(config.options?.codeAppMetadata || config.formAssets?.codeAppMetadata || {})
}

function createCodeAppModelSchema(code, name, fields = []) {
  return {
    schemaVersion: 2,
    object: {
      code,
      name,
      description: '',
    },
    appType: 'SINGLE',
    tableMode: 'CODE',
    tableName: code,
    businessName: name,
    fields: fields.map(toCodeAppModelField),
    relations: [],
    indexes: [],
    policies: {},
    children: [],
  }
}

function createCodeAppFormDesignerSchema(metadata = {}, assets = [], fields = [], code = '', name = '') {
  const defaultSchema = () => createDefaultFormDesignerSchema({
    objectCode: code,
    objectName: name,
    formKey: assets[0]?.formKey,
    formName: assets[0]?.formName || name,
    fields,
    includeReadonlyFields: true,
  })
  let source = hasUsableCodeAppFormSchema(metadata.formDesignerSchema)
    ? ensureCodeAppFormSchemaFields(cloneSchema(metadata.formDesignerSchema), fields, {
        objectCode: code,
        objectName: name,
        formKey: assets[0]?.formKey,
        formName: assets[0]?.formName || name,
      })
    : defaultSchema()
  source = ensureCodeAppPrimaryFormSchema(source, fields, {
    objectCode: code,
    objectName: name,
    formKey: assets[0]?.formKey,
    formName: assets[0]?.formName || name,
  })
  const formAssets = normalizeCodeAppAssets(assets).map(asset => ({
    ...asset,
    schema: hasUsableCodeAppFormSchema(asset.schema)
      ? ensureCodeAppFormSchemaFields(asset.schema, asset.fields?.length ? asset.fields : fields, {
          objectCode: code,
          objectName: name,
          formKey: asset.formKey,
          formName: asset.formName,
        })
      : createDefaultFormDesignerSchema({
          objectCode: code,
          objectName: name,
          formKey: asset.formKey,
          formName: asset.formName,
          fields: asset.fields?.length ? asset.fields : fields,
          includeReadonlyFields: true,
        }),
  }))
  return normalizeMultiFormDesignerSchema({
    ...source,
    objectCode: code,
    objectName: name,
    settings: {
      ...(source.settings || {}),
      formAssets,
    },
    forms: Array.isArray(source.forms) && source.forms.length
      ? source.forms
      : formAssets.map(asset => ({
          formKey: asset.formKey,
          formName: asset.formName,
          usage: ['create', 'edit', 'approve'],
          schema: asset.schema,
        })),
  })
}

function mergeCodeAppAssets(providerAssets = [], metadataAssets = [], code = '', name = '') {
  const metadataMap = new Map()
  metadataAssets.forEach((asset) => {
    assetKeys(asset).forEach(key => metadataMap.set(key, asset))
  })
  const result = []
  const seenKeys = new Set()
  const source = providerAssets.length ? providerAssets : metadataAssets
  source.forEach((asset) => {
    const configured = findCodeAppAsset(metadataMap, asset)
    appendCodeAppAsset(result, seenKeys, buildMergedCodeAppAsset(asset, configured, code, name), configured)
  })
  metadataAssets.forEach((asset) => {
    appendCodeAppAsset(result, seenKeys, buildMergedCodeAppAsset(asset, null, code, name), asset)
  })
  return result
}

function buildMergedCodeAppAsset(asset = {}, configured = null, code = '', name = '') {
  const configuredConfig = pickCodeAppAssetConfig(configured)
  const formMode = configuredConfig.formMode || configuredConfig.type || asset.formMode || asset.type || 'BUSINESS_CODE_FORM'
  const type = configuredConfig.type || configuredConfig.formMode || asset.type || asset.formMode || 'BUSINESS_CODE_FORM'
  const formKey = configuredConfig.formKey || asset.formKey || `${code || 'code_app'}_form`
  const objectName = configuredConfig.objectName || asset.objectName || name
  const businessName = configuredConfig.businessName || asset.businessName || objectName || name
  const appName = configuredConfig.appName || asset.appName || businessName || objectName || name
  const providerKey = configuredConfig.providerKey || asset.providerKey || ''
  const providerName = configuredConfig.providerName || asset.providerName || ''
  const formUrl = configuredConfig.formUrl || asset.formUrl || ''
  const formName = configuredConfig.formName || asset.formName || formKey || name
  const description = configuredConfig.description || asset.description || ''
  const supportsSave = configured && Object.prototype.hasOwnProperty.call(configured, 'supportsSave')
    ? configured.supportsSave !== false
    : asset.supportsSave !== false
  const fields = mergeCodeAppFields(
    asset.fields?.length ? asset.fields : asset.fieldCatalog || [],
    configured?.fields?.length ? configured.fields : configured?.fieldCatalog || [],
  )
  return {
    ...asset,
    ...configuredConfig,
    appName,
    objectCode: configuredConfig.objectCode || asset.objectCode || code,
    objectName,
    businessName,
    formKey,
    formName,
    formMode,
    type,
    providerKey,
    providerName,
    formUrl,
    description,
    supportsSave,
    fields,
    fieldCatalog: fields,
    fieldCount: fields.length,
  }
}

function appendCodeAppAsset(result, seenKeys, asset = {}, configured = null) {
  const keys = [...assetKeys(asset), ...assetKeys(configured)]
  if (keys.some(key => seenKeys.has(key)))
    return
  keys.forEach(key => seenKeys.add(key))
  result.push(asset)
}

function pickCodeAppAssetConfig(asset = {}) {
  if (!asset)
    return {}
  const result = {}
  ;['appName', 'objectCode', 'objectName', 'businessName', 'formKey', 'formName', 'formMode', 'type', 'providerKey', 'providerName', 'formUrl', 'description'].forEach((key) => {
    if (asset[key] !== undefined && asset[key] !== null && asset[key] !== '')
      result[key] = asset[key]
  })
  if (Object.prototype.hasOwnProperty.call(asset, 'supportsSave'))
    result.supportsSave = asset.supportsSave !== false
  return result
}

function createCodeAppPageSchema(source = {}, modelSchema = {}) {
  const base = hasUsableCodeAppPageSchema(source)
    ? cloneSchema(source)
    : createDefaultPageSchema(modelSchema)
  const fields = modelSchema?.fields || []
  if (!fields.length)
    return base
  const fieldCodes = new Set(fields.map(field => field?.field).filter(Boolean))
  const listRefs = collectCodeAppPageFieldRefs(base, ['table', 'list'])
  const editRefs = collectCodeAppPageFieldRefs(base, ['edit', 'detail', 'form'])
  const hasKnownListField = listRefs.some(ref => fieldCodes.has(ref))
  const hasKnownEditField = editRefs.some(ref => fieldCodes.has(ref))
  if (hasKnownListField && hasKnownEditField)
    return base
  const defaults = createDefaultPageSchema(modelSchema)
  return {
    ...base,
    listLayoutMode: base.listLayoutMode || defaults.listLayoutMode,
    listGridLayout: hasKnownListField ? base.listGridLayout : defaults.listGridLayout,
    zones: mergeCodeAppPageZones(base.zones, defaults.zones, { keepList: hasKnownListField, keepEdit: hasKnownEditField }),
    pages: hasKnownListField ? base.pages : defaults.pages,
  }
}

function collectCodeAppPageFieldRefs(schema = {}, zones = []) {
  const zoneSet = new Set(zones)
  const result = []
  function pushRefs(refs = []) {
    ;(Array.isArray(refs) ? refs : []).forEach((ref) => {
      if (ref && !result.includes(ref))
        result.push(ref)
    })
  }
  function visitGrid(grid = {}) {
    ;(Array.isArray(grid.items) ? grid.items : []).forEach((item) => {
      pushRefs(item.fieldRefs)
      pushRefs(item.props?.fieldRefs)
      if (item.fieldRef)
        pushRefs([item.fieldRef])
      visitGrid({ items: item.children || [] })
      ;(Array.isArray(item.props?.cells) ? item.props.cells : []).forEach(cell => visitGrid({ items: cell.children || [] }))
    })
  }
  ;(Array.isArray(schema.zones) ? schema.zones : [])
    .filter(zone => zoneSet.has(zone.zoneKey))
    .forEach((zone) => {
      pushRefs(zone.fieldRefs)
      pushRefs(zone.props?.fieldRefs)
      visitGrid(zone.props?.canvas || {})
    })
  if (zoneSet.has('table') || zoneSet.has('list')) {
    visitGrid(schema.listGridLayout || {})
    ;(Array.isArray(schema.pages) ? schema.pages : [])
      .filter(page => page.pageType === 'list' || page.pageKey === 'list')
      .forEach(page => visitGrid(page.gridLayout || {}))
  }
  if (zoneSet.has('detail')) {
    ;(Array.isArray(schema.pages) ? schema.pages : [])
      .filter(page => page.pageType === 'detail' || page.pageKey === 'detail')
      .forEach(page => visitGrid(page.gridLayout || {}))
  }
  return result
}

function mergeCodeAppPageZones(currentZones = [], defaultZones = [], options = {}) {
  const current = Array.isArray(currentZones) ? currentZones : []
  const defaults = Array.isArray(defaultZones) ? defaultZones : []
  const currentMap = new Map(current.map(zone => [zone.zoneKey, zone]))
  const result = defaults.map((zone) => {
    if (['table', 'search'].includes(zone.zoneKey) && !options.keepList)
      return zone
    if (['edit', 'detail'].includes(zone.zoneKey) && !options.keepEdit)
      return zone
    return currentMap.get(zone.zoneKey) || zone
  })
  current.forEach((zone) => {
    if (!result.some(item => item.zoneKey === zone.zoneKey))
      result.push(zone)
  })
  return result
}

function findCodeAppAsset(metadataMap, asset = {}) {
  for (const key of assetKeys(asset)) {
    if (metadataMap.has(key))
      return metadataMap.get(key)
  }
  return null
}

function assetKeys(asset = {}) {
  return [
    asset?.formKey ? `form:${asset.formKey}` : '',
    asset?.providerKey ? `provider:${asset.providerKey}` : '',
  ].filter(Boolean)
}

function mergeCodeAppFields(providerFields = [], configuredFields = []) {
  const configuredMap = new Map()
  normalizeCodeAppFields(configuredFields).forEach((field) => {
    configuredMap.set(field.field, field)
  })
  const result = []
  const seen = new Set()
  normalizeCodeAppFields(providerFields).forEach((field) => {
    const configured = configuredMap.get(field.field)
    appendCodeAppDesignField(result, seen, configured ? { ...field, ...configured } : field)
  })
  normalizeCodeAppFields(configuredFields).forEach((field) => {
    appendCodeAppDesignField(result, seen, field)
  })
  return result.sort(compareCodeAppFieldOrder)
}

function appendCodeAppDesignField(result, seen, field = {}) {
  const code = field.field || field.fieldCode
  if (!code || seen.has(code) || field.internal === true || field.systemField === true)
    return
  seen.add(code)
  result.push({
    ...field,
    field: code,
    fieldCode: code,
  })
}

function compareCodeAppFieldOrder(left = {}, right = {}) {
  const leftOrder = Number(left.sortOrder ?? left.order ?? Number.MAX_SAFE_INTEGER)
  const rightOrder = Number(right.sortOrder ?? right.order ?? Number.MAX_SAFE_INTEGER)
  if (leftOrder !== rightOrder)
    return leftOrder - rightOrder
  return 0
}

function createCodeAppViewSchema(source = {}, fields = []) {
  const defaults = createDefaultViewSchema({ fields })
  const current = sanitizeViewSchemaFieldRefs(source || {}, fields)
  return sanitizeViewSchemaFieldRefs({
    ...current,
    search: {
      ...current.search,
      fields: mergeCodeAppViewItems(defaults.search.fields, current.search?.fields || []),
    },
    list: {
      ...current.list,
      columns: mergeCodeAppViewItems(defaults.list.columns, current.list?.columns || []),
    },
    detail: {
      ...current.detail,
      sections: mergeCodeAppDetailSections(defaults.detail.sections, current.detail?.sections || []),
    },
  }, fields)
}

function mergeCodeAppViewItems(defaultItems = [], currentItems = []) {
  const result = Array.isArray(currentItems) ? [...currentItems] : []
  const seen = new Set(result.map(item => item?.fieldCode || item?.field).filter(Boolean))
  ;(Array.isArray(defaultItems) ? defaultItems : []).forEach((item) => {
    const field = item?.fieldCode || item?.field
    if (field && !seen.has(field)) {
      seen.add(field)
      result.push(item)
    }
  })
  return result
}

function mergeCodeAppDetailSections(defaultSections = [], currentSections = []) {
  if (!hasDetailViewFields(currentSections))
    return defaultSections
  const [defaultSection = { fields: [] }] = defaultSections
  return currentSections.map((section, index) => {
    if (index > 0)
      return section
    return {
      ...section,
      fields: mergeCodeAppViewItems(defaultSection.fields || [], section.fields || []),
    }
  })
}

function hasDetailViewFields(sections = []) {
  return Array.isArray(sections) && sections.some(section => Array.isArray(section?.fields) && section.fields.length)
}

function hasUsableCodeAppPageSchema(schema = {}) {
  if (!schema || typeof schema !== 'object')
    return false
  if (Array.isArray(schema.zones) && schema.zones.length)
    return true
  if (Array.isArray(schema.listGridLayout?.items) && schema.listGridLayout.items.length)
    return true
  return Array.isArray(schema.pages) && schema.pages.some(page => Array.isArray(page?.gridLayout?.items) && page.gridLayout.items.length)
}

function hasUsableCodeAppFormSchema(schema = {}) {
  if (!schema || typeof schema !== 'object')
    return false
  if (hasCodeAppFieldComponents(schema.components))
    return true
  if (Array.isArray(schema.forms) && schema.forms.some(form => hasUsableCodeAppFormSchema(form?.schema || form)))
    return true
  const assets = schema.settings?.formAssets
  return Array.isArray(assets) && assets.some(asset => hasUsableCodeAppFormSchema(asset?.schema || asset))
}

function hasCodeAppFieldComponents(components = []) {
  return (Array.isArray(components) ? components : []).some((component) => {
    if (!component || typeof component !== 'object')
      return false
    if (component.fieldBinding?.mode === 'field' && component.fieldBinding?.fieldCode)
      return true
    return hasCodeAppFieldComponents(component.children || [])
  })
}

function ensureCodeAppFormSchemaFields(schema = {}, fields = [], options = {}) {
  if (!hasUsableCodeAppFormSchema(schema))
    return schema
  const refs = collectCodeAppFormFieldRefs(schema)
  const missingFields = (fields || [])
    .filter(field => field && field.formVisible !== false && !field.internal && !field.systemField)
    .filter(field => !refs.has(field.field || field.fieldCode))
  if (!missingFields.length)
    return schema
  const appendSchema = createDefaultFormDesignerSchema({
    objectCode: options.objectCode,
    objectName: options.objectName,
    formKey: options.formKey || schema.formKey,
    formName: options.formName || schema.formName,
    fields: missingFields,
    includeReadonlyFields: true,
  })
  return {
    ...schema,
    components: [
      ...(Array.isArray(schema.components) ? schema.components : []),
      ...(appendSchema.components || []),
    ],
  }
}

function ensureCodeAppPrimaryFormSchema(schema = {}, fields = [], options = {}) {
  if (hasCodeAppFieldComponents(schema.components))
    return schema
  const fallback = createDefaultFormDesignerSchema({
    objectCode: options.objectCode,
    objectName: options.objectName,
    formKey: options.formKey || schema.formKey,
    formName: options.formName || schema.formName,
    fields,
    includeReadonlyFields: true,
  })
  return {
    ...schema,
    formKey: schema.formKey || fallback.formKey,
    formName: schema.formName || fallback.formName,
    layout: {
      ...(fallback.layout || {}),
      ...(schema.layout || {}),
    },
    components: fallback.components || [],
  }
}

function collectCodeAppFormFieldRefs(schema = {}) {
  const result = new Set()
  function visitSchema(value = {}) {
    if (!value || typeof value !== 'object')
      return
    visitComponents(value.components || [])
    ;(Array.isArray(value.forms) ? value.forms : []).forEach(form => visitSchema(form?.schema || form))
    ;(Array.isArray(value.settings?.formAssets) ? value.settings.formAssets : []).forEach(asset => visitSchema(asset?.schema || asset))
  }
  function visitComponents(components = []) {
    ;(Array.isArray(components) ? components : []).forEach((component) => {
      if (!component || typeof component !== 'object')
        return
      const fieldCode = String(component.fieldBinding?.fieldCode || '').trim()
      if (component.fieldBinding?.mode === 'field' && fieldCode)
        result.add(fieldCode)
      if (Array.isArray(component.children))
        visitComponents(component.children)
    })
  }
  visitSchema(schema)
  return result
}

function applyCodeAppFormVisibility(fields = [], formDesignerSchema = {}) {
  const visibility = collectCodeAppFormFieldVisibility(formDesignerSchema)
  if (!visibility.touched)
    return fields
  return fields.map((field) => {
    const code = field.field || field.fieldCode
    const state = visibility.map.get(code)
    return {
      ...field,
      formVisible: state ? state.visible !== false : false,
      readonly: state?.readonly === true || field.readonly === true,
      writable: state?.readonly === true ? false : field.writable,
      label: state?.label || field.label,
      fieldName: state?.label || field.fieldName,
    }
  })
}

function collectCodeAppFormFieldVisibility(schema = {}) {
  const result = new Map()
  function visitSchema(value = {}) {
    if (!value || typeof value !== 'object')
      return
    visitComponents(value.components || [])
    ;(Array.isArray(value.forms) ? value.forms : []).forEach(form => visitSchema(form?.schema || form))
    ;(Array.isArray(value.settings?.formAssets) ? value.settings.formAssets : []).forEach(asset => visitSchema(asset?.schema || asset))
  }
  function visitComponents(components = []) {
    ;(Array.isArray(components) ? components : []).forEach((component) => {
      if (!component || typeof component !== 'object')
        return
      const fieldCode = String(component.fieldBinding?.fieldCode || '').trim()
      if (component.fieldBinding?.mode === 'field' && fieldCode && !result.has(fieldCode)) {
        result.set(fieldCode, {
          visible: component.visibility?.hidden !== true,
          readonly: component.visibility?.readonly === true,
          label: component.label || '',
        })
      }
      if (Array.isArray(component.children))
        visitComponents(component.children)
    })
  }
  visitSchema(schema)
  return {
    touched: result.size > 0,
    map: result,
  }
}

function normalizeCodeAppAssets(source = []) {
  return (Array.isArray(source) ? source : [])
    .filter(Boolean)
    .map(asset => ({
      ...cloneSchema(asset),
      formMode: asset.formMode || asset.type || 'BUSINESS_CODE_FORM',
      type: asset.type || asset.formMode || 'BUSINESS_CODE_FORM',
      formKey: asset.formKey || 'default',
      formName: asset.formName || asset.objectName || '代码业务表单',
      providerKey: asset.providerKey || '',
      formUrl: asset.formUrl || '',
      fields: normalizeCodeAppFields(asset.fields?.length ? asset.fields : asset.fieldCatalog || []),
      fieldCatalog: normalizeCodeAppFields(asset.fieldCatalog?.length ? asset.fieldCatalog : asset.fields || []),
    }))
}

function collectCodeAppProviderCatalogAssets(source = []) {
  return (Array.isArray(source) ? source : [])
    .filter(Boolean)
    .flatMap((provider) => {
      const providerKey = String(provider?.providerKey || '').trim()
      const providerName = String(provider?.providerName || providerKey || '').trim()
      return normalizeCodeAppAssets(provider?.assets || []).map(asset => ({
        ...asset,
        providerKey: asset.providerKey || providerKey,
        providerName: asset.providerName || providerName,
      }))
    })
}

function hydrateCodeAppProviderAssets(assets = [], catalogAssets = []) {
  if (!assets.length)
    return catalogAssets
  const catalogMap = new Map(catalogAssets.flatMap(asset => assetKeys(asset).map(key => [key, asset])))
  const result = []
  const seen = new Set()
  assets.forEach((asset) => {
    const catalogAsset = findCodeAppAsset(catalogMap, asset)
    const fields = asset.fields?.length || asset.fieldCatalog?.length
      ? mergeCodeAppFields(asset.fields?.length ? asset.fields : asset.fieldCatalog, [])
      : mergeCodeAppFields(catalogAsset?.fields?.length ? catalogAsset.fields : catalogAsset?.fieldCatalog || [], [])
    appendCodeAppAsset(result, seen, {
      ...(catalogAsset || {}),
      ...asset,
      fields,
      fieldCatalog: fields,
      fieldCount: fields.length,
    }, catalogAsset)
  })
  catalogAssets.forEach(asset => appendCodeAppAsset(result, seen, asset))
  return result
}

function collectCodeAppAssetFields(assets = []) {
  const result = []
  const seen = new Set()
  assets.forEach((asset) => {
    const fields = asset.fields?.length ? asset.fields : asset.fieldCatalog || []
    fields.forEach((field) => {
      const code = fieldCode(field)
      if (!code || seen.has(code))
        return
      seen.add(code)
      result.push(field)
    })
  })
  return result
}

function normalizeCodeAppFields(source = []) {
  const result = []
  const seen = new Set()
  ;(Array.isArray(source) ? source : []).forEach((field) => {
    const code = fieldCode(field)
    if (!code || seen.has(code))
      return
    seen.add(code)
    const componentType = normalizeCodeAppComponentType(field.componentType || field.type || 'input')
    result.push({
      ...cloneSchema(field),
      field: code,
      fieldCode: code,
      fieldName: field.fieldName || field.label || field.fieldLabel || code,
      label: field.label || field.fieldLabel || field.fieldName || code,
      componentType,
      type: componentType,
      visible: field.visible !== false,
      writable: field.writable !== false && field.readonly !== true,
      readonly: field.readonly === true || field.writable === false,
      internal: field.internal === true,
      systemField: field.systemField === true,
      formVisible: field.formVisible !== false && field.visible !== false,
      listVisible: field.listVisible !== false && field.visible !== false,
      searchable: field.searchable === true,
    })
  })
  return result
}

function toCodeAppModelField(field = {}) {
  const code = field.field || field.fieldCode
  return {
    ...field,
    field: code,
    fieldCode: code,
    label: field.label || field.fieldName || code,
    columnName: field.columnName || code,
    dataType: field.dataType || inferCodeAppDataType(field.componentType || field.type),
    componentType: normalizeCodeAppComponentType(field.componentType || field.type),
    fieldStatus: field.fieldStatus || 'NORMAL',
    formVisible: field.formVisible !== false && field.visible !== false,
    listVisible: field.listVisible !== false && field.visible !== false,
    searchable: field.searchable === true,
    readonly: field.readonly === true,
    systemField: field.systemField === true,
    required: field.required === true,
  }
}

function fieldCode(field = {}) {
  return String(field.field || field.fieldCode || field.code || field.name || '').trim()
}

function normalizeCodeAppComponentType(type) {
  const value = String(type || 'input').trim()
  const aliases = {
    inputNumber: 'number',
    integer: 'number',
    file: 'fileUpload',
    image: 'imageUpload',
    upload: 'fileUpload',
  }
  return aliases[value] || value || 'input'
}

function inferCodeAppDataType(componentType) {
  const type = normalizeCodeAppComponentType(componentType)
  if (['number', 'money'].includes(type))
    return 'decimal'
  if (type === 'date')
    return 'date'
  if (type === 'datetime')
    return 'datetime'
  return 'varchar'
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
        formulaConfig: cloneSchema(field.formulaConfig || null),
        basicProps: cloneSchema(field.basicProps || {}),
        advancedProps: cloneSchema(field.advancedProps || {}),
      }
    }),
  }
}

function updateFormDesignerFieldGeneration(schema = {}, fieldCode = '', generation = {}) {
  const next = cloneSchema(schema || {})
  visitFormDesignerSchema(next)
  return next

  function visitFormDesignerSchema(value = {}) {
    if (!value || typeof value !== 'object')
      return
    visitFormDesignerComponents(value.components || [])
    ;(Array.isArray(value.forms) ? value.forms : []).forEach(form => visitFormDesignerSchema(form?.schema || form))
    ;(Array.isArray(value.settings?.formAssets) ? value.settings.formAssets : []).forEach(asset => visitFormDesignerSchema(asset?.schema || asset))
  }

  function visitFormDesignerComponents(components = []) {
    ;(Array.isArray(components) ? components : []).forEach((component) => {
      if (!component || typeof component !== 'object')
        return
      if (component.fieldBinding?.mode === 'field' && component.fieldBinding?.fieldCode === fieldCode) {
        component.props = {
          ...(component.props || {}),
          generation,
        }
        if (generation.enabled === true) {
          component.validation = {
            ...(component.validation || {}),
            required: false,
            requiredMessage: '',
          }
          component.visibility = {
            ...(component.visibility || {}),
            readonly: true,
            hidden: true,
          }
        }
      }
      if (Array.isArray(component.children))
        visitFormDesignerComponents(component.children)
    })
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

defineExpose({
  save: handleSave,
})
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

.grouped-designer-panel {
  box-sizing: border-box;
  min-width: 0;
  padding: 0 20px 20px;
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

.process-migration-alert {
  margin-top: 18px;
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
