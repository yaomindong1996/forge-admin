<template>
  <n-modal
    :show="show"
    preset="card"
    class="app-create-wizard"
    :style="{ width: 'min(1120px, 96vw)' }"
    title="创建应用"
    :bordered="false"
    :mask-closable="!saving"
    @update:show="updateShow"
  >
    <div class="app-create-wizard__body">
      <n-tabs v-model:value="activeMode" type="segment" animated class="create-mode-tabs">
        <n-tab name="AI">
          <n-icon><SparklesOutline /></n-icon><span>智能创建</span>
        </n-tab>
        <n-tab name="TEMPLATE">
          <n-icon><GridOutline /></n-icon><span>从模板创建</span>
        </n-tab>
        <n-tab name="EXCEL">
          <n-icon><DocumentOutline /></n-icon><span>从 Excel 创建</span>
        </n-tab>
        <n-tab name="BLANK">
          <n-icon><AddCircleOutline /></n-icon><span>空白创建</span>
        </n-tab>
      </n-tabs>

      <n-alert v-if="initializationWarning" type="warning" :bordered="false">
        {{ initializationWarning }}。应用草稿已保留，可调整配置后重试；不会重复创建应用。
      </n-alert>

      <section class="create-mode-panel">
        <AppCreateAi
          v-if="activeMode === 'AI'"
          ref="modeRef"
          @suggest-name="applySuggestedName"
        />
        <AppCreateTemplate
          v-else-if="activeMode === 'TEMPLATE'"
          ref="modeRef"
          :template-key="initialTemplateKey"
          :initial-delivery-mode="initialDeliveryMode"
        />
        <AppCreateExcel v-else-if="activeMode === 'EXCEL'" ref="modeRef" />
        <AppCreateBlank v-else ref="modeRef" />
      </section>

      <n-divider title-placement="left">
        应用信息
      </n-divider>
      <fieldset :disabled="Boolean(createdDraft)">
        <ApplicationCreateBasics
          ref="basicsRef"
          v-model="basics"
          :suites="suites"
        />
      </fieldset>
    </div>

    <template #footer>
      <div class="app-create-wizard__footer">
        <span class="create-result-hint">
          {{ createdDraft ? `草稿 ${createdDraft.applicationCode} 已创建` : modeHint }}
        </span>
        <n-space>
          <n-button :disabled="saving" @click="updateShow(false)">
            取消
          </n-button>
          <n-button type="primary" :loading="saving" @click="handleCreate">
            {{ primaryActionText }}
          </n-button>
        </n-space>
      </div>
    </template>
  </n-modal>
</template>

<script setup>
import { AddCircleOutline, DocumentOutline, GridOutline, SparklesOutline } from '@vicons/ionicons5'
import { useMessage } from 'naive-ui'
import { computed, defineAsyncComponent, ref, watch } from 'vue'
import {
  createBusinessApplication,
  initializeBusinessApplicationAi,
  initializeBusinessApplicationExcel,
  initializeBusinessApplicationTemplate,
} from '@/api/business-application'
import { resolveApplicationCreateResult } from './application-create-result'
import AppCreateBlank from './create/AppCreateBlank.vue'
import ApplicationCreateBasics from './create/ApplicationCreateBasics.vue'

const props = defineProps({
  show: { type: Boolean, default: false },
  suites: { type: Array, default: () => [] },
  defaultSuiteCode: { type: String, default: '' },
  initialMode: { type: String, default: 'BLANK' },
  initialTemplateKey: { type: String, default: '' },
  initialDeliveryMode: { type: String, default: 'ONLINE' },
})
const emit = defineEmits(['update:show', 'created', 'draftSaved'])
const AppCreateAi = defineAsyncComponent(() => import('./create/AppCreateAi.vue'))
const AppCreateExcel = defineAsyncComponent(() => import('./create/AppCreateExcel.vue'))
const AppCreateTemplate = defineAsyncComponent(() => import('./create/AppCreateTemplate.vue'))
const message = useMessage()
const activeMode = ref('BLANK')
const basics = ref(defaultBasics())
const basicsRef = ref(null)
const modeRef = ref(null)
const saving = ref(false)
const createdDraft = ref(null)
const initializationWarning = ref('')

const primaryActionText = computed(() => {
  if (createdDraft.value)
    return activeMode.value === 'BLANK' ? '进入空白应用' : '重试初始化'
  if (activeMode.value === 'TEMPLATE' && props.initialDeliveryMode === 'SOURCE')
    return '创建并生成源码'
  return activeMode.value === 'BLANK' ? '创建并进入应用' : '创建并初始化'
})
const modeHint = computed(() => ({
  AI: 'AI 方案确认后写入设计态，不自动发布',
  TEMPLATE: '模板资产在同一事务中生成',
  EXCEL: '只生成对象和页面设计，不导入业务数据',
  BLANK: '创建后进入页面管理主入口继续设计',
})[activeMode.value])

watch(() => props.show, (visible) => {
  if (!visible)
    return
  activeMode.value = normalizeMode(props.initialMode)
  basics.value = defaultBasics()
  createdDraft.value = null
  initializationWarning.value = ''
})

function defaultBasics() {
  const fallbackSuite = props.suites.find(suite => Number(suite.status) === 1)?.suiteCode || ''
  return {
    applicationName: '',
    applicationCode: '',
    suiteCode: props.defaultSuiteCode || fallbackSuite,
    icon: 'ionicons5:AppsOutline',
    description: '',
    status: 1,
  }
}

function normalizeMode(value) {
  const mode = String(value || '').toUpperCase()
  return ['AI', 'TEMPLATE', 'EXCEL', 'BLANK'].includes(mode) ? mode : 'BLANK'
}

function updateShow(value) {
  if (saving.value)
    return
  emit('update:show', value)
}

function applySuggestedName(value) {
  if (!basics.value.applicationName)
    basics.value = { ...basics.value, applicationName: value }
}

async function handleCreate() {
  try {
    await basicsRef.value?.validate()
    await modeRef.value?.validate()
  }
  catch (error) {
    message.warning(error?.message || '请完善创建信息')
    return
  }

  saving.value = true
  initializationWarning.value = ''
  try {
    const modePayload = modeRef.value?.getPayload?.() || {}
    if (!createdDraft.value)
      createdDraft.value = await createDraft()
    const initialization = await initialize(modePayload)
    const result = {
      application: { ...basics.value, ...createdDraft.value },
      created: true,
      initializeMode: activeMode.value,
      initialization,
      deliveryMode: modePayload.deliveryMode || 'ONLINE',
      template: modePayload.template || null,
    }
    message.success(activeMode.value === 'BLANK' ? '应用草稿已创建' : '应用已创建并完成初始化')
    emit('created', result)
    emit('update:show', false)
  }
  catch (error) {
    initializationWarning.value = error?.message || '应用初始化失败'
    if (createdDraft.value) {
      emit('draftSaved', {
        application: { ...basics.value, ...createdDraft.value },
        created: true,
        initializationWarning: initializationWarning.value,
      })
      message.warning('应用草稿已创建，但初始化失败，请修正后重试')
    }
    else {
      message.error(initializationWarning.value)
    }
  }
  finally {
    saving.value = false
  }
}

async function createDraft() {
  const payload = {
    applicationName: basics.value.applicationName.trim(),
    applicationCode: basics.value.applicationCode.trim() || null,
    suiteCode: basics.value.suiteCode,
    icon: basics.value.icon || null,
    description: basics.value.description.trim() || null,
    status: Number(basics.value.status),
  }
  const response = await createBusinessApplication(payload)
  const result = resolveApplicationCreateResult(response.data, payload.applicationCode)
  basics.value = { ...basics.value, applicationCode: result.applicationCode }
  return result
}

async function initialize(payload) {
  const applicationId = createdDraft.value.id
  if (activeMode.value === 'TEMPLATE') {
    const response = await initializeBusinessApplicationTemplate(applicationId, payload.initialization)
    return response.data || null
  }
  if (activeMode.value === 'AI') {
    const response = await initializeBusinessApplicationAi(applicationId, payload.plan)
    return response.data || null
  }
  if (activeMode.value === 'EXCEL') {
    const response = await initializeBusinessApplicationExcel(applicationId, payload.file, payload)
    return response.data || null
  }
  return null
}
</script>

<style scoped>
.app-create-wizard__body {
  display: grid;
  max-height: min(72vh, 760px);
  gap: 14px;
  overflow-y: auto;
  padding-right: 4px;
}

.create-mode-tabs :deep(.n-tabs-rail) {
  padding: 3px;
  border-radius: 8px;
  background: #f2f3f5;
}

/* 选中态与属性面板 segment 一致：主题蓝胶囊 + 白字 */
.create-mode-tabs :deep(.n-tabs-capsule) {
  border-radius: 6px;
  background: var(--primary-color, #3370ff);
  box-shadow: none;
}

.create-mode-tabs :deep(.n-tabs-tab) {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--text-secondary, var(--n-text-color-2));
  font-weight: 600;
}

.create-mode-tabs :deep(.n-tabs-tab--active) {
  --n-tab-text-color-active: #fff;
  color: #fff !important;
}

.create-mode-tabs :deep(.n-tabs-tab:not(.n-tabs-tab--active):hover) {
  color: var(--primary-color, #3370ff);
}

.create-mode-tabs :deep(.n-tabs-tab .n-icon) {
  color: inherit;
}

.create-mode-panel {
  min-height: 110px;
}

.app-create-wizard fieldset {
  min-width: 0;
  margin: 0;
  padding: 0;
  border: 0;
}

.app-create-wizard fieldset:disabled {
  opacity: 0.7;
}

.app-create-wizard__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.create-result-hint {
  overflow: hidden;
  color: var(--n-text-color-3);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 760px) {
  .app-create-wizard__footer {
    align-items: flex-end;
    flex-direction: column;
  }
}
</style>
