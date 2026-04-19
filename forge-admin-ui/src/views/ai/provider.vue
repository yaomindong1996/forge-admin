<template>
  <div class="ai-provider-page">
    <AiCrudPage
      ref="crudRef"
      api="/ai/provider"
      :api-config="{
        list: 'get@/ai/provider/page',
        detail: 'get@/ai/provider/{id}',
        add: 'post@/ai/provider',
        update: 'put@/ai/provider',
        delete: 'delete@/ai/provider/{id}',
      }"
      :search-schema="searchSchema"
      :columns="tableColumns"
      :edit-schema="editSchema"
      :before-submit="beforeSubmit"
      row-key="id"
      :edit-grid-cols="2"
      modal-width="800px"
      add-button-text="新增供应商"
    />
  </div>
</template>

<script setup>
import { NAvatar } from 'naive-ui'
import { computed, h, ref } from 'vue'
import { providerSetDefault, providerTest } from '@/api/ai'
import { AiCrudPage } from '@/components/ai-form'
import AuthImage from '@/components/common/AuthImage.vue'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'

defineOptions({ name: 'AiProvider' })

const crudRef = ref(null)

// 加载字典
const { dict, getLabel } = useDict('ai_provider_type', 'ai_status', 'ai_is_default')

// 供应商类型选项
const providerTypeOptions = computed(() => dict.value.ai_provider_type || [])
// 状态选项
const statusOptions = computed(() => dict.value.ai_status || [])

// 搜索表单配置
const searchSchema = computed(() => [
  {
    field: 'providerName',
    label: '供应商名称',
    type: 'input',
    props: { placeholder: '请输入供应商名称' },
  },
  {
    field: 'providerType',
    label: '供应商类型',
    type: 'select',
    props: { placeholder: '请选择类型', options: providerTypeOptions.value },
  },
  {
    field: 'status',
    label: '状态',
    type: 'select',
    props: { placeholder: '请选择状态', options: statusOptions.value },
  },
])

// 表格列配置
const tableColumns = computed(() => [
  {
    prop: 'logo',
    label: 'Logo',
    width: 60,
    render: (row) => {
      if (row.logo) {
        return h(AuthImage, {
          src: row.logo,
          imgStyle: { width: '32px', height: '32px', borderRadius: '50%', objectFit: 'cover' },
        })
      }
      return h(NAvatar, { size: 32, round: true }, { default: () => getLabel('ai_provider_type', row.providerType)?.charAt(0) || '-' })
    },
  },
  {
    prop: 'providerName',
    label: '供应商名称',
    width: 160,
  },
  {
    prop: 'providerType',
    label: '类型',
    width: 120,
    render: (row) => {
      return h(DictTag, { dictType: 'ai_provider_type', value: row.providerType, size: 'small' })
    },
  },
  {
    prop: 'baseUrl',
    label: 'Base URL',
    minWidth: 200,
  },
  {
    prop: 'defaultModel',
    label: '默认模型',
    width: 150,
  },
  {
    prop: 'isDefault',
    label: '默认供应商',
    width: 100,
    render: (row) => {
      return h(DictTag, { dictType: 'ai_is_default', value: row.isDefault, size: 'small' })
    },
  },
  {
    prop: 'status',
    label: '状态',
    width: 80,
    render: (row) => {
      return h(DictTag, { dictType: 'ai_status', value: row.status, size: 'small' })
    },
  },
  {
    prop: 'action',
    label: '操作',
    width: 220,
    fixed: 'right',
    actions: [
      { label: '编辑', key: 'edit', onClick: handleEdit },
      { label: '测试连接', key: 'test', onClick: handleTestConnection },
      { label: '设为默认', key: 'setDefault', onClick: handleSetDefault, visible: row => row.isDefault !== '1' },
      { label: '删除', key: 'delete', type: 'error', onClick: handleDelete },
    ],
  },
])

// 编辑表单配置
const editSchema = computed(() => [
  {
    field: 'providerName',
    label: '供应商名称',
    type: 'input',
    rules: [{ required: true, message: '请输入供应商名称', trigger: 'blur' }],
    props: { placeholder: '请输入供应商名称' },
  },
  {
    field: 'providerType',
    label: '供应商类型',
    type: 'select',
    rules: [{ required: true, message: '请选择供应商类型', trigger: 'change' }],
    props: { placeholder: '请选择供应商类型', options: providerTypeOptions.value },
  },
  {
    field: 'logo',
    label: '供应商Logo',
    type: 'imageUpload',
    businessType: 'ai-provider-logo',
    limit: 1,
    fileSize: 2,
    fileType: ['png', 'jpg', 'jpeg', 'svg', 'webp'],
    valueType: 'string',
    props: { showTip: true },
  },
  {
    field: 'baseUrl',
    label: 'Base URL',
    type: 'input',
    span: 2,
    rules: [{ required: true, message: '请输入 Base URL', trigger: 'blur' }],
    props: { placeholder: '如 https://api.openai.com' },
  },
  {
    field: 'apiKey',
    label: 'API Key',
    type: 'input',
    span: 2,
    rules: [{ required: true, message: '请输入 API Key', trigger: 'blur' }],
    props: { placeholder: '请输入 API Key', type: 'password', showPasswordOn: 'click' },
  },
  {
    type: 'divider',
    label: '其他配置',
    props: { titlePlacement: 'left' },
    span: 2,
  },
  {
    field: 'status',
    label: '状态',
    type: 'radio',
    defaultValue: '0',
    props: { options: statusOptions.value },
  },
  {
    field: 'remark',
    label: '备注',
    type: 'textarea',
    span: 2,
    props: { placeholder: '请输入备注', rows: 3 },
  },
])

// 表单提交前处理
function beforeSubmit(formData) {
  return formData
}

// 编辑
function handleEdit(row) {
  crudRef.value?.showEdit(row)
}

// 删除
function handleDelete(row) {
  crudRef.value?.handleDelete(row)
}

// 测试连接
async function handleTestConnection(row) {
  try {
    const res = await providerTest({
      baseUrl: row.baseUrl,
      apiKey: row.apiKey,
      defaultModel: row.defaultModel,
      providerName: row.providerName,
    })
    if (res.code === 200) {
      window.$message.success('连接成功！')
    }
    else {
      window.$message.error(res.msg || '连接失败')
    }
  }
  catch (error) {
    window.$message.error(`连接失败: ${error.message || '未知错误'}`)
  }
}

// 设为默认
async function handleSetDefault(row) {
  try {
    const res = await providerSetDefault(row.id)
    if (res.code === 200) {
      window.$message.success('设置成功')
      crudRef.value?.refresh()
    }
  }
  catch {
    window.$message.error('设置失败')
  }
}
</script>

<style scoped>
.ai-provider-page {
  height: 100%;
}
</style>
