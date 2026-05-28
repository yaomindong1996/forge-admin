<template>
  <div class="model-workbench-page">
    <aside class="model-nav">
      <section class="nav-block tree-block">
        <div class="nav-head">
          <div>
            <h2>业务域与数据模型</h2>
            <span>{{ flatDomains.length }} 个业务域 · {{ currentTreeModelCount }} 个模型</span>
          </div>
          <n-button size="small" type="primary" @click="openDomainEditor">
            <template #icon>
              <n-icon><AddOutline /></n-icon>
            </template>
            新建
          </n-button>
        </div>
        <n-input
          v-model:value="domainKeyword"
          clearable
          size="small"
          placeholder="搜索业务域"
          @keyup.enter="loadDomains"
          @clear="loadDomains"
        >
          <template #prefix>
            <n-icon><SearchOutline /></n-icon>
          </template>
        </n-input>
        <n-spin :show="domainLoading" class="tree-spin">
          <div class="asset-tree">
            <template v-for="node in visibleAssetNodes" :key="node.key">
              <button
                v-if="node.type === 'domain'"
                type="button"
                class="tree-domain"
                :class="{ active: selectedDomainId === node.domain.id && !isEditingModel, disabled: node.domain.status === 'DISABLED' }"
                :style="{ '--indent': node.level }"
                @click="selectDomain(node.domain)"
              >
                <span class="node-indent" />
                <span
                  class="node-toggle"
                  :class="{ empty: !isAssetExpandable(node.domain) }"
                  @click.stop="toggleExpanded(node.domain)"
                >
                  <n-icon v-if="isAssetExpandable(node.domain)">
                    <ChevronDownOutline v-if="isExpanded(node.domain.id)" />
                    <ChevronForwardOutline v-else />
                  </n-icon>
                </span>
                <span class="node-kind domain">域</span>
                <span class="tree-glyph">
                  <n-icon><FolderOpenOutline /></n-icon>
                </span>
                <span class="node-main">
                  <strong>{{ node.domain.domainName }}</strong>
                  <code>{{ node.domain.domainCode }}</code>
                </span>
                <n-tag
                  v-if="node.domain.status === 'DISABLED'"
                  size="small"
                  type="warning"
                  :bordered="false"
                >
                  停用
                </n-tag>
              </button>

              <button
                v-else-if="node.type === 'model'"
                type="button"
                class="tree-model"
                :class="{ active: currentModel.id === node.model.id }"
                :style="{ '--indent': node.level }"
                @click="openTreeModel(node.model, node.domain)"
              >
                <span class="node-indent" />
                <span class="node-spacer" />
                <span class="node-kind model">模型</span>
                <span class="tree-glyph">
                  <n-icon><DocumentTextOutline /></n-icon>
                </span>
                <span class="node-main">
                  <strong>{{ node.model.modelName }}</strong>
                  <code>{{ node.model.modelCode }}</code>
                </span>
                <n-tag size="small" :type="node.model.status === 'ENABLED' ? 'success' : 'warning'" :bordered="false">
                  {{ dict.lowcode_model_status?.find(d => d.value === node.model.status)?.label || '启用' }}
                </n-tag>
              </button>

              <div
                v-else-if="node.type === 'loading'"
                class="tree-state"
                :style="{ '--indent': node.level }"
              >
                <span class="node-indent" />
                <span>加载模型中...</span>
              </div>

              <div
                v-else-if="node.type === 'empty'"
                class="tree-state muted"
                :style="{ '--indent': node.level }"
              >
                <span class="node-indent" />
                <span>暂无数据模型</span>
              </div>
            </template>
          </div>
        </n-spin>
      </section>
    </aside>

    <main class="model-main">
      <header class="page-header">
        <div>
          <div class="header-kicker">
            数据模型设计
          </div>
          <h1>{{ currentModel.modelName || '新建数据模型' }}</h1>
          <p>{{ selectedDomain ? `${selectedDomain.domainName} / ${currentModel.modelCode || '未设置编码'}` : '先选择业务领域，再维护领域下的数据模型。' }}</p>
        </div>
        <div class="model-header-actions">
          <template v-if="selectedDomain && !isEditingModel">
            <n-button :disabled="!canCreateModel" type="primary" @click="createModel">
              新建模型
            </n-button>
            <n-button :disabled="!canCreateModel" @click="openDbTableImport">
              导入数据表
            </n-button>
            <n-button :disabled="!canCreateModel" @click="openAiModelGenerate">
              AI 生成模型
            </n-button>
            <n-button @click="loadModels">
              刷新
            </n-button>
          </template>
          <template v-else-if="selectedDomain && isEditingModel">
            <div class="header-action-group">
              <n-button @click="triggerModelImport">
                导入 JSON
              </n-button>
              <n-button :disabled="!currentModel.modelName" @click="exportCurrentModel">
                导出配置
              </n-button>
              <n-button :loading="ddlLoading" @click="previewDdl">
                DDL 预览
              </n-button>
            </div>
            <div class="header-action-group primary">
              <n-button :loading="saving" @click="saveModel(false)">
                保存配置
              </n-button>
              <n-button :loading="syncSaving" type="primary" @click="saveModel(true)">
                保存并同步表结构
              </n-button>
            </div>
          </template>
        </div>
      </header>

      <section v-if="!selectedDomain" class="empty-panel">
        <div class="empty-icon">
          <n-icon><FolderOpenOutline /></n-icon>
        </div>
        <h2>先选择业务领域</h2>
        <p>数据模型是领域资产，不等同于应用。一个业务领域下可以沉淀多个模型，后续创建应用时再选择一个或多个模型来设计页面。</p>
      </section>

      <section v-else-if="selectedDomain && !isEditingModel" class="domain-overview">
        <div class="overview-hero">
          <div>
            <div class="hero-code">
              {{ selectedDomain.domainCode }}
            </div>
            <h1>{{ selectedDomain.domainName }}</h1>
            <p>{{ selectedDomain.domainDesc || `${selectedDomain.domainName} 业务领域，管理该领域下的所有数据模型。` }}</p>
          </div>
          <div class="hero-actions">
            <div class="hero-metrics">
              <div class="hero-metric">
                <strong>{{ models.length }}</strong>
                <span>数据模型</span>
              </div>
              <div class="hero-metric">
                <strong>{{ enabledModels.length }}</strong>
                <span>启用模型</span>
              </div>
            </div>
            <div class="hero-action-buttons">
              <n-button type="primary" ghost @click="editSelectedDomain">
                编辑领域
              </n-button>
              <n-button type="primary" :disabled="!canCreateModel" @click="openDbTableImport">
                导入数据表
              </n-button>
            </div>
          </div>
        </div>

        <div class="domain-model-list">
          <div class="model-list-head">
            <div>
              <div class="section-title">
                领域数据模型
              </div>
              <p>当前领域下已维护的数据模型清单</p>
            </div>
            <n-button :disabled="!canCreateModel" type="primary" secondary @click="createModel">
              新建模型
            </n-button>
          </div>
          <div v-if="models.length" class="model-card-grid">
            <button
              v-for="model in models"
              :key="model.id"
              type="button"
              class="model-card"
              @click="openModel(model)"
            >
              <div class="model-card-head">
                <strong>{{ model.modelName }}</strong>
                <n-tag size="small" :type="model.status === 'ENABLED' ? 'success' : 'warning'" :bordered="false">
                  {{ dict.lowcode_model_status?.find(d => d.value === model.status)?.label || model.status }}
                </n-tag>
              </div>
              <div class="model-card-body">
                <code>{{ model.modelCode }}</code>
                <span>{{ (model.modelSchema?.fields || []).length }} 个字段</span>
                <span v-if="model.masterData">· 主数据</span>
                <span v-if="model.tenantEnabled">· 多租户</span>
              </div>
              <div v-if="model.modelDesc" class="model-card-desc">
                {{ model.modelDesc }}
              </div>
            </button>
          </div>
          <n-empty v-else size="small" description="该领域暂无数据模型" class="model-empty">
            <template #extra>
              <n-button type="primary" :disabled="!canCreateModel" @click="createModel">
                新建模型
              </n-button>
            </template>
          </n-empty>
        </div>
      </section>

      <section v-else class="model-editor">
        <section class="basic-panel">
          <div class="section-title">
            模型基本信息
          </div>
          <n-form label-placement="top" size="small" class="basic-form" :show-feedback="false">
            <n-form-item label="模型名称">
              <n-input
                v-model:value="currentModel.modelName"
                placeholder="例如：客户档案"
                @update:value="syncModelIdentity"
              />
            </n-form-item>
            <n-form-item label="模型编码 / 数据表名">
              <n-input
                :value="currentModel.modelCode"
                placeholder="tf_f_order"
                @update:value="updateModelCode"
              />
            </n-form-item>
            <n-form-item label="所属业务域">
              <n-select
                :value="currentModel.domainId"
                :options="domainOptions"
                disabled
              />
            </n-form-item>
            <n-form-item label="启用状态">
              <n-select v-model:value="currentModel.status" :options="statusOptions" />
            </n-form-item>
            <n-form-item label="多租户">
              <n-switch v-model:value="currentModel.tenantEnabled" />
            </n-form-item>
            <n-form-item label="主数据标识">
              <n-switch v-model:value="currentModel.masterData" />
            </n-form-item>
            <n-form-item class="span-2" label="绑定数据源">
              <n-select
                v-model:value="selectedDatasourceId"
                clearable
                filterable
                :loading="datasourceLoading"
                :options="datasourceOptions"
                placeholder="选择模型所属数据源"
                @update:value="handleDatasourceChange"
              />
            </n-form-item>
            <n-form-item class="span-2" label="描述">
              <n-input
                v-model:value="currentModel.modelDesc"
                type="textarea"
                :autosize="{ minRows: 2, maxRows: 4 }"
                placeholder="描述模型承载的数据范围、口径和业务边界"
              />
            </n-form-item>
            <n-form-item class="span-2" label="数据源表导入">
              <div class="table-sync-control">
                <div class="source-table-summary">
                  {{ currentModel.modelSchema?.sourceTable?.tableName || '直接读取数据源表结构生成模型，不再依赖旧表模型' }}
                </div>
                <n-button :disabled="!canCreateModel" @click="openDbTableImport">
                  选择数据表
                </n-button>
              </div>
            </n-form-item>
          </n-form>
        </section>

        <section v-if="ddlPreview" class="ddl-panel">
          <div class="section-title">
            DDL 预览
          </div>
          <n-alert
            v-for="warning in ddlPreview.warnings"
            :key="warning"
            type="warning"
            :bordered="false"
            class="ddl-warning"
          >
            {{ warning }}
          </n-alert>
          <pre v-for="ddl in ddlPreview.ddlStatements" :key="ddl" class="ddl-code">{{ ddl }}</pre>
          <n-empty v-if="!ddlPreview.ddlStatements?.length" description="当前没有需要执行的 DDL" />
        </section>

        <LowcodeModelDesigner
          v-if="currentModel.modelSchema"
          v-model="currentModel.modelSchema"
          :domain="selectedDomain"
          :data-models="models"
          :show-basic-tab="false"
          class="designer-panel"
        />
      </section>
    </main>
    <DomainEditorDrawer
      v-model:show="domainEditorVisible"
      :domain="editingDomain"
      :domains="domains"
      @saved="handleDomainSaved"
    />
    <LowcodeModelImportModal
      v-model:show="modelImportVisible"
      :domain-id="selectedDomainId"
      @apply="handleDbTableModelApply"
    />
    <n-modal
      v-model:show="aiModelVisible"
      title="AI 生成模型草稿"
      preset="card"
      style="width: min(780px, calc(100vw - 28px))"
      :mask-closable="false"
    >
      <div class="ai-model-modal">
        <n-input
          v-model:value="aiModelDescription"
          type="textarea"
          :autosize="{ minRows: 5, maxRows: 8 }"
          placeholder="描述业务对象、字段、状态、查询条件和表单要求"
        />
        <n-button type="primary" :loading="aiModelLoading" @click="generateAiModelDraft">
          生成草稿
        </n-button>
        <section v-if="aiModelResult" class="ai-result-panel">
          <div class="preview-head">
            <div>
              <strong>{{ aiModelResult.modelDraft?.modelName || aiModelResult.modelSchema?.businessName }}</strong>
              <span>{{ aiModelResult.modelDraft?.modelCode || aiModelResult.modelSchema?.object?.code }}</span>
            </div>
            <n-tag :bordered="false">
              {{ aiModelResult.modelSchema?.fields?.length || 0 }} 个字段
            </n-tag>
          </div>
          <ul class="note-list">
            <li v-for="note in aiModelResult.generationNotes" :key="note">
              {{ note }}
            </li>
          </ul>
        </section>
      </div>
      <template #footer>
        <n-space justify="end">
          <n-button @click="aiModelVisible = false">
            取消
          </n-button>
          <n-button type="primary" :disabled="!aiModelResult" @click="applyAiModelResult">
            应用到模型设计
          </n-button>
        </n-space>
      </template>
    </n-modal>
    <input
      ref="modelImportInputRef"
      class="hidden-file-input"
      type="file"
      accept="application/json,.json"
      @change="handleModelImportFile"
    >
  </div>
</template>

<script setup>
import {
  AddOutline,
  ChevronDownOutline,
  ChevronForwardOutline,
  DocumentTextOutline,
  FolderOpenOutline,
  SearchOutline,
} from '@vicons/ionicons5'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import {
  genDatasourceEnabled,
  lowcodeAiGenerateApp,
  lowcodeCreateModel,
  lowcodeDdlPreview,
  lowcodeDomainDefaults,
  lowcodeDomainTree,
  lowcodeModelDetail,
  lowcodeModelPage,
  lowcodeUpdateModel,
} from '@/api/lowcode-crud'
import DomainEditorDrawer from '@/components/lowcode-builder/domain/DomainEditorDrawer.vue'
import LowcodeModelDesigner from '@/components/lowcode-builder/model/LowcodeModelDesigner.vue'
import LowcodeModelImportModal from '@/components/lowcode-builder/model/LowcodeModelImportModal.vue'
import {
  cloneSchema,
  createDefaultModelSchema,
  ensureSystemFields,
  normalizeLowcodePolicies,
  normalizeObjectCode,
  normalizeTableName,
} from '@/components/lowcode-builder/model/model-schema'
import { useDict } from '@/composables/useDict'

defineOptions({ name: 'AiLowcodeModels' })

const route = useRoute()
const { dict } = useDict('lowcode_model_status')

const domainLoading = ref(false)
const modelLoading = ref(false)
const saving = ref(false)
const syncSaving = ref(false)
const ddlLoading = ref(false)
const domains = ref([])
const models = ref([])
const domainModelsMap = ref({})
const domainModelLoadingIds = ref(new Set())
const selectedDomainId = ref(null)
const selectedDomain = ref(null)
const domainKeyword = ref('')
const domainEditorVisible = ref(false)
const editingDomain = ref(null)
const expandedDomainIds = ref(new Set())
const modelImportInputRef = ref(null)
const modelImportVisible = ref(false)
const aiModelVisible = ref(false)
const aiModelLoading = ref(false)
const aiModelDescription = ref('')
const aiModelResult = ref(null)

const currentModel = reactive(createEmptyModel())

const datasourceLoading = ref(false)
const datasources = ref([])
const selectedDatasourceId = ref(null)
const ddlPreview = ref(null)
const isEditingModel = ref(false)
const datasourceOptions = computed(() => datasources.value.map(item => ({
  label: `${item.datasourceName}${item.isDefault === 1 ? '（默认）' : ''} / ${item.dbType || '-'}`,
  value: item.datasourceId,
})))
const datasourceMap = computed(() => new Map(datasources.value.map(item => [item.datasourceId, item])))

const flatDomains = computed(() => flattenDomains(domains.value))
const visibleAssetNodes = computed(() => flattenVisibleAssetNodes(domains.value))
const domainOptions = computed(() => flatDomains.value.map(domain => ({
  label: domain.domainName,
  value: domain.id,
})))
const canCreateModel = computed(() => selectedDomain.value?.status === 'ENABLED')
const statusOptions = computed(() => dict.value.lowcode_model_status || [])
const enabledModels = computed(() => models.value.filter(model => model.status === 'ENABLED'))
const currentTreeModelCount = computed(() => {
  return Object.values(domainModelsMap.value).reduce((total, items) => total + (Array.isArray(items) ? items.length : 0), 0)
})

onMounted(async () => {
  await Promise.all([loadDomains(), loadDatasources()])
  await applyBusinessContext()
})

watch(
  domains,
  (items) => {
    const next = new Set(expandedDomainIds.value)
    ;(items || []).forEach((domain) => {
      if (domain.children?.length)
        next.add(domain.id)
    })
    expandedDomainIds.value = next
  },
  { deep: true },
)

async function loadDomains() {
  domainLoading.value = true
  try {
    const res = await lowcodeDomainTree({
      keyword: domainKeyword.value || undefined,
    })
    domains.value = res.data || []
  }
  finally {
    domainLoading.value = false
  }
}

async function selectDomain(domain) {
  selectedDomainId.value = domain.id
  expandDomain(domain.id)
  const res = await lowcodeDomainDefaults(domain.id)
  selectedDomain.value = res.data || domain
  clearModelState()
  isEditingModel.value = false
  await loadModels()
}

async function loadDatasources() {
  datasourceLoading.value = true
  try {
    const res = await genDatasourceEnabled()
    datasources.value = res.data || []
    ensureDefaultDatasourceSelected()
  }
  catch (e) {
    datasources.value = []
    console.warn('加载数据源失败:', e)
  }
  finally {
    datasourceLoading.value = false
  }
}

function ensureDefaultDatasourceSelected() {
  if (selectedDatasourceId.value || !datasources.value.length)
    return
  const defaultDatasource = datasources.value.find(item => item.isDefault === 1) || datasources.value[0]
  selectedDatasourceId.value = defaultDatasource?.datasourceId || null
}

function handleDatasourceChange(datasourceId) {
  selectedDatasourceId.value = datasourceId || null
  syncSourceTableDatasource()
}

async function loadModels() {
  if (!selectedDomainId.value) {
    models.value = []
    return
  }
  modelLoading.value = true
  try {
    const res = await lowcodeModelPage({
      pageNum: 1,
      pageSize: 100,
      domainId: selectedDomainId.value,
    })
    models.value = res.data?.records || []
    setDomainModels(selectedDomainId.value, models.value)
  }
  finally {
    modelLoading.value = false
  }
}

async function applyBusinessContext() {
  const query = route.query || {}
  const domain = query.domainId
    ? findDomainById(query.domainId)
    : findDomainByCode(query.domainCode || query.suiteCode)
  if (domain) {
    await selectDomain(domain)
  }

  if (!selectedDomain.value || !query.objectCode)
    return

  const objectCodeValue = firstQueryValue(query.objectCode)
  const objectNameValue = firstQueryValue(query.objectName)
  const normalizedObjectCode = normalizeObjectCode(objectCodeValue)
  const modelCode = resolveContextModelCode(normalizedObjectCode)
  const existingModel = models.value.find(model =>
    model.modelCode === modelCode
    || model.modelCode === normalizedObjectCode
    || model.modelCode?.endsWith(`_${normalizedObjectCode}`),
  )
  if (existingModel) {
    await openModel(existingModel)
    return
  }

  isEditingModel.value = true
  applyModel(createEmptyModel(selectedDomain.value))
  currentModel.modelCode = modelCode
  currentModel.modelName = objectNameValue || objectCodeValue
  currentModel.modelDesc = '从业务对象中心进入的模型配置草稿'
  currentModel.modelSchema.tableName = normalizeTableName(modelCode)
  syncModelIdentity()
}

function createModel() {
  if (!canCreateModel.value) {
    window.$message?.warning('请先选择启用状态的业务领域')
    return
  }
  isEditingModel.value = true
  resetCurrentModel()
}

async function openModel(model) {
  isEditingModel.value = true
  const res = await lowcodeModelDetail(model.id)
  applyModel(res.data)
}

async function openTreeModel(model, domain) {
  if (model?.domainId && selectedDomainId.value !== model.domainId) {
    selectedDomainId.value = model.domainId
    expandDomain(model.domainId)
    const res = await lowcodeDomainDefaults(model.domainId)
    selectedDomain.value = res.data || domain || findDomainById(model.domainId)
    await loadModels()
  }
  await openModel(model)
}

async function saveModel(syncDdl = false) {
  if (!selectedDomain.value) {
    window.$message?.warning('请先选择业务领域')
    return
  }
  if (!currentModel.modelSchema) {
    window.$message?.warning('模型配置无效，请新建或选择数据模型')
    return
  }
  if (!currentModel.modelName) {
    window.$message?.warning('请填写模型名称')
    return
  }
  if (!currentModel.modelCode) {
    window.$message?.warning('请填写模型编码 / 数据表名')
    return
  }
  if (syncDdl) {
    const confirmed = await confirmSyncDdl()
    if (!confirmed)
      return
  }
  syncModelIdentity()
  currentModel.modelSchema.fields = ensureSystemFields(currentModel.modelSchema.fields || [], true)
  if (syncDdl)
    syncSaving.value = true
  else
    saving.value = true
  try {
    const payload = {
      id: currentModel.id,
      domainId: currentModel.domainId,
      modelCode: currentModel.modelCode,
      modelName: currentModel.modelName,
      modelDesc: currentModel.modelDesc,
      status: currentModel.status,
      tenantEnabled: currentModel.tenantEnabled,
      masterData: currentModel.masterData,
      modelSchema: cloneSchema(currentModel.modelSchema),
      businessSuiteCode: firstQueryValue(route.query.suiteCode || route.query.domainCode),
      businessObjectCode: firstQueryValue(route.query.objectCode),
      syncDdl,
      confirmSyncDdl: syncDdl,
    }
    const res = currentModel.id ? await lowcodeUpdateModel(payload) : await lowcodeCreateModel(payload)
    currentModel.id = res.data || currentModel.id
    window.$message?.success(syncDdl ? '模型已保存，表结构已同步' : '模型配置已保存')
    ddlPreview.value = null
    await loadModels()
  }
  catch (e) {
    window.$message?.error(e?.message || '保存模型失败')
  }
  finally {
    saving.value = false
    syncSaving.value = false
  }
}

function confirmSyncDdl() {
  return new Promise((resolve) => {
    if (!window.$dialog) {
      resolve(false)
      return
    }
    window.$dialog.warning({
      title: '确认同步表结构',
      content: '确认保存模型配置并同步表结构到数据库？已有表只会追加缺失业务字段。',
      positiveText: '确认同步',
      negativeText: '取消',
      onPositiveClick: () => resolve(true),
      onNegativeClick: () => resolve(false),
      onClose: () => resolve(false),
    })
  })
}

function triggerModelImport() {
  if (!selectedDomain.value) {
    window.$message?.warning('请先选择业务领域')
    return
  }
  modelImportInputRef.value?.click()
}

function openDbTableImport() {
  if (!canCreateModel.value) {
    window.$message?.warning('请先选择启用状态的业务领域')
    return
  }
  modelImportVisible.value = true
}

function handleDbTableModelApply(payload) {
  const schema = cloneSchema(payload?.modelSchema || {})
  if (!schema.fields?.length) {
    window.$message?.warning('数据表没有可导入字段')
    return
  }
  isEditingModel.value = true
  currentModel.id = null
  currentModel.domainId = selectedDomain.value?.id || null
  currentModel.domainCode = selectedDomain.value?.domainCode || ''
  currentModel.domainName = selectedDomain.value?.domainName || ''
  currentModel.modelCode = normalizeObjectCode(payload.modelCode || schema.object?.code || schema.tableName || '')
  currentModel.modelName = payload.modelName || schema.object?.name || schema.businessName || payload.request?.tableName || ''
  currentModel.modelDesc = schema.object?.description || schema.sourceTable?.tableComment || ''
  currentModel.status = 'ENABLED'
  currentModel.tenantEnabled = true
  currentModel.masterData = false
  currentModel.modelSchema = schema
  currentModel.modelSchema.fields = ensureSystemFields(currentModel.modelSchema.fields || [], true)
  applySourceTableSelection(currentModel.modelSchema.sourceTable)
  syncModelIdentity()
  ddlPreview.value = null
  window.$message?.success('数据表结构已应用到模型设计，请确认后保存')
}

function openAiModelGenerate() {
  if (!canCreateModel.value) {
    window.$message?.warning('请先选择启用状态的业务领域')
    return
  }
  aiModelDescription.value = ''
  aiModelResult.value = null
  aiModelVisible.value = true
}

async function generateAiModelDraft() {
  if (!aiModelDescription.value.trim()) {
    window.$message?.warning('请输入需求描述')
    return
  }
  aiModelLoading.value = true
  try {
    const res = await lowcodeAiGenerateApp({
      domainId: selectedDomainId.value,
      description: aiModelDescription.value.trim(),
      layoutType: 'simple-crud',
      autoCreateModel: true,
      includeDdl: false,
    })
    aiModelResult.value = res.data
  }
  catch (e) {
    aiModelResult.value = null
    window.$message?.error(e?.message || 'AI 生成模型失败')
  }
  finally {
    aiModelLoading.value = false
  }
}

function applyAiModelResult() {
  const modelDraft = aiModelResult.value?.modelDraft
  const modelSchema = aiModelResult.value?.modelSchema
  if (!modelDraft && !modelSchema)
    return
  applyImportedModel({
    ...(modelDraft || {}),
    modelSchema: modelDraft?.modelSchema || modelSchema,
  })
  aiModelVisible.value = false
  window.$message?.success('AI 模型草稿已应用，请确认后保存')
}

function exportCurrentModel() {
  syncModelIdentity()
  downloadJson({
    type: 'LOWCODE_MODEL_CONFIG',
    version: 1,
    exportedAt: new Date().toISOString(),
    model: {
      id: null,
      domainCode: currentModel.domainCode,
      domainName: currentModel.domainName,
      modelCode: currentModel.modelCode,
      modelName: currentModel.modelName,
      modelDesc: currentModel.modelDesc,
      status: currentModel.status,
      tenantEnabled: currentModel.tenantEnabled,
      masterData: currentModel.masterData,
      modelSchema: cloneSchema(currentModel.modelSchema),
    },
  }, `${currentModel.modelCode || 'model'}-model-config.json`)
}

async function handleModelImportFile(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file)
    return
  try {
    const payload = JSON.parse(await file.text())
    const model = payload.model || payload
    if (!model?.modelSchema) {
      window.$message?.warning('模型配置文件格式不正确')
      return
    }
    applyImportedModel(model)
    window.$message?.success('模型配置已导入，请检查后保存')
  }
  catch (e) {
    window.$message?.error(e?.message || '导入模型配置失败')
  }
}

function applyImportedModel(model) {
  isEditingModel.value = true
  const schema = cloneSchema(model.modelSchema || {})
  currentModel.id = null
  currentModel.domainId = selectedDomain.value?.id || null
  currentModel.domainCode = selectedDomain.value?.domainCode || ''
  currentModel.domainName = selectedDomain.value?.domainName || ''
  currentModel.modelCode = normalizeObjectCode(model.modelCode || schema.object?.code || schema.tableName || '')
  currentModel.modelName = model.modelName || schema.object?.name || schema.businessName || ''
  currentModel.modelDesc = model.modelDesc || schema.object?.description || ''
  currentModel.status = model.status || 'ENABLED'
  currentModel.tenantEnabled = model.tenantEnabled !== false
  currentModel.masterData = Boolean(model.masterData)
  currentModel.modelSchema = schema
  currentModel.modelSchema.fields = ensureSystemFields(currentModel.modelSchema.fields || [], currentModel.tenantEnabled)
  applySourceTableSelection(schema.sourceTable)
  syncModelIdentity()
  ddlPreview.value = null
}

async function previewDdl() {
  if (!selectedDomain.value) {
    window.$message?.warning('请先选择业务领域')
    return
  }
  if (!currentModel.modelSchema) {
    window.$message?.warning('模型配置无效，请新建或选择数据模型')
    return
  }
  syncModelIdentity()
  currentModel.modelSchema.fields = ensureSystemFields(currentModel.modelSchema.fields || [], true)
  ddlLoading.value = true
  try {
    const res = await lowcodeDdlPreview(cloneSchema(currentModel.modelSchema))
    ddlPreview.value = res.data
  }
  catch (e) {
    window.$message?.error(e?.message || 'DDL 预览失败')
  }
  finally {
    ddlLoading.value = false
  }
}

function clearModelState() {
  currentModel.id = null
  currentModel.domainId = selectedDomain.value?.id || null
  currentModel.domainCode = selectedDomain.value?.domainCode || ''
  currentModel.domainName = selectedDomain.value?.domainName || ''
  currentModel.modelCode = ''
  currentModel.modelName = ''
  currentModel.modelDesc = ''
  currentModel.status = 'ENABLED'
  currentModel.tenantEnabled = true
  currentModel.masterData = false
  currentModel.modelSchema = null
  selectedDatasourceId.value = null
  ddlPreview.value = null
}

function resetCurrentModel() {
  applyModel(createEmptyModel(selectedDomain.value))
}

function applyModel(model = {}) {
  currentModel.id = model.id || null
  currentModel.domainId = model.domainId || selectedDomain.value?.id || null
  currentModel.domainCode = model.domainCode || selectedDomain.value?.domainCode || ''
  currentModel.domainName = model.domainName || selectedDomain.value?.domainName || ''
  currentModel.modelCode = model.modelCode || ''
  currentModel.modelName = model.modelName || ''
  currentModel.modelDesc = model.modelDesc || ''
  currentModel.status = model.status || 'ENABLED'
  currentModel.tenantEnabled = model.tenantEnabled !== false
  currentModel.masterData = Boolean(model.masterData)
  currentModel.modelSchema = cloneSchema(model.modelSchema || createModelSchema(selectedDomain.value))
  applySourceTableSelection(currentModel.modelSchema.sourceTable)
  syncModelIdentity()
}

function updateModelCode(value) {
  currentModel.modelCode = normalizeObjectCode(value)
  syncTableNameFromModelCode()
  syncModelIdentity()
}

function syncTableNameFromModelCode() {
  if (!currentModel.modelSchema)
    currentModel.modelSchema = createModelSchema(selectedDomain.value)
  currentModel.modelSchema.tableName = normalizeTableName(currentModel.modelCode)
}

function applySourceTableSelection(sourceTable = {}) {
  selectedDatasourceId.value = sourceTable?.datasourceId || null
  ensureDefaultDatasourceSelected()
}

function syncSourceTableDatasource() {
  if (!currentModel.modelSchema)
    currentModel.modelSchema = createModelSchema(selectedDomain.value)
  if (!selectedDatasourceId.value || currentModel.modelSchema.tableMode !== 'EXISTING')
    return
  const currentSource = currentModel.modelSchema.sourceTable || {}
  currentModel.modelSchema.sourceTable = {
    ...currentSource,
    ...buildDatasourceRef(selectedDatasourceId.value),
  }
}

function buildDatasourceRef(datasourceId) {
  const datasource = datasourceMap.value.get(datasourceId) || {}
  return {
    datasourceId: datasource.datasourceId || datasourceId || null,
    datasourceCode: datasource.datasourceCode || '',
    datasourceName: datasource.datasourceName || '',
    dbType: datasource.dbType || '',
  }
}

function syncModelIdentity() {
  if (!currentModel.modelSchema)
    currentModel.modelSchema = createModelSchema(selectedDomain.value)
  if (!currentModel.modelSchema.object)
    currentModel.modelSchema.object = {}
  if (!currentModel.modelSchema.domain)
    currentModel.modelSchema.domain = {}
  currentModel.modelSchema.schemaVersion = 2
  currentModel.modelSchema.fields = ensureSystemFields(currentModel.modelSchema.fields || [], true)
  if (!currentModel.modelSchema.indexes)
    currentModel.modelSchema.indexes = []
  currentModel.modelSchema.domain = {
    id: selectedDomain.value?.id || currentModel.domainId,
    code: selectedDomain.value?.domainCode || currentModel.domainCode,
    name: selectedDomain.value?.domainName || currentModel.domainName,
  }
  currentModel.modelSchema.object = {
    ...currentModel.modelSchema.object,
    code: currentModel.modelCode,
    name: currentModel.modelName,
  }
  currentModel.modelSchema.businessName = currentModel.modelName
  normalizeLowcodePolicies(currentModel.modelSchema)
  if (!currentModel.modelSchema.tableName && currentModel.modelCode)
    currentModel.modelSchema.tableName = normalizeTableName(currentModel.modelCode)
  if (selectedDatasourceId.value) {
    currentModel.modelSchema.sourceTable = {
      ...(currentModel.modelSchema.sourceTable || {}),
      ...buildDatasourceRef(selectedDatasourceId.value),
      tableName: currentModel.modelSchema.sourceTable?.tableName || currentModel.modelSchema.tableName || '',
      tableComment: currentModel.modelSchema.sourceTable?.tableComment || currentModel.modelName || '',
    }
  }
}

function createEmptyModel(domain = null) {
  return {
    id: null,
    domainId: domain?.id || null,
    domainCode: domain?.domainCode || '',
    domainName: domain?.domainName || '',
    modelCode: '',
    modelName: '',
    modelDesc: '',
    status: 'ENABLED',
    tenantEnabled: true,
    masterData: false,
    modelSchema: createModelSchema(domain),
  }
}

function createModelSchema(domain = null) {
  const modelSchema = createDefaultModelSchema({
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
    appType: 'SINGLE',
  })
  modelSchema.object.code = ''
  modelSchema.object.name = ''
  modelSchema.businessName = ''
  modelSchema.tableName = ''
  return modelSchema
}

function flattenDomains(nodes, level = 0) {
  const result = []
  for (const node of nodes || []) {
    result.push({ ...node, level })
    if (node.children?.length)
      result.push(...flattenDomains(node.children, level + 1))
  }
  return result
}

function flattenVisibleAssetNodes(nodes, level = 0) {
  const result = []
  for (const domain of nodes || []) {
    result.push({
      key: `domain-${domain.id}`,
      type: 'domain',
      domain,
      level,
    })
    if (!isExpanded(domain.id))
      continue
    if (domain.children?.length)
      result.push(...flattenVisibleAssetNodes(domain.children, level + 1))
    if (isDomainModelsLoading(domain.id)) {
      result.push({
        key: `domain-${domain.id}-loading`,
        type: 'loading',
        domain,
        level: level + 1,
      })
      continue
    }
    const domainModels = getDomainModels(domain.id)
    if (domainModels.length) {
      domainModels.forEach((model) => {
        result.push({
          key: `model-${model.id}`,
          type: 'model',
          domain,
          model,
          level: level + 1,
        })
      })
    }
    else if (isDomainModelsLoaded(domain.id) && !domain.children?.length) {
      result.push({
        key: `domain-${domain.id}-empty`,
        type: 'empty',
        domain,
        level: level + 1,
      })
    }
  }
  return result
}

function hasChildren(domain) {
  return Boolean(domain?.children?.length)
}

function isAssetExpandable(domain) {
  return Boolean(domain?.id) && (hasChildren(domain) || !isDomainModelsLoaded(domain.id) || getDomainModels(domain.id).length > 0)
}

function isExpanded(id) {
  return expandedDomainIds.value.has(id)
}

function toggleExpanded(domain) {
  if (!isAssetExpandable(domain))
    return
  const next = new Set(expandedDomainIds.value)
  if (next.has(domain.id)) {
    next.delete(domain.id)
  }
  else {
    next.add(domain.id)
    ensureDomainModels(domain.id)
  }
  expandedDomainIds.value = next
}

function expandDomain(domainId) {
  if (!domainId)
    return
  const next = new Set(expandedDomainIds.value)
  next.add(domainId)
  expandedDomainIds.value = next
  ensureDomainModels(domainId)
}

async function ensureDomainModels(domainId) {
  if (!domainId || isDomainModelsLoaded(domainId) || isDomainModelsLoading(domainId))
    return
  const nextLoadingIds = new Set(domainModelLoadingIds.value)
  nextLoadingIds.add(domainId)
  domainModelLoadingIds.value = nextLoadingIds
  try {
    const res = await lowcodeModelPage({
      pageNum: 1,
      pageSize: 100,
      domainId,
    })
    setDomainModels(domainId, res.data?.records || [])
  }
  finally {
    const next = new Set(domainModelLoadingIds.value)
    next.delete(domainId)
    domainModelLoadingIds.value = next
  }
}

function getDomainModels(domainId) {
  return domainModelsMap.value[domainId] || []
}

function setDomainModels(domainId, items) {
  if (!domainId)
    return
  domainModelsMap.value = {
    ...domainModelsMap.value,
    [domainId]: Array.isArray(items) ? items : [],
  }
}

function isDomainModelsLoaded(domainId) {
  return Object.prototype.hasOwnProperty.call(domainModelsMap.value, domainId)
}

function isDomainModelsLoading(domainId) {
  return domainModelLoadingIds.value.has(domainId)
}

function findDomainById(domainId) {
  return flatDomains.value.find(domain => String(domain.id) === String(domainId)) || null
}

function findDomainByCode(domainCode) {
  const code = firstQueryValue(domainCode)
  if (!code)
    return null
  return flatDomains.value.find(domain => domain.domainCode === code) || null
}

function resolveContextModelCode(objectCode) {
  const prefix = selectedDomain.value?.configKeyPrefix || selectedDomain.value?.tablePrefix || ''
  if (!prefix || objectCode.startsWith(prefix))
    return objectCode
  return normalizeObjectCode(`${prefix}${objectCode}`)
}

function firstQueryValue(value) {
  return Array.isArray(value) ? value[0] : String(value || '').trim()
}

function openDomainEditor() {
  editingDomain.value = null
  domainEditorVisible.value = true
}

function editSelectedDomain() {
  if (!selectedDomain.value) {
    window.$message?.warning('请先选择业务领域')
    return
  }
  editingDomain.value = { ...selectedDomain.value }
  domainEditorVisible.value = true
}

async function handleDomainSaved() {
  domainEditorVisible.value = false
  editingDomain.value = null
  await loadDomains()
  if (selectedDomainId.value) {
    const res = await lowcodeDomainDefaults(selectedDomainId.value)
    selectedDomain.value = res.data || selectedDomain.value
  }
}

function downloadJson(payload, filename) {
  const blob = new Blob([JSON.stringify(payload, null, 2)], { type: 'application/json;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}
</script>

<style scoped>
.model-workbench-page {
  display: grid;
  min-height: 100%;
  grid-template-columns: 268px minmax(0, 1fr);
  gap: 14px;
  padding: 14px;
  background: #f3f6fa;
}

.model-nav {
  display: grid;
  position: sticky;
  top: 14px;
  height: calc(100vh - 28px);
  max-height: calc(100vh - 28px);
  min-height: 0;
  grid-template-rows: minmax(420px, 1fr);
  align-self: start;
}

.nav-block,
.page-header,
.basic-panel,
.ddl-panel,
.empty-panel {
  border: 1px solid #d8dee8;
  border-radius: 8px;
  background: #fff;
}

.nav-block {
  display: grid;
  min-height: 0;
  overflow: hidden;
  gap: 12px;
  padding: 12px;
  grid-template-rows: auto auto minmax(0, 1fr);
}

.nav-head,
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.nav-head h2 {
  margin: 0;
  color: #0f172a;
  font-size: 15px;
}

.nav-head span {
  color: #64748b;
  font-size: 12px;
}

.asset-tree {
  display: grid;
  height: 100%;
  min-height: 0;
  gap: 4px;
  overflow: auto;
  align-content: start;
}

.tree-spin {
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.tree-spin :deep(.n-spin-content) {
  height: 100%;
  min-height: 0;
}

.tree-domain,
.tree-model {
  display: grid;
  width: 100%;
  min-height: 38px;
  align-items: center;
  gap: 8px;
  border: 1px solid transparent;
  border-radius: 6px;
  background: transparent;
  color: inherit;
  cursor: pointer;
  padding: 5px 8px;
  text-align: left;
}

.tree-domain {
  grid-template-columns: calc(var(--indent, 0) * 14px) 18px 34px 20px minmax(0, 1fr) auto;
}

.tree-model {
  grid-template-columns: calc(var(--indent, 0) * 14px) 18px 34px 20px minmax(0, 1fr) auto;
}

.tree-domain:hover,
.tree-model:hover,
.tree-domain.active,
.tree-model.active {
  border-color: #93c5fd;
  background: #eff6ff;
}

.tree-model.create {
  border-style: dashed;
  color: #2563eb;
}

.model-list {
  display: grid;
  min-height: 0;
  gap: 4px;
  overflow-y: auto;
  align-content: start;
}

.model-list .tree-model {
  grid-template-columns: 22px minmax(0, 1fr) auto;
  border-color: #e5e7eb;
  background: #fbfdff;
}

.model-list .tree-model.active {
  border-color: #60a5fa;
  background: #eff6ff;
}

.nav-head.compact {
  align-items: flex-start;
}

.model-empty {
  padding: 20px 0;
}

.tree-domain.disabled {
  opacity: 0.68;
}

.node-indent,
.node-spacer,
.model-indent {
  display: block;
}

.node-spacer {
  width: 18px;
}

.node-toggle {
  display: inline-flex;
  width: 20px;
  height: 24px;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  color: #64748b;
}

.node-toggle:not(.empty):hover {
  background: #e0ecff;
  color: #2563eb;
}

.node-toggle.empty {
  cursor: default;
}

.model-indent {
  width: 26px;
  border-left: 1px solid #cbd5e1;
  justify-self: center;
  min-height: 24px;
}

.tree-glyph {
  display: inline-flex;
  width: 20px;
  height: 20px;
  align-items: center;
  justify-content: center;
  color: #2563eb;
  font-size: 16px;
}

.node-kind {
  display: inline-flex;
  width: 34px;
  height: 20px;
  align-items: center;
  justify-content: center;
  border-radius: 5px;
  font-size: 11px;
  font-weight: 700;
  line-height: 1;
}

.node-kind.domain {
  background: #e0f2fe;
  color: #075985;
}

.node-kind.model {
  background: #ecfdf5;
  color: #047857;
}

.node-main {
  display: grid;
  min-width: 0;
  gap: 2px;
}

.node-main strong,
.node-main code {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.node-main strong {
  color: #0f172a;
  font-size: 13px;
}

.node-main code {
  color: #64748b;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
}

.tree-state {
  display: grid;
  min-height: 30px;
  grid-template-columns: calc(var(--indent, 0) * 14px) minmax(0, 1fr);
  align-items: center;
  color: #64748b;
  font-size: 12px;
  padding: 4px 8px;
}

.tree-state.muted {
  color: #94a3b8;
}

.model-main {
  display: grid;
  min-width: 0;
  gap: 12px;
}

.page-header {
  align-items: flex-start;
  padding: 14px;
}

.page-header > div {
  min-width: 0;
}

.model-header-actions {
  display: flex;
  max-width: min(820px, 62%);
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.model-header-actions :deep(.n-button) {
  min-width: 88px;
}

.header-action-group {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.header-action-group.primary {
  padding-left: 8px;
  border-left: 1px solid #d8dee8;
}

.header-kicker {
  color: #2563eb;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  font-weight: 700;
}

.page-header h1 {
  margin: 4px 0 0;
  color: #0f172a;
  font-size: 22px;
}

.page-header p {
  margin: 5px 0 0;
  color: #64748b;
  font-size: 13px;
}

.basic-panel {
  padding: 14px;
}

.ddl-panel {
  padding: 14px;
}

.section-title {
  margin-bottom: 12px;
  color: #0f172a;
  font-size: 14px;
  font-weight: 700;
}

.basic-form {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 10px 12px;
}

.basic-form :deep(.n-form-item) {
  margin-bottom: 0;
}

.span-2 {
  grid-column: span 2;
}

.table-sync-control {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 8px;
  width: 100%;
  align-items: center;
}

.source-table-summary {
  min-width: 0;
  overflow: hidden;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: #f8fafc;
  color: #64748b;
  font-size: 12px;
  line-height: 30px;
  padding: 0 9px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hidden-file-input {
  display: none;
}

.ai-model-modal {
  display: grid;
  gap: 12px;
}

.ai-result-panel {
  border: 1px solid #d8dee8;
  border-radius: 8px;
  background: #f8fafc;
  padding: 12px;
}

.preview-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.preview-head > div {
  display: grid;
  min-width: 0;
  gap: 2px;
}

.preview-head strong,
.preview-head span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-head strong {
  color: #0f172a;
  font-size: 14px;
}

.preview-head span {
  color: #64748b;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
}

.note-list {
  margin: 10px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.7;
  padding-left: 18px;
}

.ddl-warning {
  margin-bottom: 10px;
}

.ddl-code {
  max-height: 320px;
  overflow: auto;
  border-radius: 6px;
  background: #0f172a;
  color: #dbeafe;
  font-size: 12px;
  line-height: 1.6;
  padding: 12px;
  white-space: pre-wrap;
}

.designer-panel {
  min-height: 720px;
}

.empty-panel {
  display: grid;
  min-height: 520px;
  place-items: center;
  align-content: center;
  gap: 12px;
  padding: 40px;
  text-align: center;
}

.empty-icon {
  display: inline-flex;
  width: 60px;
  height: 60px;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: #eff6ff;
  color: #2563eb;
  font-size: 28px;
}

.empty-panel h2 {
  margin: 0;
  color: #0f172a;
  font-size: 20px;
}

.empty-panel p {
  max-width: 560px;
  margin: 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.7;
}

.domain-overview {
  display: grid;
  gap: 14px;
}

.overview-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  border: 1px solid #d8dee8;
  border-radius: 8px;
  background: linear-gradient(135deg, rgba(15, 23, 42, 0.96), rgba(30, 41, 59, 0.92)), #0f172a;
  color: #f8fafc;
  padding: 18px;
}

.hero-code {
  color: #86efac;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  font-weight: 700;
}

.overview-hero h1 {
  margin: 6px 0 0;
  color: #f8fafc;
  font-size: 22px;
  line-height: 1.25;
}

.overview-hero p {
  max-width: 720px;
  margin: 8px 0 0;
  color: #cbd5e1;
  font-size: 13px;
  line-height: 1.6;
}

.hero-metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.hero-actions {
  display: grid;
  width: 380px;
  gap: 12px;
  flex-shrink: 0;
}

.hero-action-buttons {
  display: grid;
  width: 100%;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.hero-action-buttons :deep(.n-button) {
  width: 100%;
  min-width: 0;
}

.hero-metric {
  display: grid;
  gap: 3px;
  padding: 10px 18px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.06);
  text-align: center;
}

.hero-metric strong {
  font-size: 26px;
  line-height: 1.1;
  color: #f8fafc;
}

.hero-metric span {
  color: #cbd5e1;
  font-size: 11px;
}

.domain-model-list {
  border: 1px solid #d8dee8;
  border-radius: 8px;
  background: #fff;
  padding: 14px;
}

.model-list-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.model-list-head .section-title {
  margin-bottom: 3px;
}

.model-list-head p {
  margin: 0;
  color: #64748b;
  font-size: 12px;
}

.model-card-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.model-card {
  display: grid;
  gap: 8px;
  min-height: 78px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  color: inherit;
  cursor: pointer;
  padding: 12px;
  text-align: left;
  transition:
    border-color 180ms ease,
    box-shadow 180ms ease,
    transform 180ms ease;
}

.model-card:hover {
  border-color: #93c5fd;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.06);
  transform: translateY(-1px);
}

.model-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.model-card-head strong {
  overflow: hidden;
  color: #0f172a;
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.model-card-body {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  color: #64748b;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
}

.model-card-body code {
  color: #0f172a;
  font-size: 11px;
}

.model-card-desc {
  overflow: hidden;
  color: #94a3b8;
  font-size: 11px;
  line-height: 1.5;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

@media (max-width: 1160px) {
  .model-workbench-page {
    grid-template-columns: 1fr;
  }

  .model-nav {
    grid-template-columns: 1fr;
    position: static;
    height: auto;
    max-height: none;
    grid-template-rows: auto;
  }

  .asset-tree {
    max-height: 320px;
  }

  .basic-form {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .page-header {
    flex-direction: column;
  }

  .model-header-actions {
    max-width: none;
    justify-content: flex-start;
  }

  .header-action-group.primary {
    padding-left: 0;
    border-left: 0;
  }

  .overview-hero {
    flex-direction: column;
  }

  .hero-actions {
    width: 100%;
  }
}

@media (max-width: 760px) {
  .model-workbench-page,
  .model-nav,
  .basic-form {
    grid-template-columns: 1fr;
  }

  .span-2 {
    grid-column: span 1;
  }

  .model-header-actions :deep(.n-button),
  .header-action-group :deep(.n-button) {
    flex: 1 1 calc(50% - 6px);
    min-width: 0;
  }

  .header-action-group {
    width: 100%;
    justify-content: flex-start;
  }

  .hero-action-buttons,
  .hero-metrics,
  .model-card-grid {
    grid-template-columns: 1fr;
  }
}
</style>
