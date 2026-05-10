<template>
  <div class="go-external-api">
    <div class="header">
      <n-space justify="space-between">
        <div>
          <h2 class="title">
            <span class="accent">//</span>
            外部接口管理
          </h2>
          <p class="subtitle">管理外部系统接口配置</p>
        </div>
        <n-button type="primary" @click="handleAdd">
          <template #icon>
            <n-icon><AddIcon /></n-icon>
          </template>
          新增接口
        </n-button>
      </n-space>
    </div>

    <div class="content">
      <n-data-table
        :columns="columns"
        :data="tableData"
        :loading="loading"
        :pagination="pagination"
        @update:page="handlePageChange"
        @update:page-size="handlePageSizeChange"
      />
    </div>

    <n-modal
      v-model:show="modalVisible"
      preset="dialog"
      :title="modalTitle"
      style="width: 600px;"
      :mask-closable="false"
    >
      <n-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-placement="left"
        label-width="auto"
      >
        <n-form-item label="接口名称" path="apiName">
          <n-input v-model:value="formData.apiName" placeholder="请输入接口名称" />
        </n-form-item>

        <n-form-item label="所属系统" path="systemId">
          <n-select
            v-model:value="formData.systemId"
            :options="systemOptions"
            placeholder="请选择所属系统"
          />
        </n-form-item>

        <n-form-item label="接口路径" path="apiPath">
          <n-input v-model:value="formData.apiPath" placeholder="例如: /api/v1/data" />
        </n-form-item>

        <n-form-item label="请求方法" path="method">
          <n-select
            v-model:value="formData.method"
            :options="methodOptions"
            placeholder="请选择请求方法"
          />
        </n-form-item>

        <n-form-item label="适配类型" path="adapterType">
          <n-select
            v-model:value="formData.adapterType"
            :options="adapterTypeOptions"
            placeholder="请选择适配类型"
            @update:value="handleAdapterTypeChange"
          />
        </n-form-item>

        <n-form-item v-if="formData.adapterType === 'JsonPath'" label="适配配置" path="adapterConfig">
          <n-input
            v-model:value="jsonPathExpr"
            placeholder="例如: $.data.list"
            type="textarea"
            :rows="2"
          />
        </n-form-item>

        <n-form-item v-if="formData.adapterType === 'Script'" label="适配配置" path="adapterConfig">
          <n-input
            v-model:value="scriptContent"
            placeholder="输入 JavaScript 脚本"
            type="textarea"
            :rows="4"
          />
        </n-form-item>

        <n-form-item label="描述" path="description">
          <n-input
            v-model:value="formData.description"
            placeholder="请输入描述"
            type="textarea"
            :rows="2"
          />
        </n-form-item>

        <n-form-item label="状态" path="status">
          <n-switch
            v-model:value="formData.status"
            :checked-value="1"
            :unchecked-value="0"
          >
            <template #checked>启用</template>
            <template #unchecked>禁用</template>
          </n-switch>
        </n-form-item>
      </n-form>

      <template #action>
        <n-space justify="end">
          <n-button @click="modalVisible = false">取消</n-button>
          <n-button type="primary" @click="handleSubmit" :loading="submitLoading">
            确定
          </n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, h, onMounted } from 'vue'
import { NButton, NSpace, NTag, useMessage } from 'naive-ui'
import { icon } from '@/plugins'
import {
  getExternalApiPageApi,
  getExternalApiDetailApi,
  createExternalApiApi,
  updateExternalApiApi,
  deleteExternalApiApi,
  ExternalApi
} from '@/api/external/api'
import { getExternalSystemListApi, ExternalSystem } from '@/api/external/system'

const { AddIcon, TrashIcon, EditIcon } = icon.carbon
const message = useMessage()

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref<ExternalApi[]>([])
const systemOptions = ref<{ label: string; value: number }[]>([])
const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 30, 40]
})

const modalVisible = ref(false)
const modalTitle = ref('')
const formRef = ref()
const formData = reactive<Partial<ExternalApi>>({
  id: undefined,
  apiName: '',
  systemId: undefined,
  apiPath: '',
  method: 'GET',
  adapterType: 'None',
  adapterConfig: '{}',
  description: '',
  status: 1
})

const jsonPathExpr = ref('')
const scriptContent = ref('')

const methodOptions = [
  { label: 'GET', value: 'GET' },
  { label: 'POST', value: 'POST' },
  { label: 'PUT', value: 'PUT' },
  { label: 'DELETE', value: 'DELETE' },
  { label: 'PATCH', value: 'PATCH' }
]

const adapterTypeOptions = [
  { label: '无适配', value: 'None' },
  { label: 'JsonPath', value: 'JsonPath' },
  { label: 'JavaScript', value: 'Script' }
]

const formRules = {
  apiName: { required: true, message: '请输入接口名称', trigger: 'blur' },
  systemId: { required: true, type: 'number', message: '请选择所属系统', trigger: 'change' },
  apiPath: { required: true, message: '请输入接口路径', trigger: 'blur' },
  method: { required: true, message: '请选择请求方法', trigger: 'change' },
  adapterType: { required: true, message: '请选择适配类型', trigger: 'change' }
}

const columns = [
  {
    title: 'ID',
    key: 'id',
    width: 80
  },
  {
    title: '接口名称',
    key: 'apiName',
    width: 150
  },
  {
    title: '所属系统',
    key: 'systemId',
    width: 150,
    render: (row: ExternalApi) => {
      const system = systemOptions.value.find(s => s.value === row.systemId)
      return system?.label || '--'
    }
  },
  {
    title: '接口路径',
    key: 'apiPath',
    width: 200
  },
  {
    title: '请求方法',
    key: 'method',
    width: 80,
    render: (row: ExternalApi) => {
      const methodColors = {
        'GET': 'success',
        'POST': 'info',
        'PUT': 'warning',
        'DELETE': 'error',
        'PATCH': 'default'
      }
      return h(NTag, {
        type: methodColors[row.method] || 'default',
        size: 'small'
      }, { default: () => row.method })
    }
  },
  {
    title: '适配类型',
    key: 'adapterType',
    width: 120,
    render: (row: ExternalApi) => {
      const adapterTypeMap = {
        'None': '无适配',
        'JsonPath': 'JsonPath',
        'Script': 'JavaScript'
      }
      return adapterTypeMap[row.adapterType] || row.adapterType
    }
  },
  {
    title: '状态',
    key: 'status',
    width: 80,
    render: (row: ExternalApi) => {
      return h(NTag, {
        type: row.status === 1 ? 'success' : 'error',
        size: 'small'
      }, { default: () => row.status === 1 ? '启用' : '禁用' })
    }
  },
  {
    title: '创建时间',
    key: 'createTime',
    width: 180
  },
  {
    title: '操作',
    key: 'actions',
    width: 150,
    render: (row: ExternalApi) => {
      return h(NSpace, null, {
        default: () => [
          h(NButton, {
            size: 'small',
            text: true,
            type: 'primary',
            onClick: () => handleEdit(row)
          }, { default: () => '编辑' }),
          h(NButton, {
            size: 'small',
            text: true,
            type: 'error',
            onClick: () => handleDelete(row)
          }, { default: () => '删除' })
        ]
      })
    }
  }
]

const loadTableData = async () => {
  loading.value = true
  try {
    const res = await getExternalApiPageApi({
      pageNum: pagination.page,
      pageSize: pagination.pageSize
    })
    if (res.code === 0) {
      tableData.value = res.data.records
      pagination.itemCount = res.data.total
    }
  } catch (error) {
    message.error('加载失败')
  } finally {
    loading.value = false
  }
}

const loadSystemOptions = async () => {
  try {
    const res = await getExternalSystemListApi()
    if (res.code === 0) {
      systemOptions.value = res.data.map(sys => ({
        label: sys.systemName,
        value: sys.id as number
      }))
    }
  } catch (error) {
    message.error('加载系统列表失败')
  }
}

const handlePageChange = (page: number) => {
  pagination.page = page
  loadTableData()
}

const handlePageSizeChange = (pageSize: number) => {
  pagination.pageSize = pageSize
  pagination.page = 1
  loadTableData()
}

const handleAdd = () => {
  modalTitle.value = '新增外部接口'
  resetForm()
  modalVisible.value = true
}

const handleEdit = async (row: ExternalApi) => {
  modalTitle.value = '编辑外部接口'
  try {
    const res = await getExternalApiDetailApi(row.id)
    if (res.code === 0) {
      Object.assign(formData, res.data)
      if (formData.adapterType === 'JsonPath') {
        const config = JSON.parse(formData.adapterConfig || '{}')
        jsonPathExpr.value = config.jsonPath || ''
      } else if (formData.adapterType === 'Script') {
        const config = JSON.parse(formData.adapterConfig || '{}')
        scriptContent.value = config.script || ''
      }
    }
    modalVisible.value = true
  } catch (error) {
    message.error('加载详情失败')
  }
}

const handleDelete = async (row: ExternalApi) => {
  try {
    const res = await deleteExternalApiApi(row.id)
    if (res.code === 0) {
      message.success('删除成功')
      loadTableData()
    }
  } catch (error) {
    message.error('删除失败')
  }
}

const handleAdapterTypeChange = (value: string) => {
  formData.adapterType = value
  if (value === 'None') {
    formData.adapterConfig = '{}'
    jsonPathExpr.value = ''
    scriptContent.value = ''
  }
}

const handleSubmit = async () => {
  await formRef.value?.validate()
  
  submitLoading.value = true
  try {
    if (formData.adapterType === 'JsonPath') {
      formData.adapterConfig = JSON.stringify({ jsonPath: jsonPathExpr.value })
    } else if (formData.adapterType === 'Script') {
      formData.adapterConfig = JSON.stringify({ script: scriptContent.value })
    }
    
    const res = formData.id
      ? await updateExternalApiApi(formData)
      : await createExternalApiApi(formData)
    
    if (res.code === 0) {
      message.success(formData.id ? '修改成功' : '新增成功')
      modalVisible.value = false
      loadTableData()
    }
  } catch (error) {
    message.error('操作失败')
  } finally {
    submitLoading.value = false
  }
}

const resetForm = () => {
  formData.id = undefined
  formData.apiName = ''
  formData.systemId = undefined
  formData.apiPath = ''
  formData.method = 'GET'
  formData.adapterType = 'None'
  formData.adapterConfig = '{}'
  formData.description = ''
  formData.status = 1
  jsonPathExpr.value = ''
  scriptContent.value = ''
}

onMounted(() => {
  loadTableData()
  loadSystemOptions()
})
</script>

<style lang="scss" scoped>
.go-external-api {
  padding: 24px;
  min-height: 100vh;

  .header {
    margin-bottom: 24px;

    .title {
      font-size: 22px;
      font-weight: 700;
      margin: 0;
      letter-spacing: 1px;

      .accent {
        color: var(--color-accent);
        font-weight: 300;
        margin-right: 8px;
      }
    }

    .subtitle {
      margin: 4px 0 0;
      font-size: 12px;
      color: #64748b;
    }
  }

  .content {
    background: rgba(255, 255, 255, 0.03);
    border-radius: 8px;
    padding: 20px;
  }
}
</style>