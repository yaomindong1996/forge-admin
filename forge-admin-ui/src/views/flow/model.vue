<template>
  <div class="p-16">
    <div class="bg-white rounded p-16">
      <h2 class="text-18 font-bold mb-16">流程模型</h2>
      
      <!-- 搜索栏 -->
      <n-space class="mb-16" :vertical="false">
        <n-input
          v-model:value="queryParams.modelName"
          placeholder="搜索模型名称"
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
        <n-button type="primary" @click="handleSearch">
          <template #icon>
            <i class="i-material-symbols:search" />
          </template>
          搜索
        </n-button>
        <n-button @click="handleReset">
          <template #icon>
            <i class="i-material-symbols:refresh" />
          </template>
          重置
        </n-button>
        <n-button type="primary" @click="handleAdd">
          <template #icon>
            <i class="i-material-symbols:add" />
          </template>
          新增模型
        </n-button>
      </n-space>

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
        style="width: 600px"
      >
        <n-form ref="formRef" :model="formData" :rules="rules" label-placement="left" label-width="100">
          <n-form-item label="模型名称" path="modelName">
            <n-input v-model:value="formData.modelName" placeholder="请输入模型名称" />
          </n-form-item>
          <n-form-item label="模型Key" path="modelKey">
            <n-input v-model:value="formData.modelKey" placeholder="请输入模型Key" :disabled="isEdit" />
          </n-form-item>
          <n-form-item label="流程分类" path="category">
            <n-select v-model:value="formData.category" placeholder="请选择分类" :options="categoryOptions" />
          </n-form-item>
          <n-form-item label="流程类型" path="flowType">
            <n-input v-model:value="formData.flowType" placeholder="请输入流程类型" />
          </n-form-item>
          <n-form-item label="表单类型" path="formType">
            <n-select v-model:value="formData.formType" placeholder="请选择表单类型" :options="formTypeOptions" />
          </n-form-item>
          <n-form-item label="描述" path="description">
            <n-input
              v-model:value="formData.description"
              type="textarea"
              placeholder="请输入描述"
              :rows="3"
            />
          </n-form-item>
        </n-form>
        <template #footer>
          <n-space justify="end">
            <n-button @click="showModal = false">取消</n-button>
            <n-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</n-button>
          </n-space>
        </template>
      </n-modal>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, reactive, h, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NTag, NButton, NSpace, NDropdown } from 'naive-ui'
import flowApi from '@/api/flow'

const router = useRouter()

// 状态选项
const statusOptions = [
  { label: '设计中', value: 0 },
  { label: '已部署', value: 1 },
  { label: '已禁用', value: 2 },
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
  modelName: '',
  category: null,
  status: null,
})

// 状态标签颜色
const statusTagType = {
  0: 'warning',
  1: 'success',
  2: 'default',
}

const statusText = {
  0: '设计中',
  1: '已部署',
  2: '已禁用',
}

// 获取操作下拉选项
function getActionOptions(row) {
  const options = [
    { label: '设计流程', key: 'design' },
    { label: '编辑信息', key: 'edit' },
    { label: '复制模型', key: 'copy' },
  ]
  
  // 已部署状态可以查看实例
  if (row.status === 1) {
    options.push({ label: '查看实例', key: 'instances' })
  }
  
  // 设计中状态可以部署
  if (row.status === 0) {
    options.push({ label: '部署', key: 'deploy' })
  }
  
  // 已部署状态可以禁用
  if (row.status === 1) {
    options.push({ label: '挂起', key: 'suspend' })
  }
  
  // 已挂起状态可以激活
  if (row.status === 2) {
    options.push({ label: '激活', key: 'activate' })
  }
  
  options.push({ type: 'divider', key: 'd1' })
  options.push({ label: '删除', key: 'delete', props: { style: 'color: #d03050' } })
  
  return options
}

// 处理操作下拉点击
function handleActionSelect(key, row) {
  switch (key) {
    case 'design':
      handleDesign(row)
      break
    case 'edit':
      handleEdit(row)
      break
    case 'copy':
      handleCopy(row)
      break
    case 'instances':
      handleViewInstances(row)
      break
    case 'deploy':
      handleDeploy(row)
      break
    case 'suspend':
      handleSuspend(row)
      break
    case 'activate':
      handleActivate(row)
      break
    case 'delete':
      handleDelete(row)
      break
  }
}

// 表格列
const columns = [
  {
    title: '模型名称',
    key: 'modelName',
    width: 180,
  },
  {
    title: '模型Key',
    key: 'modelKey',
    width: 150,
  },
  {
    title: '分类',
    key: 'category',
    width: 100,
  },
  {
    title: '版本',
    key: 'version',
    width: 80,
    render: (row) => `v${row.version || 1}`,
  },
  {
    title: '状态',
    key: 'status',
    width: 100,
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
    width: 200,
    render: (row) => {
      return h(NSpace, { size: 'small' }, {
        default: () => [
          // 主要操作按钮：设计
          h(NButton, {
            size: 'small',
            type: 'primary',
            onClick: () => handleDesign(row),
          }, { default: () => '设计' }),
          // 已部署状态显示查看实例按钮
          row.status === 1 ? h(NButton, {
            size: 'small',
            type: 'info',
            onClick: () => handleViewInstances(row),
          }, { default: () => '实例' }) : null,
          // 更多操作下拉
          h(NDropdown, {
            options: getActionOptions(row),
            trigger: 'click',
            onSelect: (key) => handleActionSelect(key, row),
          }, {
            default: () => h(NButton, { size: 'small' }, { default: () => '更多' })
          }),
        ].filter(Boolean)
      })
    },
  },
]

// 数据
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
    fetchData()
  },
  onUpdatePageSize: (pageSize) => {
    pagination.pageSize = pageSize
    pagination.page = 1
    fetchData()
  },
})

// 弹窗
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
  formType: 'dynamic',
  description: '',
})

const rules = {
  modelName: { required: true, message: '请输入模型名称', trigger: 'blur' },
  modelKey: { required: true, message: '请输入模型Key', trigger: 'blur' },
  category: { required: true, message: '请选择分类', trigger: 'change' },
}

// 获取分类列表
async function fetchCategories() {
  try {
    const res = await flowApi.getEnabledCategories()
    if (res.code === 200) {
      categoryOptions.value = (res.data || []).map(item => ({
        label: item.categoryName,
        value: item.categoryCode,
      }))
    }
  } catch (error) {
    console.error('获取分类列表失败:', error)
  }
}

// 获取数据
async function fetchData() {
  loading.value = true
  try {
    const res = await flowApi.getModelPage({
      pageNum: pagination.page,
      pageSize: pagination.pageSize,
      ...queryParams,
    })
    if (res.code === 200) {
      dataSource.value = res.data?.records || []
      pagination.itemCount = res.data?.total || 0
    }
  } catch (error) {
    console.error('获取模型列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 搜索
function handleSearch() {
  pagination.page = 1
  fetchData()
}

// 重置
function handleReset() {
  queryParams.modelName = ''
  queryParams.category = null
  queryParams.status = null
  pagination.page = 1
  fetchData()
}

// 新增
function handleAdd() {
  isEdit.value = false
  modalTitle.value = '新增模型'
  Object.assign(formData, {
    id: '',
    modelName: '',
    modelKey: '',
    category: '',
    flowType: '',
    formType: 'dynamic',
    description: '',
  })
  showModal.value = true
}

// 设计流程
function handleDesign(row) {
  router.push({
    path: '/flow/design',
    query: { id: row.id }
  })
}

// 编辑
function handleEdit(row) {
  isEdit.value = true
  modalTitle.value = '编辑模型'
  Object.assign(formData, row)
  showModal.value = true
}

// 提交
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
    } else {
      window.$message?.error(res.message || '操作失败')
    }
  } catch (error) {
    console.error('提交失败:', error)
  } finally {
    submitLoading.value = false
  }
}

// 部署
async function handleDeploy(row) {
  window.$dialog?.info({
    title: '确认部署',
    content: `确定要部署模型"${row.modelName}"吗？部署后流程将可以发起。`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await flowApi.deployModel(row.id)
        if (res.code === 200) {
          window.$message?.success('部署成功')
          fetchData()
        } else {
          window.$message?.error(res.message || '部署失败')
        }
      } catch (error) {
        console.error('部署失败:', error)
      }
    },
  })
}

// 查看流程实例
function handleViewInstances(row) {
  router.push({
    path: '/flow/monitor',
    query: { modelKey: row.modelKey }
  })
}

// 复制模型
async function handleCopy(row) {
  window.$dialog?.info({
    title: '复制模型',
    content: `确定要复制模型"${row.modelName}"吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await flowApi.copyModel(row.id, `${row.modelName} - 副本`)
        if (res.code === 200) {
          window.$message?.success('复制成功')
          fetchData()
        } else {
          window.$message?.error(res.message || '复制失败')
        }
      } catch (error) {
        console.error('复制失败:', error)
      }
    },
  })
}

// 挂起
async function handleSuspend(row) {
  window.$dialog?.warning({
    title: '确认挂起',
    content: `确定要挂起模型"${row.modelName}"吗？挂起后流程实例将暂停运行。`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await flowApi.suspendModel(row.id)
        if (res.code === 200) {
          window.$message?.success('挂起成功')
          fetchData()
        } else {
          window.$message?.error(res.message || '挂起失败')
        }
      } catch (error) {
        console.error('挂起失败:', error)
      }
    },
  })
}

// 激活
async function handleActivate(row) {
  try {
    const res = await flowApi.activateModel(row.id)
    if (res.code === 200) {
      window.$message?.success('激活成功')
      fetchData()
    } else {
      window.$message?.error(res.message || '激活失败')
    }
  } catch (error) {
    console.error('激活失败:', error)
  }
}

// 删除
async function handleDelete(row) {
  window.$dialog?.warning({
    title: '确认删除',
    content: `确定要删除模型"${row.modelName}"吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await flowApi.deleteModel(row.id)
        if (res.code === 200) {
          window.$message?.success('删除成功')
          fetchData()
        } else {
          window.$message?.error(res.message || '删除失败')
        }
      } catch (error) {
        console.error('删除失败:', error)
      }
    },
  })
}

// 初始化
onMounted(() => {
  fetchCategories()
  fetchData()
})
</script>