<template>
  <n-drawer :show="show" width="760" @update:show="handleDrawerShow">
    <n-drawer-content :title="form.id ? '编辑访问入口' : '新建访问入口'" closable>
      <div class="entry-wizard">
        <n-steps :current="currentStep" size="small">
          <n-step title="选择场景" />
          <n-step title="配置入口" />
        </n-steps>

        <section v-if="currentStep === 1" class="wizard-step scene-step">
          <button
            v-for="scene in sceneList"
            :key="scene.key"
            class="scene-card"
            :class="{ active: sceneKey === scene.key, disabled: Boolean(form.id) }"
            type="button"
            :disabled="Boolean(form.id)"
            @click="selectScene(scene.key)"
          >
            <span class="scene-icon" :class="scene.tone">
              <n-icon :component="scene.icon" />
            </span>
            <span class="scene-copy">
              <strong>{{ scene.label }}</strong>
              <small>{{ scene.description }}</small>
            </span>
          </button>
          <n-alert v-if="form.id" type="info" :bordered="false">
            编辑已有入口时保留原场景。需要切换入口模式时，请使用高级编辑。
          </n-alert>
        </section>

        <section v-else-if="currentStep === 2" class="wizard-step">
          <n-form ref="formRef" :model="form" label-placement="top" :show-feedback="false">
            <div class="section-title">
              <span>基础信息</span>
              <small>已按{{ currentScene.label }}自动填充</small>
            </div>
            <n-grid :cols="2" :x-gap="12" :y-gap="2" responsive="screen">
              <n-form-item-gi label="入口名称">
                <n-input
                  :value="form.appName"
                  placeholder="例如：采购管理"
                  @update:value="handleAppNameChange"
                />
              </n-form-item-gi>
              <n-form-item-gi v-if="!lockSuite" label="所属业务域">
                <n-select
                  v-model:value="form.suiteCode"
                  filterable
                  :options="suiteOptions"
                  placeholder="选择业务域"
                />
              </n-form-item-gi>
              <n-form-item-gi v-if="showObjectSelect" label="关联业务单元">
                <n-select
                  v-model:value="form.objectCode"
                  clearable
                  filterable
                  :loading="objectLoading"
                  :options="objectOptions"
                  :placeholder="currentScene.objectPlaceholder"
                />
              </n-form-item-gi>
            </n-grid>
            <div v-if="isRuntimeScene && lockSuite" class="auto-config-summary">
              <span class="auto-config-mark">自动</span>
              <span v-if="selectedObject">已关联应用内业务单元“{{ selectedObjectName }}”</span>
              <span v-else-if="objectLoading">正在读取应用内业务单元...</span>
              <span v-else-if="objectOptions.length">请选择一个应用内业务单元，页面和表单配置会自动带出。</span>
              <span v-else>当前应用没有可用业务单元，请先在“数据对象”中添加。</span>
            </div>

            <template v-if="isRuntimeScene">
              <div class="section-title">
                <span>打开内容</span>
                <small>默认配置自动生效</small>
              </div>
              <n-grid :cols="2" :x-gap="12" :y-gap="2" responsive="screen">
                <n-form-item-gi
                  v-if="sceneKey === 'DATA_MANAGE' && runtimePageOptions.length > 1"
                  label="目标页面"
                >
                  <n-select
                    v-model:value="form.targetPageKey"
                    :options="runtimePageOptions"
                    :loading="designerLoading"
                    filterable
                    placeholder="列表页（默认）"
                  />
                </n-form-item-gi>
                <n-form-item-gi v-if="runtimeFormOptions.length > 1" label="目标表单">
                  <n-select
                    v-model:value="form.targetFormKey"
                    :options="runtimeFormOptions"
                    :loading="designerLoading"
                    clearable
                    filterable
                    placeholder="默认表单"
                  />
                </n-form-item-gi>
              </n-grid>
              <div class="auto-config-summary runtime-summary">
                <span class="auto-config-mark">自动</span>
                <span>{{ runtimeAutoSummary }}</span>
              </div>
            </template>

            <template v-if="sceneKey === 'DASHBOARD'">
              <div class="section-title">
                <span>页面路由</span>
                <small>打开系统已有页面</small>
              </div>
              <n-form-item label="页面地址">
                <n-input v-model:value="form.entryUrl" placeholder="例如：/app-center/stats" />
              </n-form-item>
            </template>

            <template v-if="sceneKey === 'EXTERNAL_PAGE'">
              <div class="section-title">
                <span>外部页面</span>
                <small>内嵌或新窗口打开</small>
              </div>
              <n-grid :cols="2" :x-gap="12" :y-gap="2" responsive="screen">
                <n-form-item-gi label="打开方式">
                  <n-select v-model:value="form.entryMode" :options="externalModeOptions" />
                </n-form-item-gi>
                <n-form-item-gi label="页面地址">
                  <n-input v-model:value="form.entryUrl" placeholder="https://example.com/dashboard" />
                </n-form-item-gi>
              </n-grid>
              <n-form-item label="域名白名单">
                <n-input
                  v-model:value="form.allowedDomains"
                  type="textarea"
                  placeholder="example.com 或 *.example.com，多个域名换行"
                />
              </n-form-item>
            </template>

            <template v-if="sceneKey === 'MOBILE'">
              <div class="section-title">
                <span>移动入口</span>
                <small>H5 / 移动端业务</small>
              </div>
              <n-grid :cols="2" :x-gap="12" :y-gap="2" responsive="screen">
                <n-form-item-gi label="H5 地址">
                  <n-input v-model:value="form.entryUrl" placeholder="https://m.example.com/customer" />
                </n-form-item-gi>
                <n-form-item-gi label="移动场景">
                  <n-select v-model:value="form.mobileScene" :options="mobileSceneOptions" />
                </n-form-item-gi>
                <n-form-item-gi label="可见范围">
                  <n-select v-model:value="form.visibleScope" :options="visibleScopeOptions" />
                </n-form-item-gi>
              </n-grid>
            </template>

            <template v-if="sceneKey === 'INTEGRATION'">
              <div class="section-title">
                <span>接口服务</span>
                <small>API / Webhook / 系统集成</small>
              </div>
              <n-grid :cols="2" :x-gap="12" :y-gap="2" responsive="screen">
                <n-form-item-gi label="集成类型">
                  <n-select v-model:value="form.platformType" :options="platformTypeOptions" />
                </n-form-item-gi>
                <n-form-item-gi label="业务资源键">
                  <n-input v-model:value="form.integrationResource" placeholder="例如：crm.customer.webhook" />
                </n-form-item-gi>
              </n-grid>
              <n-form-item label="事件范围">
                <n-input v-model:value="form.integrationEvents" placeholder="例如：created,updated,approved" />
              </n-form-item>
            </template>

            <template v-if="showAdminMenuConfig">
              <div class="section-title">
                <span>菜单配置</span>
                <small>可选</small>
              </div>
              <div class="menu-config-row">
                <div>
                  <strong>添加到管理端菜单</strong>
                  <span>开启后自动放到“{{ selectedSuiteName }}”业务域目录，其他参数使用系统默认值。</span>
                </div>
                <n-switch v-model:value="form.adminMenuSyncEnabled" />
              </div>
            </template>

            <n-collapse class="optional-settings">
              <n-collapse-item title="更多设置" name="optional">
                <n-form-item label="入口图标">
                  <IconSelector v-model="form.icon" />
                </n-form-item>
                <n-form-item label="业务说明">
                  <n-input v-model:value="form.description" type="textarea" placeholder="说明这个入口面向的业务场景" />
                </n-form-item>
                <n-form-item label="启用状态">
                  <n-switch v-model:value="form.status" :checked-value="1" :unchecked-value="0" />
                </n-form-item>
              </n-collapse-item>
            </n-collapse>
          </n-form>
        </section>
      </div>

      <template #footer>
        <n-space justify="space-between" align="center">
          <n-button secondary @click="openAdvancedEditor">
            高级编辑
          </n-button>
          <n-space>
            <n-button @click="handleCancel">
              取消
            </n-button>
            <n-button v-if="currentStep > firstStep" @click="currentStep -= 1">
              上一步
            </n-button>
            <n-button v-if="currentStep < 2" type="primary" @click="goNext">
              下一步
            </n-button>
            <n-button v-else type="primary" :loading="saving" @click="save">
              <template #icon>
                <n-icon><SaveOutline /></n-icon>
              </template>
              {{ form.id ? '保存' : '创建入口' }}
            </n-button>
          </n-space>
        </n-space>
      </template>
    </n-drawer-content>
  </n-drawer>

  <AppEditorDrawer
    v-model:show="advancedVisible"
    :app="advancedApp"
    :suites="suites"
    @saved="handleAdvancedSaved"
  />
</template>

<script setup>
import {
  CodeSlashOutline,
  DocumentTextOutline,
  GlobeOutline,
  ListOutline,
  PhonePortraitOutline,
  SaveOutline,
  StatsChartOutline,
} from '@vicons/ionicons5'
import { useMessage } from 'naive-ui'
import { computed, defineAsyncComponent, reactive, ref, watch } from 'vue'
import { businessObjectDesigner, businessObjectList, createBusinessApp, updateBusinessApp } from '@/api/business-app'
import IconSelector from '@/components/IconSelector.vue'
import { useDict } from '@/composables/useDict'
import { normalizeMultiFormDesignerSchema } from './designer/form-first/formDesignerSchema'

const props = defineProps({
  show: {
    type: Boolean,
    default: false,
  },
  app: {
    type: Object,
    default: null,
  },
  suites: {
    type: Array,
    default: () => [],
  },
  applicationId: {
    type: [Number, String],
    default: null,
  },
  defaultSuiteCode: {
    type: String,
    default: null,
  },
  lockSuite: {
    type: Boolean,
    default: false,
  },
  objects: {
    type: Array,
    default: null,
  },
})

const emit = defineEmits(['update:show', 'saved'])
const AppEditorDrawer = defineAsyncComponent(() => import('./AppEditorDrawer.vue'))
const message = useMessage()
const { dict } = useDict(
  'ai_business_app_entry_mode',
  'ai_business_app_mobile_scene',
  'ai_business_app_visible_scope',
  'ai_business_app_platform_type',
)

const SCENE_TEMPLATES = {
  DATA_MANAGE: {
    key: 'DATA_MANAGE',
    label: '管理数据',
    icon: ListOutline,
    tone: 'blue',
    description: '列表、搜索、增删改查和流程处理。',
    objectPlaceholder: '选择要管理的业务单元',
    nameSuffix: '管理',
    codeSuffix: 'MANAGE',
    defaults: {
      appType: 'BUSINESS',
      mountTarget: 'ADMIN',
      entryMode: 'RUNTIME',
      entryType: 'OBJECT_LIST',
      runtimeOpenMode: 'LIST',
      appMode: 'DYNAMIC_RENDER',
      targetPageKey: 'list',
    },
  },
  FORM_SUBMIT: {
    key: 'FORM_SUBMIT',
    label: '填报单据',
    icon: DocumentTextOutline,
    tone: 'green',
    description: '申请、登记、提交和上报，不先进入列表。',
    objectPlaceholder: '选择承载表单的业务单元',
    nameSuffix: '填报',
    codeSuffix: 'FORM',
    defaults: {
      appType: 'BUSINESS',
      mountTarget: 'ADMIN',
      entryMode: 'RUNTIME',
      entryType: 'CREATE_FORM',
      runtimeOpenMode: 'CREATE_FORM',
      appMode: 'DYNAMIC_RENDER',
      targetPageKey: 'list',
    },
  },
  DASHBOARD: {
    key: 'DASHBOARD',
    label: '数据看板',
    icon: StatsChartOutline,
    tone: 'cyan',
    description: '跳转报表、统计页或经营驾驶舱。',
    objectPlaceholder: '可选，关联后按业务单元归集',
    nameSuffix: '看板',
    codeSuffix: 'DASHBOARD',
    defaults: {
      appType: 'EMBEDDED',
      mountTarget: 'ADMIN',
      entryMode: 'ROUTE',
      entryType: 'REPORT_DASHBOARD',
      runtimeOpenMode: 'LIST',
      appMode: 'DYNAMIC_RENDER',
      targetPageKey: 'list',
    },
  },
  EXTERNAL_PAGE: {
    key: 'EXTERNAL_PAGE',
    label: '外部页面',
    icon: GlobeOutline,
    tone: 'amber',
    description: '内嵌或新窗口打开第三方系统页面。',
    objectPlaceholder: '可选，关联后按业务单元归集',
    nameSuffix: '外部入口',
    codeSuffix: 'LINK',
    defaults: {
      appType: 'EMBEDDED',
      mountTarget: 'ADMIN',
      entryMode: 'IFRAME',
      entryType: 'EXTERNAL_OR_API',
      runtimeOpenMode: 'LIST',
      appMode: 'DYNAMIC_RENDER',
      targetPageKey: 'list',
    },
  },
  MOBILE: {
    key: 'MOBILE',
    label: '移动入口',
    icon: PhonePortraitOutline,
    tone: 'violet',
    description: 'H5、移动端业务和轻应用入口。',
    objectPlaceholder: '可选，关联后按业务单元归集',
    nameSuffix: '移动端',
    codeSuffix: 'MOBILE',
    defaults: {
      appType: 'MOBILE',
      mountTarget: 'MOBILE',
      entryMode: 'H5',
      entryType: 'EXTERNAL_OR_API',
      runtimeOpenMode: 'LIST',
      appMode: 'DYNAMIC_RENDER',
      targetPageKey: 'list',
    },
  },
  INTEGRATION: {
    key: 'INTEGRATION',
    label: '接口服务',
    icon: CodeSlashOutline,
    tone: 'slate',
    description: '登记 API、Webhook 和外部系统集成资源。',
    objectPlaceholder: '可选，用于标识接口所属业务单元',
    nameSuffix: '接口服务',
    codeSuffix: 'API',
    defaults: {
      appType: 'INTEGRATION',
      mountTarget: 'API',
      entryMode: 'API',
      entryType: 'EXTERNAL_OR_API',
      runtimeOpenMode: 'LIST',
      appMode: 'DYNAMIC_RENDER',
      targetPageKey: 'list',
    },
  },
}

const sceneList = Object.values(SCENE_TEMPLATES)
const currentStep = ref(1)
const sceneKey = ref('DATA_MANAGE')
const formRef = ref(null)
const form = reactive(defaultForm())
const objectOptions = ref([])
const objectLoading = ref(false)
const designerLoading = ref(false)
const selectedObjectDesigner = ref(null)
const saving = ref(false)
const appNameTouched = ref(false)
const appCodeTouched = ref(false)
const initializing = ref(false)
const advancedVisible = ref(false)
const advancedApp = ref(null)
let designerRequestSeq = 0
let objectRequestSeq = 0

const currentScene = computed(() => SCENE_TEMPLATES[sceneKey.value] || SCENE_TEMPLATES.DATA_MANAGE)
const firstStep = computed(() => form.id ? 2 : 1)
const isRuntimeScene = computed(() => ['DATA_MANAGE', 'FORM_SUBMIT'].includes(sceneKey.value))
const showObjectSelect = computed(() => {
  if (sceneKey.value === 'INTEGRATION' && !form.suiteCode)
    return false
  return !(props.lockSuite && Array.isArray(props.objects) && objectOptions.value.length <= 1)
})
const showAdminMenuConfig = computed(() => form.mountTarget === 'ADMIN')
const selectedObject = computed(() => objectOptions.value.find(item => item.value === form.objectCode) || null)
const selectedSuiteName = computed(() => {
  const suite = props.suites.find(item => item.suiteCode === form.suiteCode)
  return suite?.suiteName || form.suiteCode || '-'
})
const selectedObjectName = computed(() => selectedObject.value?.label || form.objectCode || (isRuntimeScene.value ? '-' : '未关联'))
const suiteOptions = computed(() => props.suites.map(item => ({
  label: item.suiteName || item.suiteCode,
  value: item.suiteCode,
})))
const runtimePageOptions = computed(() => {
  const pageSchema = selectedObjectDesigner.value?.pageSchema || {}
  const pages = Array.isArray(pageSchema.pages) ? pageSchema.pages : []
  const options = pages
    .filter(page => page?.pageKey)
    .map(page => ({
      label: `${page.pageName || page.pageKey}${page.pageKey === 'list' ? '（默认）' : ''}`,
      value: page.pageKey,
    }))
  if (!options.length)
    options.push({ label: '列表页（默认）', value: 'list' })
  if (form.targetPageKey && !options.some(item => item.value === form.targetPageKey))
    options.push({ label: pageLabelFallback(form.targetPageKey), value: form.targetPageKey })
  return options
})
const runtimeFormOptions = computed(() => {
  if (!selectedObjectDesigner.value?.formDesignerSchema)
    return []
  const schema = normalizeMultiFormDesignerSchema(selectedObjectDesigner.value.formDesignerSchema || {})
  return (schema.forms || [])
    .filter(item => item?.formKey)
    .map(item => ({
      label: `${item.formName || item.formKey}${item.formKey === schema.defaultFormKey ? '（默认）' : ''}`,
      value: item.formKey,
    }))
})
const runtimeAutoSummary = computed(() => {
  const formOption = runtimeFormOptions.value.find(item => item.value === form.targetFormKey)
  const formLabel = formOption?.label || '默认表单'
  const target = form.runtimeOpenMode === 'CREATE_FORM'
    ? `直接打开${formLabel}`
    : `打开${pageLabel(form.targetPageKey)}，使用${formLabel}`
  return form.configKey ? `${target}；业务页面配置已自动关联` : `${target}；业务单元发布后自动关联页面配置`
})

const externalModeOptions = computed(() => (dict.value.ai_business_app_entry_mode || [])
  .filter(item => ['IFRAME', 'EXTERNAL'].includes(item.value)))
const mobileSceneOptions = computed(() => dict.value.ai_business_app_mobile_scene || [])
const visibleScopeOptions = computed(() => dict.value.ai_business_app_visible_scope || [])
const platformTypeOptions = computed(() => dict.value.ai_business_app_platform_type || [])

watch(() => props.show, (visible) => {
  if (visible)
    initializeForm()
})

watch(sceneKey, (key) => {
  if (initializing.value)
    return
  applySceneDefaults(key)
  refreshSuggestedName()
  refreshSuggestedCode()
}, { flush: 'sync' })

watch(() => form.suiteCode, async (suiteCode, previous) => {
  if (initializing.value)
    return
  if (!suiteCode) {
    objectOptions.value = []
    form.objectCode = null
    return
  }
  if (previous && previous !== suiteCode)
    form.objectCode = null
  await loadObjects()
  refreshSuggestedCode()
}, { flush: 'sync' })

watch(() => form.objectCode, () => {
  if (initializing.value)
    return
  const object = selectedObject.value
  if (object?.configKey)
    form.configKey = object.configKey
  else if (!form.id && !initializing.value)
    form.configKey = ''
  if (!form.permissionCode)
    form.permissionCode = defaultPermissionCode()
  refreshSuggestedName()
  refreshSuggestedCode()
  loadSelectedObjectDesigner()
}, { flush: 'sync' })

function initializeForm() {
  initializing.value = true
  designerRequestSeq += 1
  objectRequestSeq += 1
  objectOptions.value = []
  selectedObjectDesigner.value = null
  objectLoading.value = false
  designerLoading.value = false
  Object.assign(form, defaultForm(), props.app || {})
  form.applicationId = props.applicationId || form.applicationId || null
  form.suiteCode = props.defaultSuiteCode || form.suiteCode
  hydrateOptions()
  sceneKey.value = inferSceneKey(form)
  if (!form.suiteCode && props.suites.length)
    form.suiteCode = props.suites[0].suiteCode
  appNameTouched.value = Boolean(form.appName)
  appCodeTouched.value = Boolean(form.appCode)
  currentStep.value = firstStep.value
  initializing.value = false
  loadObjects()
}

function selectScene(key) {
  if (form.id)
    return
  sceneKey.value = key
}

function applySceneDefaults(key) {
  const defaults = SCENE_TEMPLATES[key]?.defaults || SCENE_TEMPLATES.DATA_MANAGE.defaults
  Object.assign(form, defaults)
  if (key === 'MOBILE') {
    form.mobileScene = form.mobileScene || 'h5'
    form.visibleScope = form.visibleScope || 'all'
  }
  if (key === 'INTEGRATION')
    form.platformType = form.platformType || 'api'
  if (key !== 'EXTERNAL_PAGE')
    form.allowedDomains = ''
}

function handleDrawerShow(value) {
  emit('update:show', value)
}

function handleCancel() {
  emit('update:show', false)
}

async function goNext() {
  const valid = await validateCurrentStep()
  if (!valid)
    return
  currentStep.value += 1
}

async function validateCurrentStep() {
  if (currentStep.value === 1)
    return true
  if (currentStep.value !== 2)
    return true
  if (!String(form.appName || '').trim()) {
    message.warning('请输入入口名称')
    return false
  }
  if (!String(form.appCode || '').trim()) {
    message.warning('请输入入口编码')
    return false
  }
  if (!form.suiteCode) {
    message.warning('请选择业务域')
    return false
  }
  if (isRuntimeScene.value && !form.objectCode) {
    message.warning('业务页面入口需要关联业务单元')
    return false
  }
  if (isRuntimeScene.value && !form.configKey) {
    message.warning('关联业务单元尚未发布页面配置，请先发布业务单元后再创建入口')
    return false
  }
  if (['DASHBOARD', 'EXTERNAL_PAGE', 'MOBILE'].includes(sceneKey.value) && !String(form.entryUrl || '').trim()) {
    message.warning('请输入入口地址')
    return false
  }
  if (sceneKey.value === 'INTEGRATION' && !String(form.integrationResource || '').trim()) {
    message.warning('请输入业务资源键')
    return false
  }
  return true
}

async function save() {
  const valid = await validateCurrentStep()
  if (!valid)
    return
  saving.value = true
  try {
    const payload = buildDraftPayload()
    if (payload.id)
      await updateBusinessApp(payload)
    else
      await createBusinessApp(payload)
    message.success('访问入口已保存')
    emit('saved')
    emit('update:show', false)
  }
  finally {
    saving.value = false
  }
}

function openAdvancedEditor() {
  advancedApp.value = buildDraftPayload({ strict: false })
  emit('update:show', false)
  advancedVisible.value = true
}

function handleAdvancedSaved() {
  advancedVisible.value = false
  emit('saved')
}

async function loadObjects() {
  if (!form.suiteCode)
    return
  const seq = ++objectRequestSeq
  objectLoading.value = true
  try {
    const previousObjectCode = form.objectCode
    const source = Array.isArray(props.objects)
      ? props.objects.filter(item => Number(item.objectStatus ?? item.status ?? 1) === 1)
      : (await businessObjectList({ suiteCode: form.suiteCode, status: 1 })).data || []
    if (seq !== objectRequestSeq)
      return
    objectOptions.value = source.map(item => ({
      label: item.objectName || item.objectCode,
      value: item.objectCode,
      objectId: item.objectId || item.id,
      objectType: item.objectType,
      configKey: item.configKey,
      objectRole: item.objectRole,
    }))
    if (form.objectCode && !objectOptions.value.some(item => item.value === form.objectCode))
      objectOptions.value.push({ label: form.objectCode, value: form.objectCode, configKey: form.configKey })
    if (!form.objectCode && !form.id) {
      const preferred = objectOptions.value.find(item => item.objectRole === 'PRIMARY')
        || (objectOptions.value.length === 1 ? objectOptions.value[0] : null)
      if (preferred)
        form.objectCode = preferred.value
    }
    if (selectedObject.value?.configKey && !form.configKey)
      form.configKey = selectedObject.value.configKey
    if (form.objectCode === previousObjectCode)
      loadSelectedObjectDesigner()
  }
  finally {
    if (seq === objectRequestSeq)
      objectLoading.value = false
  }
}

async function loadSelectedObjectDesigner() {
  const objectId = selectedObject.value?.objectId
  const seq = ++designerRequestSeq
  if (!objectId) {
    selectedObjectDesigner.value = null
    return
  }
  designerLoading.value = true
  try {
    const res = await businessObjectDesigner(objectId)
    if (seq === designerRequestSeq) {
      selectedObjectDesigner.value = res.data || null
      applyRuntimeDesignerDefaults()
    }
  }
  catch (error) {
    if (seq === designerRequestSeq) {
      selectedObjectDesigner.value = null
      console.warn('[AppEntryWizard] 加载业务对象设计数据失败', error?.message || error)
    }
  }
  finally {
    if (seq === designerRequestSeq)
      designerLoading.value = false
  }
}

function handleAppNameChange(value) {
  form.appName = value
  appNameTouched.value = true
  if (!form.id && sceneKey.value === 'DATA_MANAGE' && /填报|申请|提交|录入|上报|登记/.test(String(value || '')))
    sceneKey.value = 'FORM_SUBMIT'
  refreshSuggestedCode()
}

function refreshSuggestedName() {
  if (appNameTouched.value)
    return
  const objectName = selectedObject.value?.label
  if (objectName)
    form.appName = `${objectName}${currentScene.value.nameSuffix}`
}

function applyRuntimeDesignerDefaults() {
  const formOptions = runtimeFormOptions.value
  if (!form.targetFormKey || !formOptions.some(item => item.value === form.targetFormKey)) {
    const schema = normalizeMultiFormDesignerSchema(selectedObjectDesigner.value?.formDesignerSchema || {})
    form.targetFormKey = schema.defaultFormKey || formOptions[0]?.value || ''
  }
  const pageOptions = runtimePageOptions.value
  if (!form.targetPageKey || !pageOptions.some(item => item.value === form.targetPageKey))
    form.targetPageKey = pageOptions[0]?.value || defaultTargetPageKey(form.runtimeOpenMode)
}

function refreshSuggestedCode() {
  if (appCodeTouched.value)
    return
  form.appCode = suggestAppCode()
}

function suggestAppCode() {
  const base = form.objectCode || form.suiteCode || 'APP'
  return normalizeAppCode(`${base}_${currentScene.value.codeSuffix}`)
}

function normalizeAppCode(value) {
  const normalized = String(value || '')
    .trim()
    .toUpperCase()
    .replace(/[^A-Z0-9_]/g, '_')
    .replace(/_+/g, '_')
    .replace(/^_+|_+$/g, '')
  const withPrefix = /^[A-Z]/.test(normalized) ? normalized : `APP_${normalized || Date.now().toString(36).toUpperCase()}`
  return withPrefix.slice(0, 64)
}

function buildDraftPayload() {
  const payload = {
    id: form.id || null,
    applicationId: form.applicationId || null,
    appName: String(form.appName || '').trim(),
    appCode: normalizeAppCode(form.appCode),
    appType: resolveAppType(),
    suiteCode: form.suiteCode,
    objectCode: form.objectCode || null,
    entryMode: form.entryMode,
    entryUrl: normalizeEntryUrl(),
    configKey: form.entryMode === 'RUNTIME' ? form.configKey || '' : '',
    runtimeOpenMode: form.entryMode === 'RUNTIME' ? form.runtimeOpenMode : 'LIST',
    appMode: form.entryMode === 'RUNTIME' ? form.appMode || 'DYNAMIC_RENDER' : 'DYNAMIC_RENDER',
    icon: form.icon || '',
    description: form.description || '',
    status: form.status,
    sortOrder: Number(form.sortOrder || 0),
    options: buildOptions(),
  }
  if (!payload.id)
    delete payload.id
  return payload
}

function normalizeEntryUrl() {
  if (form.entryMode === 'API')
    return form.integrationResource ? `api://${form.integrationResource}` : ''
  if (form.entryMode === 'RUNTIME')
    return ''
  return String(form.entryUrl || '').trim()
}

function buildOptions() {
  const options = parseOptions(form.options)
  options.mountTarget = form.mountTarget
  options.entryType = form.entryType
  const permissionCode = String(form.permissionCode || '').trim()
  if (permissionCode)
    options.permissionCode = permissionCode
  else
    delete options.permissionCode

  if (form.mountTarget === 'ADMIN') {
    options.adminMenu = {
      parentId: form.adminMenuParentId || null,
      originalParentId: form.adminMenuParentId || null,
      syncEnabled: Boolean(form.adminMenuSyncEnabled),
      suiteAsParent: Boolean(form.suiteAsMenuParent),
      sort: Number(form.menuSort || 0),
    }
  }
  else {
    delete options.adminMenu
  }

  if (form.entryMode === 'RUNTIME') {
    options.runtimeOpenMode = form.runtimeOpenMode
    options.appMode = form.appMode || 'DYNAMIC_RENDER'
    options.targetPageKey = form.targetPageKey || defaultTargetPageKey(form.runtimeOpenMode)
    options.targetFormKey = form.targetFormKey || null
  }
  else {
    delete options.runtimeOpenMode
    delete options.appMode
    delete options.targetPageKey
    delete options.targetFormKey
    delete options.defaultParams
  }

  const domains = String(form.allowedDomains || '')
    .split(/[\n,，]/)
    .map(item => item.trim().toLowerCase())
    .filter(Boolean)
  if (['IFRAME', 'EXTERNAL', 'H5'].includes(form.entryMode) && domains.length)
    options.allowedDomains = domains
  else
    delete options.allowedDomains

  if (sceneKey.value === 'MOBILE') {
    options.mobileScene = form.mobileScene || 'h5'
    options.visibleScope = form.visibleScope || 'all'
  }
  else {
    delete options.mobileScene
    delete options.visibleScope
  }

  if (sceneKey.value === 'INTEGRATION') {
    options.platformType = form.platformType || 'api'
    options.integrationResource = String(form.integrationResource || '').trim() || null
    options.integrationEvents = String(form.integrationEvents || '')
      .split(/[,，\n]/)
      .map(item => item.trim())
      .filter(Boolean)
  }
  else {
    delete options.platformType
    delete options.integrationResource
    delete options.integrationEvents
  }

  Object.keys(options).forEach((key) => {
    const value = options[key]
    if (value === null || value === undefined || value === '')
      delete options[key]
    if (Array.isArray(value) && !value.length)
      delete options[key]
  })
  return Object.keys(options).length ? JSON.stringify(options) : null
}

function hydrateOptions() {
  const options = parseOptions(form.options)
  const defaults = SCENE_TEMPLATES[inferSceneKey(form)].defaults
  form.mountTarget = options.mountTarget || defaults.mountTarget || 'ADMIN'
  form.entryType = options.entryType || defaults.entryType || 'OBJECT_LIST'
  form.runtimeOpenMode = form.runtimeOpenMode || options.runtimeOpenMode || defaults.runtimeOpenMode || 'LIST'
  form.appMode = form.appMode || options.appMode || defaults.appMode || 'DYNAMIC_RENDER'
  form.targetPageKey = options.targetPageKey || defaultTargetPageKey(form.runtimeOpenMode)
  form.targetFormKey = options.targetFormKey || ''
  form.permissionCode = options.permissionCode || ''
  const adminMenu = options.adminMenu || {}
  form.adminMenuParentId = adminMenu.originalParentId || adminMenu.parentId || null
  form.adminMenuSyncEnabled = Boolean(adminMenu.syncEnabled)
  form.suiteAsMenuParent = adminMenu.suiteAsParent !== false
  form.menuSort = Number(adminMenu.sort || form.sortOrder || 0)
  form.allowedDomains = Array.isArray(options.allowedDomains) ? options.allowedDomains.join('\n') : ''
  form.mobileScene = options.mobileScene || 'h5'
  form.visibleScope = options.visibleScope || 'all'
  form.platformType = options.platformType || 'api'
  form.integrationResource = options.integrationResource || stripApiPrefix(form.entryUrl)
  form.integrationEvents = Array.isArray(options.integrationEvents) ? options.integrationEvents.join(',') : options.integrationEvents || ''
}

function inferSceneKey(app = {}) {
  const options = parseOptions(app.options)
  const entryMode = String(app.entryMode || options.entryMode || '').toUpperCase()
  const appType = String(app.appType || '').toUpperCase()
  const mountTarget = String(options.mountTarget || '').toUpperCase()
  const runtimeOpenMode = String(app.runtimeOpenMode || options.runtimeOpenMode || '').toUpperCase()
  if (appType === 'INTEGRATION' || mountTarget === 'API' || entryMode === 'API')
    return 'INTEGRATION'
  if (appType === 'MOBILE' || mountTarget === 'MOBILE' || entryMode === 'H5')
    return 'MOBILE'
  if (['IFRAME', 'EXTERNAL'].includes(entryMode))
    return 'EXTERNAL_PAGE'
  if (entryMode === 'ROUTE')
    return 'DASHBOARD'
  if (runtimeOpenMode === 'CREATE_FORM')
    return 'FORM_SUBMIT'
  return 'DATA_MANAGE'
}

function resolveAppType() {
  if (sceneKey.value === 'MOBILE')
    return 'MOBILE'
  if (sceneKey.value === 'INTEGRATION')
    return 'INTEGRATION'
  if (form.entryMode === 'RUNTIME' || form.objectCode)
    return 'BUSINESS'
  return 'EMBEDDED'
}

function defaultTargetPageKey(runtimeOpenMode) {
  return runtimeOpenMode === 'DETAIL' ? 'detail' : 'list'
}

function defaultPermissionCode() {
  const objectCode = String(form.objectCode || '').trim()
  if (!objectCode)
    return ''
  if (form.entryType === 'CREATE_FORM')
    return `ai:business:${objectCode}:add`
  if (form.entryType === 'DETAIL_PAGE')
    return `ai:business:${objectCode}:query`
  if (form.entryType === 'REPORT_DASHBOARD')
    return 'ai:businessStats:view'
  return `ai:business:${objectCode}:list`
}

function pageLabel(value) {
  return runtimePageOptions.value.find(item => item.value === value)?.label || value || '列表页'
}

function pageLabelFallback(value) {
  return {
    list: '列表页（默认）',
    detail: '详情页',
    create: '新增页',
    edit: '编辑页',
  }[value] || value || '列表页'
}

function stripApiPrefix(value) {
  return String(value || '').replace(/^api:\/\//i, '')
}

function parseOptions(options) {
  if (!options)
    return {}
  if (typeof options === 'string') {
    try {
      const parsed = JSON.parse(options)
      return parsed && typeof parsed === 'object' ? parsed : {}
    }
    catch {
      return {}
    }
  }
  return typeof options === 'object' ? { ...options } : {}
}

function defaultForm() {
  return {
    id: null,
    applicationId: null,
    appName: '',
    appCode: '',
    appType: 'BUSINESS',
    mountTarget: 'ADMIN',
    suiteCode: null,
    objectCode: null,
    entryMode: 'RUNTIME',
    entryType: 'OBJECT_LIST',
    appMode: 'DYNAMIC_RENDER',
    runtimeOpenMode: 'LIST',
    entryUrl: '',
    configKey: '',
    icon: '',
    description: '',
    status: 1,
    sortOrder: 0,
    options: '',
    adminMenuParentId: null,
    adminMenuSyncEnabled: false,
    suiteAsMenuParent: true,
    menuSort: 0,
    allowedDomains: '',
    mobileScene: 'h5',
    visibleScope: 'all',
    platformType: 'api',
    integrationResource: '',
    integrationEvents: '',
    permissionCode: '',
    targetPageKey: 'list',
    targetFormKey: '',
  }
}
</script>

<style scoped>
.entry-wizard {
  display: grid;
  gap: 18px;
}

.wizard-step {
  display: grid;
  gap: 14px;
  min-height: 420px;
  align-content: start;
  padding-top: 4px;
}

.scene-step {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.scene-step :deep(.n-alert) {
  grid-column: 1 / -1;
}

.scene-card {
  display: grid;
  grid-template-columns: 40px minmax(0, 1fr);
  gap: 12px;
  align-items: flex-start;
  min-height: 104px;
  cursor: pointer;
  border: 1px solid var(--n-border-color, #e5e7eb);
  border-radius: 8px;
  background: var(--n-color, #fff);
  padding: 14px;
  text-align: left;
  transition:
    border-color 160ms ease,
    background 160ms ease,
    box-shadow 160ms ease;
}

.scene-card:hover {
  border-color: var(--n-primary-color-suppl);
  background: var(--n-color-hover, #f7f8fa);
}

.scene-card.active {
  border-color: var(--n-primary-color);
  background: color-mix(in srgb, var(--n-primary-color) 8%, var(--n-color));
  box-shadow: inset 0 0 0 1px var(--n-primary-color);
}

.scene-card.disabled {
  cursor: not-allowed;
  opacity: 0.72;
}

.scene-icon {
  display: grid;
  width: 40px;
  height: 40px;
  place-items: center;
  border-radius: 8px;
  font-size: 20px;
}

.scene-icon.blue {
  background: #eff6ff;
  color: #2563eb;
}

.scene-icon.green {
  background: #ecfdf5;
  color: #16a34a;
}

.scene-icon.cyan {
  background: #ecfeff;
  color: #0891b2;
}

.scene-icon.amber {
  background: #fffbeb;
  color: #d97706;
}

.scene-icon.violet {
  background: #f5f3ff;
  color: #7c3aed;
}

.scene-icon.slate {
  background: #f1f5f9;
  color: #475569;
}

.scene-copy {
  display: grid;
  gap: 5px;
  min-width: 0;
}

.scene-copy strong {
  color: var(--n-text-color, #111827);
  font-size: 15px;
  line-height: 20px;
}

.scene-copy small {
  color: var(--n-text-color-2, #64748b);
  font-size: 12px;
  line-height: 18px;
}

.section-title {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin: 2px 0 8px;
  border-bottom: 1px solid var(--n-border-color, #eef2f7);
  padding-bottom: 7px;
}

.section-title span {
  color: var(--n-text-color, #111827);
  font-size: 13px;
  font-weight: 700;
}

.section-title small {
  color: var(--n-text-color-3, #94a3b8);
  font-size: 12px;
}

.auto-config-summary,
.menu-config-row {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 44px;
  padding: 10px 12px;
  border: 1px solid var(--n-border-color, #e5e7eb);
  border-radius: 7px;
  background: var(--n-color-embedded, #f7f8fa);
  color: var(--n-text-color-2, #4e5969);
  font-size: 12px;
}

.runtime-summary {
  margin-top: -4px;
}

.auto-config-mark {
  flex: 0 0 auto;
  padding: 2px 6px;
  border-radius: 4px;
  color: var(--n-primary-color);
  background: color-mix(in srgb, var(--n-primary-color) 10%, transparent);
  font-size: 11px;
  font-weight: 600;
}

.menu-config-row {
  justify-content: space-between;
  align-items: flex-start;
}

.menu-config-row > div:first-child {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 3px;
}

.menu-config-row strong {
  color: var(--n-text-color);
  font-size: 13px;
}

.menu-config-row span {
  color: var(--n-text-color-3);
  line-height: 1.5;
}

.menu-config-row :deep(.n-switch) {
  flex: 0 0 auto;
}

.optional-settings {
  border-top: 1px solid var(--n-border-color, #e5e7eb);
}

@media (max-width: 720px) {
  .scene-step {
    grid-template-columns: 1fr;
  }

  .wizard-step {
    min-height: 360px;
  }
}
</style>
