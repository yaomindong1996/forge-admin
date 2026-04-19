<template>
  <div class="p-16">
    <div class="rounded bg-white p-16">
      <h2 class="text-18 mb-16 font-bold">
        流程模板
      </h2>

      <!-- 搜索栏 -->
      <NSpace class="mb-16" :vertical="false">
        <n-input
          v-model:value="queryParams.templateName"
          placeholder="搜索模板名称"
          clearable
          style="width: 200px"
        >
          <template #prefix>
            <i class="i-material-symbols:search" />
          </template>
        </n-input>
        <n-select
          v-model:value="queryParams.category"
          placeholder="流程分类"
          clearable
          style="width: 150px"
          :options="categoryOptions"
        />
        <n-select
          v-model:value="queryParams.status"
          placeholder="状态"
          clearable
          style="width: 120px"
          :options="statusOptions"
        />
        <NButton type="primary" @click="handleSearch">
          <template #icon>
            <i class="i-material-symbols:search" />
          </template>
          搜索
        </NButton>
        <NButton @click="handleReset">
          <template #icon>
            <i class="i-material-symbols:refresh" />
          </template>
          重置
        </NButton>
        <NButton type="primary" @click="handleAdd">
          <template #icon>
            <i class="i-material-symbols:add" />
          </template>
          新增模板
        </NButton>
      </NSpace>

      <!-- 数据表格 -->
      <n-data-table
        :columns="columns"
        :data="dataSource"
        :loading="loading"
        :pagination="pagination"
        :row-key="row => row.id"
      />
    </div>

    <!-- 新增/编辑弹窗 -->
    <Teleport to="body">
      <n-modal
        v-model:show="showModal"
        preset="card"
        :title="modalTitle"
        style="width: 800px"
      >
        <n-form ref="formRef" :model="formData" :rules="rules" label-placement="left" label-width="100">
          <n-grid :cols="2" :x-gap="16">
            <n-gi>
              <n-form-item label="模板名称" path="templateName">
                <n-input v-model:value="formData.templateName" placeholder="请输入模板名称" />
              </n-form-item>
            </n-gi>
            <n-gi>
              <n-form-item label="模板Key" path="templateKey">
                <n-input v-model:value="formData.templateKey" placeholder="自动生成" disabled />
              </n-form-item>
            </n-gi>
            <n-gi>
              <n-form-item label="流程分类" path="category">
                <n-select v-model:value="formData.category" placeholder="请选择分类" :options="categoryOptions" />
              </n-form-item>
            </n-gi>
            <n-gi>
              <n-form-item label="表单类型" path="formType">
                <n-select v-model:value="formData.formType" placeholder="请选择表单类型" :options="formTypeOptions" />
              </n-form-item>
            </n-gi>
            <n-gi :span="2">
              <n-form-item label="描述" path="description">
                <n-input
                  v-model:value="formData.description"
                  type="textarea"
                  placeholder="请输入描述"
                  :rows="2"
                />
              </n-form-item>
            </n-gi>
            <n-gi>
              <n-form-item label="图标" path="icon">
                <n-input v-model:value="formData.icon" placeholder="图标类名" />
              </n-form-item>
            </n-gi>
            <n-gi>
              <n-form-item label="排序" path="sortOrder">
                <n-input-number v-model:value="formData.sortOrder" :min="0" style="width: 100%" />
              </n-form-item>
            </n-gi>
          </n-grid>

          <!-- BPMN设计器 -->
          <n-divider>流程设计</n-divider>
          <div class="bpmn-container">
            <FlowModeler
              ref="modelerRef"
              :xml="formData.bpmnXml || defaultBpmnXml"
              @change="handleBpmnChange"
            />
          </div>
        </n-form>
        <template #footer>
          <NSpace justify="end">
            <NButton @click="showModal = false">
              取消
            </NButton>
            <NButton type="primary" :loading="submitLoading" @click="handleSubmit">
              确定
            </NButton>
          </NSpace>
        </template>
      </n-modal>
    </Teleport>

    <!-- 从模板创建流程弹窗 -->
    <Teleport to="body">
      <n-modal
        v-model:show="showCreateModal"
        preset="card"
        title="从模板创建流程"
        style="width: 500px"
      >
        <n-form :model="createFormData" label-placement="left" label-width="100">
          <n-form-item label="模板">
            <n-input :value="selectedTemplate?.templateName" disabled />
          </n-form-item>
          <n-form-item label="流程名称" required>
            <n-input v-model:value="createFormData.modelName" placeholder="请输入流程名称" />
          </n-form-item>
          <n-form-item label="流程Key">
            <n-input v-model:value="createFormData.modelKey" placeholder="不填自动生成" />
          </n-form-item>
        </n-form>
        <template #footer>
          <NSpace justify="end">
            <NButton @click="showCreateModal = false">
              取消
            </NButton>
            <NButton type="primary" :loading="createLoading" @click="handleCreateModel">
              创建
            </NButton>
          </NSpace>
        </template>
      </n-modal>
    </Teleport>
  </div>
</template>

<script setup>
import { NButton, NSpace, NTag } from 'naive-ui'
import { h, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import flowApi from '@/api/flow'
import FlowModeler from '@/components/bpmn/FlowModeler.vue'

const router = useRouter()

// 状态选项
const statusOptions = [
  { label: '启用', value: 1 },
  { label: '禁用', value: 0 },
]

// 表单类型选项
const formTypeOptions = [
  { label: '动态表单', value: 'dynamic' },
  { label: '外置表单', value: 'external' },
  { label: '无表单', value: 'none' },
]

// 分类选项
const categoryOptions = ref([])

// 查询参数
const queryParams = reactive({
  templateName: '',
  category: null,
  status: null,
})

// 状态标签颜色
const statusTagType = {
  1: 'success',
  0: 'default',
}

const statusText = {
  1: '启用',
  0: '禁用',
}

// 表格列
const columns = [
  {
    title: '模板名称',
    key: 'templateName',
    width: 200,
  },
  {
    title: '模板Key',
    key: 'templateKey',
    width: 150,
  },
  {
    title: '分类',
    key: 'category',
    width: 100,
  },
  {
    title: '表单类型',
    key: 'formType',
    width: 100,
    render: (row) => {
      const types = { dynamic: '动态表单', external: '外置表单', none: '无表单' }
      return types[row.formType] || row.formType
    },
  },
  {
    title: '使用次数',
    key: 'usageCount',
    width: 100,
  },
  {
    title: '状态',
    key: 'status',
    width: 80,
    render: (row) => {
      return h(NTag, {
        type: statusTagType[row.status] || 'default',
        size: 'small',
      }, { default: () => statusText[row.status] || '未知' })
    },
  },
  {
    title: '描述',
    key: 'description',
    ellipsis: { tooltip: true },
  },
  {
    title: '创建时间',
    key: 'createTime',
    width: 180,
  },
  {
    title: '操作',
    key: 'actions',
    width: 300,
    render: (row) => {
      const actions = [
        h(NButton, {
          size: 'small',
          type: 'primary',
          quaternary: true,
          onClick: () => handleCreateFromTemplate(row),
        }, { default: () => '创建流程' }),
        h(NButton, {
          size: 'small',
          quaternary: true,
          onClick: () => handleEdit(row),
        }, { default: () => '编辑' }),
        h(NButton, {
          size: 'small',
          quaternary: true,
          onClick: () => handleCopy(row),
        }, { default: () => '复制' }),
      ]

      if (row.status === 1) {
        actions.push(h(NButton, {
          size: 'small',
          quaternary: true,
          onClick: () => handleDisable(row),
        }, { default: () => '禁用' }))
      }
      else {
        actions.push(h(NButton, {
          size: 'small',
          quaternary: true,
          onClick: () => handleEnable(row),
        }, { default: () => '启用' }))
      }

      if (row.isSystem !== 1) {
        actions.push(h(NButton, {
          size: 'small',
          type: 'error',
          quaternary: true,
          onClick: () => handleDelete(row),
        }, { default: () => '删除' }))
      }

      return h(NSpace, { size: 'small' }, { default: () => actions })
    },
  },
]

// 数据源
const dataSource = ref([])
const loading = ref(false)
const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50],
  onChange: (page) => {
    pagination.page = page
    loadData()
  },
  onUpdatePageSize: (pageSize) => {
    pagination.pageSize = pageSize
    pagination.page = 1
    loadData()
  },
})

// 弹窗相关
const showModal = ref(false)
const modalTitle = ref('新增模板')
const formRef = ref(null)
const modelerRef = ref(null)
const submitLoading = ref(false)
const isEdit = ref(false)

const formData = reactive({
  id: '',
  templateName: '',
  templateKey: '',
  category: null,
  formType: 'dynamic',
  description: '',
  icon: '',
  sortOrder: 0,
  bpmnXml: '',
  formJson: '',
  variables: '',
})

const rules = {
  templateName: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
}

// 默认BPMN XML
const defaultBpmnXml = `<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
                  xmlns:flowable="http://flowable.org/bpmn"
                  targetNamespace="http://flowable.org/processdef">
  <bpmn:process id="Process_1" name="新流程" isExecutable="true">
    <bpmn:startEvent id="StartEvent_1" name="开始"/>
  </bpmn:process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="Process_1">
      <bpmndi:BPMNShape id="StartEvent_1_di" bpmnElement="StartEvent_1">
        <dc:Bounds x="180" y="160" width="36" height="36"/>
      </bpmndi:BPMNShape>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</bpmn:definitions>`

// 创建流程弹窗
const showCreateModal = ref(false)
const selectedTemplate = ref(null)
const createFormData = reactive({
  modelName: '',
  modelKey: '',
})
const createLoading = ref(false)

// 加载数据
async function loadData() {
  loading.value = true
  try {
    const res = await flowApi.getTemplatePage({
      pageNum: pagination.page,
      pageSize: pagination.pageSize,
      ...queryParams,
    })
    if (res.data) {
      dataSource.value = res.data.records || []
      pagination.itemCount = res.data.total || 0
    }
  }
  catch (error) {
    console.error('加载数据失败:', error)
  }
  finally {
    loading.value = false
  }
}

// 加载分类
async function loadCategories() {
  try {
    const res = await flowApi.getEnabledCategories()
    if (res.data) {
      categoryOptions.value = res.data.map(item => ({
        label: item.categoryName,
        value: item.categoryCode,
      }))
    }
  }
  catch (error) {
    console.error('加载分类失败:', error)
  }
}

// 搜索
function handleSearch() {
  pagination.page = 1
  loadData()
}

// 重置
function handleReset() {
  queryParams.templateName = ''
  queryParams.category = null
  queryParams.status = null
  handleSearch()
}

// 新增
function handleAdd() {
  isEdit.value = false
  modalTitle.value = '新增模板'
  Object.assign(formData, {
    id: '',
    templateName: '',
    templateKey: '',
    category: null,
    formType: 'dynamic',
    description: '',
    icon: '',
    sortOrder: 0,
    bpmnXml: '',
    formJson: '',
    variables: '',
  })
  showModal.value = true
}

// 编辑
async function handleEdit(row) {
  isEdit.value = true
  modalTitle.value = '编辑模板'

  try {
    const res = await flowApi.getTemplateDetail(row.id)
    if (res.data) {
      Object.assign(formData, res.data)
      showModal.value = true
    }
  }
  catch (error) {
    console.error('获取详情失败:', error)
  }
}

// BPMN变更
function handleBpmnChange(xml) {
  formData.bpmnXml = xml
}

// 提交
async function handleSubmit() {
  try {
    await formRef.value?.validate()
  }
  catch (error) {
    return
  }

  submitLoading.value = true
  try {
    // 获取最新的BPMN XML
    const xml = await modelerRef.value?.getXML()
    formData.bpmnXml = xml

    if (isEdit.value) {
      await flowApi.updateTemplate(formData)
    }
    else {
      await flowApi.createTemplate(formData)
    }

    showModal.value = false
    loadData()
  }
  catch (error) {
    console.error('保存失败:', error)
  }
  finally {
    submitLoading.value = false
  }
}

// 删除
async function handleDelete(row) {
  try {
    await flowApi.deleteTemplate(row.id)
    loadData()
  }
  catch (error) {
    console.error('删除失败:', error)
  }
}

// 启用
async function handleEnable(row) {
  try {
    await flowApi.enableTemplate(row.id)
    loadData()
  }
  catch (error) {
    console.error('启用失败:', error)
  }
}

// 禁用
async function handleDisable(row) {
  try {
    await flowApi.disableTemplate(row.id)
    loadData()
  }
  catch (error) {
    console.error('禁用失败:', error)
  }
}

// 复制
async function handleCopy(row) {
  try {
    const newName = `${row.templateName}_副本`
    await flowApi.copyTemplate(row.id, newName)
    loadData()
  }
  catch (error) {
    console.error('复制失败:', error)
  }
}

// 从模板创建流程
function handleCreateFromTemplate(row) {
  selectedTemplate.value = row
  createFormData.modelName = ''
  createFormData.modelKey = ''
  showCreateModal.value = true
}

// 创建流程模型
async function handleCreateModel() {
  if (!createFormData.modelName) {
    return
  }

  createLoading.value = true
  try {
    const res = await flowApi.createModelFromTemplate(
      selectedTemplate.value.templateKey,
      createFormData.modelName,
      createFormData.modelKey,
    )

    showCreateModal.value = false

    // 跳转到流程设计页面
    if (res.data) {
      router.push(`/flow/design?id=${res.data}`)
    }
  }
  catch (error) {
    console.error('创建失败:', error)
  }
  finally {
    createLoading.value = false
  }
}

// 初始化
onMounted(() => {
  loadCategories()
  loadData()
})
</script>

<style scoped>
.bpmn-container {
  height: 400px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  overflow: hidden;
}
</style>
