<template>
  <div
    ref="rootRef"
    class="business-flow-config-designer"
    :class="[rootStageClass, { 'code-app-mode': isCodeApp, compact }]"
  >
    <header v-if="!compact" class="flow-config-topbar">
      <div class="flow-config-breadcrumb">
        <strong>业务配置中心</strong>
        <span>›</span>
        <em>{{ activeStepTitle }}</em>
      </div>
    </header>

    <div v-if="!compact" class="flow-config-tabs">
      <span class="tabs-prefix">配置项</span>
      <div class="tabs-list">
        <button
          v-for="step in visibleSteps"
          :key="step.key"
          type="button"
          class="tab-button"
          :class="{ active: step.key === activeSection, done: step.done }"
          @click="handleStepClick(step)"
        >
          {{ step.label }}
        </button>
      </div>
    </div>

    <div class="flow-config-body">
      <n-alert v-if="formWarnings.length" type="warning" :bordered="false" class="flow-config-alert">
        {{ formWarnings.join('；') }}
      </n-alert>

      <div class="flow-config-shell">
        <section v-if="!compact" class="flow-stage-track">
          <button
            v-for="stage in activeStages"
            :key="stage.key"
            type="button"
            class="stage-card"
            :class="{ active: stage.active, done: stage.done }"
            @click="handleStageClick(stage)"
          >
            <span class="stage-index">{{ stage.index }}</span>
            <span class="stage-copy">
              <strong>{{ stage.title }}</strong>
              <em>{{ stage.description }}</em>
            </span>
          </button>
        </section>

        <section v-if="isCodeApp" class="code-app-banner">
          <div>
            <span>代码应用</span>
            <strong>{{ codeAppMessage }}</strong>
          </div>
          <div class="code-app-facts">
            <span>{{ objectCode }}</span>
            <span>{{ assetCount }} 个表单资产</span>
            <span>代码适配器回写</span>
          </div>
        </section>

        <main class="flow-config-workspace">
          <div v-if="!compact" class="workspace-context">
            <div>
              <span>{{ activeStepTitle }}</span>
              <strong>{{ activeStageMeta.title }}</strong>
              <em>{{ activeStageMeta.description }}</em>
            </div>
          </div>

          <section
            v-if="!isCodeApp && !compact"
            v-show="activeSection === 'document'"
            class="flow-config-panel"
            data-section="document"
          >
            <BusinessDocumentPanel
              ref="documentPanelRef"
              :object-id="objectId"
              :suite-code="suiteCode"
              :object-code="objectCode"
              :object-name="objectName"
              :fields="fields"
              :initial-config="initialConfig"
              @saved="handleDocumentSaved"
              @configure-flow="activeSection = 'flow'"
              @dirty-change="handleDirtyChange"
              @update-field-generation="handleFieldGenerationUpdate"
            />
          </section>

          <section
            v-if="isCodeApp"
            v-show="activeSection === 'assets'"
            class="flow-config-panel"
            data-section="assets"
          >
            <BusinessCodeAppFormAssetPanel
              ref="codeFormAssetPanelRef"
              :object-code="objectCode"
              :object-name="objectName"
              :form-asset-catalog="codeAppFormAssets"
              :provider-catalog="codeAppProviderCatalog"
              :metadata="codeAppMetadata"
              @dirty-change="handleDirtyChange"
            />
          </section>

          <section
            v-show="compact || activeSection === 'flow'"
            class="flow-config-panel"
            data-section="flow"
          >
            <BusinessFlowBindingPanel
              ref="flowBindingPanelRef"
              :object-id="objectId"
              :object-code="objectCode"
              :object-name="objectName"
              :application-code="applicationCode"
              :application-id="applicationId"
              :fields="fields"
              :code-app="isCodeApp"
              :compact="compact"
              @saved="handleFlowSaved"
              @context-change="handleFlowContextChange"
              @dirty-change="handleDirtyChange"
            />
          </section>
        </main>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useMessage } from 'naive-ui'
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { businessFlowAppConfig, saveBusinessFlowAppConfig } from '@/api/business-app'
import BusinessCodeAppFormAssetPanel from './BusinessCodeAppFormAssetPanel.vue'
import BusinessDocumentPanel from './BusinessDocumentPanel.vue'
import BusinessFlowBindingPanel from './BusinessFlowBindingPanel.vue'

const props = defineProps({
  objectId: {
    type: [Number, String],
    default: null,
  },
  suiteCode: {
    type: String,
    default: '',
  },
  objectCode: {
    type: String,
    default: '',
  },
  objectName: {
    type: String,
    default: '',
  },
  fields: {
    type: Array,
    default: () => [],
  },
  initialConfig: {
    type: Object,
    default: null,
  },
  initialSection: {
    type: String,
    default: '',
  },
  codeApp: {
    type: Boolean,
    default: false,
  },
  compact: {
    type: Boolean,
    default: false,
  },
  applicationCode: {
    type: String,
    default: '',
  },
  applicationId: {
    type: [String, Number],
    default: '',
  },
})

const emit = defineEmits(['dirtyChange', 'saved', 'openTrigger', 'openPublish', 'updateFieldGeneration', 'flowContextChange'])

const message = useMessage()
const rootRef = ref(null)
const documentPanelRef = ref(null)
const codeFormAssetPanelRef = ref(null)
const flowBindingPanelRef = ref(null)
const loading = ref(false)
const saving = ref(false)
const configState = ref({})
const activeSection = ref(props.compact ? 'flow' : normalizeInitialSection(props.initialSection))
const activeStageKey = ref('')

const isCodeApp = computed(() => {
  return props.codeApp
    || configState.value?.options?.codeApp === true
    || configState.value?.summary?.codeApp === true
})
const codeAppMessage = computed(() => {
  return configState.value?.options?.documentMessage
    || '业务数据和状态流转由代码模块维护；表单/列表/详情展示在左侧设计器配置，节点字段权限在流程设计器中配置。'
})
const flowBinding = computed(() => configState.value?.flowBinding || {})
const documentConfig = computed(() => configState.value?.documentConfig || props.initialConfig || {})
const assetCount = computed(() => {
  const assets = configState.value?.formAssets?.formAssets
  return Number(configState.value?.summary?.formAssetCount ?? (Array.isArray(assets) ? assets.length : 0))
})
const codeAppFormAssets = computed(() => {
  const assets = configState.value?.formAssets?.formAssets
  return Array.isArray(assets) ? assets : []
})
const codeAppProviderCatalog = computed(() => {
  const providers = configState.value?.formAssets?.providerCatalog
  return Array.isArray(providers) ? providers : []
})
const codeAppMetadata = computed(() => {
  return configState.value?.options?.codeAppMetadata || configState.value?.formAssets?.codeAppMetadata || {}
})
const formWarnings = computed(() => {
  const warnings = configState.value?.summary?.warnings || configState.value?.formAssets?.warnings || []
  return Array.isArray(warnings) ? warnings : []
})
const visibleSteps = computed(() => {
  if (isCodeApp.value) {
    return [
      {
        key: 'assets',
        label: '业务表单资产',
        status: assetCount.value ? '已配置' : '未配置',
        done: assetCount.value > 0,
      },
      {
        key: 'flow',
        label: '流程配置',
        status: flowBinding.value?.flowModelKey ? '已选择流程' : '未选择',
        done: !!flowBinding.value?.flowModelKey,
      },
    ]
  }
  return [
    {
      key: 'document',
      label: '单据状态',
      status: configState.value?.summary?.statusField ? '已配置' : '未配置',
      done: !!configState.value?.summary?.statusField,
    },
    {
      key: 'flow',
      label: '流程绑定',
      status: flowBinding.value?.flowModelKey ? '已选择流程' : '未选择',
      done: !!flowBinding.value?.flowModelKey,
    },
  ]
})
const activeStepTitle = computed(() => {
  return visibleSteps.value.find(step => step.key === activeSection.value)?.label || '业务流程配置'
})
const defaultStageKey = computed(() => {
  if (isCodeApp.value && activeSection.value === 'assets')
    return 'form-assets'
  return isCodeApp.value || activeSection.value === 'flow' ? 'flow-model' : 'base'
})
const currentStageKey = computed(() => activeStageKey.value || defaultStageKey.value)
const rootStageClass = computed(() => `stage-${currentStageKey.value}`)
const activeStages = computed(() => {
  if (isCodeApp.value && activeSection.value === 'assets') {
    return [
      {
        key: 'form-assets',
        index: 1,
        title: '业务表单资产',
        description: assetCount.value ? '维护流程可引用的业务表单' : '配置业务表单资产',
        done: assetCount.value > 0,
        active: isActiveStage('form-assets'),
        section: 'assets',
      },
      {
        key: 'flow-model',
        index: 2,
        title: '默认流程',
        description: flowBinding.value?.flowModelKey ? '已选择流程模型' : '选择流程后进入节点配置',
        done: !!flowBinding.value?.flowModelKey,
        active: isActiveStage('flow-model'),
        section: 'flow',
      },
    ]
  }
  if (isCodeApp.value || activeSection.value === 'flow') {
    return [
      {
        key: 'flow-model',
        index: 1,
        title: '默认流程',
        description: flowBinding.value?.flowModelKey ? '已选择流程模型' : '选择已发布流程模型',
        done: !!flowBinding.value?.flowModelKey,
        active: isActiveStage('flow-model'),
        section: 'flow',
      },
      {
        key: 'business-binding',
        index: 2,
        title: '业务记录绑定',
        description: isCodeApp.value ? '代码适配器接入' : '关联业务表和状态字段',
        done: isCodeApp.value || !!flowBinding.value?.businessBinding?.statusField,
        active: isActiveStage('business-binding'),
        section: 'flow',
      },
      {
        key: 'node-forms',
        index: 3,
        title: '节点配置',
        description: '在流程设计器中维护节点表单和字段权限',
        done: !!flowBinding.value?.flowModelKey,
        active: isActiveStage('node-forms'),
        section: 'flow',
      },
    ]
  }
  const summary = configState.value?.summary || {}
  const mainFlow = documentConfig.value?.mainFlowSummary || {}
  const statusValues = Array.isArray(documentConfig.value?.statusMappingRows)
    ? documentConfig.value.statusMappingRows.length
    : 0
  return [
    {
      key: 'base',
      index: 1,
      title: '基础配置',
      description: documentConfig.value?.documentEnabled ? '单据生命周期已启用' : '启用单据生命周期',
      done: !!documentConfig.value?.documentEnabled,
      active: isActiveStage('base'),
      section: 'document',
    },
    {
      key: 'status-dict',
      index: 2,
      title: '状态字典',
      description: statusValues ? `${statusValues} 个状态映射` : '配置状态与流转选项',
      done: !!summary.statusField || statusValues > 0,
      active: isActiveStage('status-dict'),
      section: 'document',
    },
    {
      key: 'flow-model',
      index: 3,
      title: '主流程',
      description: mainFlow.flowModelName || mainFlow.flowModelKey || '配置业务流程与节点',
      done: !!mainFlow.configured || !!flowBinding.value?.flowModelKey,
      active: isActiveStage('flow-model'),
      section: 'flow',
    },
  ]
})
const activeStageMeta = computed(() => {
  return activeStages.value.find(stage => stage.key === currentStageKey.value) || activeStages.value[0] || {
    title: '业务流程配置',
    description: '配置单据规则、流程绑定和节点表单策略',
  }
})

function handleFlowContextChange(context = {}) {
  emit('flowContextChange', context)
}

function isActiveStage(key) {
  return currentStageKey.value === key
}

watch(isCodeApp, (value) => {
  if (value) {
    activeSection.value = normalizeSection(props.initialSection, true)
    activeStageKey.value = defaultStageKey.value
  }
}, { immediate: true })

onMounted(async () => {
  await loadConfig()
  activeSection.value = props.compact ? 'flow' : normalizeSection(props.initialSection, isCodeApp.value)
  activeStageKey.value = defaultStageKey.value
})

async function loadConfig() {
  loading.value = true
  try {
    if (!props.objectCode) {
      await Promise.all([
        documentPanelRef.value?.loadConfig?.(),
        flowBindingPanelRef.value?.loadBinding?.(),
      ])
      emit('dirtyChange', false)
      return
    }
    const res = await businessFlowAppConfig(props.objectCode)
    const config = res.data || {}
    configState.value = config
    await nextTick()
    codeFormAssetPanelRef.value?.assignDrafts?.()
    if (!isCodeAppConfig(config)) {
      documentPanelRef.value?.assignConfig?.(config.documentConfig || props.initialConfig || {})
    }
    if (flowBindingPanelRef.value?.applyBindingConfig) {
      await flowBindingPanelRef.value.applyBindingConfig(config.flowBinding || {}, config.formAssets || null)
    }
    else {
      await flowBindingPanelRef.value?.loadBinding?.()
    }
    emit('dirtyChange', false)
  }
  catch (error) {
    message.error(error?.message || '业务流程配置加载失败')
  }
  finally {
    loading.value = false
  }
}

async function saveConfig(options = {}) {
  saving.value = true
  try {
    if (!props.objectCode) {
      await documentPanelRef.value?.saveConfig?.()
      await flowBindingPanelRef.value?.saveConfig?.()
      emit('dirtyChange', false)
      emit('saved')
      return
    }
    if (!isCodeApp.value && documentPanelRef.value?.validateBeforeSave?.() === false)
      return
    const documentConfig = isCodeApp.value ? null : documentPanelRef.value?.buildPayload?.()
    const flowBindingPayload = flowBindingPanelRef.value?.buildPayload?.()
    const codeAppMetadataPayload = isCodeApp.value ? buildCodeAppMetadataPayload(options?.codeAppMetadata) : null
    const shouldSaveFlowBinding = !!flowBindingPayload?.flowModelKey
    if (shouldSaveFlowBinding && flowBindingPanelRef.value?.validateBeforeSave?.() === false) {
      activeSection.value = 'flow'
      return
    }
    const res = await saveBusinessFlowAppConfig(props.objectCode, {
      documentConfig,
      flowBinding: shouldSaveFlowBinding ? flowBindingPayload : null,
      options: codeAppMetadataPayload ? { codeAppMetadata: codeAppMetadataPayload } : {},
    })
    const config = res.data || {}
    configState.value = config
    await nextTick()
    if (!isCodeAppConfig(config)) {
      documentPanelRef.value?.assignConfig?.(config.documentConfig || documentConfig || {})
    }
    if (flowBindingPanelRef.value?.applyBindingConfig) {
      await flowBindingPanelRef.value.applyBindingConfig(config.flowBinding || flowBindingPayload || {}, config.formAssets || null)
    }
    codeFormAssetPanelRef.value?.assignDrafts?.()
    message.success('业务流程配置已保存')
    emit('dirtyChange', false)
    emit('saved', config)
  }
  finally {
    saving.value = false
  }
}

function handleStepClick(step = {}) {
  activeSection.value = normalizeSection(step.key, isCodeApp.value)
  activeStageKey.value = defaultStageKey.value
}

async function handleStageClick(stage = {}) {
  activeSection.value = normalizeSection(stage.section || activeSection.value, isCodeApp.value)
  activeStageKey.value = stage.key || ''
  await nextTick()
  if (stage.key === 'node-forms') {
    flowBindingPanelRef.value?.openNodeConfigDesigner?.()
    return
  }
  rootRef.value?.querySelector('.flow-config-workspace')?.scrollIntoView?.({
    behavior: 'smooth',
    block: 'start',
  })
}

function normalizeInitialSection(section) {
  if (section === 'flow' || section === 'nodeForms')
    return 'flow'
  if (section === 'assets' || section === 'forms')
    return 'assets'
  return 'document'
}

function normalizeSection(section, codeApp) {
  if (codeApp) {
    if (section === 'flow' || section === 'nodeForms')
      return 'flow'
    return 'assets'
  }
  if (section === 'flow' || section === 'nodeForms')
    return 'flow'
  return 'document'
}

function isCodeAppConfig(config = {}) {
  return props.codeApp || config?.options?.codeApp === true || config?.summary?.codeApp === true
}

function handleDocumentSaved(config) {
  if (saving.value)
    return
  emit('saved', { source: 'document', config })
}

function handleFlowSaved(config) {
  if (saving.value)
    return
  emit('saved', { source: 'flow', config })
}

function handleDirtyChange(value) {
  emit('dirtyChange', value)
}

function handleFieldGenerationUpdate(payload) {
  emit('updateFieldGeneration', payload)
  emit('dirtyChange', true)
}

function buildCodeAppMetadataPayload(incomingMetadata = null) {
  const base = clonePlain(incomingMetadata || codeAppMetadata.value || {})
  return codeFormAssetPanelRef.value?.buildMetadata?.(base) || base
}

function clonePlain(value) {
  return JSON.parse(JSON.stringify(value || {}))
}

defineExpose({
  loadConfig,
  saveConfig,
})
</script>

<style scoped>
.business-flow-config-designer {
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr);
  height: 100%;
  min-height: 100%;
  background: #f5f7fb;
}

.business-flow-config-designer.compact {
  display: block;
  height: auto;
  min-height: 0;
  background: transparent;
}

.business-flow-config-designer.compact .flow-config-body,
.business-flow-config-designer.compact .flow-config-shell,
.business-flow-config-designer.compact .flow-config-workspace {
  min-height: 0;
  background: transparent;
}

.business-flow-config-designer.compact .flow-config-body {
  display: block;
  padding: 0;
}

.business-flow-config-designer.compact .flow-config-shell {
  max-width: none;
  margin: 0;
}

.flow-config-topbar {
  border-bottom: 1px solid #e4e4e7;
  background: color-mix(in srgb, var(--bg-primary) 96%, transparent);
  padding: 16px 28px;
}

.flow-config-breadcrumb {
  display: inline-flex;
  align-items: center;
  gap: 14px;
  color: #25324d;
  font-size: 16px;
  line-height: 24px;
}

.flow-config-breadcrumb strong {
  font-weight: 700;
}

.flow-config-breadcrumb span {
  color: #93a0b6;
}

.flow-config-breadcrumb em {
  color: #25324d;
  font-style: normal;
  font-weight: 600;
}

.flow-config-tabs {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 32px;
  height: 56px;
  border-bottom: 1px solid #e8edf5;
  background: color-mix(in srgb, var(--bg-primary) 94%, transparent);
  padding: 0 28px;
}

.tabs-prefix {
  color: #4b5873;
  font-size: 14px;
  line-height: 54px;
  white-space: nowrap;
}

.tabs-list {
  display: flex;
  align-self: stretch;
  min-width: 0;
  gap: 34px;
  overflow-x: auto;
}

.tab-button {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  border: 0;
  background: transparent;
  color: var(--text-primary);
  cursor: pointer;
  font-size: 14px;
  font-weight: 700;
  line-height: 54px;
  padding: 0 2px;
  white-space: nowrap;
}

.tab-button::after {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  height: 3px;
  border-radius: 999px 999px 0 0;
  background: transparent;
  content: '';
}

.tab-button:hover,
.tab-button.active {
  color: #2364f3;
}

.tab-button.active::after {
  background: #2364f3;
}

.flow-config-alert {
  max-width: 1360px;
  margin: 0 auto 16px;
}

.flow-config-body {
  display: grid;
  min-height: 0;
  background: #f5f7fb;
  padding: 28px 36px 32px;
}

.flow-config-shell {
  width: 100%;
  max-width: 1360px;
  min-width: 0;
  margin: 0 auto;
}

.flow-stage-track {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 48px;
  margin-bottom: 24px;
}

.stage-card {
  position: relative;
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr);
  align-items: center;
  min-height: 70px;
  border: 1px solid #e3e8f2;
  border-radius: 8px;
  background: color-mix(in srgb, var(--bg-primary) 96%, transparent);
  box-shadow: 0 6px 18px rgba(30, 41, 59, 0.06);
  color: #25324d;
  cursor: pointer;
  padding: 0 22px;
  text-align: left;
}

.stage-card:not(:last-child)::after {
  position: absolute;
  top: 50%;
  right: -31px;
  width: 9px;
  height: 9px;
  border-top: 2px solid #9aa6ba;
  border-right: 2px solid #9aa6ba;
  content: '';
  transform: translateY(-50%) rotate(45deg);
}

.stage-card:hover {
  border-color: #8fb3ff;
  box-shadow: 0 10px 26px rgba(35, 100, 243, 0.1);
}

.stage-card.active {
  border-color: #2364f3;
  box-shadow:
    0 0 0 1px rgba(35, 100, 243, 0.06),
    0 10px 26px rgba(35, 100, 243, 0.11);
}

.stage-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border: 1px solid #c8d1e1;
  border-radius: 50%;
  background: var(--bg-primary);
  color: #7b879b;
  font-size: 16px;
  font-weight: 700;
}

.stage-card.active .stage-index {
  border-color: #2364f3;
  background: linear-gradient(135deg, #2f6df5, #1d56e8);
  color: #fff;
}

.stage-card.done:not(.active) .stage-index {
  border-color: #bdd1ff;
  background: #eff5ff;
  color: #2364f3;
}

.stage-copy {
  display: grid;
  min-width: 0;
  gap: 3px;
}

.stage-copy strong {
  overflow: hidden;
  color: #25324d;
  font-size: 15px;
  font-weight: 700;
  line-height: 20px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.stage-copy em {
  overflow: hidden;
  color: #7b879b;
  font-size: 12px;
  font-style: normal;
  line-height: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.code-app-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #f8fbff;
  padding: 10px 12px;
}

.code-app-banner span {
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 16px;
}

.code-app-banner strong {
  display: block;
  margin-top: 2px;
  color: var(--text-primary);
  font-size: 13px;
  line-height: 20px;
}

.code-app-facts {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  justify-content: flex-end;
}

.code-app-facts span {
  border: 1px solid #bfdbfe;
  border-radius: 6px;
  background: var(--bg-primary);
  color: #3153d8;
  padding: 4px 8px;
}

.flow-config-workspace {
  width: 100%;
  min-width: 0;
  min-height: 0;
}

.workspace-context {
  display: flex;
  align-items: center;
  min-height: 66px;
  margin-bottom: 14px;
  border: 1px solid #e3e8f2;
  border-radius: 8px;
  background: var(--bg-primary);
  padding: 0 22px;
  box-shadow: 0 6px 18px rgba(30, 41, 59, 0.04);
}

.workspace-context div {
  display: grid;
  min-width: 0;
  gap: 2px;
}

.workspace-context span {
  color: #7b879b;
  font-size: 12px;
  line-height: 18px;
}

.workspace-context strong {
  overflow: hidden;
  color: #1f2a44;
  font-size: 16px;
  line-height: 22px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.workspace-context em {
  overflow: hidden;
  color: #667085;
  font-size: 13px;
  font-style: normal;
  line-height: 19px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.flow-config-panel {
  overflow: visible;
  min-height: 0;
  border: 0;
  background: transparent;
}

.flow-config-panel :deep(.document-head),
.flow-config-panel :deep(.flow-head),
.flow-config-panel :deep(.document-side),
.flow-config-panel :deep(.flow-side) {
  display: none;
}

.flow-config-panel :deep(.document-body),
.flow-config-panel :deep(.flow-body) {
  display: block;
  min-height: 0;
}

.flow-config-panel :deep(.document-main),
.flow-config-panel :deep(.flow-main) {
  overflow: visible;
  background: transparent;
  padding: 0;
}

.flow-config-panel :deep(.document-rail) {
  display: none;
}

.flow-config-panel :deep(.document-config-grid) {
  grid-template-columns: minmax(0, 1fr);
  gap: 18px;
}

.flow-config-panel :deep(.flow-main > .n-spin-container > .n-spin-content) {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 18px;
}

.flow-config-panel :deep(.flow-main .flow-card) {
  min-width: 0;
  margin-top: 0;
}

.flow-config-panel :deep(.flow-main .flow-card:nth-child(n + 3)) {
  grid-column: auto;
}

.flow-config-panel[data-section='document'] :deep(.document-config-grid .document-section),
.flow-config-panel[data-section='document'] :deep(.n-spin-content > .document-section),
.flow-config-panel[data-section='flow'] :deep(.flow-card) {
  display: none;
}

.stage-base .flow-config-panel[data-section='document'] :deep(.document-config-grid .document-section:nth-child(1)),
.stage-status-dict
  .flow-config-panel[data-section='document']
  :deep(.n-spin-content > .document-section:nth-of-type(1)),
.stage-flow-model .flow-config-panel[data-section='flow'] :deep(.flow-card:nth-child(1)),
.stage-business-binding .flow-config-panel[data-section='flow'] :deep(.flow-card:nth-child(2)),
.stage-node-forms .flow-config-panel[data-section='flow'] :deep(.flow-card:nth-child(3)) {
  display: block;
}

.stage-status-dict .flow-config-panel[data-section='document'] :deep(.document-config-grid) {
  display: none;
}

.flow-config-panel :deep(.document-section),
.flow-config-panel :deep(.flow-card) {
  border-color: #e3e7ef;
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(30, 41, 59, 0.05);
  padding: 22px;
}

.flow-config-panel :deep(.document-section + .document-section),
.flow-config-panel :deep(.flow-card + .flow-card) {
  margin-top: 18px;
}

.flow-config-panel :deep(.section-head),
.flow-config-panel :deep(.flow-card-head) {
  min-height: 44px;
  margin-bottom: 14px;
  padding-bottom: 12px;
  border-bottom: 1px solid #eef1f6;
}

.flow-config-panel :deep(.section-head h4),
.flow-config-panel :deep(.flow-card-head h4) {
  color: #25324d;
  font-size: 15px;
  line-height: 22px;
}

.flow-config-panel :deep(.section-head p),
.flow-config-panel :deep(.flow-card-head p) {
  max-width: 680px;
  color: var(--text-secondary);
  line-height: 18px;
}

.flow-config-panel :deep(.field-hints) {
  margin-top: 12px;
}

.flow-config-panel :deep(.node-form-row) {
  border-color: #e3e7ef;
  border-radius: 6px;
  background: #fbfcff;
  padding: 14px;
}

.flow-config-panel :deep(.adapter-facts div),
.flow-config-panel :deep(.display-switches label) {
  border-color: #e3e7ef;
  border-radius: 6px;
  background: #fbfcff;
}

.flow-config-panel :deep(.n-form-item) {
  --n-label-font-size: 12px;
}

.flow-config-panel :deep(.n-input),
.flow-config-panel :deep(.n-base-selection),
.flow-config-panel :deep(.n-input-number) {
  --n-border-radius: 6px !important;
}

@media (max-width: 1180px) {
  .flow-stage-track {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 14px;
  }

  .stage-card:not(:last-child)::after {
    display: none;
  }

  .flow-config-panel :deep(.document-config-grid),
  .flow-config-panel :deep(.flow-main > .n-spin-container > .n-spin-content) {
    grid-template-columns: 1fr;
  }

  .flow-config-panel :deep(.flow-main .flow-card:nth-child(n + 3)) {
    grid-column: auto;
  }
}

@media (max-width: 980px) {
  .flow-config-topbar,
  .flow-config-tabs {
    padding-right: 16px;
    padding-left: 16px;
  }

  .flow-config-tabs {
    gap: 18px;
  }

  .tabs-list {
    gap: 22px;
  }

  .flow-config-body {
    padding: 18px 16px 24px;
  }

  .code-app-banner {
    align-items: flex-start;
    flex-direction: column;
  }

  .code-app-facts {
    justify-content: flex-start;
  }
}

@media (max-width: 640px) {
  .flow-stage-track {
    grid-template-columns: 1fr;
  }

  .stage-card {
    min-height: 64px;
  }
}
</style>
