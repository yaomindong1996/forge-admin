<template>
  <div class="app-builder-page">
    <header class="app-header">
      <div class="header-left">
        <n-button quaternary circle @click="router.push('/ai/lowcode-apps')">
          <template #icon>
            <n-icon><ArrowBackOutline /></n-icon>
          </template>
        </n-button>
        <div class="title-block">
          <div class="breadcrumb">
            <span>低代码应用</span>
            <span>{{ selectedDomain?.domainName || '选择业务域' }}</span>
            <span>应用页面设计</span>
          </div>
          <h1>{{ draft.appName || '新建应用页面' }}</h1>
          <p>{{ headerSubtitle }}</p>
        </div>
      </div>
      <n-space>
        <n-button @click="router.push('/ai/lowcode-models')">
          <template #icon>
            <n-icon><GitBranchOutline /></n-icon>
          </template>
          数据模型设计
        </n-button>
        <n-button :disabled="!canPreview" @click="currentStep = 'preview'">
          <template #icon>
            <n-icon><EyeOutline /></n-icon>
          </template>
          预览
        </n-button>
        <n-button :disabled="!currentAppId" @click="openCodePreview">
          <template #icon>
            <n-icon><CodeOutline /></n-icon>
          </template>
          代码预览
        </n-button>
        <n-button :disabled="!currentAppId" :loading="codeDownloading" @click="downloadBuilderCode">
          <template #icon>
            <n-icon><DownloadOutline /></n-icon>
          </template>
          下载代码
        </n-button>
        <n-button :loading="saving" @click="saveDraft">
          <template #icon>
            <n-icon><SaveOutline /></n-icon>
          </template>
          保存草稿
        </n-button>
        <n-button type="primary" :disabled="!canPreview" :loading="saving" @click="openPublishStep">
          <template #icon>
            <n-icon><RocketOutline /></n-icon>
          </template>
          发布上线
        </n-button>
      </n-space>
    </header>

    <n-spin :show="pageLoading" description="正在加载低代码应用..." class="builder-spin">
      <div class="app-shell">
        <main class="app-main">
          <section v-if="!selectedDomain" class="empty-canvas">
            <div class="empty-illustration">
              <n-icon><FolderOpenOutline /></n-icon>
            </div>
            <h2>从低代码应用页新建应用</h2>
            <p>应用页面归属于业务领域。请先在低代码应用页选择领域并点击新建应用，再选择一个或多个数据模型设计页面。</p>
            <n-space justify="center">
              <n-button type="primary" @click="router.push('/ai/lowcode-apps')">
                返回低代码应用
              </n-button>
              <n-button @click="router.push('/ai/lowcode-models')">
                进入数据模型设计
              </n-button>
            </n-space>
          </section>

          <template v-else>
            <n-alert
              v-if="selectedDomain.status === 'DISABLED' && !draft.id"
              type="warning"
              :bordered="false"
              class="status-alert"
            >
              当前业务领域已停用，不能新建应用。可以查看已有应用，或回到低代码应用页启用后再创建。
            </n-alert>

            <section class="domain-context">
              <div class="context-main">
                <div class="context-code">
                  {{ selectedDomain.domainCode }}
                </div>
                <h2>{{ selectedDomain.domainName }}</h2>
                <p>{{ selectedDomain.domainDesc || selectedDomain.domainSchema?.aiContext?.description || '该领域尚未维护业务说明。' }}</p>
              </div>
              <div class="metric-strip">
                <div>
                  <span>已选模型</span>
                  <strong>{{ selectedDataModels.length }}</strong>
                </div>
                <div>
                  <span>主模型</span>
                  <strong>{{ primaryModel?.modelName || '-' }}</strong>
                </div>
                <div>
                  <span>页面字段</span>
                  <strong>{{ boundFieldCount }}</strong>
                </div>
              </div>
            </section>

            <section class="app-config">
              <div class="step-strip">
                <button
                  v-for="step in stepItems"
                  :key="step.value"
                  type="button"
                  class="step-node"
                  :class="{ active: currentStep === step.value }"
                  @click="currentStep = step.value"
                >
                  <strong>{{ step.title }}</strong>
                  <span>{{ step.desc }}</span>
                </button>
              </div>

              <n-form label-placement="top" size="small" class="app-form" :show-feedback="false">
                <n-form-item label="应用名称">
                  <n-input
                    :value="draft.appName"
                    placeholder="例如：客户经营看板"
                    @update:value="updateAppName"
                  />
                </n-form-item>
                <n-form-item label="应用编码">
                  <n-input
                    :value="draft.configKey"
                    :disabled="!!appId"
                    placeholder="customer_operation"
                    @update:value="updateConfigKey"
                  />
                </n-form-item>
                <n-form-item label="所属业务域">
                  <n-input :value="selectedDomain.domainName" disabled />
                </n-form-item>
                <n-form-item label="页面模板">
                  <n-select
                    v-model:value="draft.pageSchema.layoutType"
                    :options="layoutOptions"
                    @update:value="handleLayoutTypeChange"
                  />
                </n-form-item>
                <n-form-item label="发布菜单">
                  <n-input v-model:value="draft.menuName" placeholder="默认同应用名称" />
                </n-form-item>
                <n-form-item label="菜单父级">
                  <MenuParentSelect v-model:value="draft.menuParentId" />
                </n-form-item>
              </n-form>
            </section>

            <section class="model-source-panel">
              <div class="source-head">
                <div>
                  <div class="section-title">
                    应用数据源
                  </div>
                  <p>标准列表使用一个数据模型；左树右表和主子表可选择多个数据模型，并指定一个主模型承接当前发布运行时。</p>
                </div>
                <n-space>
                  <n-button :loading="dataModelLoading" @click="loadDataModels">
                    刷新模型
                  </n-button>
                  <n-button @click="router.push('/ai/lowcode-models')">
                    维护数据模型
                  </n-button>
                </n-space>
              </div>
              <div class="source-select-row">
                <n-select
                  :value="selectedModelValue"
                  :multiple="allowMultipleModels"
                  filterable
                  clearable
                  :loading="dataModelLoading"
                  :options="modelOptions"
                  :placeholder="allowMultipleModels ? '选择一个或多个数据模型' : '选择一个数据模型'"
                  @update:value="handleModelSelectionChange"
                />
              </div>
              <div v-if="selectedDataModels.length" class="selected-model-grid">
                <button
                  v-for="model in selectedDataModels"
                  :key="resolveModelKey(model)"
                  type="button"
                  class="selected-model-card"
                  :class="{ primary: resolveModelKey(model) === primaryModelKey }"
                  @click="setPrimaryModel(resolveModelKey(model))"
                >
                  <span class="model-card-head">
                    <span>
                      <strong>{{ model.modelName }}</strong>
                      <code>{{ model.modelCode }}</code>
                    </span>
                    <n-tag
                      size="small"
                      :type="resolveModelKey(model) === primaryModelKey ? 'success' : 'default'"
                      :bordered="false"
                    >
                      {{ resolveModelKey(model) === primaryModelKey ? '主模型' : '引用模型' }}
                    </n-tag>
                  </span>
                  <span class="model-card-meta">
                    {{ model.modelSchema?.fields?.length || 0 }} 个字段
                    <span v-if="model.masterData"> · 主数据</span>
                    <span v-if="model.tenantEnabled"> · 多租户</span>
                  </span>
                </button>
              </div>
              <n-empty
                v-else
                size="small"
                description="当前应用尚未选择数据模型"
                class="model-empty"
              />
            </section>

            <section class="stage-panel">
              <div v-show="currentStep === 'source'" class="source-stage">
                <div class="field-pool-head">
                  <div>
                    <div class="section-title">
                      模型字段池
                    </div>
                    <p>页面设计阶段可从这些字段中选择绑定来源。</p>
                  </div>
                  <n-tag :bordered="false">
                    {{ pageDesignFields.length }} 个可用字段
                  </n-tag>
                </div>
                <div v-if="fieldPools.length" class="field-pool-grid">
                  <div v-for="pool in fieldPools" :key="pool.key" class="field-pool-card">
                    <div class="pool-title">
                      <strong>{{ pool.name }}</strong>
                      <code>{{ pool.code }}</code>
                    </div>
                    <div class="field-chip-list">
                      <span
                        v-for="field in pool.fields.slice(0, 16)"
                        :key="field.field"
                        class="field-chip"
                      >
                        {{ field.label || field.field }}
                      </span>
                      <span v-if="pool.fields.length > 16" class="field-chip muted">
                        +{{ pool.fields.length - 16 }}
                      </span>
                    </div>
                  </div>
                </div>
                <n-empty v-else description="请先选择数据模型" />
              </div>

              <LowcodePageBuilder
                v-show="currentStep === 'page'"
                v-model="draft.pageSchema"
                :model-schema="pageDesignModelSchema"
              />
              <LowcodePreviewPane
                v-show="currentStep === 'preview'"
                :app-id="appId"
                :draft="draft"
              />
              <PublishPanel
                v-show="currentStep === 'publish'"
                :app-id="appId"
                :draft="draft"
                @published="reloadDetail"
                @rolled-back="reloadDetail"
              />
              <section v-show="currentStep === 'code'" class="code-output-stage">
                <div class="code-output-head">
                  <div>
                    <div class="section-title">
                      应用代码输出
                    </div>
                    <p>基于已保存草稿或发布版本生成前后端代码包，下载后用于二次开发。</p>
                  </div>
                  <n-tag :bordered="false">
                    {{ currentAppId ? '可生成' : '请先保存草稿' }}
                  </n-tag>
                </div>
                <n-form label-placement="top" size="small" class="code-output-form" :show-feedback="false">
                  <div class="code-settings-grid">
                    <n-form-item label="生成来源">
                      <n-select v-model:value="codeSourceType" :options="codeSourceOptions" />
                    </n-form-item>
                    <n-form-item label="Group ID">
                      <n-input
                        v-model:value="codegenForm.groupId"
                        clearable
                        :disabled="codegenOptionsLoading"
                        placeholder="com.mdframe.forge.business"
                      />
                    </n-form-item>
                    <div class="code-output-actions">
                      <n-button :loading="codegenSaving" :disabled="!currentAppId" @click="saveCodegenOptions">
                        保存设置
                      </n-button>
                      <n-button :disabled="!currentAppId" @click="openCodePreview">
                        预览代码
                      </n-button>
                      <n-button type="primary" :loading="codeDownloading" :disabled="!currentAppId" @click="downloadBuilderCode">
                        下载代码
                      </n-button>
                    </div>
                  </div>
                </n-form>
              </section>
            </section>
          </template>
        </main>
      </div>
    </n-spin>
    <LowcodeCodePreviewModal
      v-model:show="codePreviewVisible"
      :app-id="currentAppId"
      :app-name="draft.appName || draft.configKey"
      :default-source-type="codeSourceType"
      :codegen-options="builderCodegenOptions"
    />
  </div>
</template>

<script setup>
import {
  ArrowBackOutline,
  CodeOutline,
  DownloadOutline,
  EyeOutline,
  FolderOpenOutline,
  GitBranchOutline,
  RocketOutline,
  SaveOutline,
} from '@vicons/ionicons5'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  lowcodeAppCodeOptions,
  lowcodeAppDetail,
  lowcodeDomainDefaults,
  lowcodeDomainTree,
  lowcodeDownloadAppCode,
  lowcodeModelList,
  lowcodeSaveAppCodeOptions,
  lowcodeSaveDraft,
} from '@/api/lowcode-crud'
import LowcodeCodePreviewModal from '@/components/lowcode-builder/code/LowcodeCodePreviewModal.vue'
import {
  cloneSchema,
  createDefaultModelSchema,
  ensureSystemFields,
  normalizeLowcodePolicies,
  normalizeObjectCode,
  normalizeTableName,
} from '@/components/lowcode-builder/model/model-schema'
import LowcodePageBuilder from '@/components/lowcode-builder/page/LowcodePageBuilder.vue'
import {
  buildPageDesignModelSchema,
  createDefaultPageSchema,
  createPageModelRef,
  isHiddenPageField,
  syncPageSchemaWithModel,
} from '@/components/lowcode-builder/page/page-schema'
import LowcodePreviewPane from '@/components/lowcode-builder/preview/LowcodePreviewPane.vue'
import PublishPanel from '@/components/lowcode-builder/publish/PublishPanel.vue'
import MenuParentSelect from '@/components/lowcode-builder/shared/MenuParentSelect.vue'

defineOptions({ name: 'AiLowcodeBuilder' })

const route = useRoute()
const router = useRouter()

const currentStep = ref('source')
const saving = ref(false)
const pageBootLoading = ref(true)
const domainLoading = ref(false)
const dataModelLoading = ref(false)
const dataModels = ref([])
const selectedDomainId = ref(null)
const selectedDomain = ref(null)
const selectedModelKeys = ref([])
const primaryModelKey = ref(null)
const appCodeTouched = ref(false)
const codePreviewVisible = ref(false)
const codeDownloading = ref(false)
const codegenSaving = ref(false)
const codegenOptionsLoading = ref(false)
const codeSourceType = ref('DRAFT')
const codegenForm = reactive(createBlankCodegenForm())

const appId = computed(() => route.params.id || null)
const pageLoading = computed(() => pageBootLoading.value || domainLoading.value)
const emptyRuntimeModel = createEmptyRuntimeModel()
const draft = reactive({
  id: null,
  domainId: null,
  domainCode: '',
  domainName: '',
  objectCode: '',
  objectName: '',
  businessSuiteCode: '',
  businessObjectCode: '',
  businessObjectName: '',
  configKey: '',
  appName: '',
  menuName: '',
  menuParentId: null,
  menuSort: 0,
  publishStatus: 'DRAFT',
  modelSchema: emptyRuntimeModel,
  pageSchema: createDefaultPageSchema(emptyRuntimeModel),
})
const currentAppId = computed(() => draft.id || appId.value || null)

const stepItems = [
  { value: 'source', title: '数据源', desc: '选择模型 / 指定主模型' },
  { value: 'page', title: '页面设计', desc: '字段绑定 / 布局配置' },
  { value: 'preview', title: '实时预览', desc: '查看运行态效果' },
  { value: 'publish', title: '发布上线', desc: '菜单 / DDL / 版本' },
  { value: 'code', title: '代码输出', desc: '预览 / 下载代码包' },
]
const layoutOptions = [
  { label: '标准列表', value: 'simple-crud' },
  { label: '左树右表', value: 'tree-crud' },
  { label: '主子表', value: 'master-detail-crud' },
]
const codeSourceOptions = [
  { label: '已保存草稿', value: 'DRAFT' },
  { label: '已发布版本', value: 'PUBLISHED' },
]
const selectedDataModels = computed(() => selectedModelKeys.value
  .map(key => dataModels.value.find(model => resolveModelKey(model) === key))
  .filter(Boolean))
const allowMultipleModels = computed(() => ['tree-crud', 'master-detail-crud'].includes(draft.pageSchema?.layoutType))
const selectedModelValue = computed(() => allowMultipleModels.value ? selectedModelKeys.value : selectedModelKeys.value[0] || null)
const primaryModel = computed(() => selectedDataModels.value.find(model => resolveModelKey(model) === primaryModelKey.value) || selectedDataModels.value[0] || null)
const pageDesignModelSchema = computed(() => buildPageDesignModelSchema(draft.modelSchema, draft.pageSchema?.modelRefs || []))
const pageDesignFields = computed(() => pageDesignModelSchema.value.fields || [])
const canPreview = computed(() => Boolean(selectedDomain.value && selectedDataModels.value.length && draft.configKey))
const modelOptions = computed(() => dataModels.value.map(model => ({
  label: `${model.modelName}（${model.modelCode}）`,
  value: resolveModelKey(model),
  disabled: model.status === 'DISABLED',
})))
const boundFieldCount = computed(() => {
  const refs = new Set()
  ;(draft.pageSchema?.zones || []).forEach((zone) => {
    ;(zone.fieldRefs || []).forEach(ref => refs.add(ref))
  })
  return refs.size
})
const headerSubtitle = computed(() => {
  if (!selectedDomain.value)
    return '先选择业务领域，再创建应用页面'
  const models = selectedDataModels.value.map(model => model.modelName).join('、')
  return models ? `${selectedDomain.value.domainCode} / ${models}` : `${selectedDomain.value.domainCode} / 未选择数据模型`
})
const fieldPools = computed(() => selectedDataModels.value.map(model => ({
  key: resolveModelKey(model),
  name: model.modelName,
  code: model.modelCode,
  fields: (model.modelSchema?.fields || []).filter(field => !isHiddenPageField(field)),
})))
const builderCodegenOptions = computed(() => compactCodegenOptions(codegenForm))
const builderCodegenRequest = computed(() => ({
  sourceType: codeSourceType.value,
  ...builderCodegenOptions.value,
}))

onMounted(async () => {
  try {
    if (appId.value) {
      await reloadDetail()
      applyBusinessContext()
      return
    }
    if (route.query.domainId)
      await selectDomainById(route.query.domainId, { resetDraft: true })
    else if (route.query.domainCode || route.query.suiteCode)
      await selectDomainByCode(route.query.domainCode || route.query.suiteCode, { resetDraft: true })
    applyBusinessContext()
  }
  finally {
    pageBootLoading.value = false
  }
})

watch(
  () => route.params.id,
  async (value, oldValue) => {
    if (value === oldValue)
      return
    if (value) {
      pageBootLoading.value = true
      try {
        await reloadDetail()
      }
      finally {
        pageBootLoading.value = false
      }
    }
    else if (route.query.domainId) {
      pageBootLoading.value = true
      try {
        await selectDomainById(route.query.domainId, { resetDraft: true })
      }
      finally {
        pageBootLoading.value = false
      }
    }
  },
)

watch(
  () => draft.pageSchema?.layoutType,
  () => enforceModelSelectionLimit(),
)

async function selectDomainById(domainId, options = {}) {
  if (!domainId)
    return
  domainLoading.value = true
  try {
    const res = await lowcodeDomainDefaults(domainId)
    const domain = res.data
    selectedDomain.value = domain
    selectedDomainId.value = domain?.id || null
    applyDomainToDraft(domain)
    await loadDataModels()
    if (options.resetDraft)
      resetDraftForDomain(domain)
  }
  finally {
    domainLoading.value = false
  }
}

async function selectDomainByCode(domainCode, options = {}) {
  const code = firstQueryValue(domainCode)
  if (!code)
    return
  domainLoading.value = true
  try {
    const res = await lowcodeDomainTree({ keyword: code })
    const domain = flattenDomains(res.data || []).find(item => item.domainCode === code)
    if (domain)
      await selectDomainById(domain.id, options)
  }
  finally {
    domainLoading.value = false
  }
}

async function loadDataModels() {
  if (!selectedDomainId.value) {
    dataModels.value = []
    return
  }
  dataModelLoading.value = true
  try {
    const res = await lowcodeModelList({
      domainId: selectedDomainId.value,
      status: 'ENABLED',
    })
    dataModels.value = res.data || []
  }
  finally {
    dataModelLoading.value = false
  }
}

async function reloadDetail() {
  if (!appId.value)
    return
  const res = await lowcodeAppDetail(appId.value)
  const detail = res.data || {}
  draft.id = detail.id
  draft.domainId = detail.domainId
  draft.domainCode = detail.domainCode || ''
  draft.domainName = detail.domainName || ''
  draft.objectCode = detail.objectCode || detail.modelSchema?.object?.code || ''
  draft.objectName = detail.objectName || detail.modelSchema?.object?.name || detail.tableComment || ''
  draft.configKey = detail.configKey || ''
  draft.appName = detail.appName || detail.tableComment || ''
  draft.menuName = detail.menuName || draft.appName
  draft.menuParentId = detail.menuParentId || null
  draft.menuSort = detail.menuSort || 0
  draft.publishStatus = detail.publishStatus || 'DRAFT'
  draft.modelSchema = cloneSchema(detail.modelSchema || createEmptyRuntimeModel())
  draft.pageSchema = cloneSchema(detail.pageSchema || createDefaultPageSchema(draft.modelSchema))
  if (!draft.pageSchema.layoutType)
    draft.pageSchema.layoutType = detail.layoutType || 'simple-crud'
  appCodeTouched.value = true
  if (draft.domainId) {
    await selectDomainById(draft.domainId, { resetDraft: false })
    restoreSelectedModels(detail)
    syncSelectedModelsToDraft()
    await loadCodegenOptions()
  }
}

function resetDraftForDomain(domain) {
  draft.id = null
  draft.configKey = ''
  draft.appName = ''
  draft.menuName = ''
  draft.menuParentId = null
  draft.menuSort = 0
  draft.publishStatus = 'DRAFT'
  draft.objectCode = ''
  draft.objectName = ''
  draft.businessSuiteCode = ''
  draft.businessObjectCode = ''
  draft.businessObjectName = ''
  draft.modelSchema = createEmptyRuntimeModel(domain)
  draft.pageSchema = createDefaultPageSchema(draft.modelSchema)
  draft.pageSchema.layoutType = resolveDomainDefaults(domain).layoutType
  draft.pageSchema.modelRefs = []
  selectedModelKeys.value = []
  primaryModelKey.value = null
  appCodeTouched.value = false
  currentStep.value = 'source'
  applyDomainToDraft(domain)
}

function applyDomainToDraft(domain) {
  if (!domain)
    return
  const defaults = resolveDomainDefaults(domain)
  draft.domainId = domain.id
  draft.domainCode = domain.domainCode
  draft.domainName = domain.domainName
  if (!draft.menuParentId)
    draft.menuParentId = defaults.menuParentId || null
  if (!draft.pageSchema.layoutType)
    draft.pageSchema.layoutType = defaults.layoutType
  applyCodegenDefaults(domain)
}

function restoreSelectedModels(detail) {
  const refs = draft.pageSchema?.modelRefs || []
  if (refs.length) {
    selectedModelKeys.value = refs.map(ref => ensureDataModelFromRef(ref)).filter(Boolean)
    const primary = refs.find(ref => ref.primary) || refs[0]
    primaryModelKey.value = primary ? ensureDataModelFromRef(primary) : selectedModelKeys.value[0] || null
    return
  }
  const modelCode = detail.objectCode || detail.modelSchema?.object?.code
  let model = dataModels.value.find(item => item.modelCode === modelCode)
  if (!model && detail.modelSchema) {
    model = createSyntheticModelFromSchema(detail.modelSchema, {
      modelCode,
      modelName: detail.objectName || detail.modelSchema?.object?.name || detail.appName,
    })
    dataModels.value = [...dataModels.value, model]
  }
  if (model) {
    const key = resolveModelKey(model)
    selectedModelKeys.value = [key]
    primaryModelKey.value = key
  }
}

function applyBusinessContext() {
  if (!selectedDomain.value)
    return
  const requestedStep = normalizeStep(route.query.step)
  const objectCode = normalizeObjectCode(firstQueryValue(route.query.objectCode))
  const objectName = firstQueryValue(route.query.objectName)
  draft.businessSuiteCode = firstQueryValue(route.query.suiteCode || route.query.domainCode)
  draft.businessObjectCode = firstQueryValue(route.query.objectCode)
  draft.businessObjectName = objectName
  if (objectCode) {
    const modelCode = resolveContextModelCode(objectCode)
    const model = dataModels.value.find(item =>
      item.modelCode === modelCode
      || item.modelCode === objectCode
      || item.modelCode?.endsWith(`_${objectCode}`),
    )
    if (model) {
      const key = resolveModelKey(model)
      selectedModelKeys.value = [key]
      primaryModelKey.value = key
      syncSelectedModelsToDraft()
    }
    else {
      draft.objectCode = modelCode
      draft.objectName = objectName || ''
      if (objectName) {
        draft.appName = `${objectName}管理`
        draft.menuName = draft.appName
      }
      deriveAppCode()
      window.$message?.info('未找到关联模型，请先选择或创建数据模型')
    }
  }
  if (requestedStep && (requestedStep === 'source' || selectedModelKeys.value.length))
    currentStep.value = requestedStep
}

function ensureDataModelFromRef(ref) {
  const found = dataModels.value.find((model) => {
    if (ref.modelId && model.id && `${model.id}` === `${ref.modelId}`)
      return true
    return ref.modelCode && model.modelCode === ref.modelCode
  })
  if (found)
    return resolveModelKey(found)
  const synthetic = createSyntheticModelFromRef(ref)
  dataModels.value = [...dataModels.value, synthetic]
  return resolveModelKey(synthetic)
}

function handleModelSelectionChange(keys) {
  const nextKeys = Array.isArray(keys) ? keys : keys ? [keys] : []
  selectedModelKeys.value = allowMultipleModels.value ? nextKeys : nextKeys.slice(-1)
  if (!selectedModelKeys.value.includes(primaryModelKey.value))
    primaryModelKey.value = selectedModelKeys.value[0] || null
  syncSelectedModelsToDraft()
}

function handleLayoutTypeChange(value) {
  draft.pageSchema.layoutType = value
  enforceModelSelectionLimit()
  syncSelectedModelsToDraft()
}

function enforceModelSelectionLimit() {
  if (allowMultipleModels.value || selectedModelKeys.value.length <= 1)
    return
  const retainedKey = selectedModelKeys.value.includes(primaryModelKey.value)
    ? primaryModelKey.value
    : selectedModelKeys.value[0]
  selectedModelKeys.value = retainedKey ? [retainedKey] : []
  primaryModelKey.value = retainedKey || null
  window.$message?.info('标准列表只支持一个数据模型，已保留主模型')
}

function setPrimaryModel(key) {
  primaryModelKey.value = key
  syncSelectedModelsToDraft()
}

function syncSelectedModelsToDraft() {
  const models = selectedDataModels.value
  if (!models.length) {
    draft.pageSchema = {
      ...draft.pageSchema,
      modelRefs: [],
    }
    return
  }
  if (!primaryModelKey.value || !models.some(model => resolveModelKey(model) === primaryModelKey.value))
    primaryModelKey.value = resolveModelKey(models[0])

  const refs = models.map(model => createPageModelRef(model, {
    primary: resolveModelKey(model) === primaryModelKey.value,
  }))
  const primary = primaryModel.value || models[0]
  const runtimeModel = cloneSchema(primary.modelSchema || createEmptyRuntimeModel(selectedDomain.value))
  normalizeRuntimeModel(runtimeModel, primary)
  draft.modelSchema = runtimeModel
  draft.objectCode = primary.modelCode || runtimeModel.object?.code || ''
  draft.objectName = primary.modelName || runtimeModel.object?.name || runtimeModel.businessName || ''
  if (!draft.appName)
    draft.appName = draft.objectName ? `${draft.objectName}管理` : ''
  if (!draft.menuName)
    draft.menuName = draft.appName
  deriveAppCode()

  const nextPageSchema = {
    ...draft.pageSchema,
    modelRefs: refs,
    primaryModelId: refs.find(ref => ref.primary)?.modelId || null,
    primaryModelCode: refs.find(ref => ref.primary)?.modelCode || '',
  }
  const designSchema = buildPageDesignModelSchema(draft.modelSchema, refs)
  draft.pageSchema = syncPageSchemaWithModel(nextPageSchema, designSchema)
}

function updateAppName(value) {
  const previousName = draft.appName
  draft.appName = value
  if (!draft.menuName || draft.menuName === previousName)
    draft.menuName = value
}

function updateConfigKey(value) {
  appCodeTouched.value = true
  draft.configKey = normalizeObjectCode(value)
}

function deriveAppCode(force = false) {
  if (!selectedDomain.value || !primaryModel.value)
    return
  if (!force && appCodeTouched.value && draft.configKey)
    return
  const defaults = resolveDomainDefaults(selectedDomain.value)
  const base = normalizeObjectCode(primaryModel.value.modelCode || draft.objectCode || 'app')
  draft.configKey = normalizeObjectCode(`${defaults.configKeyPrefix || ''}${base}`)
}

async function saveDraft(options = {}) {
  if (!selectedDomain.value || !draft.domainId) {
    window.$message?.warning('请先选择业务领域')
    return null
  }
  if (!draft.id && selectedDomain.value.status !== 'ENABLED') {
    window.$message?.warning('停用领域不能新建应用')
    return null
  }
  if (!selectedDataModels.value.length) {
    window.$message?.warning('请至少选择一个数据模型')
    return null
  }
  if (!primaryModel.value) {
    window.$message?.warning('请指定主数据模型')
    return null
  }
  if (!draft.appName) {
    window.$message?.warning('请填写应用名称')
    return null
  }
  deriveAppCode()
  if (!draft.configKey) {
    window.$message?.warning('请填写应用编码')
    return null
  }
  saving.value = true
  try {
    syncSelectedModelsToDraft()
    const res = await lowcodeSaveDraft({
      id: draft.id,
      domainId: draft.domainId,
      domainCode: draft.domainCode,
      domainName: draft.domainName,
      objectCode: draft.objectCode,
      objectName: draft.objectName,
      configKey: draft.configKey,
      appName: draft.appName,
      menuName: draft.menuName || draft.appName,
      menuParentId: draft.menuParentId,
      menuSort: draft.menuSort,
      modelSchema: cloneSchema(draft.modelSchema),
      pageSchema: cloneSchema(draft.pageSchema),
    })
    const id = res.data
    if (!options.silent)
      window.$message?.success('草稿已保存')
    if (!appId.value && id) {
      draft.id = id
      await router.replace(`/ai/lowcode-builder/${id}`)
    }
    else {
      await reloadDetail()
    }
    return id || draft.id
  }
  catch (e) {
    window.$message?.error(e?.message || '保存草稿失败')
    return null
  }
  finally {
    saving.value = false
  }
}

async function openPublishStep() {
  const id = draft.id || await saveDraft({ silent: true })
  if (!id)
    return
  currentStep.value = 'publish'
}

function openCodePreview() {
  if (!currentAppId.value) {
    window.$message?.warning('请先保存应用草稿')
    return
  }
  codePreviewVisible.value = true
}

async function downloadBuilderCode() {
  if (!currentAppId.value) {
    window.$message?.warning('请先保存应用草稿')
    return
  }
  codeDownloading.value = true
  try {
    const blob = await lowcodeDownloadAppCode(currentAppId.value, builderCodegenRequest.value)
    downloadBlob(blob, `${draft.configKey || 'lowcode-app'}-code.zip`)
    window.$message?.success('代码包下载成功')
  }
  catch (e) {
    window.$message?.error(e?.message || '下载代码失败')
  }
  finally {
    codeDownloading.value = false
  }
}

async function saveCodegenOptions() {
  if (!currentAppId.value) {
    window.$message?.warning('请先保存应用草稿')
    return
  }
  codegenSaving.value = true
  try {
    await lowcodeSaveAppCodeOptions(currentAppId.value, builderCodegenOptions.value)
    window.$message?.success('代码生成设置已保存')
    await loadCodegenOptions()
  }
  catch (e) {
    window.$message?.error(e?.message || '保存代码生成设置失败')
  }
  finally {
    codegenSaving.value = false
  }
}

function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}

function normalizeRuntimeModel(modelSchema, model) {
  modelSchema.schemaVersion = 2
  modelSchema.domain = {
    id: draft.domainId,
    code: draft.domainCode,
    name: draft.domainName,
  }
  modelSchema.object = {
    ...(modelSchema.object || {}),
    code: model.modelCode || modelSchema.object?.code || '',
    name: model.modelName || modelSchema.object?.name || '',
  }
  modelSchema.businessName = model.modelName || modelSchema.businessName || ''
  modelSchema.fields = ensureSystemFields(modelSchema.fields || [], true)
  if (!isValidTableName(modelSchema.tableName))
    modelSchema.tableName = buildRuntimeTableName(modelSchema, model)
  if (!modelSchema.relations)
    modelSchema.relations = []
  normalizeLowcodePolicies(modelSchema)
  if (!modelSchema.children)
    modelSchema.children = []
}

function buildRuntimeTableName(modelSchema, model) {
  const defaults = resolveDomainDefaults(selectedDomain.value)
  const base = normalizeObjectCode(
    model.modelCode
    || modelSchema.object?.code
    || draft.objectCode
    || draft.configKey
    || 'runtime_model',
    'runtime_model',
  )
  return normalizeTableName(`${defaults.tablePrefix || 'biz_'}${base}`)
}

function isValidTableName(value) {
  return /^[a-z][a-z0-9_]{1,63}$/.test(String(value || ''))
}

function createEmptyRuntimeModel(domain = null) {
  const model = createDefaultModelSchema({
    domain: domain
      ? {
          id: domain.id,
          code: domain.domainCode,
          name: domain.domainName,
        }
      : undefined,
    objectCode: '',
    objectName: '',
    tableName: '',
  })
  model.object.code = ''
  model.object.name = ''
  model.businessName = ''
  model.tableName = ''
  model.fields = []
  return model
}

function createSyntheticModelFromRef(ref = {}) {
  return createSyntheticModelFromSchema({
    schemaVersion: 2,
    domain: {
      id: draft.domainId,
      code: draft.domainCode,
      name: draft.domainName,
    },
    object: {
      code: ref.modelCode || '',
      name: ref.modelName || ref.modelCode || '',
      description: '',
    },
    appType: 'SINGLE',
    tableMode: 'CREATE',
    tableName: '',
    businessName: ref.modelName || ref.modelCode || '',
    fields: (ref.fields || []).map(field => ({
      ...field,
      field: field.sourceField || field.field,
      label: field.rawLabel || field.label || field.sourceField || field.field,
    })),
    relations: [],
    policies: {},
    children: [],
  }, {
    modelCode: ref.modelCode,
    modelName: ref.modelName,
  })
}

function createSyntheticModelFromSchema(schema = {}, options = {}) {
  const modelCode = options.modelCode || schema.object?.code || 'runtime_model'
  const modelName = options.modelName || schema.object?.name || schema.businessName || modelCode
  const modelSchema = cloneSchema(schema)
  normalizeLowcodePolicies(modelSchema)
  return {
    id: `schema:${modelCode}`,
    domainId: draft.domainId,
    domainCode: draft.domainCode,
    domainName: draft.domainName,
    modelCode,
    modelName,
    status: 'ENABLED',
    tenantEnabled: true,
    masterData: false,
    modelSchema,
  }
}

function resolveModelKey(model) {
  return model?.id !== null && model?.id !== undefined ? `${model.id}` : `schema:${model?.modelCode || 'model'}`
}

function resolveContextModelCode(objectCode) {
  const prefix = selectedDomain.value?.configKeyPrefix || selectedDomain.value?.tablePrefix || ''
  if (!prefix || objectCode.startsWith(prefix))
    return objectCode
  return normalizeObjectCode(`${prefix}${objectCode}`)
}

function flattenDomains(nodes) {
  const result = []
  for (const node of nodes || []) {
    result.push(node)
    if (node.children?.length)
      result.push(...flattenDomains(node.children))
  }
  return result
}

function firstQueryValue(value) {
  return Array.isArray(value) ? value[0] : String(value || '').trim()
}

function normalizeStep(value) {
  const step = firstQueryValue(value)
  return stepItems.some(item => item.value === step) ? step : ''
}

function resolveDomainDefaults(domain) {
  const schema = domain?.domainSchema || {}
  return {
    tablePrefix: domain?.tablePrefix || schema.naming?.tablePrefix || (domain?.domainCode ? `biz_${domain.domainCode}_` : 'biz_'),
    configKeyPrefix: domain?.configKeyPrefix || schema.naming?.configKeyPrefix || (domain?.domainCode ? `${domain.domainCode}_` : ''),
    layoutType: 'simple-crud',
    menuParentId: domain?.menuParentId || schema.defaults?.menuParentId || null,
  }
}

function createBlankCodegenForm() {
  return {
    groupId: '',
  }
}

async function loadCodegenOptions() {
  applyCodegenDefaults(selectedDomain.value)
  if (!currentAppId.value)
    return
  codegenOptionsLoading.value = true
  try {
    const res = await lowcodeAppCodeOptions(currentAppId.value)
    applyCodegenOptions(res.data || {})
  }
  catch (e) {
    console.warn('加载代码生成配置失败:', e)
  }
  finally {
    codegenOptionsLoading.value = false
  }
}

function applyCodegenDefaults(domain) {
  Object.assign(codegenForm, createBlankCodegenForm(), resolveDomainCodegenDefaults(domain))
}

function applyCodegenOptions(options = {}) {
  Object.assign(codegenForm, {
    ...resolveDomainCodegenDefaults(selectedDomain.value),
    groupId: options.groupId || resolveDomainCodegenDefaults(selectedDomain.value).groupId,
  })
}

function resolveDomainCodegenDefaults(domain) {
  const codegen = domain?.domainSchema?.codegen || {}
  return {
    groupId: codegen.groupId || codegen.domainPackage || '',
  }
}

function compactCodegenOptions(options = {}) {
  const result = {}
  Object.entries(options).forEach(([key, value]) => {
    if (value !== undefined && value !== null && String(value).trim() !== '')
      result[key] = String(value).trim()
  })
  return result
}
</script>

<style scoped>
.app-builder-page {
  min-height: 100%;
  background: #f3f6fa;
}

.app-header {
  display: flex;
  min-height: 72px;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 18px;
  border-bottom: 1px solid #d8dee8;
  background: #fff;
}

.header-left {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 12px;
}

.title-block {
  min-width: 0;
}

.breadcrumb {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  color: #64748b;
  font-size: 12px;
}

.breadcrumb span:not(:last-child)::after {
  content: '/';
  margin-left: 7px;
  color: #cbd5e1;
}

.title-block h1 {
  overflow: hidden;
  margin: 3px 0 0;
  color: #0f172a;
  font-size: 20px;
  line-height: 1.25;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.title-block p {
  margin: 3px 0 0;
  color: #64748b;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
}

.app-shell {
  display: block;
  padding: 14px;
}

.builder-spin {
  display: block;
  min-height: calc(100vh - 73px);
}

:deep(.builder-spin > .n-spin-content) {
  min-height: calc(100vh - 73px);
}

.domain-context,
.app-config,
.model-source-panel,
.stage-panel,
.empty-canvas {
  border: 1px solid #d8dee8;
  border-radius: 8px;
  background: #fff;
}

.source-head,
.field-pool-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.source-head p,
.field-pool-head p {
  margin: 2px 0 0;
  color: #64748b;
  font-size: 12px;
}

.app-main {
  display: grid;
  min-width: 0;
  gap: 12px;
}

.empty-canvas {
  display: grid;
  min-height: calc(100vh - 128px);
  place-items: center;
  align-content: center;
  gap: 12px;
  padding: 40px;
  text-align: center;
}

.empty-illustration {
  display: inline-flex;
  width: 62px;
  height: 62px;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: #eff6ff;
  color: #2563eb;
  font-size: 28px;
}

.empty-canvas h2 {
  margin: 0;
  color: #0f172a;
  font-size: 22px;
}

.empty-canvas p {
  max-width: 560px;
  margin: 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.7;
}

.status-alert {
  border-radius: 8px;
}

.domain-context {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 420px;
  gap: 14px;
  padding: 14px;
}

.context-code {
  color: #2563eb;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  font-weight: 700;
}

.context-main h2 {
  margin: 4px 0 0;
  color: #0f172a;
  font-size: 20px;
}

.context-main p {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}

.metric-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.metric-strip div {
  display: grid;
  align-content: center;
  gap: 4px;
  min-width: 0;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #f8fafc;
  padding: 10px;
}

.metric-strip span {
  color: #64748b;
  font-size: 11px;
}

.metric-strip strong {
  overflow: hidden;
  color: #0f172a;
  font-size: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-config,
.model-source-panel,
.stage-panel {
  display: grid;
  gap: 12px;
  padding: 12px;
}

.step-strip {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px;
}

.step-node {
  display: grid;
  gap: 4px;
  min-height: 58px;
  border: 1px solid #d8dee8;
  border-radius: 8px;
  background: #f8fafc;
  color: inherit;
  cursor: pointer;
  padding: 9px 10px;
  text-align: left;
}

.step-node.active {
  border-color: #2563eb;
  background: #eff6ff;
}

.step-node strong {
  color: #0f172a;
  font-size: 13px;
}

.step-node span {
  overflow: hidden;
  color: #64748b;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-form {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 10px 12px;
}

.app-form :deep(.n-form-item) {
  margin-bottom: 0;
}

.section-title {
  color: #0f172a;
  font-size: 14px;
  font-weight: 700;
}

.source-select-row {
  max-width: 860px;
}

.selected-model-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.selected-model-card {
  display: grid;
  gap: 8px;
  min-height: 86px;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  background: #fff;
  color: inherit;
  cursor: pointer;
  padding: 12px;
  text-align: left;
}

.selected-model-card.primary {
  border-color: #22c55e;
  background: #f0fdf4;
}

.model-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.model-card-head strong,
.model-card-head code {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.model-card-head strong {
  color: #0f172a;
  font-size: 14px;
}

.model-card-head code,
.model-card-meta {
  color: #64748b;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
}

.model-empty {
  padding: 8px 0;
}

.source-stage {
  display: grid;
  gap: 12px;
}

.field-pool-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.field-pool-card {
  display: grid;
  gap: 10px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #f8fafc;
  padding: 12px;
}

.pool-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.pool-title strong {
  color: #0f172a;
  font-size: 13px;
}

.pool-title code {
  color: #64748b;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
}

.field-chip-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.field-chip {
  max-width: 180px;
  min-height: 26px;
  overflow: hidden;
  border: 1px solid #dbe3ee;
  border-radius: 6px;
  background: #fff;
  color: #334155;
  font-size: 12px;
  line-height: 24px;
  padding: 0 8px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.field-chip.muted {
  color: #94a3b8;
}

.code-output-stage {
  display: grid;
  gap: 12px;
}

.code-output-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.code-output-head p {
  margin: 2px 0 0;
  color: #64748b;
  font-size: 12px;
}

.code-output-form {
  max-width: 860px;
}

.code-settings-grid {
  display: grid;
  align-items: end;
  grid-template-columns: minmax(180px, 220px) minmax(260px, 1fr) auto;
  gap: 8px;
}

.code-settings-grid :deep(.n-form-item) {
  margin-bottom: 0;
}

.code-output-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-start;
}

@media (max-width: 1320px) {
  .domain-context {
    grid-template-columns: 1fr;
  }

  .selected-model-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 1080px) {
  .app-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .app-form,
  .step-strip,
  .metric-strip,
  .field-pool-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .app-shell {
    padding: 10px;
  }

  .app-form,
  .step-strip,
  .metric-strip,
  .selected-model-grid,
  .field-pool-grid,
  .code-settings-grid {
    grid-template-columns: 1fr;
  }

  .code-output-actions {
    flex-wrap: wrap;
  }
}
</style>
