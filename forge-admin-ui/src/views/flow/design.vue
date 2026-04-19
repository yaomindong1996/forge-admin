<template>
  <div class="model-design-page">
    <!-- 顶部工具栏 -->
    <div class="top-bar">
      <div class="left">
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
      </div>
      <div class="center">
        <NTag :type="statusTag.type">
          {{ statusTag.label }}
        </NTag>
      </div>
      <div class="right">
        <n-space>
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
        </n-space>
      </div>
    </div>

    <!-- 主体内容 -->
    <div class="main-content">
      <!-- 左侧配置面板 -->
      <div class="config-panel">
        <n-tabs type="line" size="small">
          <n-tab-pane name="basic" tab="基本设置">
            <n-form :model="modelInfo" label-placement="top" size="small">
              <n-form-item label="流程分类">
                <n-select
                  v-model:value="modelInfo.category"
                  :options="categoryOptions"
                  placeholder="选择分类"
                />
              </n-form-item>
              <n-form-item label="流程类型">
                <n-input v-model:value="modelInfo.flowType" placeholder="如：审批流程" />
              </n-form-item>
              <n-form-item label="流程描述">
                <n-input
                  v-model:value="modelInfo.description"
                  type="textarea"
                  :rows="3"
                  placeholder="描述流程用途"
                />
              </n-form-item>
            </n-form>
          </n-tab-pane>

          <n-tab-pane name="form" tab="表单配置">
            <div class="form-config">
              <n-form-item label="表单类型">
                <n-select
                  v-model:value="modelInfo.formType"
                  :options="formTypeOptions"
                  @update:value="handleFormTypeChange"
                />
              </n-form-item>

              <!-- 动态表单：选择已有表单或设计新表单 -->
              <template v-if="modelInfo.formType === 'dynamic'">
                <n-form-item label="表单选择">
                  <n-select
                    v-model:value="modelInfo.formId"
                    :options="formOptions"
                    placeholder="选择已有表单"
                    clearable
                    @update:value="handleFormSelect"
                  />
                </n-form-item>

                <n-space vertical size="small">
                  <n-button
                    type="primary"
                    dashed
                    block
                    @click="handleOpenFormDesigner"
                  >
                    <template #icon>
                      <i class="i-material-symbols:edit-document" />
                    </template>
                    {{ modelInfo.formJson ? '编辑表单设计' : '设计新表单' }}
                  </n-button>

                  <n-button
                    v-if="modelInfo.formJson"
                    dashed
                    block
                    @click="handlePreviewForm"
                  >
                    <template #icon>
                      <i class="i-material-symbols:visibility" />
                    </template>
                    预览表单
                  </n-button>

                  <n-button
                    v-if="modelInfo.formJson"
                    type="error"
                    text
                    block
                    @click="handleClearForm"
                  >
                    <template #icon>
                      <i class="i-material-symbols:delete" />
                    </template>
                    清除表单
                  </n-button>
                </n-space>

                <!-- 表单字段预览 -->
                <div v-if="formSchema.length > 0" class="form-fields-preview">
                  <n-divider style="margin: 12px 0">
                    表单字段预览
                  </n-divider>
                  <div class="field-list">
                    <NTag
                      v-for="(field, index) in formSchema"
                      :key="index"
                      size="small"
                      type="info"
                      style="margin: 2px"
                    >
                      {{ field.label || field.field }}
                    </NTag>
                  </div>
                </div>
              </template>

              <!-- 外部表单：输入URL -->
              <template v-else-if="modelInfo.formType === 'external'">
                <n-form-item label="表单URL">
                  <n-input
                    v-model:value="modelInfo.formUrl"
                    placeholder="请输入外部表单URL"
                  />
                </n-form-item>
              </template>
            </div>
          </n-tab-pane>

          <n-tab-pane name="listener" tab="监听器">
            <n-form label-placement="top" size="small">
              <n-form-item label="开始监听器">
                <n-input
                  v-model:value="modelInfo.startListener"
                  placeholder="全限定类名"
                />
              </n-form-item>
              <n-form-item label="结束监听器">
                <n-input
                  v-model:value="modelInfo.endListener"
                  placeholder="全限定类名"
                />
              </n-form-item>
            </n-form>
          </n-tab-pane>
        </n-tabs>
      </div>

      <!-- 流程设计器 -->
      <div class="designer-container">
        <FlowModeler
          ref="modelerRef"
          :xml="bpmnXml"
          @change="handleBpmnChange"
        />
      </div>
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
  </div>
</template>

<script setup>
import { NTag } from 'naive-ui'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import flowApi from '@/api/flow'
import FlowModeler from '@/components/bpmn/FlowModeler.vue'
import FormDesigner from '@/components/form-designer/FormDesigner.vue'
import FormPreview from '@/components/form-designer/FormPreview.vue'

const route = useRoute()
const router = useRouter()
// 使用全局 message 实例
const message = window.$message

// 状态
const saving = ref(false)
const deploying = ref(false)
const modelerRef = ref(null)
const bpmnXml = ref('')
const hasChanges = ref(false)

// 模型信息
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
  startListener: '',
  endListener: '',
})

// 表单设计器相关
const showFormDesigner = ref(false)
const formDesignerRef = ref(null)
const formSchema = ref([])
const formOptions = ref([])
const showFormPreview = ref(false)

// 分类选项
const categoryOptions = ref([])

// 表单类型选项
const formTypeOptions = [
  { label: '动态表单', value: 'dynamic' },
  { label: '外置表单', value: 'external' },
  { label: '无表单', value: 'none' },
]

// 状态标签
const statusTag = computed(() => {
  const statusMap = {
    0: { type: 'warning', label: '设计中' },
    1: { type: 'success', label: '已部署' },
    2: { type: 'default', label: '已禁用' },
  }
  return statusMap[modelInfo.status] || { type: 'default', label: '未知' }
})

const isReadonly = computed(() => modelInfo.status === 1)

// 初始化
onMounted(async () => {
  await loadCategories()
  await loadForms()

  const modelId = route.query.id
  if (modelId) {
    await loadModel(modelId)
  }
  else {
    // 新建模型
    modelInfo.modelKey = `process_${Date.now()}`
    modelInfo.modelName = '新流程'
  }
})

// 加载分类
async function loadCategories() {
  try {
    const res = await flowApi.getEnabledCategories()
    if (res.code === 200) {
      categoryOptions.value = (res.data || []).map(item => ({
        label: item.categoryName,
        value: item.categoryCode,
      }))
    }
  }
  catch (error) {
    console.error('加载分类失败:', error)
  }
}

// 加载表单列表
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

// 加载模型
async function loadModel(id) {
  try {
    const res = await flowApi.getModelDetail(id)
    if (res.code === 200 && res.data) {
      Object.assign(modelInfo, res.data)
      bpmnXml.value = res.data.bpmnXml || ''

      // 解析表单Schema
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

// 表单类型变更
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

// 选择已有表单
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

// 打开表单设计器
function handleOpenFormDesigner() {
  showFormDesigner.value = true
}

// 保存表单Schema
function handleSaveFormSchema(schema) {
  formSchema.value = schema
  modelInfo.formJson = JSON.stringify(schema)
  showFormDesigner.value = false
  hasChanges.value = true
  message.success('表单设计已保存')
}

// 预览表单
function handlePreviewForm() {
  showFormPreview.value = true
}

// 清除表单
function handleClearForm() {
  window.$dialog?.warning({
    title: '确认清除',
    content: '确定要清除已设计的表单吗？此操作不可撤销。',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: () => {
      formSchema.value = []
      modelInfo.formJson = ''
      modelInfo.formId = null
      hasChanges.value = true
      message.success('表单已清除')
    },
  })
}

// BPMN 变更
function handleBpmnChange(xml) {
  bpmnXml.value = xml
  hasChanges.value = true
}

// 保存草稿
async function handleSaveDraft() {
  try {
    saving.value = true

    const xml = await modelerRef.value?.getXML()
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

// 发布部署
async function handleDeploy() {
  try {
    deploying.value = true

    // 先保存
    await handleSaveDraft()

    if (!modelInfo.id) {
      window.$message?.error('请先保存模型')
      return
    }

    const res = await flowApi.deployModel(modelInfo.id)
    if (res.code === 200) {
      window.$message?.success('部署成功')
      modelInfo.status = 1
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

// 返回
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
</script>

<style scoped>
.model-design-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

.top-bar {
  height: 56px;
  padding: 0 16px;
  background: #fff;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.top-bar .left,
.top-bar .right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.top-bar .center {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
}

.main-content {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.config-panel {
  width: 300px;
  background: #fff;
  border-right: 1px solid #e0e0e0;
  overflow-y: auto;
}

.config-panel :deep(.n-tabs-nav) {
  padding: 0 16px;
}

.config-panel :deep(.n-tab-pane) {
  padding: 16px;
}

.form-config {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.form-fields-preview {
  margin-top: 8px;
}

.field-list {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.designer-container {
  flex: 1;
  overflow: hidden;
}

.h-full {
  height: 100%;
}

.-m-4 {
  margin: -16px;
}
</style>
