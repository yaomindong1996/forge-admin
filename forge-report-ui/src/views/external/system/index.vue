<template>
  <div class="go-external-system">
    <div class="header">
      <n-space justify="space-between">
        <div>
          <h2 class="title">
            <span class="accent">//</span>
            外部系统管理
          </h2>
          <p class="subtitle">管理外部系统接入配置</p>
        </div>
        <n-button type="primary" @click="handleAdd">
          <template #icon>
            <n-icon><AddIcon /></n-icon>
          </template>
          新增系统
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
        <n-form-item label="系统名称" path="systemName">
          <n-input v-model:value="formData.systemName" placeholder="请输入系统名称" />
        </n-form-item>

        <n-form-item label="基础URL" path="baseUrl">
          <n-input v-model:value="formData.baseUrl" placeholder="例如: https://api.example.com" />
        </n-form-item>

        <n-form-item label="认证类型" path="authType">
          <n-select
            v-model:value="formData.authType"
            :options="authTypeOptions"
            placeholder="请选择认证类型"
            @update:value="handleAuthTypeChange"
          />
        </n-form-item>

        <n-form-item v-if="formData.authType === 'BearerToken'" label="认证配置" path="authConfig">
          <n-input
            v-model:value="bearerToken"
            placeholder="请输入 Bearer Token"
            type="textarea"
            :rows="2"
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
  getExternalSystemPageApi,
  getExternalSystemDetailApi,
  createExternalSystemApi,
  updateExternalSystemApi,
  deleteExternalSystemApi,
  ExternalSystem
} from '@/api/external/system'

const { AddIcon, TrashIcon, EditIcon } = icon.carbon
const message = useMessage()

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref<ExternalSystem[]>([])
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
const formData = reactive<Partial<ExternalSystem>>({
  id: undefined,
  systemName: '',
  baseUrl: '',
  authType: 'None',
  authConfig: '{}',
  description: '',
  status: 1
})

const bearerToken = ref('')

const authTypeOptions = [
  { label: '无认证', value: 'None' },
  { label: 'Bearer Token', value: 'BearerToken' }
]

const formRules = {
  systemName: { required: true, message: '请输入系统名称', trigger: 'blur' },
  baseUrl: { required: true, message: '请输入基础URL', trigger: 'blur' },
  authType: { required: true, message: '请选择认证类型', trigger: 'change' }
}

const columns = [
  {
    title: 'ID',
    key: 'id',
    width: 80
  },
  {
    title: '系统名称',
    key: 'systemName',
    width: 150
  },
  {
    title: '基础URL',
    key: 'baseUrl',
    width: 250
  },
  {
    title: '认证类型',
    key: 'authType',
    width: 120,
    render: (row: ExternalSystem) => {
      const authTypeMap = {
        'None': '无认证',
        'BearerToken': 'Bearer Token'
      }
      return authTypeMap[row.authType] || row.authType
    }
  },
  {
    title: '状态',
    key: 'status',
    width: 80,
    render: (row: ExternalSystem) => {
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
    render: (row: ExternalSystem) => {
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
    const res = await getExternalSystemPageApi({
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
  modalTitle.value = '新增外部系统'
  resetForm()
  modalVisible.value = true
}

const handleEdit = async (row: ExternalSystem) => {
  modalTitle.value = '编辑外部系统'
  try {
    const res = await getExternalSystemDetailApi(row.id)
    if (res.code === 0) {
      Object.assign(formData, res.data)
      if (formData.authType === 'BearerToken') {
        const config = JSON.parse(formData.authConfig || '{}')
        bearerToken.value = config.token || ''
      }
    }
    modalVisible.value = true
  } catch (error) {
    message.error('加载详情失败')
  }
}

const handleDelete = async (row: ExternalSystem) => {
  try {
    const res = await deleteExternalSystemApi(row.id)
    if (res.code === 0) {
      message.success('删除成功')
      loadTableData()
    }
  } catch (error) {
    message.error('删除失败')
  }
}

const handleAuthTypeChange = (value: string) => {
  formData.authType = value
  if (value === 'None') {
    formData.authConfig = '{}'
    bearerToken.value = ''
  }
}

const handleSubmit = async () => {
  await formRef.value?.validate()
  
  submitLoading.value = true
  try {
    if (formData.authType === 'BearerToken') {
      formData.authConfig = JSON.stringify({ token: bearerToken.value })
    }
    
    const res = formData.id
      ? await updateExternalSystemApi(formData)
      : await createExternalSystemApi(formData)
    
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
  formData.systemName = ''
  formData.baseUrl = ''
  formData.authType = 'None'
  formData.authConfig = '{}'
  formData.description = ''
  formData.status = 1
  bearerToken.value = ''
}

onMounted(() => {
  loadTableData()
})
</script>

<style lang="scss" scoped>
.go-external-system {
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