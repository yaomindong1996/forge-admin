<template>
  <div class="flow-page">
    <!-- 统计卡片 -->
    <FlowModelStats
      :total-count="totalCount"
      :designing-count="designingCount"
      :deployed-count="deployedCount"
      :suspended-count="suspendedCount"
      :disabled-count="disabledCount"
      :active-status="activeStatsStatus"
      @filter="handleFilter"
    />

    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <div class="title-row">
          <div class="title-icon">
            <i class="i-material-symbols:device-hub" />
          </div>
          <h2 class="page-title">
            流程模型
          </h2>
        </div>
      </div>
      <div class="header-right">
        <n-input
          v-model:value="queryParams.modelName"
          placeholder="搜索模型名称或 Key"
          clearable
          class="search-input"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <i class="i-material-symbols:search" />
          </template>
        </n-input>
        <NTreeSelect
          v-model:value="queryParams.category"
          placeholder="流程分类"
          clearable
          class="category-select"
          :options="categoryTreeOptions"
          :default-expand-all="true"
        />
        <n-select
          v-model:value="queryParams.status"
          placeholder="状态"
          clearable
          class="status-select"
          :options="statusOptions"
          @update:value="handleStatusSelect"
        />
        <div class="toolbar-actions">
          <n-button type="primary" @click="handleSearch">
            <template #icon>
              <i class="i-material-symbols:search" />
            </template>
            查询
          </n-button>
          <n-button @click="handleReset">
            <template #icon>
              <i class="i-material-symbols:restart-alt" />
            </template>
            清空
          </n-button>
          <n-button type="primary" @click="handleAdd">
            <template #icon>
              <i class="i-material-symbols:add" />
            </template>
            新增模型
          </n-button>
          <n-button
            v-if="dataSource.length"
            :type="sortMode ? 'primary' : 'default'"
            :loading="sortSaving"
            @click="sortMode ? saveModelOrder() : (sortMode = true)"
          >
            {{ sortMode ? '保存排序' : '调整排序' }}
          </n-button>
          <n-button v-if="sortMode" quaternary :disabled="sortSaving" @click="cancelModelOrder">
            取消
          </n-button>
        </div>
      </div>
    </div>

    <!-- 模型列表 -->
    <section class="model-workbench">
      <n-spin :show="loading" class="model-list-spin">
        <div class="model-list-body">
          <div v-if="dataSource.length > 0" class="model-grid">
            <div
              v-for="item in dataSource"
              :key="item.id"
              class="model-card"
              :class="{ 'model-card-sortable': sortMode }"
              :draggable="sortMode"
              @dragstart="handleDragStart(item)"
              @dragover.prevent
              @drop="handleDrop(item)"
            >
              <div class="card-header">
                <div class="card-title-block">
                  <div class="card-title-row">
                    <div class="card-title-icon-box">
                      <i class="i-lucide:git-merge card-title-icon" />
                    </div>
                    <div class="card-title-main">
                      <div class="card-title">
                        {{ item.modelName }}
                      </div>
                      <div v-if="sortMode" class="card-sort-hint">
                        拖动调整顺序
                      </div>
                      <div class="card-key">
                        {{ item.modelKey }}
                      </div>
                    </div>
                  </div>
                </div>
                <span class="status-tag" :class="statusClass(item.status)">
                  {{ getLabel('flow_model_status', item.status) }}
                </span>
              </div>
              <div class="card-body">
                <div class="card-tags">
                  <span class="designer-type-badge" :class="designerTypeClass(item.designerType)">
                    {{ designerTypeLabel(item.designerType) }}
                  </span>
                  <span v-if="getCategoryDisplayName(item)" class="category-badge">
                    {{ getCategoryDisplayName(item) }}
                  </span>
                </div>
                <div class="card-binding" :class="{ empty: !item.businessBindings?.length }">
                  <i class="i-lucide:link-2" />
                  <span>{{ formatBusinessBindings(item) }}</span>
                </div>
                <div class="card-desc">
                  {{ item.description || '暂无描述' }}
                </div>
              </div>
              <div class="card-footer">
                <div class="card-metadata">
                  <div class="meta-item">
                    <i class="i-lucide:calendar" />
                    {{ formatDate(item.updateTime) || '未更新' }}
                  </div>
                  <div class="meta-item">
                    <i class="i-lucide:git-commit" />
                    v{{ item.version || 1 }}
                  </div>
                </div>
                <div class="card-actions">
                  <button
                    type="button"
                    class="card-action-link"
                    @click.stop="handleDesign(item)"
                  >
                    编辑
                  </button>
                  <template v-if="item.status === 0 || item.status === 1">
                    <span class="card-action-separator" />
                    <button
                      v-if="item.status === 0"
                      type="button"
                      class="card-action-link"
                      :disabled="isModelActionLocked(item, 'deploy')"
                      @click.stop="handleDeploy(item)"
                    >
                      发布
                    </button>
                    <button
                      v-else
                      type="button"
                      class="card-action-link"
                      @click.stop="handleViewInstances(item)"
                    >
                      实例
                    </button>
                  </template>
                  <span class="card-action-separator" />
                  <n-dropdown
                    trigger="click"
                    :options="getActionOptions(item)"
                    :disabled="isModelActionBusy(item)"
                    @select="key => handleActionSelect(key, item)"
                  >
                    <button type="button" class="card-more-action" aria-label="更多操作" @click.stop>
                      <i class="i-lucide:more-horizontal" />
                    </button>
                  </n-dropdown>
                </div>
              </div>
            </div>
          </div>

          <!-- 加载占位：首次加载无数据时撑开高度，保证 loading 可见 -->
          <div v-else-if="loading" class="model-list-loading" />

          <!-- 空状态 -->
          <n-empty
            v-else
            description="暂无流程模型，点击「新增模型」开始设计"
            class="empty-state"
          >
            <template #extra>
              <n-button type="primary" @click="handleAdd">
                <template #icon>
                  <i class="i-material-symbols:add" />
                </template>
                新增模型
              </n-button>
            </template>
          </n-empty>
        </div>
      </n-spin>

      <!-- 分页 -->
      <div v-if="pagination.itemCount > 0" class="pagination-wrapper">
        <n-pagination
          v-model:page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :item-count="pagination.itemCount"
          :page-sizes="[12, 24, 48]"
          show-size-picker
          :disabled="sortMode || sortSaving"
          @update:page="fetchData"
          @update:page-size="handlePageSizeChange"
        />
      </div>
    </section>

    <!-- 新增/编辑弹窗 -->
    <Teleport to="body">
      <NModal
        v-model:show="showModal"
        preset="card"
        :title="modalTitle"
        style="width: min(760px, calc(100vw - 32px))"
        :mask-closable="false"
      >
        <n-form
          ref="formRef"
          :model="formData"
          :rules="rules"
          label-placement="left"
          label-width="100"
        >
          <n-grid :cols="2" :x-gap="16">
            <n-form-item-gi label="流程模式" path="designerType" :span="2">
              <div class="designer-type-chooser" :class="{ disabled: isEdit }">
                <button
                  v-for="option in designerTypeOptions"
                  :key="option.value"
                  type="button"
                  class="designer-type-option"
                  :class="{ active: formData.designerType === option.value }"
                  :disabled="isEdit"
                  @click="formData.designerType = option.value"
                >
                  <span class="designer-type-icon">
                    <i :class="option.icon" />
                  </span>
                  <span class="designer-type-main">
                    <span class="designer-type-title">{{ option.label }}</span>
                    <span class="designer-type-desc">{{ option.desc }}</span>
                  </span>
                </button>
              </div>
            </n-form-item-gi>
            <n-form-item-gi label="模型名称" path="modelName" :span="2">
              <n-input v-model:value="formData.modelName" placeholder="请输入模型名称" />
            </n-form-item-gi>
            <n-form-item-gi label="模型Key" path="modelKey" :span="2" class="model-key-form-item">
              <n-input
                v-model:value="formData.modelKey"
                placeholder="请输入有意义的模型Key，留空自动生成"
                :disabled="isEdit && [1, 2].includes(Number(formData.status))"
              />
              <div class="model-key-help">
                以字母开头，只能包含字母、数字、下划线或短横线；已发布或挂起模型不能修改。
              </div>
            </n-form-item-gi>
            <n-form-item-gi label="流程分类" path="category" :span="2">
              <NTreeSelect
                v-model:value="formData.category"
                placeholder="请选择分类"
                :options="categoryTreeOptions"
                :default-expand-all="true"
              />
            </n-form-item-gi>
            <n-form-item-gi label="描述" path="description" :span="2">
              <n-input
                v-model:value="formData.description"
                type="textarea"
                placeholder="请输入描述"
                :rows="3"
              />
            </n-form-item-gi>
          </n-grid>
        </n-form>
        <template #footer>
          <n-space justify="end">
            <n-button @click="showModal = false">
              取消
            </n-button>
            <n-button type="primary" :loading="submitLoading" @click="handleSubmit">
              确定
            </n-button>
          </n-space>
        </template>
      </NModal>
    </Teleport>

    <Teleport to="body">
      <NModal
        v-model:show="showStartTestModal"
        preset="card"
        :title="startTestTitle"
        style="width: min(760px, calc(100vw - 32px))"
        content-style="max-height: calc(100vh - 180px); overflow: auto;"
        :mask-closable="false"
      >
        <div class="start-test-modal">
          <div class="start-test-summary">
            <div class="summary-icon">
              <i class="i-material-symbols:play-circle-outline" />
            </div>
            <div class="summary-main">
              <div class="summary-title">
                {{ currentStartModel?.modelName || '-' }}
              </div>
              <div class="summary-subtitle">
                {{ currentStartModel?.modelKey || '-' }}
              </div>
            </div>
            <span class="status-tag deployed">测试发起</span>
          </div>

          <n-alert v-if="startTestAlert" :type="startTestAlert.type" :show-icon="false" class="start-test-alert">
            {{ startTestAlert.text }}
          </n-alert>
          <n-alert
            v-if="startTestPreflightDiagnostics.length"
            type="warning"
            :show-icon="true"
            class="start-test-alert"
          >
            <div>启动前审批人预检发现配置问题：</div>
            <div v-for="diagnostic in startTestPreflightDiagnostics" :key="diagnostic">
              {{ diagnostic }}
            </div>
          </n-alert>

          <div v-if="startTestBusinessFormLoading" class="start-test-form-loading">
            <n-spin size="small" />
            <span>正在加载业务应用表单...</span>
          </div>
          <AiForm
            v-else-if="startTestBusinessFormActive && startTestFormSchema.length"
            ref="startTestFormRef"
            v-model:value="startTestFormData"
            :schema="startTestFormSchema"
            :grid-cols="startTestBusinessFormLayout.gridCols"
            :label-placement="startTestBusinessFormLayout.labelPlacement"
            :label-width="startTestBusinessFormLayout.labelWidth"
            :show-actions="false"
            :show-feedback="true"
            :context="{ formAssets: startTestBusinessFormAssets }"
            :form-assets="startTestBusinessFormAssets"
          />
          <FlowFormCreateRenderer
            v-else-if="showStartTestModal && startTestFormSchema.length"
            ref="startTestFormRef"
            v-model="startTestFormData"
            :schema="startTestFormSchema"
          />
          <n-empty
            v-else
            size="small"
            :description="startTestBusinessFormActive
              ? '当前业务应用表单没有可渲染字段，将以空变量发起测试流程'
              : '当前模型没有可渲染的动态表单，将以空变量发起测试流程'"
          />
          <div v-if="startTestApproverNodes.length" class="start-test-approver-section">
            <div class="start-test-approver-title">
              发起人自选审批人
            </div>
            <div class="start-test-approver-tip">
              请为流程设计中标记为“发起人自选”的节点选择审批人。
            </div>
            <n-form label-placement="top">
              <n-form-item
                v-for="node in startTestApproverNodes"
                :key="node.nodeKey"
                :label="node.nodeName || node.nodeKey"
                required
              >
                <UserSelectPicker
                  v-model="startTestApproverSelections[node.nodeKey]"
                  v-model:label-value="startTestApproverLabels[node.nodeKey]"
                  :multiple="node.multiple !== false"
                  :title="`选择${node.nodeName || node.nodeKey}审批人`"
                  :placeholder="node.multiple === false ? '请选择一名审批人' : '请选择一名或多名审批人'"
                />
              </n-form-item>
            </n-form>
          </div>
        </div>

        <template #footer>
          <n-space justify="end">
            <n-button @click="showStartTestModal = false">
              取消
            </n-button>
            <n-button secondary @click="handleViewStarted">
              我发起的
            </n-button>
            <n-button type="primary" :loading="startTestLoading" @click="handleSubmitStartTest">
              发起测试
            </n-button>
          </n-space>
        </template>
      </NModal>
    </Teleport>

    <Teleport to="body">
      <NModal
        v-model:show="showDesignModal"
        :mask-closable="false"
        :close-on-esc="false"
        display-directive="if"
        class="flow-design-modal"
        style="width: 100vw; height: 100vh; max-width: none; margin: 0;"
      >
        <div class="flow-design-modal-shell">
          <FlowDesignPage
            v-if="currentDesignModelId"
            embedded
            :model-id="currentDesignModelId"
            :business-object-code="currentDesignBinding?.objectCode || ''"
            :business-object-name="currentDesignBinding?.objectName || ''"
            :application-id="currentDesignBinding?.applicationId || ''"
            :business-entry-route="currentDesignBinding?.entryRoute || ''"
            :code-app="isCodeAppBinding(currentDesignBinding)"
            @close="handleDesignModalClose"
            @saved="fetchData"
            @deployed="fetchData"
          />
        </div>
      </NModal>
    </Teleport>

    <VersionHistory
      v-if="showVersionHistory"
      :model-id="currentModelId"
      :current-version="currentModelVersion"
      @close="showVersionHistory = false"
      @refresh="fetchData"
    />
  </div>
</template>

<script setup>
import { CopyOutline, CreateOutline, PauseCircleOutline, PlayCircleOutline, TimeOutline, TrashOutline } from '@vicons/ionicons5'
import { NIcon, NModal, NTreeSelect } from 'naive-ui'
import { computed, defineAsyncComponent, h, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { businessFlowFormAssets, businessFlowModelBindings } from '@/api/business-app'
import flowApi from '@/api/flow'
import AiForm from '@/components/ai-form/AiForm.vue'
import UserSelectPicker from '@/components/common/UserSelectPicker.vue'
import FlowModelStats from '@/components/flow/FlowModelStats.vue'
import { useDict } from '@/composables/useDict'
import { collectInitiatorSelectSelections } from '@/utils/initiatorSelect'
import DesignerAsyncLoader from '@/views/app-center/components/designer/DesignerAsyncLoader.vue'
import { buildFlowCategoryTreeOptions, resolveFlowCategoryLabel, resolveFlowCategoryValue } from './utils/categoryOptions'

const router = useRouter()

const DEFAULT_TODO_DETAIL_URL_TEMPLATE = '/#/pages/todo-detail?taskId={taskId}'

const { dict, getLabel } = useDict('flow_model_status', 'flow_process_form_type', 'flow_designer_type')

const FlowDesignAsyncLoader = {
  name: 'FlowDesignAsyncLoader',
  setup() {
    return () => h(DesignerAsyncLoader, {
      title: '正在打开流程设计器',
      description: '首次加载需要准备流程画布与属性面板资源',
      overlay: true,
    })
  },
}

const FlowFormRendererAsyncLoader = {
  name: 'FlowFormRendererAsyncLoader',
  setup() {
    return () => h(DesignerAsyncLoader, {
      title: '正在加载测试表单',
      description: '首次打开需要准备表单渲染资源',
      overlay: true,
    })
  },
}

const FlowModalAsyncLoader = {
  name: 'FlowModalAsyncLoader',
  setup() {
    return () => h(NModal, {
      show: true,
      preset: 'card',
      title: '正在加载',
      style: 'width: min(560px, calc(100vw - 32px))',
      maskClosable: false,
    }, {
      default: () => h(DesignerAsyncLoader, {
        title: '正在加载版本历史',
        description: '首次打开需要准备流程图查看资源',
        overlay: true,
      }),
    })
  },
}

const FlowDesignPage = defineAsyncComponent({
  loader: () => import('./design.vue'),
  loadingComponent: FlowDesignAsyncLoader,
  delay: 120,
  suspensible: false,
})
const FlowFormCreateRenderer = defineAsyncComponent({
  loader: () => import('@/components/form-create/FlowFormCreateRenderer.vue'),
  loadingComponent: FlowFormRendererAsyncLoader,
  delay: 120,
  suspensible: false,
})
const VersionHistory = defineAsyncComponent({
  loader: () => import('./version.vue'),
  loadingComponent: FlowModalAsyncLoader,
  delay: 120,
  suspensible: false,
})

const statusOptions = computed(() => toNumberOptions(dict.value.flow_model_status))
const categoryTreeOptions = ref([])
const designerTypePresentation = {
  approval: {
    icon: 'i-material-symbols:approval-delegation-outline',
    desc: '适合人员审批、条件分支、抄送和表单权限配置。',
  },
  business: {
    icon: 'i-material-symbols:account-tree-outline',
    desc: '适合完整 BPMN 工作流、服务任务、事件和复杂业务编排。',
  },
}
const designerTypeOptions = computed(() => (dict.value.flow_designer_type || []).map(item => ({
  ...item,
  ...designerTypePresentation[item.value],
})))

function statusClass(status) {
  const cls = { 0: 'designing', 1: 'deployed', 2: 'suspended', 3: 'disabled' }
  return cls[status] || 'default'
}

function toNumberOptions(options = []) {
  return options.map(item => ({
    ...item,
    value: Number(item.value),
  }))
}

function formatDate(d) {
  if (!d)
    return ''
  return d.slice(0, 10)
}

function normalizeDesignerType(value) {
  return value === 'business' ? 'business' : 'approval'
}

function designerTypeLabel(value) {
  const normalizedValue = normalizeDesignerType(value)
  return designerTypeOptions.value.find(item => item.value === normalizedValue)?.label || normalizedValue
}

function designerTypeClass(value) {
  return normalizeDesignerType(value) === 'business' ? 'business' : 'approval'
}

function getCategoryDisplayName(row) {
  return row?.categoryName || resolveFlowCategoryLabel(row?.category, categoryTreeOptions.value, '')
}

function isCodeAppBinding(binding) {
  const value = binding?.codeApp
  if (value === true || value === 1)
    return true
  return ['true', '1', 'yes', 'y'].includes(String(value || '').trim().toLowerCase())
}

const modelActionLocks = ref(new Set())

function modelActionKey(row, action) {
  return `${String(row?.id || row?.modelKey || '')}:${action}`
}

function isModelActionLocked(row, action) {
  return modelActionLocks.value.has(modelActionKey(row, action))
}

function isModelActionBusy(row) {
  const modelId = String(row?.id || row?.modelKey || '')
  return [...modelActionLocks.value].some(key => key.startsWith(`${modelId}:`))
}

function lockModelAction(row, action) {
  const key = modelActionKey(row, action)
  if (modelActionLocks.value.has(key))
    return null
  modelActionLocks.value = new Set(modelActionLocks.value).add(key)
  return key
}

function unlockModelAction(key) {
  if (!key)
    return
  const next = new Set(modelActionLocks.value)
  next.delete(key)
  modelActionLocks.value = next
}

function getActionOptions(row) {
  const renderIcon = (icon) => {
    return () => h(NIcon, null, { default: () => h(icon) })
  }
  const opts = [
    { label: '编辑信息', key: 'edit', icon: renderIcon(CreateOutline), disabled: isModelActionBusy(row) },
    { label: '版本历史', key: 'versionHistory', icon: renderIcon(TimeOutline), disabled: isModelActionBusy(row) },
    { label: '复制模型', key: 'copy', icon: renderIcon(CopyOutline), disabled: isModelActionLocked(row, 'copy') },
  ]
  if (row.status === 1) {
    opts.splice(1, 0, { label: '发起测试', key: 'startTest', icon: renderIcon(PlayCircleOutline), disabled: isModelActionBusy(row) })
    opts.push({ label: '挂起', key: 'suspend', icon: renderIcon(PauseCircleOutline), disabled: isModelActionLocked(row, 'suspend') })
  }
  if (row.status === 2) {
    opts.push({ label: '激活', key: 'activate', icon: renderIcon(PlayCircleOutline), disabled: isModelActionLocked(row, 'activate') })
  }
  opts.push({ type: 'divider', key: 'd1' })
  opts.push({ label: '删除', key: 'delete', icon: renderIcon(TrashOutline), disabled: isModelActionLocked(row, 'delete'), props: { style: 'color: #d03050' } })
  return opts
}

function handleActionSelect(key, row) {
  const map = { edit: handleEdit, startTest: handleStartTest, copy: handleCopy, versionHistory: handleVersionHistory, suspend: handleSuspend, activate: handleActivate, delete: handleDelete }
  map[key]?.(row)
}

const queryParams = reactive({ modelName: '', category: null, status: null })
const activeStatsStatus = computed(() => queryParams.status ?? 'all')
const dataSource = ref([])
const loading = ref(false)
const sortMode = ref(false)
const sortSaving = ref(false)
const draggingModelId = ref('')
const pagination = reactive({ page: 1, pageSize: 12, itemCount: 0 })
const showVersionHistory = ref(false)
const currentModelId = ref('')
const currentModelVersion = ref(null)
const showDesignModal = ref(false)
const currentDesignModelId = ref('')
const currentDesignBinding = ref(null)
const showStartTestModal = ref(false)
const startTestLoading = ref(false)
const startTestFormRef = ref(null)
const startTestFormData = ref({})
const startTestFormSchema = ref([])
const startTestBusinessFormActive = ref(false)
const startTestBusinessFormLoading = ref(false)
const startTestBusinessFormAssets = ref([])
const startTestBusinessFormLayout = reactive({
  gridCols: 1,
  labelPlacement: 'left',
  labelWidth: '100',
})
const startTestApproverNodes = ref([])
const startTestApproverSelections = ref({})
const startTestApproverLabels = ref({})
const startTestPreflightDiagnostics = ref([])
const currentStartModel = ref(null)
const startTestTitle = computed(() => `发起测试 - ${currentStartModel.value?.modelName || '流程模型'}`)
const startTestAlert = computed(() => {
  if (!currentStartModel.value)
    return null
  if (currentStartModel.value.status !== 1) {
    return { type: 'warning', text: '当前模型尚未部署，部署后才能发起测试流程。' }
  }
  if (currentStartModel.value.formType === 'external') {
    return { type: 'warning', text: '当前模型使用外置表单，测试工具不会渲染外置页面，将以空变量发起。' }
  }
  if (startTestBusinessFormActive.value) {
    return { type: 'info', text: '测试发起会使用当前业务应用表单收集变量；测试流程不创建真实业务单据。' }
  }
  if (!startTestFormSchema.value.length) {
    return { type: 'info', text: '当前模型没有动态表单字段，发起后仅使用系统内置流程变量。' }
  }
  return { type: 'info', text: '这是流程模型测试入口，只用于验证流程流转；正式业务入口仍由低代码应用或业务页面承载。' }
})

const totalCount = ref(0)
const designingCount = ref(0)
const deployedCount = ref(0)
const suspendedCount = ref(0)
const disabledCount = ref(0)

async function fetchCategories() {
  try {
    const res = await flowApi.getCategoryTreeSelect(false)
    if (res.code === 200) {
      categoryTreeOptions.value = buildFlowCategoryTreeOptions(res.data || [])
    }
  }
  catch {
    console.error('加载分类失败')
  }
}

async function fetchData() {
  if (sortMode.value)
    return
  loading.value = true
  try {
    await Promise.all([
      fetchModelPage(),
      fetchModelStatistics(),
    ])
  }
  catch {
    console.error('加载模型列表失败')
  }
  finally {
    loading.value = false
  }
}

async function fetchModelPage() {
  const res = await flowApi.getModelPage({
    pageNum: pagination.page,
    pageSize: pagination.pageSize,
    ...queryParams,
  })
  if (res.code === 200) {
    const records = res.data?.records || []
    dataSource.value = await enrichModelBusinessBindings(records)
    pagination.itemCount = res.data?.total || 0
  }
}

async function fetchModelStatistics() {
  const res = await flowApi.getModelStatistics({
    modelName: queryParams.modelName,
    category: queryParams.category,
  })
  if (res.code === 200) {
    applyModelStatistics(res.data || {})
  }
}

function applyModelStatistics(data = {}) {
  totalCount.value = toCount(data.total)
  designingCount.value = toCount(data.designing)
  deployedCount.value = toCount(data.deployed)
  suspendedCount.value = toCount(data.suspended)
  disabledCount.value = toCount(data.disabled)
}

function toCount(value) {
  const count = Number(value)
  return Number.isFinite(count) ? count : 0
}

function handlePageSizeChange(v) {
  if (sortMode.value)
    return
  pagination.pageSize = v
  pagination.page = 1
  fetchData()
}

function handleSearch() {
  if (sortMode.value)
    return
  pagination.page = 1
  fetchData()
}

function handleStatusSelect() {
  if (sortMode.value)
    return
  pagination.page = 1
  fetchData()
}

function handleReset() {
  if (sortMode.value)
    return
  Object.assign(queryParams, { modelName: '', category: null, status: null })
  pagination.page = 1
  fetchData()
}

function handleDragStart(row) {
  if (!sortMode.value)
    return
  draggingModelId.value = String(row?.id || '')
}

function handleDrop(target) {
  if (!sortMode.value || !draggingModelId.value || String(target?.id || '') === draggingModelId.value)
    return
  const fromIndex = dataSource.value.findIndex(item => String(item.id) === draggingModelId.value)
  const toIndex = dataSource.value.findIndex(item => String(item.id) === String(target?.id || ''))
  if (fromIndex < 0 || toIndex < 0)
    return
  const next = [...dataSource.value]
  const [moved] = next.splice(fromIndex, 1)
  next.splice(toIndex, 0, moved)
  dataSource.value = next
  draggingModelId.value = ''
}

function cancelModelOrder() {
  sortMode.value = false
  draggingModelId.value = ''
  fetchModelPage()
}

async function saveModelOrder() {
  if (sortSaving.value || !dataSource.value.length)
    return
  sortSaving.value = true
  try {
    const baseOrder = (pagination.page - 1) * pagination.pageSize
    const res = await flowApi.sortModels({
      items: dataSource.value.map((item, index) => ({
        modelId: String(item.id),
        sortOrder: baseOrder + index,
      })),
    })
    if (res.code !== 200)
      throw new Error(res.message || '排序保存失败')
    window.$message?.success('模型排序已保存')
    sortMode.value = false
    draggingModelId.value = ''
    await fetchData()
  }
  catch (error) {
    window.$message?.error(error?.message || '排序保存失败，请刷新后重试')
  }
  finally {
    sortSaving.value = false
  }
}

function handleFilter(status) {
  if (status === 'all') {
    queryParams.status = null
  }
  else {
    queryParams.status = status
  }
  pagination.page = 1
  fetchData()
}

const showModal = ref(false)
const modalTitle = ref('新增模型')
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref(null)
const formData = reactive({
  id: '',
  modelName: '',
  modelKey: '',
  category: '',
  flowType: '',
  designerType: 'approval',
  formType: 'dynamic',
  description: '',
  notifyType: 'redis',
  webhookUrl: '',
  todoDetailUrlTemplate: DEFAULT_TODO_DETAIL_URL_TEMPLATE,
  notifyConfig: null,
  allowMultiReturn: false,
})
const rules = {
  modelName: { required: true, message: '请输入模型名称', trigger: 'blur' },
  category: { required: true, message: '请选择分类', trigger: 'change' },
  designerType: { required: true, message: '请选择流程模式', trigger: 'change' },
}

function handleAdd() {
  isEdit.value = false
  modalTitle.value = '新增模型'
  Object.assign(formData, { id: '', modelName: '', modelKey: generateModelKey(), category: '', flowType: '', designerType: 'approval', formType: 'dynamic', description: '', notifyType: 'redis', webhookUrl: '', todoDetailUrlTemplate: DEFAULT_TODO_DETAIL_URL_TEMPLATE, notifyConfig: null, allowMultiReturn: false })
  showModal.value = true
}

function generateModelKey() {
  const suffix = `${Date.now().toString(36)}${Math.random().toString(36).slice(2, 8)}`
  return `process_${suffix}`
}

function handleEdit(row) {
  isEdit.value = true
  modalTitle.value = '编辑模型'
  Object.assign(formData, row, {
    designerType: normalizeDesignerType(row.designerType),
    category: resolveFlowCategoryValue(row.category, categoryTreeOptions.value),
    notifyConfig: row.notifyConfig || null,
    todoDetailUrlTemplate: textValue(row.todoDetailUrlTemplate) || DEFAULT_TODO_DETAIL_URL_TEMPLATE,
    allowMultiReturn: row.allowMultiReturn === true || row.allowMultiReturn === 1 || String(row.allowMultiReturn) === '1',
  })
  showModal.value = true
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
    submitLoading.value = true
    const api = isEdit.value ? flowApi.updateModel : flowApi.createModel
    const res = await api(formData)
    if (res.code === 200) {
      window.$message?.success(isEdit.value ? '编辑成功' : '新增成功')
      showModal.value = false
      fetchData()
    }
    else {
      window.$message?.error(res.message || '操作失败')
    }
  }
  catch {
    console.error('提交失败')
  }
  finally {
    submitLoading.value = false
  }
}

async function enrichModelBusinessBindings(records = []) {
  await Promise.all(records.map(async (row) => {
    if (!row?.modelKey) {
      row.businessBindings = []
      return
    }
    try {
      const res = await businessFlowModelBindings(row.modelKey)
      const bindings = extractBusinessBindingRows(res)
      row.businessBindings = mergeModelBusinessBindings(bindings, buildModelFormBinding(row))
    }
    catch (error) {
      console.warn('[FlowModel] 加载业务绑定失败:', row.modelKey, error?.message || error)
      row.businessBindings = buildModelFormBinding(row)
    }
  }))
  return records
}

function mergeModelBusinessBindings(bindings = [], formBindings = []) {
  const normalizedBindings = Array.isArray(bindings) ? bindings : []
  const configuredBinding = Array.isArray(formBindings) ? formBindings[0] : null
  if (!configuredBinding)
    return normalizedBindings

  // 表单引用保存了用户实际选择的应用。对象被多个应用复用时，接口反查的
  // “主应用”不一定就是当前流程配置中的应用，因此优先展示表单引用中的应用。
  const hasApplicationContext = textValue(configuredBinding.applicationId)
    || textValue(configuredBinding.applicationName)
  if (!hasApplicationContext)
    return normalizedBindings.length ? normalizedBindings : formBindings

  const configuredObjectCode = textValue(configuredBinding.objectCode)
  return [
    configuredBinding,
    ...normalizedBindings.filter(item => textValue(item?.objectCode) !== configuredObjectCode),
  ]
}

function extractBusinessBindingRows(res) {
  if (Array.isArray(res))
    return res.filter(item => item && typeof item === 'object')
  if (res?.code !== undefined && res.code !== 200)
    return []
  const data = res?.data
  if (Array.isArray(data))
    return data.filter(item => item && typeof item === 'object')
  if (Array.isArray(data?.records))
    return data.records.filter(item => item && typeof item === 'object')
  if (Array.isArray(data?.list))
    return data.list.filter(item => item && typeof item === 'object')
  return []
}

function buildModelFormBinding(row = {}) {
  const reference = parseBusinessFormReference(row.formJson)
  const objectCode = textValue(reference.objectCode)
  const applicationId = textValue(reference.applicationId)
  const applicationName = textValue(reference.applicationName || reference.applicationCode)
  if (!objectCode && !applicationId)
    return []
  return [{
    flowModelKey: row.modelKey,
    bindingName: applicationName || textValue(reference.formName) || row.modelName,
    applicationId,
    applicationName,
    objectCode,
    objectName: textValue(reference.objectName || objectCode),
    entryRoute: textValue(reference.entryRoute),
  }]
}

function formatBusinessBindings(row = {}) {
  const bindings = Array.isArray(row.businessBindings) ? row.businessBindings : []
  if (!bindings.length)
    return '未绑定业务应用'
  const names = bindings.map((item) => {
    const application = textValue(item.applicationName || item.applicationCode)
    const object = textValue(item.objectName || item.objectCode)
    return application && object && application !== object
      ? `${application} · ${object}`
      : application || object || textValue(item.suiteName) || textValue(item.bindingName)
  }).filter(Boolean)
  if (!names.length)
    return '未绑定业务应用'
  return names.length > 2 ? `${names.slice(0, 2).join('、')} 等 ${names.length} 个业务应用` : names.join('、')
}

async function handleDesign(row) {
  if (!Array.isArray(row.businessBindings)) {
    const enriched = await enrichModelBusinessBindings([row])
    row.businessBindings = enriched[0]?.businessBindings || []
  }
  currentDesignBinding.value = row.businessBindings?.[0] || null
  currentDesignModelId.value = row.id
  showDesignModal.value = true
}

function handleDesignModalClose() {
  showDesignModal.value = false
  currentDesignModelId.value = ''
  currentDesignBinding.value = null
  fetchData()
}

function handleViewInstances(row) {
  router.push({ path: '/flow/monitor', query: { modelKey: row.modelKey } })
}

async function handleStartTest(row) {
  if (row.status !== 1) {
    window.$message?.warning('请先部署流程模型后再发起测试')
    return
  }
  const lockKey = lockModelAction(row, 'startTest')
  if (!lockKey)
    return
  currentStartModel.value = row
  startTestFormData.value = {}
  startTestFormSchema.value = []
  startTestBusinessFormActive.value = false
  startTestBusinessFormLoading.value = false
  startTestBusinessFormAssets.value = []
  resetStartTestBusinessFormLayout()
  startTestApproverNodes.value = []
  startTestApproverSelections.value = {}
  startTestApproverLabels.value = {}
  startTestPreflightDiagnostics.value = []
  showStartTestModal.value = true
  try {
    const res = await flowApi.getModelDetail(row.id)
    if (res.code === 200 && res.data) {
      currentStartModel.value = { ...row, ...res.data }
      if (isBusinessStartTestForm(currentStartModel.value)) {
        await loadStartTestBusinessForm(currentStartModel.value)
      }
      else {
        startTestFormSchema.value = parseFormSchema(res.data.formJson)
      }
    }
    const startConfig = await flowApi.getModelStartConfig(row.modelKey)
    if (startConfig.code === 200) {
      startTestApproverNodes.value = Array.isArray(startConfig.data?.initiatorSelectNodes)
        ? startConfig.data.initiatorSelectNodes
        : []
      startTestPreflightDiagnostics.value = Array.isArray(startConfig.data?.diagnostics)
        ? startConfig.data.diagnostics
        : []
    }
  }
  catch (error) {
    console.error('加载流程模型表单失败:', error)
    window.$message?.warning('加载流程模型表单失败，将以空变量发起')
  }
  finally {
    unlockModelAction(lockKey)
  }
}

function isBusinessStartTestForm(model = {}) {
  const formType = String(model.formType || '').trim().toLowerCase()
  if (['business', 'business_object_form', 'business_code_form'].includes(formType))
    return true
  const formRef = parseBusinessFormReference(model.formJson)
  const mode = String(formRef.formMode || formRef.type || '').trim().toUpperCase()
  if (mode === 'BUSINESS_OBJECT_FORM' || mode === 'BUSINESS_CODE_FORM')
    return true
  // 兼容早期已保存的业务引用：历史数据可能把 formType 写成 dynamic，
  // 但 formJson 仍保留 objectCode/formKey 两个业务资产身份。
  return Boolean(textValue(formRef.objectCode) && textValue(formRef.formKey))
}

async function loadStartTestBusinessForm(model = {}) {
  startTestBusinessFormActive.value = true
  startTestBusinessFormLoading.value = true
  try {
    const formRef = parseBusinessFormReference(model.formJson)
    const bindings = Array.isArray(model.businessBindings) ? model.businessBindings : []
    // 历史流程模型的 formJson 可能还保存旧 objectCode。优先使用同一应用下
    // 由业务绑定接口返回的规范编码，避免按旧编码查询不到应用页面表单资产。
    const formApplicationId = textValue(formRef.applicationId)
    const configuredObjectCode = textValue(formRef.objectCode)
    const fallbackBinding = bindings.find(binding =>
      textValue(binding?.objectCode)
      && (!configuredObjectCode || textValue(binding.objectCode) !== configuredObjectCode)
      && (!formApplicationId || textValue(binding?.applicationId) === formApplicationId),
    ) || bindings.find(binding => textValue(binding?.objectCode)) || null
    const formKey = textValue(formRef.formKey)
    const objectCode = textValue(fallbackBinding?.objectCode || formRef.objectCode)
    const applicationId = textValue(
      formRef.applicationId
      || fallbackBinding?.applicationId
      || resolveApplicationIdFromFormKey(formKey),
    )
    if (!objectCode) {
      window.$message?.warning('业务表单缺少业务对象信息，将以空变量发起测试')
      return
    }

    const res = await businessFlowFormAssets(objectCode, {
      includeInternal: true,
      applicationId: applicationId || undefined,
    })
    if (res.code !== undefined && res.code !== 200)
      throw new Error(res.message || '业务表单资产查询失败')
    const assetData = Array.isArray(res.data) ? res.data : res.data?.formAssets
    const assets = normalizeStartTestBusinessAssets(assetData || [])
    startTestBusinessFormAssets.value = assets
    const selectedAsset = assets.find(asset => asset.formKey === formKey)
      || assets[0]
      || null
    startTestFormSchema.value = normalizeStartTestBusinessFields(resolveStartTestBusinessFields(selectedAsset))
    applyStartTestBusinessFormLayout(selectedAsset)
  }
  catch (error) {
    console.warn('[FlowModel] 加载测试业务表单失败:', error?.message || error)
    startTestBusinessFormAssets.value = []
    startTestFormSchema.value = []
    window.$message?.warning('加载业务应用表单失败，将以空变量发起测试')
  }
  finally {
    startTestBusinessFormLoading.value = false
  }
}

function parseBusinessFormReference(formJson) {
  if (!formJson)
    return {}
  if (typeof formJson === 'object' && !Array.isArray(formJson)) {
    const nested = formJson.formRef && typeof formJson.formRef === 'object' ? formJson.formRef : {}
    return normalizeBusinessFormReference(nested, formJson)
  }
  try {
    const parsed = JSON.parse(formJson)
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed))
      return {}
    const nested = parsed.formRef && typeof parsed.formRef === 'object' ? parsed.formRef : {}
    return normalizeBusinessFormReference(nested, parsed)
  }
  catch {
    return {}
  }
}

function normalizeBusinessFormReference(nested = {}, root = {}) {
  const firstText = (...values) => values.map(textValue).find(Boolean) || ''
  return {
    ...nested,
    ...root,
    objectCode: firstText(nested.objectCode, root.objectCode),
    objectName: firstText(nested.objectName, root.objectName),
    applicationId: firstText(nested.applicationId, root.applicationId),
    applicationName: firstText(nested.applicationName, root.applicationName),
    formKey: firstText(nested.formKey, root.formKey),
    formName: firstText(nested.formName, root.formName),
    providerKey: firstText(nested.providerKey, root.providerKey),
    formMode: firstText(nested.formMode, nested.type, root.formMode, root.type),
    type: firstText(nested.type, nested.formMode, root.type, root.formMode),
  }
}

function resolveApplicationIdFromFormKey(formKey) {
  const match = textValue(formKey).match(/^app_([^_]+)_page_/i)
  return match ? textValue(match[1]) : ''
}

function normalizeStartTestBusinessAssets(assets = []) {
  return (Array.isArray(assets) ? assets : [])
    .map((asset) => {
      const schema = normalizeStartTestAssetSchema(
        hasStartTestAssetSchema(asset?.schema) ? asset.schema : asset?.formDesignerSchema,
      )
      const normalized = {
        ...asset,
        schema,
        formKey: textValue(asset.formKey || asset.key || asset.id || schema.formKey),
        formName: textValue(asset.formName || asset.name || asset.label || asset.formKey || schema.formName),
        fieldCatalog: Array.isArray(asset.fieldCatalog)
          ? asset.fieldCatalog
          : Array.isArray(asset.fields) ? asset.fields : [],
      }
      if (!normalized.fieldCatalog.length)
        normalized.fieldCatalog = resolveStartTestBusinessFields(normalized)
      return normalized
    })
    .filter(asset => asset.formKey)
}

/**
 * 业务表单资产在不同来源下的字段位置不完全一致：
 * - 业务对象页面通常返回 fieldCatalog/fields；
 * - 旧版本或代码 Provider 可能只返回 schema.components；
 * - 部分资产会保留空的 fieldCatalog，同时把字段放在 fields 中。
 * 统一在发起测试入口展开，避免空数组优先级导致表单被误判为无字段。
 */
function resolveStartTestBusinessFields(asset = null) {
  if (!asset || typeof asset !== 'object')
    return []
  const fieldCatalog = Array.isArray(asset.fieldCatalog) ? asset.fieldCatalog : []
  const fields = Array.isArray(asset.fields) ? asset.fields : []
  if (fieldCatalog.length)
    return fieldCatalog
  if (fields.length)
    return fields

  const schema = normalizeStartTestAssetSchema(
    hasStartTestAssetSchema(asset.schema) ? asset.schema : asset.formDesignerSchema,
  )
  const schemaFields = Array.isArray(schema.fieldCatalog) ? schema.fieldCatalog : []
  if (schemaFields.length)
    return schemaFields
  if (Array.isArray(schema.fields) && schema.fields.length)
    return schema.fields
  return flattenStartTestBusinessComponents(schema.components)
}

function normalizeStartTestAssetSchema(value) {
  if (Array.isArray(value))
    return { components: value }
  if (value && typeof value === 'object')
    return value
  if (typeof value === 'string') {
    try {
      const parsed = JSON.parse(value)
      return Array.isArray(parsed) ? { components: parsed } : (parsed && typeof parsed === 'object' ? parsed : {})
    }
    catch {
      return {}
    }
  }
  return {}
}

function hasStartTestAssetSchema(value) {
  if (Array.isArray(value))
    return value.length > 0
  if (value && typeof value === 'object')
    return Object.keys(value).length > 0
  return typeof value === 'string' && value.trim().length > 0
}

function flattenStartTestBusinessComponents(components, result = []) {
  if (!Array.isArray(components))
    return result
  components.forEach((component) => {
    if (!component || typeof component !== 'object')
      return
    const binding = component.fieldBinding && typeof component.fieldBinding === 'object'
      ? component.fieldBinding
      : {}
    const props = component.props && typeof component.props === 'object' ? component.props : {}
    const fieldCode = textValue(binding.fieldCode || component.field || props.field)
    // 布局节点没有字段编码，只递归其子节点。
    if (fieldCode) {
      result.push({
        ...component,
        ...props,
        field: fieldCode,
        fieldCode,
        componentType: component.componentType || component.componentKey || component.type,
        type: component.type || component.componentType || component.componentKey,
        label: component.label || props.label || props.title,
        required: component.required ?? component.validation?.required,
        validation: component.validation,
      })
    }
    flattenStartTestBusinessComponents(component.children, result)
  })
  return result
}

function normalizeStartTestBusinessFields(fields = []) {
  const seen = new Set()
  return (Array.isArray(fields) ? fields : [])
    .filter(field => field && field.visible !== false && field.formVisible !== false
      && field.internal !== true && field.systemField !== true)
    .map((field) => {
      const binding = field.fieldBinding && typeof field.fieldBinding === 'object' ? field.fieldBinding : {}
      const fieldProps = field.props && typeof field.props === 'object' ? field.props : {}
      const fieldCode = textValue(
        field.field || field.fieldCode || binding.fieldCode || fieldProps.field || field.code || field.name,
      )
      if (!fieldCode || seen.has(fieldCode))
        return null
      seen.add(fieldCode)
      const type = normalizeStartTestFieldType(
        field.type || field.componentType || field.componentKey || field.fieldType,
      )
      const props = { ...fieldProps }
      const options = Array.isArray(field.options)
        ? field.options
        : Array.isArray(props.options) ? props.options : undefined
      delete props.disabled
      delete props.readonly
      return {
        ...field,
        field: fieldCode,
        code: fieldCode,
        prop: fieldCode,
        label: textValue(field.label || field.fieldName || field.title || fieldProps.label || fieldProps.title || fieldCode),
        type,
        required: field.required === true || field.validation?.required === true,
        readonly: false,
        disabled: false,
        props,
        ...(options ? { options } : {}),
      }
    })
    .filter(Boolean)
}

function normalizeStartTestFieldType(value) {
  const raw = textValue(value)
  const normalized = raw.replace(/[-_\s]/g, '').toLowerCase()
  if (['inputnumber', 'integer', 'decimal', 'money', 'number'].includes(normalized))
    return 'number'
  if (['dict', 'dictselect', 'dictionary'].includes(normalized))
    return 'dictSelect'
  if (['textarea', 'textareafield'].includes(normalized))
    return 'textarea'
  if (['datepicker', 'date'].includes(normalized))
    return 'date'
  if (['datetimepicker', 'datetime'].includes(normalized))
    return 'datetime'
  if (['datetimerange'].includes(normalized))
    return 'datetimerange'
  if (['daterange'].includes(normalized))
    return 'daterange'
  if (['timepicker', 'time'].includes(normalized))
    return 'time'
  if (['select', 'radio', 'radiobutton', 'checkbox', 'switch', 'cascader', 'treeselect', 'orgtreeselect', 'orgselect', 'regiontreeselect', 'userselect', 'userpicker', 'recordselector', 'objectreference', 'slider', 'rate', 'color', 'colorpicker', 'upload', 'fileupload', 'imageupload', 'customselect', 'transfer', 'month', 'year', 'timerange', 'barcodescanner', 'text'].includes(normalized)) {
    const aliases = {
      radiobutton: 'radioButton',
      treeselect: 'treeSelect',
      orgtreeselect: 'orgTreeSelect',
      orgselect: 'orgTreeSelect',
      regiontreeselect: 'regionTreeSelect',
      userselect: 'userSelect',
      userpicker: 'userSelect',
      recordselector: 'recordSelector',
      objectreference: 'objectReference',
      fileupload: 'fileUpload',
      imageupload: 'imageUpload',
      colorpicker: 'color',
      barcodescanner: 'barcodeScanner',
      customselect: 'customSelect',
    }
    return aliases[normalized] || normalized
  }
  return 'input'
}

function applyStartTestBusinessFormLayout(asset) {
  const schema = asset?.schema && typeof asset.schema === 'object' ? asset.schema : {}
  const settings = schema.settings && typeof schema.settings === 'object' ? schema.settings : {}
  const layout = settings.layout && typeof settings.layout === 'object' ? settings.layout : {}
  const gridCols = Number(layout.gridCols || layout.gridColumns || settings.gridCols || settings.gridColumns)
  startTestBusinessFormLayout.gridCols = Number.isFinite(gridCols) && gridCols > 0 ? gridCols : 1
  startTestBusinessFormLayout.labelPlacement = ['left', 'top'].includes(layout.labelPlacement || settings.labelPlacement)
    ? (layout.labelPlacement || settings.labelPlacement)
    : 'left'
  startTestBusinessFormLayout.labelWidth = layout.labelWidth || settings.labelWidth || '100'
}

function resetStartTestBusinessFormLayout() {
  Object.assign(startTestBusinessFormLayout, { gridCols: 1, labelPlacement: 'left', labelWidth: '100' })
}

function textValue(value) {
  return String(Array.isArray(value) ? value[0] || '' : value || '').trim()
}

function parseFormSchema(formJson) {
  if (!formJson)
    return []
  if (Array.isArray(formJson))
    return formJson
  try {
    const parsed = JSON.parse(formJson)
    return Array.isArray(parsed) ? parsed : []
  }
  catch {
    return []
  }
}

async function handleSubmitStartTest() {
  if (!currentStartModel.value)
    return
  startTestLoading.value = true
  try {
    const variables = await collectStartTestFormData()
    if (startTestApproverNodes.value.length) {
      try {
        variables.PROCESS_START_USER = collectInitiatorSelectSelections(
          startTestApproverNodes.value,
          startTestApproverSelections.value,
        )
      }
      catch (error) {
        window.$message?.warning(error?.message || '请选择审批人')
        return
      }
    }
    const now = Date.now()
    const businessKey = `FLOW_TEST:${currentStartModel.value.modelKey}:${now}`
    const title = `${currentStartModel.value.modelName || currentStartModel.value.modelKey}-测试发起`
    const res = await flowApi.startProcess(currentStartModel.value.modelKey, {
      businessKey,
      businessType: 'FLOW_MODEL_TEST',
      title,
      variables: variables || {},
      testStart: true,
    })
    if (res.code === 200) {
      window.$message?.success('测试流程已发起')
      showStartTestModal.value = false
      router.push({ path: '/flow/started', query: { title } })
    }
    else {
      window.$message?.error(res.message || '发起测试失败')
    }
  }
  catch (error) {
    console.error('发起测试失败:', error)
    window.$message?.error(error?.message || '发起测试失败')
  }
  finally {
    startTestLoading.value = false
  }
}

async function collectStartTestFormData() {
  if (!startTestFormSchema.value.length)
    return {}
  if (startTestBusinessFormActive.value) {
    await startTestFormRef.value?.validate?.()
    return startTestFormRef.value?.getFormData?.() || { ...startTestFormData.value }
  }
  return (await startTestFormRef.value?.submit?.()) || {}
}

function handleViewStarted() {
  showStartTestModal.value = false
  router.push('/flow/started')
}

async function handleDeploy(row) {
  const lockKey = lockModelAction(row, 'deploy')
  if (!lockKey)
    return
  window.$dialog?.info({
    title: '确认部署',
    content: `确定要部署「${row.modelName}」吗？部署后流程将可以发起。`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await flowApi.deployModel(row.id)
        if (res.code === 200) {
          window.$message?.success('部署成功')
          await fetchData()
        }
        else {
          window.$message?.error(res.message || '部署失败')
        }
      }
      catch (error) {
        window.$message?.error(error?.message || error?.response?.data?.message || '部署失败')
      }
      finally {
        unlockModelAction(lockKey)
      }
    },
    onNegativeClick: () => unlockModelAction(lockKey),
    onClose: () => unlockModelAction(lockKey),
  })
}

async function handleCopy(row) {
  const lockKey = lockModelAction(row, 'copy')
  if (!lockKey)
    return
  window.$dialog?.info({
    title: '复制模型',
    content: `确定要复制「${row.modelName}」吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await flowApi.copyModel(row.id, `${row.modelName} - 副本`)
        if (res.code === 200) {
          window.$message?.success('复制成功')
          await fetchData()
        }
        else {
          window.$message?.error(res.message || '复制失败')
        }
      }
      catch (error) {
        window.$message?.error(error?.message || error?.response?.data?.message || '复制失败')
      }
      finally {
        unlockModelAction(lockKey)
      }
    },
    onNegativeClick: () => unlockModelAction(lockKey),
    onClose: () => unlockModelAction(lockKey),
  })
}

async function handleSuspend(row) {
  const lockKey = lockModelAction(row, 'suspend')
  if (!lockKey)
    return
  window.$dialog?.warning({
    title: '确认挂起',
    content: `挂起后，「${row.modelName}」相关的进行中流程实例将暂停，确定继续？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await flowApi.suspendModel(row.id)
        if (res.code === 200) {
          window.$message?.success('已挂起')
          await fetchData()
        }
        else {
          window.$message?.error(res.message || '挂起失败')
        }
      }
      catch (error) {
        window.$message?.error(error?.message || error?.response?.data?.message || '挂起失败')
      }
      finally {
        unlockModelAction(lockKey)
      }
    },
    onNegativeClick: () => unlockModelAction(lockKey),
    onClose: () => unlockModelAction(lockKey),
  })
}

async function handleActivate(row) {
  const lockKey = lockModelAction(row, 'activate')
  if (!lockKey)
    return
  try {
    const res = await flowApi.activateModel(row.id)
    if (res.code === 200) {
      window.$message?.success('已激活')
      await fetchData()
    }
    else {
      window.$message?.error(res.message || '激活失败')
    }
  }
  catch (error) {
    window.$message?.error(error?.message || error?.response?.data?.message || '激活失败')
  }
  finally {
    unlockModelAction(lockKey)
  }
}

async function handleDelete(row) {
  const lockKey = lockModelAction(row, 'delete')
  if (!lockKey)
    return
  window.$dialog?.error({
    title: '确认删除',
    content: `删除「${row.modelName}」前会校验该模型下是否存在流程实例或历史数据；如存在数据，系统会拒绝删除。删除后不可恢复，确定继续？`,
    positiveText: '确定删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await flowApi.deleteModel(row.id)
        if (res.code === 200) {
          window.$message?.success('删除成功')
          fetchData()
        }
        else {
          window.$message?.error(res.message || '删除失败')
        }
      }
      catch (error) {
        window.$message?.error(error?.message || error?.response?.data?.message || '删除失败')
      }
      finally {
        unlockModelAction(lockKey)
      }
    },
    onNegativeClick: () => unlockModelAction(lockKey),
    onClose: () => unlockModelAction(lockKey),
  })
}

function handleVersionHistory(row) {
  currentModelId.value = row.id
  currentModelVersion.value = row.version
  showVersionHistory.value = true
}

onMounted(() => {
  fetchCategories()
  fetchData()
})
</script>

<style scoped>
.start-test-approver-section {
  margin-top: 16px;
  padding: 14px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fafafa;
}

.start-test-approver-title {
  color: #1f2937;
  font-weight: 600;
}

.start-test-approver-tip {
  margin: 4px 0 12px;
  color: #6b7280;
  font-size: 12px;
}

.flow-page {
  display: flex;
  flex-direction: column;
  gap: 10px;
  height: 100%;
  min-height: 0;
  width: 100%;
  min-width: 0;
  box-sizing: border-box;
  overflow: hidden;
}

.model-key-form-item :deep(.n-form-item-blank) {
  display: block;
}

.model-key-form-item :deep(.n-input) {
  width: 100%;
}

.model-key-help {
  margin-top: 6px;
  color: #94a3b8;
  font-size: 12px;
  line-height: 1.5;
}

.page-header {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  padding: 12px 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-shrink: 0;
  min-width: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 120px;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-icon {
  width: 30px;
  height: 30px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #475569;
  font-size: 17px;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #111827;
  margin: 0;
  letter-spacing: 0;
}

.header-right {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  flex: 1;
  min-width: 0;
  flex-wrap: nowrap;
}

.search-input {
  width: 240px;
  max-width: 100%;
}

.category-select {
  width: 148px;
  max-width: 100%;
}

.status-select {
  width: 124px;
  max-width: 100%;
}

.toolbar-actions {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.model-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(100%, 300px), 1fr));
  align-content: start;
  gap: 10px;
  min-width: 0;
  padding: 0;
}

.model-card {
  position: relative;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 2px;
  cursor: default;
  transition:
    box-shadow 180ms ease,
    border-color 180ms ease,
    transform 180ms ease;
  display: flex;
  flex-direction: column;
  min-height: 0;
  width: 100%;
  box-sizing: border-box;
  overflow: hidden;
}

.model-card:hover {
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.08);
}

.model-card-sortable {
  cursor: grab;
  border-style: dashed;
}

.model-card-sortable:active {
  cursor: grabbing;
}

.card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 16px 12px;
  min-width: 0;
}

.card-title-block {
  min-width: 0;
}

.card-title-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  min-width: 0;
}

.card-title-icon-box {
  width: 32px;
  height: 32px;
  border-radius: 2px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: #eff6ff;
  border: 1px solid #dbeafe;
}

.card-title-icon {
  color: #2563eb;
  font-size: 16px;
}

.status-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 20px;
  padding: 0 6px;
  border-radius: 2px;
  font-size: 11px;
  font-weight: 600;
  line-height: 1;
  border: 1px solid transparent;
  white-space: nowrap;
  flex-shrink: 0;
  letter-spacing: 0.01em;
}

.status-tag.designing {
  background: #fffbeb;
  color: #92400e;
  border-color: #fde68a;
}

.status-tag.deployed {
  background: #ecfdf5;
  color: #047857;
  border-color: #bbf7d0;
}

.status-tag.suspended {
  background: #f8fafc;
  color: #475569;
  border-color: #e2e8f0;
}

.status-tag.disabled {
  background: #fef2f2;
  color: #b91c1c;
  border-color: #fecaca;
}

.card-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 0 16px 10px;
}

.card-title {
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 20px;
  letter-spacing: 0;
}

.card-sort-hint {
  margin-top: 2px;
  color: #2563eb;
  font-size: 11px;
}

.card-key {
  margin-top: 4px;
  font-size: 11px;
  color: #6b7280;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-variant-numeric: tabular-nums;
  letter-spacing: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-tags {
  display: flex;
  align-items: center;
  gap: 6px;
  min-height: 0;
  min-width: 0;
  flex-wrap: wrap;
}

.designer-type-badge {
  display: inline-flex;
  align-items: center;
  height: 20px;
  padding: 0 6px;
  border: 1px solid #e2e8f0;
  border-radius: 2px;
  font-size: 11px;
  font-weight: 500;
  line-height: 1;
  color: #475569;
  background: #fff;
}

.designer-type-badge.approval {
  color: #1e3a8a;
  background: #f8fbff;
  border-color: #dbe7ff;
}

.designer-type-badge.business {
  color: #065f46;
  background: #f7fcfa;
  border-color: #d7eee4;
}

.category-badge {
  display: inline-flex;
  max-width: 132px;
  height: 20px;
  align-items: center;
  padding: 0 6px;
  border: 1px solid #e2e8f0;
  border-radius: 2px;
  background: #fff;
  color: #475569;
  font-size: 11px;
  font-weight: 500;
  line-height: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-binding {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  min-width: 0;
  max-width: 100%;
  width: fit-content;
  color: #6b7280;
  font-size: 12px;
  line-height: 18px;
  padding: 6px 10px;
  border-radius: 2px;
  background: #f9fafb;
  border: 1px solid #f1f5f9;
}

.card-binding.empty {
  color: #94a3b8;
}

.card-binding span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-desc {
  font-size: 12px;
  color: #6b7280;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  line-height: 18px;
  min-height: 18px;
  text-wrap: pretty;
}

.card-metadata {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  flex: 1;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  border-top: 1px solid #f1f5f9;
  background: #f9fafb;
  min-height: 0;
  min-width: 0;
  gap: 12px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  color: #64748b;
  white-space: nowrap;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
  min-width: 0;
  flex-shrink: 0;
}

.card-action-link,
.card-more-action {
  border: 0;
  background: transparent;
  padding: 0;
  font: inherit;
  font-size: 12px;
  font-weight: 500;
  line-height: 1;
  color: #2563eb;
  cursor: pointer;
  transition:
    color 160ms ease,
    transform 160ms ease;
}

.card-action-link:hover,
.card-more-action:hover {
  color: #1d4ed8;
}

.card-action-link:active,
.card-more-action:active {
  transform: translateY(1px);
}

.card-action-separator {
  width: 1px;
  height: 12px;
  background: #d1d5db;
}

.card-more-action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #9ca3af;
}

.card-more-action i {
  font-size: 14px;
}

.model-workbench {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  min-width: 0;
  overflow: hidden;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  background: #fff;
}

.model-list-spin {
  display: flex;
  flex: 1;
  min-height: 0;
  min-width: 0;
  max-width: 100%;
}

.model-list-spin :deep(.n-spin-container) {
  flex: 1;
  min-height: 0;
  min-width: 0;
}

.model-list-body {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  min-width: 0;
  overflow-y: auto;
  overflow-x: hidden;
  scrollbar-gutter: stable;
  padding: 10px;
}

.model-list-loading {
  min-height: 280px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  width: 100%;
}

.empty-state {
  padding: 48px 0;
  background: #fff;
  border-radius: 4px;
  border: 1px solid #e5e7eb;
  width: 100%;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: 10px 14px;
  border-top: 1px solid #eef2f7;
  flex-shrink: 0;
  max-width: 100%;
  overflow-x: auto;
}

.pagination-wrapper :deep(.n-pagination) {
  min-width: max-content;
}

.designer-type-chooser {
  width: 100%;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.designer-type-chooser.disabled {
  opacity: 0.72;
}

.designer-type-option {
  min-width: 0;
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  background: #fff;
  color: #334155;
  text-align: left;
  cursor: pointer;
  transition:
    border-color 160ms ease,
    background 160ms ease,
    box-shadow 160ms ease;
}

.designer-type-option:hover:not(:disabled) {
  border-color: #93c5fd;
  background: #f8fbff;
}

.designer-type-option.active {
  border-color: #2563eb;
  background: #eff6ff;
  box-shadow: inset 0 0 0 1px rgba(37, 99, 235, 0.12);
}

.designer-type-option:disabled {
  cursor: not-allowed;
}

.designer-type-icon {
  width: 34px;
  height: 34px;
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: #f1f5f9;
  color: #2563eb;
}

.designer-type-icon i {
  font-size: 20px;
}

.designer-type-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.designer-type-title {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.designer-type-desc {
  font-size: 12px;
  line-height: 1.5;
  color: #64748b;
}

.start-test-modal {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-width: 0;
}

.start-test-summary {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  background: #f8fafc;
}

.summary-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #2563eb;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  font-size: 20px;
  flex-shrink: 0;
}

.summary-main {
  flex: 1;
  min-width: 0;
}

.summary-title {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.summary-subtitle {
  margin-top: 2px;
  font-size: 12px;
  color: #64748b;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.start-test-alert {
  border-radius: 8px;
}

.start-test-form-loading {
  min-height: 96px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #64748b;
  font-size: 13px;
}

.radio-group {
  margin-bottom: 8px;
}

.cursor-help {
  cursor: help;
  color: #64748b;
}

:deep(.n-spin-container),
:deep(.n-spin-content) {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.model-list-spin {
  display: block;
  min-width: 0;
  max-width: 100%;
}

.model-list-spin :deep(.n-spin-container) {
  min-width: 0;
}

:deep(.flow-design-modal) {
  width: 100vw !important;
  height: 100vh !important;
  max-width: none !important;
  margin: 0 !important;
  padding: 0 !important;
}

.flow-design-modal-shell {
  width: 100vw;
  height: 100vh;
  min-height: 0;
  display: flex;
  overflow: hidden;
  background: #f8fafc;
}

.flow-design-modal-shell :deep(.model-design-page) {
  flex: 1;
  width: 100%;
  height: 100%;
  min-height: 0;
}

@media (max-width: 1500px) {
  .model-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 1180px) {
  .page-header {
    align-items: stretch;
    flex-direction: column;
  }

  .model-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .header-right {
    justify-content: flex-start;
    flex-wrap: wrap;
  }

  .search-input {
    flex: 1 1 220px;
  }

  .category-select {
    flex: 0 1 160px;
  }

  .status-select {
    flex: 0 1 128px;
  }
}

@media (max-width: 760px) {
  .flow-design-modal {
    width: 100vw;
    height: 100vh;
  }

  .flow-page {
    padding: 12px;
  }

  .header-right {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    align-items: stretch;
  }

  .search-input,
  .category-select,
  .status-select {
    width: 100%;
    min-width: 0;
  }

  .toolbar-actions {
    grid-column: 1 / -1;
    justify-content: flex-end;
    width: 100%;
  }

  .model-grid {
    grid-template-columns: 1fr;
    min-width: 0;
  }

  .card-footer {
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
  }

  .card-actions {
    margin-left: auto;
    justify-content: flex-end;
    flex-wrap: wrap;
  }
}

@media (max-width: 480px) {
  .header-right {
    grid-template-columns: 1fr;
  }

  .toolbar-actions {
    justify-content: stretch;
  }

  .toolbar-actions :deep(.n-button) {
    flex: 1;
  }
}
</style>
