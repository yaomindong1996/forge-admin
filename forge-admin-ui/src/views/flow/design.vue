<template>
  <div class="model-design-page">
    <!-- 顶部工具栏 -->
    <div class="top-bar">
      <div class="top-bar-left">
        <n-button text @click="handleBack">
          <template #icon>
            <i class="i-material-symbols:arrow-back" />
          </template>
          返回
        </n-button>
        <n-divider vertical />
        <n-input
          v-model:value="modelInfo.modelName"
          placeholder="流程名称"
          style="width: 200px"
          :disabled="isReadonly"
        />
        <n-input
          v-model:value="modelInfo.modelKey"
          placeholder="流程Key"
          style="width: 150px"
          disabled
        />
        <NTag :type="statusTag.type" size="small">
          {{ statusTag.label }}
        </NTag>
      </div>
      <div class="top-bar-right">
        <n-button @click="handleOpenVersionHistory">
          <template #icon>
            <i class="i-material-symbols:history" />
          </template>
          更改记录
        </n-button>
        <n-button :type="aiPanelVisible ? 'primary' : 'default'" @click="toggleAiPanel">
          <template #icon>
            <i class="i-material-symbols:auto-awesome" />
          </template>
          AI生成
        </n-button>
        <n-button :loading="saving" @click="handleSaveDraft">
          <template #icon>
            <i class="i-material-symbols:save" />
          </template>
          保存草稿
        </n-button>
        <n-button type="primary" :loading="deploying" @click="handleDeploy">
          <template #icon>
            <i class="i-material-symbols:rocket-launch" />
          </template>
          发布部署
        </n-button>
      </div>
    </div>

    <!-- 主体内容 -->
    <div class="main-content">
      <!-- 中间区域（画布 + 顶部配置栏） -->
      <div class="center-area">
        <!-- 顶部横向配置栏 -->
        <div class="config-bar" :class="{ collapsed: configCollapsed }">
          <div class="config-bar-toggle" @click="configCollapsed = !configCollapsed">
            <i class="i-material-symbols:settings-outline-rounded config-toggle-icon" />
            <span class="config-bar-title">流程配置</span>
            <i class="i-material-symbols:expand-more config-toggle-arrow" :class="{ expanded: !configCollapsed }" />
          </div>
          <div v-show="!configCollapsed" class="config-bar-body">
            <div class="config-bar-row">
              <div class="config-field">
                <span class="config-field-label">流程分类</span>
                <n-tree-select
                  v-model:value="modelInfo.category"
                  :options="categoryTreeOptions"
                  placeholder="选择分类"
                  size="small"
                  style="width: 160px"
                  :default-expand-all="true"
                />
              </div>
              <div class="config-field">
                <span class="config-field-label">流程类型</span>
                <n-input v-model:value="modelInfo.flowType" placeholder="如：审批流程" size="small" style="width: 140px" />
              </div>
              <div class="config-field">
                <span class="config-field-label">表单类型</span>
                <n-select
                  v-model:value="modelInfo.formType"
                  :options="formTypeOptions"
                  size="small"
                  style="width: 120px"
                  @update:value="handleFormTypeChange"
                />
              </div>
              <template v-if="modelInfo.formType === 'dynamic'">
                <div class="config-field">
                  <span class="config-field-label">表单选择</span>
                  <n-select
                    v-model:value="modelInfo.formId"
                    :options="formOptions"
                    placeholder="选择已有表单"
                    clearable
                    size="small"
                    style="width: 160px"
                    @update:value="handleFormSelect"
                  />
                </div>
                <n-button size="small" type="primary" dashed @click="handleOpenFormDesigner">
                  <template #icon>
                    <i class="i-material-symbols:edit-document" />
                  </template>
                  {{ modelInfo.formJson ? '编辑表单' : '设计表单' }}
                </n-button>
              </template>
              <div class="config-field" style="flex: 1; min-width: 200px;">
                <span class="config-field-label">流程描述</span>
                <n-input v-model:value="modelInfo.description" placeholder="描述流程用途" size="small" />
              </div>
            </div>
          </div>
        </div>

        <!-- 流程设计器 -->
        <div class="designer-container">
          <FlowModeler
            ref="modelerRef"
            :xml="bpmnXml"
            @change="handleBpmnChange"
            @ready="handleModelerReady"
            @import-start="handleDiagramImportStart"
            @import-end="handleDiagramImportEnd"
          />
          <Transition name="designer-loading-fade">
            <div v-if="designerLoading" class="designer-loading-mask">
              <n-spin size="large">
                <template #description>
                  {{ pageLoading ? '正在加载流程数据...' : '正在渲染流程图...' }}
                </template>
              </n-spin>
            </div>
          </Transition>
        </div>
      </div>

      <!-- 右侧面板区域（抽屉覆盖式） -->
      <Transition name="drawer">
        <div v-if="dockedElement || aiPanelVisible" class="right-panels">
          <!-- 右侧面板 Tab 切换栏 -->
          <div class="right-panel-tabs">
            <button
              type="button"
              class="panel-tab" :class="{ active: rightActiveTab === 'properties' }"
              :disabled="!dockedElement"
              @click="rightActiveTab = 'properties'"
            >
              <i class="i-material-symbols:tune" />
              <span>属性</span>
            </button>
            <button
              type="button"
              class="panel-tab" :class="{ active: rightActiveTab === 'ai' }"
              @click="rightActiveTab = 'ai'; aiPanelVisible = true"
            >
              <i class="i-material-symbols:auto-awesome" />
              <span>AI助手</span>
              <span v-if="aiSending" class="panel-tab-dot" />
            </button>
            <button
              type="button"
              class="panel-tab-close"
              @click="handleCloseRightPanel"
            >
              <i class="i-material-symbols:close" />
            </button>
          </div>

          <!-- 属性面板 -->
          <div v-if="dockedElement && rightActiveTab === 'properties'" class="docked-properties-panel">
            <div class="docked-panel-header">
              <span class="docked-panel-title">{{ getElementTitle(dockedElement) }}</span>
            </div>
            <div class="docked-panel-body">
              <NodePropertiesPanel
                v-if="modelerInstance"
                :element="dockedElement"
                :modeler="modelerInstance"
                @update="handlePropertiesUpdate"
              />
            </div>
          </div>

          <!-- AI流程助手面板 -->
          <div v-if="aiPanelVisible && rightActiveTab === 'ai'" class="ai-flow-panel">
            <div class="ai-flow-header">
              <div class="ai-flow-header-info">
                <div class="ai-flow-title">
                  <i class="i-material-symbols:auto-awesome ai-flow-title-icon" />
                  AI流程助手
                </div>
                <div class="ai-flow-subtitle">
                  自然语言生成或修改当前流程图
                </div>
              </div>
            </div>

            <!-- 生成阶段进度条 -->
            <div v-if="aiSending && currentAiStageIndex >= 0" class="ai-stage-progress">
              <div
                v-for="(stage, idx) in aiStages"
                :key="stage.key"
                class="ai-stage-step" :class="[{
                  done: idx < currentAiStageIndex,
                  active: idx === currentAiStageIndex,
                }]"
              >
                <div class="ai-stage-step-dot" />
                <span class="ai-stage-step-label">{{ stage.label }}</span>
              </div>
            </div>

            <div ref="aiMessageListRef" class="ai-flow-body">
              <div v-if="aiMessages.length === 0" class="ai-flow-empty">
                <div class="ai-empty-hero">
                  <i class="i-material-symbols:auto-awesome ai-empty-icon" />
                  <div class="ai-empty-title">
                    描述你的流程需求
                  </div>
                  <div class="ai-empty-tip">
                    AI 将自动生成完整的 BPMN 流程图
                  </div>
                </div>
                <div class="ai-example-list">
                  <div
                    v-for="example in aiExamples"
                    :key="example.label"
                    class="ai-example"
                    @click="aiPrompt = example.text"
                  >
                    <div class="ai-example-icon">
                      <i class="i-material-symbols:add-circle-outline" />
                    </div>
                    <div class="ai-example-text">
                      <span class="ai-example-label">{{ example.label }}</span>
                      <span class="ai-example-desc">{{ example.text }}</span>
                    </div>
                  </div>
                </div>
              </div>
              <div v-else class="ai-message-list">
                <div
                  v-for="(msg, index) in aiMessages"
                  :key="index"
                  class="ai-message"
                  :class="msg.role"
                >
                  <div class="ai-message-avatar">
                    <div v-if="msg.role === 'user'" class="avatar user-avatar">
                      我
                    </div>
                    <div v-else class="avatar ai-avatar">
                      AI
                    </div>
                  </div>
                  <div class="ai-message-body">
                    <div v-if="msg.reasoning && msg.reasoning.trim()" class="reasoning-section">
                      <div class="reasoning-header" @click="toggleReasoning(index)">
                        <div class="reasoning-header-left">
                          <i class="i-material-symbols:psychology reasoning-icon" />
                          <span class="reasoning-label">思考过程</span>
                          <span v-if="msg.reasoningTime" class="reasoning-duration">用时 {{ msg.reasoningTime }}s</span>
                          <span v-else-if="msg.isReasoning" class="reasoning-duration thinking">思考中...</span>
                        </div>
                        <i class="i-material-symbols:expand-more reasoning-toggle" :class="{ expanded: expandedReasonings[index] }" />
                      </div>
                      <div v-if="expandedReasonings[index]" class="reasoning-content">
                        {{ msg.reasoning }}
                      </div>
                    </div>
                    <div class="ai-message-content">
                      <div class="ai-message-text">
                        {{ msg.content }}
                      </div>
                      <div v-if="msg.streaming && msg.isReasoning" class="message-typing">
                        <span /><span /><span />
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <div v-if="aiDraft" class="ai-draft">
                <div class="ai-draft-title">
                  <span>{{ aiDraft.modelName || modelInfo.modelName || '流程草稿' }}</span>
                  <NTag size="small" type="success">
                    可加载
                  </NTag>
                </div>
                <div class="ai-draft-desc">
                  {{ aiDraft.summary || aiDraft.description || '已生成 BPMN 流程配置' }}
                </div>
                <n-space>
                  <n-button size="small" type="primary" @click="handleApplyAiDraft">
                    <template #icon>
                      <i class="i-material-symbols:download-done" />
                    </template>
                    一键加载
                  </n-button>
                  <n-button size="small" @click="handlePreviewAiXml">
                    <template #icon>
                      <i class="i-material-symbols:code" />
                    </template>
                    预览XML
                  </n-button>
                </n-space>
              </div>
              <div ref="aiMessageEndRef" class="ai-message-end" />
            </div>

            <div class="ai-flow-input">
              <n-input
                v-model:value="aiPrompt"
                type="textarea"
                :autosize="{ minRows: 2, maxRows: 5 }"
                placeholder="例如：生成一个请假审批流程，3天以内直属上级审批，超过3天增加HR审批"
                :disabled="aiSending"
                @keydown.ctrl.enter.prevent="handleAiSend"
                @keydown.meta.enter.prevent="handleAiSend"
              />
              <div class="ai-flow-actions">
                <div class="ai-actions-left">
                  <n-popover
                    v-model:show="showModelPanel"
                    trigger="click"
                    placement="top-start"
                    :show-arrow="false"
                    :width="300"
                    raw
                  >
                    <template #trigger>
                      <button
                        type="button"
                        class="model-trigger" :class="[{ active: showModelPanel, empty: !aiModelId }]"
                        :title="aiModelId ? `${currentProviderLabel} · ${currentModelLabel}` : '请选择对话模型'"
                      >
                        <i class="i-material-symbols:sparkles model-trigger-icon" />
                        <span class="model-trigger-provider">{{ currentProviderLabel }}</span>
                        <span class="model-trigger-divider">·</span>
                        <span class="model-trigger-model">{{ currentModelLabel }}</span>
                        <i class="i-material-symbols:expand-more model-trigger-chevron" />
                      </button>
                    </template>

                    <div class="model-panel">
                      <div class="model-panel-section">
                        <div class="model-panel-label">
                          <span>供应商</span>
                          <span v-if="providerOptions.length === 0" class="model-panel-empty-tip">暂无可用供应商</span>
                        </div>
                        <n-select
                          v-model:value="aiProviderId"
                          :options="providerOptions"
                          placeholder="选择供应商"
                          size="small"
                          filterable
                        />
                      </div>
                      <div class="model-panel-section">
                        <div class="model-panel-label">
                          <span>模型</span>
                          <span v-if="modelOptions.length > 0" class="model-panel-count">{{ modelOptions.length }} 个</span>
                        </div>
                        <div v-if="modelOptions.length === 0" class="model-panel-empty">
                          {{ aiProviderId ? '该供应商暂无可用模型' : '请先选择供应商' }}
                        </div>
                        <div v-else class="model-list">
                          <div
                            v-for="m in modelOptions"
                            :key="m.value"
                            class="model-list-item" :class="[{ active: aiModelId === m.value }]"
                            @click="aiModelId = m.value; showModelPanel = false"
                          >
                            <div class="model-list-item-main">
                              <span class="model-list-item-name">{{ m.modelCode || m.label }}</span>
                              <span v-if="m.isDefault === '1'" class="model-tag">默认</span>
                            </div>
                            <div v-if="m.label && m.label !== m.modelCode" class="model-list-item-desc">
                              {{ m.label }}
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </n-popover>
                  <n-button size="small" text @click="handleNewAiSession">
                    新对话
                  </n-button>
                </div>
                <div class="ai-actions-right">
                  <n-button v-if="aiSending" type="error" size="small" @click="handleAbortAi">
                    <template #icon>
                      <i class="i-material-symbols:stop-circle" />
                    </template>
                    停止
                  </n-button>
                  <n-button v-else size="small" type="primary" :disabled="!aiPrompt.trim() || !aiModelId" @click="handleAiSend">
                    <template #icon>
                      <i class="i-material-symbols:send" />
                    </template>
                    发送
                  </n-button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </div>

    <!-- 表单设计器弹窗 -->
    <Teleport to="body">
      <n-modal
        v-model:show="showFormDesigner"
        preset="card"
        title="表单设计器"
        style="width: 95vw; height: 90vh"
        :mask-closable="false"
      >
        <div class="h-full -m-4">
          <FormDesigner
            ref="formDesignerRef"
            v-model="formSchema"
            @save="handleSaveFormSchema"
          />
        </div>
      </n-modal>
    </Teleport>

    <!-- 表单预览弹窗 -->
    <Teleport to="body">
      <n-modal
        v-model:show="showFormPreview"
        preset="card"
        title="表单预览"
        style="width: 800px"
      >
        <FormPreview
          v-if="formSchema.length > 0"
          :form-items="formSchema"
        />
      </n-modal>
    </Teleport>

    <Teleport to="body">
      <n-modal
        v-model:show="showAiXmlPreview"
        preset="card"
        title="AI生成的 BPMN XML"
        style="width: min(900px, 92vw); max-height: 80vh"
        content-style="overflow: hidden;"
      >
        <div class="xml-preview-container">
          <n-code
            :code="aiDraft?.bpmnXml || ''"
            language="xml"
            :show-line-numbers="true"
            :word-wrap="true"
          />
        </div>
      </n-modal>
    </Teleport>

    <VersionHistory
      v-if="showVersionHistory"
      :model-id="modelInfo.id"
      :current-version="modelInfo.version"
      @close="showVersionHistory = false"
      @refresh="handleVersionHistoryRefresh"
    />
  </div>
</template>

<script setup>
import { NTag, NTreeSelect } from 'naive-ui'
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { modelListByProvider, providerPage } from '@/api/ai'
import flowApi from '@/api/flow'
import { streamFlowGenerate } from '@/api/flow-generator'
import FlowModeler from '@/components/bpmn/FlowModeler.vue'
import NodePropertiesPanel from '@/components/bpmn/NodePropertiesPanel.vue'
import FormDesigner from '@/components/form-designer/FormDesigner.vue'
import FormPreview from '@/components/form-designer/FormPreview.vue'
import VersionHistory from './version.vue'

const route = useRoute()
const router = useRouter()
const message = window.$message

const saving = ref(false)
const deploying = ref(false)
const pageLoading = ref(true)
const diagramLoadingCount = ref(0)
const modelerRef = ref(null)
const bpmnXml = ref('')
const hasChanges = ref(false)
const aiPanelVisible = ref(false)
const aiSending = ref(false)
const aiPrompt = ref('')
const aiSessionId = ref('')
const aiMessages = ref([])
const aiDraft = ref(null)
const showAiXmlPreview = ref(false)
const showVersionHistory = ref(false)
const aiAbortController = ref(null)
const aiRawContent = ref('')
const aiReasoningContent = ref('')
const aiIsReasoningPhase = ref(false)
const aiReasoningStartTime = ref(null)
const aiReasoningEndTime = ref(null)
const aiCurrentStage = ref('')
const expandedReasonings = ref({})
const aiMessageListRef = ref(null)
const aiMessageEndRef = ref(null)
const showModelPanel = ref(false)

const aiProviderId = ref(null)
const aiModelId = ref(null)
const providerOptions = ref([])
const modelOptions = ref([])

const rightActiveTab = ref('ai')

const dockedElement = ref(null)
const modelerInstance = ref(null)
const configCollapsed = ref(true)

const modelInfo = reactive({
  id: '',
  modelName: '',
  modelKey: '',
  category: '',
  flowType: '',
  formType: 'dynamic',
  formId: null,
  formUrl: '',
  formJson: '',
  description: '',
  status: 0,
  version: 1,
  startListener: '',
  endListener: '',
})

const showFormDesigner = ref(false)
const formDesignerRef = ref(null)
const formSchema = ref([])
const formOptions = ref([])
const showFormPreview = ref(false)

const categoryOptions = ref([])
const categoryTreeOptions = ref([])

function buildTreeSelectOptions(treeData) {
  return treeData.map(item => ({
    label: item.categoryName,
    value: item.id,
    key: item.id,
    children: item.children && item.children.length > 0 ? buildTreeSelectOptions(item.children) : undefined,
  }))
}

const formTypeOptions = [
  { label: '动态表单', value: 'dynamic' },
  { label: '外置表单', value: 'external' },
  { label: '无表单', value: 'none' },
]

const aiExamples = [
  { label: '请假审批', text: '生成一个请假审批流程：3天以内直属上级审批，超过3天先直属上级再HR审批。' },
  { label: '报销审批', text: '把当前流程改成报销审批：金额超过5000增加财务经理审批，否则财务专员审批。' },
  { label: '驳回路径', text: '给当前流程增加驳回路径，审批不通过时回到发起人修改。' },
]

const aiStages = [
  { key: 'analyzing', label: '分析需求' },
  { key: 'generating', label: '生成流程' },
  { key: 'reasoning', label: '推理结构' },
  { key: 'complete', label: '完成' },
]

const currentAiStageIndex = computed(() => {
  return aiStages.findIndex(s => s.key === aiCurrentStage.value)
})

const statusTag = computed(() => {
  const statusMap = {
    0: { type: 'warning', label: '设计中' },
    1: { type: 'success', label: '已部署' },
    2: { type: 'default', label: '已禁用' },
  }
  return statusMap[modelInfo.status] || { type: 'default', label: '未知' }
})

const isReadonly = computed(() => modelInfo.status === 1)

const designerLoading = computed(() => pageLoading.value || diagramLoadingCount.value > 0)

const currentProviderLabel = computed(() => {
  const item = providerOptions.value.find(p => p.value === aiProviderId.value)
  return item?.label || '未选择供应商'
})

const currentModelLabel = computed(() => {
  const item = modelOptions.value.find(m => m.value === aiModelId.value)
  if (!item)
    return '请选择模型'
  return item.modelCode || item.label
})

watch(aiProviderId, async (val, old) => {
  if (val && val !== old) {
    await loadModelOptions(val, false)
  }
})

onMounted(async () => {
  try {
    await loadCategories()
    await loadForms()
    await loadProviderOptions()

    const modelId = route.query.id
    if (modelId) {
      await loadModel(modelId)
    }
    else {
      modelInfo.modelKey = `process_${Date.now()}`
      modelInfo.modelName = '新流程'
    }
  }
  finally {
    pageLoading.value = false
  }
})

onUnmounted(() => {
  if (aiAbortController.value) {
    aiAbortController.value.abort()
    aiAbortController.value = null
  }
})

async function loadProviderOptions(preserveSelection = false) {
  try {
    const res = await providerPage({ pageNum: 1, pageSize: 100 })
    const records = (res.data?.records || []).filter(item => item.status === undefined || item.status === '0')
    providerOptions.value = records.map(item => ({
      label: item.providerName,
      value: item.id,
    }))

    const hasSelectedProvider = providerOptions.value.some(item => item.value === aiProviderId.value)
    if (!hasSelectedProvider) {
      aiProviderId.value = null
    }

    if (!aiProviderId.value && records.length > 0) {
      const defaultProvider = records.find(item => item.isDefault === '1') || records[0]
      aiProviderId.value = defaultProvider?.id || null
    }

    await loadModelOptions(aiProviderId.value, preserveSelection)
  }
  catch (e) {
    console.warn('[FlowDesign] 加载供应商列表失败:', e.message)
    providerOptions.value = []
  }
}

async function loadModelOptions(selectedProviderId, preserveSelection = false) {
  if (!selectedProviderId) {
    modelOptions.value = []
    aiModelId.value = null
    return
  }
  try {
    const res = await modelListByProvider(selectedProviderId)
    const models = (res.data || []).filter(item => item.status === undefined || item.status === '0')
    modelOptions.value = models.map(item => ({
      label: item.modelName ? `${item.modelName} (${item.modelId})` : item.modelId,
      value: item.modelId,
      modelCode: item.modelId,
      maxTokens: item.maxTokens,
      isDefault: item.isDefault,
    }))

    const hasSelectedModel = modelOptions.value.some(item => item.value === aiModelId.value)
    if (!preserveSelection || !hasSelectedModel) {
      const defaultModel = modelOptions.value.find(item => item.isDefault === '1') || modelOptions.value[0]
      aiModelId.value = defaultModel?.value || null
    }
  }
  catch (e) {
    console.warn('[FlowDesign] 加载模型列表失败:', e.message)
    modelOptions.value = []
    aiModelId.value = null
  }
}

async function loadCategories() {
  try {
    const res = await flowApi.getCategoryTreeSelect(false)
    if (res.code === 200) {
      categoryTreeOptions.value = buildTreeSelectOptions(res.data || [])
      categoryOptions.value = (res.data || []).map(item => ({
        label: item.categoryName,
        value: item.id,
      }))
    }
  }
  catch (error) {
    console.error('加载分类失败:', error)
  }
}

async function loadForms() {
  try {
    const res = await flowApi.getEnabledForms()
    if (res.code === 200) {
      formOptions.value = (res.data || []).map(item => ({
        label: item.formName,
        value: item.id,
      }))
    }
  }
  catch (error) {
    console.error('加载表单列表失败:', error)
  }
}

async function loadModel(id) {
  try {
    const res = await flowApi.getModelDetail(id)
    if (res.code === 200 && res.data) {
      Object.assign(modelInfo, res.data)
      bpmnXml.value = res.data.bpmnXml || ''

      if (res.data.formJson) {
        try {
          formSchema.value = JSON.parse(res.data.formJson)
        }
        catch (e) {
          console.error('解析表单配置失败:', e)
          formSchema.value = []
        }
      }
    }
  }
  catch (error) {
    console.error('加载模型失败:', error)
  }
}

function handleOpenVersionHistory() {
  if (!modelInfo.id) {
    window.$message?.warning('请先保存模型后查看更改记录')
    return
  }
  showVersionHistory.value = true
}

async function handleVersionHistoryRefresh() {
  if (!modelInfo.id)
    return

  await loadModel(modelInfo.id)
  await nextTick()
  if (bpmnXml.value) {
    await modelerRef.value?.setXML(bpmnXml.value)
  }
  hasChanges.value = false
}

function handleFormTypeChange(value) {
  if (value !== 'dynamic') {
    formSchema.value = []
    modelInfo.formId = null
    modelInfo.formJson = ''
  }
  if (value !== 'external') {
    modelInfo.formUrl = ''
  }
}

async function handleFormSelect(formId) {
  if (!formId) {
    formSchema.value = []
    modelInfo.formJson = ''
    return
  }

  try {
    const res = await flowApi.getFormById(formId)
    if (res.code === 200 && res.data) {
      if (res.data.formSchema) {
        formSchema.value = JSON.parse(res.data.formSchema)
        modelInfo.formJson = res.data.formSchema
      }
    }
  }
  catch (error) {
    console.error('加载表单失败:', error)
    message.error('加载表单失败')
  }
}

function handleOpenFormDesigner() {
  showFormDesigner.value = true
}

function handleSaveFormSchema(schema) {
  formSchema.value = schema
  modelInfo.formJson = JSON.stringify(schema)
  showFormDesigner.value = false
  hasChanges.value = true
  message.success('表单设计已保存')
}

function toggleAiPanel() {
  aiPanelVisible.value = !aiPanelVisible.value
  if (aiPanelVisible.value) {
    rightActiveTab.value = 'ai'
  }
}

function handleCloseRightPanel() {
  aiPanelVisible.value = false
  dockedElement.value = null
  if (modelerInstance.value) {
    const selection = modelerInstance.value.get('selection')
    if (selection)
      selection.select(null)
  }
}

async function handleAiSend() {
  const prompt = aiPrompt.value.trim()
  if (!prompt) {
    message.warning('请输入流程需求')
    return
  }
  if (aiSending.value) {
    return
  }
  if (!aiProviderId.value) {
    message.warning('请选择AI供应商')
    return
  }
  if (!aiModelId.value) {
    message.warning('请选择对话模型')
    return
  }

  try {
    aiSending.value = true
    aiDraft.value = null
    aiRawContent.value = ''
    aiReasoningContent.value = ''
    aiIsReasoningPhase.value = false
    aiReasoningStartTime.value = null
    aiReasoningEndTime.value = null
    aiCurrentStage.value = 'analyzing'

    if (!aiSessionId.value) {
      aiSessionId.value = `flow_${Date.now()}_${Math.random().toString(36).slice(2, 10)}`
    }
    aiMessages.value.push({ role: 'user', content: prompt })
    aiMessages.value.push({
      role: 'assistant',
      content: '正在分析流程需求...',
      streaming: true,
      isReasoning: false,
      reasoning: '',
      reasoningTime: null,
    })
    aiPrompt.value = ''
    scrollAiToBottom()

    const currentXml = await modelerRef.value?.getXML(true)

    aiAbortController.value = streamFlowGenerate(
      {
        sessionId: aiSessionId.value,
        description: prompt,
        providerId: aiProviderId.value,
        modelId: aiModelId.value,
        flowModelId: modelInfo.id || modelInfo.modelKey || '',
        aiModelName: currentModelLabel.value,
        modelKey: modelInfo.modelKey || `process_${Date.now()}`,
        modelName: modelInfo.modelName || '新流程',
        category: modelInfo.category || '',
        flowType: modelInfo.flowType || '',
        formType: modelInfo.formType || 'dynamic',
        currentBpmnXml: currentXml || bpmnXml.value || '',
        currentFormJson: modelInfo.formJson || '',
        temperature: 0.2,
        maxTokens: 12000,
      },
      handleFlowSSEChunk,
      handleFlowSSEComplete,
      handleFlowSSEError,
    )
  }
  catch (error) {
    console.error('AI生成流程失败:', error)
    message.error(error.message || 'AI生成流程失败')
    aiSending.value = false
    aiCurrentStage.value = ''
    updateLastAiAssistantMessage('AI生成流程失败', { reasoning: aiReasoningContent.value, isReasoning: false, reasoningTime: null })
  }
}

function handleAbortAi() {
  if (aiAbortController.value) {
    aiAbortController.value.abort()
    aiAbortController.value = null
  }
  aiSending.value = false
  aiCurrentStage.value = ''
  updateLastAiAssistantMessage('已停止生成', { reasoning: aiReasoningContent.value, isReasoning: false, reasoningTime: null })
}

function handleFlowSSEChunk({ event, data }) {
  if (event === 'progress') {
    aiCurrentStage.value = 'generating'
    updateLastAiAssistantMessage(data.message || '正在生成流程配置...', { reasoning: aiReasoningContent.value, isReasoning: aiIsReasoningPhase.value, reasoningTime: null })
    scrollAiToBottom()
    return
  }

  if (event !== 'chunk') {
    return
  }

  const chunkContent = data.content || ''
  if (!chunkContent) {
    return
  }

  if (chunkContent.includes('==================== 思考过程 ====================')) {
    aiIsReasoningPhase.value = true
    aiCurrentStage.value = 'reasoning'
    aiReasoningStartTime.value = Date.now()
    aiReasoningEndTime.value = null
    aiReasoningContent.value = ''
    const afterDelimiter = chunkContent.split('==================== 思考过程 ====================')[1] || ''
    aiReasoningContent.value += afterDelimiter.replace('\n', '')
    updateLastAiAssistantMessage('', { reasoning: aiReasoningContent.value, isReasoning: true, reasoningTime: null })
  }
  else if (chunkContent.includes('==================== 完整回复 ====================')) {
    aiIsReasoningPhase.value = false
    aiCurrentStage.value = 'generating'
    aiReasoningEndTime.value = Date.now()
    const reasoningTime = aiReasoningStartTime.value ? Math.round((aiReasoningEndTime.value - aiReasoningStartTime.value) / 1000) : null
    const afterDelimiter = chunkContent.split('==================== 完整回复 ====================')[1] || ''
    aiRawContent.value += afterDelimiter.replace('\n', '')
    updateLastAiAssistantMessage(aiRawContent.value || '正在生成 BPMN XML...', { reasoning: aiReasoningContent.value, isReasoning: false, reasoningTime })
  }
  else if (aiIsReasoningPhase.value) {
    aiReasoningContent.value += chunkContent
    updateLastAiAssistantMessage('', { reasoning: aiReasoningContent.value, isReasoning: true, reasoningTime: null })
  }
  else {
    aiRawContent.value += chunkContent
    updateLastAiAssistantMessage(aiRawContent.value || '正在生成 BPMN XML...', { reasoning: aiReasoningContent.value, isReasoning: false })
  }
  scrollAiToBottom()
}

function handleFlowSSEComplete(data) {
  aiSending.value = false
  aiAbortController.value = null
  aiCurrentStage.value = 'complete'

  if (data?.sessionId) {
    aiSessionId.value = data.sessionId
  }

  const reasoningTime = aiReasoningStartTime.value ? Math.round((Date.now() - aiReasoningStartTime.value) / 1000) : null

  const parsed = parseAiFlowResponse(aiRawContent.value)
  if (parsed?.bpmnXml) {
    aiDraft.value = normalizeAiFlowDraft(parsed)
    updateLastAiAssistantMessage(aiDraft.value.summary || aiDraft.value.description || '已生成流程配置，可一键加载到画布。', { reasoning: aiReasoningContent.value, isReasoning: false, reasoningTime })
  }
  else {
    updateLastAiAssistantMessage(aiRawContent.value || 'AI未返回可解析的流程配置', { reasoning: aiReasoningContent.value, isReasoning: false, reasoningTime })
    message.warning('AI响应未解析到 BPMN XML，请继续追问或调整需求')
  }
  scrollAiToBottom()
}

function handleFlowSSEError(errorMessage) {
  aiSending.value = false
  aiAbortController.value = null
  aiCurrentStage.value = ''
  const reasoningTime = aiReasoningStartTime.value ? Math.round((Date.now() - aiReasoningStartTime.value) / 1000) : null
  updateLastAiAssistantMessage(`生成失败: ${errorMessage}`, { reasoning: aiReasoningContent.value, isReasoning: false, reasoningTime })
  message.error(errorMessage || 'AI生成流程失败')
}

function updateLastAiAssistantMessage(content, reasoningData = null) {
  const last = aiMessages.value[aiMessages.value.length - 1]
  if (last && last.role === 'assistant') {
    last.content = content
    last.streaming = aiSending.value
    if (reasoningData) {
      last.reasoning = reasoningData.reasoning
      last.isReasoning = reasoningData.isReasoning
      if (reasoningData.reasoningTime !== undefined && reasoningData.reasoningTime !== null) {
        last.reasoningTime = reasoningData.reasoningTime
      }
    }
  }
  else {
    aiMessages.value.push({
      role: 'assistant',
      content,
      streaming: aiSending.value,
      isReasoning: reasoningData?.isReasoning || false,
      reasoning: reasoningData?.reasoning || '',
      reasoningTime: reasoningData?.reasoningTime || null,
    })
  }
}

function toggleReasoning(idx) {
  expandedReasonings.value[idx] = !expandedReasonings.value[idx]
}

function scrollAiToBottom() {
  nextTick(() => {
    requestAnimationFrame(() => {
      if (aiMessageEndRef.value) {
        aiMessageEndRef.value.scrollIntoView({ block: 'end' })
      }
      if (aiMessageListRef.value) {
        aiMessageListRef.value.scrollTop = aiMessageListRef.value.scrollHeight
      }
    })
  })
}

watch(() => aiMessages.value.length, () => scrollAiToBottom())
watch(() => [aiRawContent.value, aiReasoningContent.value, aiDraft.value], () => scrollAiToBottom(), { flush: 'post' })
watch(aiSending, (val) => {
  if (val)
    scrollAiToBottom()
})

async function handleApplyAiDraft() {
  if (!aiDraft.value?.bpmnXml) {
    message.warning('暂无可加载的流程配置')
    return
  }

  try {
    if (aiDraft.value.modelName) {
      modelInfo.modelName = aiDraft.value.modelName
    }
    if (!modelInfo.id && aiDraft.value.modelKey) {
      modelInfo.modelKey = aiDraft.value.modelKey
    }
    if (aiDraft.value.category) {
      modelInfo.category = aiDraft.value.category
    }
    if (aiDraft.value.flowType) {
      modelInfo.flowType = aiDraft.value.flowType
    }
    if (aiDraft.value.description) {
      modelInfo.description = aiDraft.value.description
    }
    if (aiDraft.value.formType) {
      modelInfo.formType = aiDraft.value.formType
    }
    if (aiDraft.value.formJson) {
      modelInfo.formJson = typeof aiDraft.value.formJson === 'string'
        ? aiDraft.value.formJson
        : JSON.stringify(aiDraft.value.formJson)
      try {
        formSchema.value = JSON.parse(modelInfo.formJson)
      }
      catch {
        formSchema.value = []
      }
    }

    if (!aiDraft.value.bpmnXml.includes('BPMNDiagram') && !aiDraft.value.bpmnXml.includes('bpmndi:BPMNDiagram')) {
      throw new Error('BPMN XML 缺少图形坐标信息')
    }

    bpmnXml.value = aiDraft.value.bpmnXml
    await modelerRef.value?.setXML(aiDraft.value.bpmnXml)
    hasChanges.value = true
    message.success('AI流程配置已加载到画布')
  }
  catch (error) {
    console.error('加载AI流程配置失败:', error)
    message.error(`加载失败: ${error.message}`)
  }
}

function handleNewAiSession() {
  if (aiAbortController.value) {
    aiAbortController.value.abort()
    aiAbortController.value = null
  }
  aiSending.value = false
  aiCurrentStage.value = ''
  aiSessionId.value = ''
  aiPrompt.value = ''
  aiMessages.value = []
  aiDraft.value = null
  aiRawContent.value = ''
  aiReasoningContent.value = ''
  aiIsReasoningPhase.value = false
  aiReasoningStartTime.value = null
  aiReasoningEndTime.value = null
  expandedReasonings.value = {}
}

function handlePreviewAiXml() {
  showAiXmlPreview.value = true
}

function normalizeAiFlowDraft(draft) {
  const modelKey = draft.modelKey || modelInfo.modelKey || `process_${Date.now()}`
  const bpmnXml = ensureProcessId(draft.bpmnXml, modelKey)
  return {
    ...draft,
    modelKey,
    modelName: draft.modelName || modelInfo.modelName || 'AI生成流程',
    bpmnXml: repairBpmnXml(bpmnXml, modelKey),
  }
}

function parseAiFlowResponse(content) {
  if (!content)
    return null

  const cleaned = stripCodeFence(content)
  const jsonCandidates = [
    cleaned,
    extractJsonObject(cleaned),
  ].filter(Boolean)

  for (const candidate of jsonCandidates) {
    try {
      return JSON.parse(candidate)
    }
    catch {
      // try next candidate
    }
  }

  const xml = extractBpmnXml(cleaned)
  if (xml) {
    return {
      bpmnXml: xml,
      summary: '已从AI响应中提取 BPMN XML',
    }
  }
  return null
}

function stripCodeFence(content) {
  let text = content.trim()
  if (text.startsWith('```json')) {
    text = text.slice(7)
  }
  else if (text.startsWith('```xml')) {
    text = text.slice(6)
  }
  else if (text.startsWith('```')) {
    text = text.slice(3)
  }
  if (text.endsWith('```')) {
    text = text.slice(0, -3)
  }
  return text.trim()
}

function extractJsonObject(text) {
  const start = text.indexOf('{')
  const end = text.lastIndexOf('}')
  if (start >= 0 && end > start) {
    return text.slice(start, end + 1)
  }
  return ''
}

function extractBpmnXml(text) {
  const match = text.match(/<\?xml[\s\S]*<\/(?:bpmn:)?definitions>/)
  return match ? match[0].trim() : ''
}

function ensureProcessId(xml, modelKey) {
  if (!xml || !modelKey)
    return xml
  return xml
    .replace(/(<bpmn:process\s[^>]*id=")[^"]+(")/, `$1${modelKey}$2`)
    .replace(/(<bpmndi:BPMNPlane\s[^>]*bpmnElement=")[^"]+(")/, `$1${modelKey}$2`)
}

function repairBpmnXml(xml, modelKey) {
  if (!xml)
    return xml
  const parseError = getXmlParseError(xml)
  if (!parseError && xml.includes('bpmndi:BPMNDiagram'))
    return xml

  const repaired = rebuildDiagramInfo(xml, modelKey)
  if (!repaired)
    return xml

  const repairedError = getXmlParseError(repaired)
  if (repairedError) {
    console.warn('[FlowDesign] BPMN XML 修复后仍不可解析:', repairedError)
    return xml
  }
  console.warn('[FlowDesign] AI 返回的 BPMNDI 坐标信息无效，已重新生成图形信息:', parseError || '缺少 BPMNDiagram')
  return repaired
}

function getXmlParseError(xml) {
  try {
    const doc = new DOMParser().parseFromString(xml, 'application/xml')
    const error = doc.querySelector('parsererror')
    return error?.textContent || ''
  }
  catch (error) {
    return error.message
  }
}

function rebuildDiagramInfo(xml, modelKey) {
  const semanticXml = stripDiagramInfo(xml)
  const semanticError = getXmlParseError(semanticXml)
  if (semanticError) {
    console.warn('[FlowDesign] BPMN 语义 XML 不可解析，无法重建图形信息:', semanticError)
    return ''
  }

  const doc = new DOMParser().parseFromString(semanticXml, 'application/xml')
  const processEl = Array.from(doc.getElementsByTagName('*')).find(el => el.localName === 'process')
  if (!processEl)
    return ''

  const processId = processEl.getAttribute('id') || modelKey
  const nodes = []
  const flows = []

  Array.from(processEl.children).forEach((el) => {
    const id = el.getAttribute('id')
    if (!id)
      return
    if (el.localName === 'sequenceFlow') {
      flows.push({
        id,
        sourceRef: el.getAttribute('sourceRef'),
        targetRef: el.getAttribute('targetRef'),
      })
      return
    }
    if (isBpmnFlowNode(el.localName)) {
      nodes.push({
        id,
        type: el.localName,
      })
    }
  })

  if (nodes.length === 0)
    return ''

  const bounds = layoutBpmnNodes(nodes, flows)
  const diagramXml = buildDiagramXml(processId, nodes, flows, bounds)
  const normalizedSemanticXml = ensureDiagramNamespaces(semanticXml)
  return normalizedSemanticXml.replace(/<\/(?:bpmn:)?definitions>\s*$/i, `${diagramXml}\n</bpmn:definitions>`)
}

function stripDiagramInfo(xml) {
  const start = xml.search(/<bpmndi:BPMNDiagram\b/)
  if (start < 0)
    return xml

  const closeTag = '</bpmndi:BPMNDiagram>'
  const end = xml.indexOf(closeTag, start)
  if (end >= 0) {
    return `${xml.slice(0, start)}${xml.slice(end + closeTag.length)}`
  }

  const definitionsEnd = xml.search(/<\/(?:bpmn:)?definitions>\s*$/i)
  if (definitionsEnd >= 0) {
    return `${xml.slice(0, start)}${xml.slice(definitionsEnd)}`
  }
  return xml.slice(0, start)
}

function ensureDiagramNamespaces(xml) {
  const namespaces = [
    ['bpmndi', 'http://www.omg.org/spec/BPMN/20100524/DI'],
    ['dc', 'http://www.omg.org/spec/DD/20100524/DC'],
    ['di', 'http://www.omg.org/spec/DD/20100524/DI'],
  ]
  return namespaces.reduce((text, [prefix, uri]) => {
    if (text.includes(`xmlns:${prefix}=`))
      return text
    return text.replace(/<bpmn:definitions\b/, `<bpmn:definitions xmlns:${prefix}="${uri}"`)
  }, xml)
}

function isBpmnFlowNode(localName) {
  return localName.endsWith('Event')
    || localName.endsWith('Task')
    || localName.endsWith('Gateway')
    || ['subProcess', 'callActivity'].includes(localName)
}

function layoutBpmnNodes(nodes, flows) {
  const nodeMap = new Map(nodes.map(node => [node.id, node]))
  const rankMap = new Map()
  const outgoing = new Map()
  const incomingCount = new Map(nodes.map(node => [node.id, 0]))

  flows.forEach((flow) => {
    if (!nodeMap.has(flow.sourceRef) || !nodeMap.has(flow.targetRef))
      return
    if (!outgoing.has(flow.sourceRef))
      outgoing.set(flow.sourceRef, [])
    outgoing.get(flow.sourceRef).push(flow.targetRef)
    incomingCount.set(flow.targetRef, (incomingCount.get(flow.targetRef) || 0) + 1)
  })

  const startNodes = nodes.filter(node => node.type === 'startEvent' || incomingCount.get(node.id) === 0)
  const queue = startNodes.length > 0 ? [...startNodes] : [nodes[0]]
  queue.forEach(node => rankMap.set(node.id, 0))

  while (queue.length > 0) {
    const node = queue.shift()
    const nextRank = (rankMap.get(node.id) || 0) + 1
    ;(outgoing.get(node.id) || []).forEach((targetId) => {
      if (rankMap.has(targetId))
        return
      rankMap.set(targetId, nextRank)
      queue.push(nodeMap.get(targetId))
    })
  }

  nodes.forEach((node, index) => {
    if (!rankMap.has(node.id))
      rankMap.set(node.id, index)
  })

  const rankCounts = new Map()
  const bounds = new Map()
  nodes.forEach((node) => {
    const rank = rankMap.get(node.id)
    const lane = rankCounts.get(rank) || 0
    rankCounts.set(rank, lane + 1)
    const size = getBpmnNodeSize(node.type)
    bounds.set(node.id, {
      x: 100 + rank * 180,
      y: 120 + lane * 140,
      ...size,
    })
  })
  return bounds
}

function getBpmnNodeSize(type) {
  if (type.endsWith('Gateway'))
    return { width: 50, height: 50 }
  if (type.endsWith('Event'))
    return { width: 36, height: 36 }
  return { width: 100, height: 80 }
}

function buildDiagramXml(processId, nodes, flows, bounds) {
  const lines = [
    '  <bpmndi:BPMNDiagram id="BPMNDiagram_1">',
    `    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="${escapeXmlAttr(processId)}">`,
  ]

  nodes.forEach((node) => {
    const b = bounds.get(node.id)
    lines.push(`      <bpmndi:BPMNShape id="${escapeXmlAttr(node.id)}_di" bpmnElement="${escapeXmlAttr(node.id)}">`)
    lines.push(`        <dc:Bounds x="${b.x}" y="${b.y}" width="${b.width}" height="${b.height}" />`)
    lines.push('      </bpmndi:BPMNShape>')
  })

  flows.forEach((flow) => {
    const source = bounds.get(flow.sourceRef)
    const target = bounds.get(flow.targetRef)
    if (!source || !target)
      return
    const waypoints = buildWaypoints(source, target)
    lines.push(`      <bpmndi:BPMNEdge id="${escapeXmlAttr(flow.id)}_di" bpmnElement="${escapeXmlAttr(flow.id)}">`)
    waypoints.forEach(point => lines.push(`        <di:waypoint x="${point.x}" y="${point.y}" />`))
    lines.push('      </bpmndi:BPMNEdge>')
  })

  lines.push('    </bpmndi:BPMNPlane>')
  lines.push('  </bpmndi:BPMNDiagram>')
  return lines.join('\n')
}

function buildWaypoints(source, target) {
  const sourceRight = { x: source.x + source.width, y: source.y + Math.round(source.height / 2) }
  const targetLeft = { x: target.x, y: target.y + Math.round(target.height / 2) }
  if (targetLeft.x > sourceRight.x) {
    return [sourceRight, targetLeft]
  }

  const sourceBottom = { x: source.x + Math.round(source.width / 2), y: source.y + source.height }
  const targetBottom = { x: target.x + Math.round(target.width / 2), y: target.y + target.height }
  const routeY = Math.max(sourceBottom.y, targetBottom.y) + 40
  return [
    sourceBottom,
    { x: sourceBottom.x, y: routeY },
    { x: targetBottom.x, y: routeY },
    targetBottom,
  ]
}

function escapeXmlAttr(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('"', '&quot;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
}

async function handleBpmnChange(xml) {
  if (xml) {
    bpmnXml.value = xml
    hasChanges.value = true
  }
  else {
    try {
      const newXml = await modelerRef.value?.getXML(true)
      if (newXml) {
        bpmnXml.value = newXml
        hasChanges.value = true
      }
    }
    catch (error) {
      console.error('获取 XML 失败:', error)
    }
  }
}

function handlePropertiesUpdate() {
  hasChanges.value = true
}

function handleDiagramImportStart() {
  diagramLoadingCount.value += 1
}

function handleDiagramImportEnd() {
  diagramLoadingCount.value = Math.max(0, diagramLoadingCount.value - 1)
}

async function handleSaveDraft() {
  try {
    saving.value = true

    const xml = await modelerRef.value?.getXML()

    if (!xml) {
      window.$message?.warning('流程图验证失败，请检查后重试')
      return
    }

    const data = {
      ...modelInfo,
      bpmnXml: xml,
      formJson: modelInfo.formJson || (formSchema.value.length > 0 ? JSON.stringify(formSchema.value) : ''),
    }

    let res
    if (modelInfo.id) {
      res = await flowApi.updateModel(data)
    }
    else {
      res = await flowApi.createModel(data)
      if (res.code === 200 && res.data) {
        modelInfo.id = res.data.id
      }
    }

    if (res.code === 200) {
      window.$message?.success('保存成功')
      hasChanges.value = false
    }
    else {
      window.$message?.error(res.message || '保存失败')
    }
  }
  catch (error) {
    console.error('保存失败:', error)
    window.$message?.error('保存失败')
  }
  finally {
    saving.value = false
  }
}

async function handleDeploy() {
  try {
    deploying.value = true

    const xml = await modelerRef.value?.getXML()
    if (!xml) {
      window.$message?.error('流程图验证失败，无法部署。请检查：\n1. 所有连接线是否完整连接\n2. 是否包含开始和结束节点')
      return
    }

    await handleSaveDraft()

    if (!modelInfo.id) {
      window.$message?.error('请先保存模型')
      return
    }

    const res = await flowApi.deployModel(modelInfo.id)
    if (res.code === 200) {
      window.$message?.success('部署成功')
      await loadModel(modelInfo.id)
    }
    else {
      window.$message?.error(res.message || '部署失败')
    }
  }
  catch (error) {
    console.error('部署失败:', error)
    window.$message?.error('部署失败')
  }
  finally {
    deploying.value = false
  }
}

function handleBack() {
  if (hasChanges.value) {
    window.$dialog?.warning({
      title: '提示',
      content: '有未保存的更改，确定要离开吗？',
      positiveText: '保存并离开',
      negativeText: '直接离开',
      onPositiveClick: async () => {
        await handleSaveDraft()
        router.back()
      },
      onNegativeClick: () => {
        router.back()
      },
    })
  }
  else {
    router.back()
  }
}

function handleModelerReady() {
  if (modelerRef.value) {
    modelerInstance.value = modelerRef.value.modeler()
    watch(() => modelerRef.value?.selectedElement, (el) => {
      if (el) {
        dockedElement.value = el
        rightActiveTab.value = 'properties'
      }
    }, { immediate: true })
  }
}

function getElementTitle(el) {
  if (!el)
    return '属性设置'
  const typeNames = {
    'bpmn:StartEvent': '开始节点',
    'bpmn:EndEvent': '结束节点',
    'bpmn:UserTask': '用户任务',
    'bpmn:ServiceTask': '服务任务',
    'bpmn:ScriptTask': '脚本任务',
    'bpmn:BusinessRuleTask': '业务规则任务',
    'bpmn:ManualTask': '手工任务',
    'bpmn:ExclusiveGateway': '排他网关',
    'bpmn:ParallelGateway': '并行网关',
    'bpmn:InclusiveGateway': '包容网关',
    'bpmn:SequenceFlow': '序列流',
    'bpmn:SubProcess': '子流程',
    'bpmn:CallActivity': '调用活动',
  }
  return el.businessObject?.name || typeNames[el.type] || '属性设置'
}
</script>

<style scoped>
.model-design-page {
  height: 100%;
  max-height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #f8fafc;
}

.top-bar {
  height: 52px;
  padding: 0 20px;
  background: #fff;
  border-bottom: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
}

.top-bar-left,
.top-bar-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.top-bar-left {
  flex: 1;
  min-width: 0;
}

.main-content {
  flex: 1;
  display: flex;
  overflow: hidden;
  position: relative;
}

.center-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
}

.config-bar {
  background: #fff;
  border-bottom: 1px solid #e2e8f0;
  flex-shrink: 0;
}

.config-bar.collapsed .config-bar-body {
  display: none;
}

.config-bar-toggle {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 16px;
  cursor: pointer;
  user-select: none;
  transition: background 150ms ease;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

.config-bar-toggle:hover {
  background: #f1f5f9;
}

.config-toggle-icon {
  font-size: 16px;
  color: #6366f1;
  flex-shrink: 0;
}

.config-bar-title {
  font-size: 12px;
  font-weight: 600;
  color: #1e1b4b;
}

.config-toggle-arrow {
  font-size: 16px;
  color: #94a3b8;
  transition: transform 200ms ease;
}

.config-toggle-arrow.expanded {
  transform: rotate(180deg);
}

.config-bar-body {
  padding: 10px 16px;
  background: #fff;
}

.config-bar-row {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}

.config-field {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.config-field-label {
  font-size: 11px;
  font-weight: 500;
  color: #64748b;
  white-space: nowrap;
}

.designer-container {
  flex: 1;
  overflow: hidden;
  background: #f8fafc;
  position: relative;
}

.designer-loading-mask {
  position: absolute;
  inset: 0;
  z-index: 80;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(248, 250, 252, 0.82);
  backdrop-filter: blur(3px);
}

.designer-loading-fade-enter-active,
.designer-loading-fade-leave-active {
  transition: opacity 180ms ease;
}

.designer-loading-fade-enter-from,
.designer-loading-fade-leave-to {
  opacity: 0;
}

.right-panels {
  position: absolute;
  right: 0;
  top: 12px;
  bottom: 12px;
  height: auto;
  max-height: calc(100% - 24px);
  min-height: 0;
  display: flex;
  flex-direction: column;
  border: 1px solid #e2e8f0;
  border-right: none;
  border-radius: 8px 0 0 8px;
  background: #fff;
  width: 380px;
  flex-shrink: 0;
  overflow: hidden;
  z-index: 50;
  box-shadow: -4px 0 12px rgba(0, 0, 0, 0.08);
}

.drawer-enter-active {
  transition: transform 250ms ease-out;
}

.drawer-leave-active {
  transition: transform 200ms ease-in;
}

.drawer-enter-from,
.drawer-leave-to {
  transform: translateX(100%);
}

.right-panel-tabs {
  display: flex;
  align-items: center;
  border-bottom: 1px solid #e2e8f0;
  background: #fff;
  flex-shrink: 0;
  height: 40px;
  padding: 0 4px;
}

.panel-tab {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 14px;
  border: none;
  background: none;
  cursor: pointer;
  font-size: 13px;
  color: #64748b;
  border-radius: 6px;
  transition: all 150ms ease;
  position: relative;
}

.panel-tab:hover:not(:disabled) {
  color: #1e1b4b;
  background: #f1f5f9;
}

.panel-tab.active {
  color: #6366f1;
  background: #eef2ff;
  font-weight: 600;
}

.panel-tab:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.panel-tab-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #6366f1;
  animation: dotPulse 1.5s ease-in-out infinite;
}

@keyframes dotPulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.3;
  }
}

.panel-tab-close {
  margin-left: auto;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  background: none;
  cursor: pointer;
  color: #94a3b8;
  border-radius: 4px;
  transition: all 150ms ease;
}

.panel-tab-close:hover {
  color: #ef4444;
  background: #fef2f2;
}

.docked-properties-panel {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.docked-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  background: #6366f1;
  color: white;
  flex-shrink: 0;
}

.docked-panel-title {
  font-size: 13px;
  font-weight: 600;
}

.docked-panel-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 10px;
}

.docked-panel-body::-webkit-scrollbar {
  width: 4px;
}

.docked-panel-body::-webkit-scrollbar-track {
  background: transparent;
}

.docked-panel-body::-webkit-scrollbar-thumb {
  background: #e2e8f0;
  border-radius: 2px;
}

.ai-flow-panel {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.ai-flow-header {
  padding: 10px 14px;
  border-bottom: 1px solid #e2e8f0;
  flex-shrink: 0;
}

.ai-flow-header-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.ai-flow-title {
  font-size: 14px;
  font-weight: 700;
  color: #1e1b4b;
  display: flex;
  align-items: center;
  gap: 6px;
}

.ai-flow-title-icon {
  color: #6366f1;
  font-size: 18px;
}

.ai-flow-subtitle {
  font-size: 12px;
  color: #64748b;
}

.ai-stage-progress {
  display: flex;
  align-items: center;
  padding: 6px 12px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
  gap: 4px;
  flex-shrink: 0;
  overflow-x: auto;
}

.ai-stage-step {
  display: flex;
  align-items: center;
  gap: 4px;
  white-space: nowrap;
}

.ai-stage-step-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #e2e8f0;
  transition: all 200ms ease;
  flex-shrink: 0;
}

.ai-stage-step.done .ai-stage-step-dot {
  background: #10b981;
}

.ai-stage-step.active .ai-stage-step-dot {
  background: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.2);
}

.ai-stage-step-label {
  font-size: 11px;
  color: #94a3b8;
}

.ai-stage-step.done .ai-stage-step-label {
  color: #10b981;
}

.ai-stage-step.active .ai-stage-step-label {
  color: #6366f1;
  font-weight: 600;
}

.ai-stage-step + .ai-stage-step::before {
  content: '';
  display: block;
  width: 16px;
  height: 1px;
  background: #e2e8f0;
  margin-right: 4px;
}

.ai-flow-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 10px 12px;
  background: #f8fafc;
  scroll-behavior: smooth;
}

.ai-flow-body::-webkit-scrollbar {
  width: 4px;
}

.ai-flow-body::-webkit-scrollbar-track {
  background: transparent;
}

.ai-flow-body::-webkit-scrollbar-thumb {
  background: #e2e8f0;
  border-radius: 2px;
}

.ai-flow-empty {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.ai-empty-hero {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 20px 0;
}

.ai-empty-icon {
  font-size: 32px;
  color: #6366f1;
}

.ai-empty-title {
  font-size: 15px;
  font-weight: 600;
  color: #1e1b4b;
}

.ai-empty-tip {
  font-size: 12px;
  color: #64748b;
}

.ai-example-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.ai-example {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  padding: 10px 12px;
  cursor: pointer;
  transition:
    border-color 150ms ease,
    box-shadow 150ms ease;
}

.ai-example:hover {
  border-color: #6366f1;
  box-shadow: 0 1px 3px rgba(99, 102, 241, 0.1);
}

.ai-example-icon {
  color: #6366f1;
  font-size: 16px;
  flex-shrink: 0;
  margin-top: 1px;
}

.ai-example-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.ai-example-label {
  font-size: 13px;
  font-weight: 600;
  color: #1e1b4b;
}

.ai-example-desc {
  font-size: 12px;
  color: #64748b;
  line-height: 1.5;
}

.ai-message-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.ai-message-end {
  height: 1px;
}

.ai-message {
  display: flex;
  gap: 8px;
}

.ai-message.user {
  flex-direction: row-reverse;
}

.ai-message-avatar {
  flex-shrink: 0;
}

.avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 600;
}

.user-avatar {
  background: #6366f1;
  color: #fff;
}

.ai-avatar {
  background: #eef2ff;
  color: #6366f1;
}

.ai-message-body {
  max-width: 85%;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.ai-message.user .ai-message-body {
  align-items: flex-end;
}

.reasoning-section {
  border: 1px solid #e0e7ff;
  border-radius: 8px;
  background: #f5f3ff;
  overflow: hidden;
}

.reasoning-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 10px;
  cursor: pointer;
  transition: background 150ms ease;
}

.reasoning-header:hover {
  background: #ede9fe;
}

.reasoning-header-left {
  display: flex;
  align-items: center;
  gap: 6px;
}

.reasoning-icon {
  color: #6366f1;
  font-size: 16px;
}

.reasoning-label {
  font-size: 12px;
  font-weight: 600;
  color: #4f46e5;
}

.reasoning-duration {
  font-size: 11px;
  color: #818cf8;
}

.reasoning-duration.thinking {
  color: #6366f1;
  animation: dotPulse 1.5s ease-in-out infinite;
}

.reasoning-toggle {
  color: #94a3b8;
  font-size: 18px;
  transition: transform 200ms ease;
}

.reasoning-toggle.expanded {
  transform: rotate(180deg);
}

.reasoning-content {
  padding: 8px 10px;
  font-size: 12px;
  color: #4f46e5;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  border-top: 1px solid #e0e7ff;
  background: #ede9fe;
  max-height: 200px;
  overflow-y: auto;
}

.ai-message-content {
  border-radius: 8px;
  padding: 8px 10px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  background: #fff;
  color: #1e1b4b;
  border: 1px solid #e2e8f0;
  font-size: 13px;
}

.ai-message.user .ai-message-content {
  background: #eef2ff;
  border-color: #c7d2fe;
}

.ai-message-text {
  white-space: pre-wrap;
  word-break: break-word;
}

.message-typing {
  display: flex;
  gap: 4px;
  padding-top: 6px;
}

.message-typing span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #6366f1;
  animation: typingBounce 1.4s ease-in-out infinite;
}

.message-typing span:nth-child(2) {
  animation-delay: 0.2s;
}

.message-typing span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typingBounce {
  0%,
  60%,
  100% {
    transform: translateY(0);
    opacity: 0.4;
  }
  30% {
    transform: translateY(-6px);
    opacity: 1;
  }
}

.ai-draft {
  margin-top: 12px;
  padding: 12px;
  border: 1px solid #bbf7d0;
  border-radius: 8px;
  background: #f0fdf4;
}

.ai-draft-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 6px;
  font-weight: 700;
  color: #14532d;
  font-size: 13px;
}

.ai-draft-desc {
  margin-bottom: 10px;
  color: #166534;
  font-size: 12px;
  line-height: 1.5;
}

.xml-preview-container {
  max-height: 60vh;
  overflow: auto;
  background: #f8f9fa;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 16px;
}

:deep(.xml-preview-container .n-code) {
  background: transparent;
}

:deep(.xml-preview-container .n-code pre) {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
  color: #1e1e1e;
  background: transparent;
}

:deep(.xml-preview-container .n-code .hljs) {
  background: transparent;
}

.ai-flow-input {
  flex-shrink: 0;
  padding: 10px 12px;
  border-top: 1px solid #e2e8f0;
  background: #fff;
}

.ai-flow-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}

.ai-actions-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ai-actions-right {
  display: flex;
  align-items: center;
}

.model-trigger {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: #fff;
  cursor: pointer;
  font-size: 12px;
  color: #64748b;
  transition: all 150ms ease;
}

.model-trigger:hover {
  border-color: #6366f1;
  color: #6366f1;
}

.model-trigger.active {
  border-color: #6366f1;
  background: #eef2ff;
  color: #6366f1;
}

.model-trigger.empty {
  border-color: #fca5a5;
  color: #ef4444;
}

.model-trigger-icon {
  font-size: 14px;
  color: #6366f1;
}

.model-trigger-provider {
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.model-trigger-divider {
  color: #cbd5e1;
}

.model-trigger-model {
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 600;
}

.model-trigger-chevron {
  font-size: 16px;
  color: #94a3b8;
}

.model-panel {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.model-panel-section {
  padding: 10px;
}

.model-panel-section + .model-panel-section {
  border-top: 1px solid #e2e8f0;
}

.model-panel-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
}

.model-panel-empty-tip {
  font-size: 11px;
  color: #ef4444;
  font-weight: 400;
}

.model-panel-count {
  font-size: 11px;
  color: #94a3b8;
  font-weight: 400;
}

.model-panel-empty {
  padding: 12px;
  text-align: center;
  font-size: 12px;
  color: #94a3b8;
}

.model-list {
  max-height: 200px;
  overflow-y: auto;
}

.model-list-item {
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 150ms ease;
}

.model-list-item:hover {
  background: #f1f5f9;
}

.model-list-item.active {
  background: #eef2ff;
}

.model-list-item-main {
  display: flex;
  align-items: center;
  gap: 6px;
}

.model-list-item-name {
  font-size: 13px;
  font-weight: 500;
  color: #1e1b4b;
}

.model-tag {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 4px;
  background: #eef2ff;
  color: #6366f1;
  font-weight: 600;
}

.model-list-item-desc {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 2px;
}

.h-full {
  height: 100%;
}

.-m-4 {
  margin: -16px;
}

@media (max-width: 1024px) {
  .right-panels {
    width: 340px;
  }

  .config-bar-row {
    gap: 8px;
  }
}

@media (max-width: 768px) {
  .top-bar {
    padding: 0 12px;
    height: 48px;
  }

  .top-bar-left {
    gap: 6px;
  }

  .right-panels {
    position: absolute;
    right: 0;
    top: 8px;
    bottom: 8px;
    max-height: calc(100% - 16px);
    z-index: 120;
    width: 92%;
    max-width: 400px;
    border-radius: 8px 0 0 8px;
    box-shadow: -4px 0 12px rgba(0, 0, 0, 0.1);
  }

  .config-bar-row {
    flex-direction: column;
    align-items: stretch;
  }

  .config-field {
    width: 100%;
  }

  .config-field :deep(.n-select),
  .config-field :deep(.n-input) {
    width: 100% !important;
  }
}

@media (max-height: 760px) {
  .right-panel-tabs {
    height: 34px;
  }

  .ai-flow-header,
  .docked-panel-header {
    padding: 7px 10px;
  }

  .ai-flow-subtitle {
    display: none;
  }

  .ai-stage-progress {
    padding: 5px 10px;
  }

  .ai-flow-body {
    padding: 8px 10px;
  }

  .ai-flow-input {
    padding: 7px 9px;
  }
}

@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}

:deep(button:focus-visible),
:deep(input:focus-visible),
:deep(select:focus-visible),
:deep(textarea:focus-visible) {
  outline: 2px solid #6366f1;
  outline-offset: 2px;
}

.config-bar-toggle:focus-visible {
  outline: 2px solid #6366f1;
  outline-offset: -2px;
}
</style>
