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
        delete: 'delete@/ai/provider/{id}'
      }"
      :search-schema="searchSchema"
      :columns="tableColumns"
      :edit-schema="editSchema"
      :before-submit="beforeSubmit"
      row-key="id"
      :edit-grid-cols="2"
      :modal-width="'800px'"
      add-button-text="新增供应商"
    />
  </div>
</template>

<script setup>
import { ref, h, computed } from 'vue'
import { NTag, NButton } from 'naive-ui'
import { AiCrudPage } from '@/components/ai-form'
import { providerTest, providerSetDefault } from '@/api/ai'

defineOptions({ name: 'AiProvider' })

const crudRef = ref(null)

// 供应商类型选项
const providerTypeOptions = [
  { label: '阿里百炼', value: 'alibaba' },
  { label: 'OpenAI', value: 'openai' },
  { label: '智谱 AI', value: 'zhipu' },
  { label: 'Moonshot', value: 'moonshot' },
  { label: 'DeepSeek', value: 'deepseek' },
  { label: 'Ollama（本地）', value: 'ollama' },
  { label: 'Azure', value: 'azure' },
  { label: '自定义', value: 'custom' }
]

// 状态选项
const statusOptions = [
  { label: '正常', value: '0' },
  { label: '停用', value: '1' }
]

// 搜索表单配置
const searchSchema = [
  {
    field: 'providerName',
    label: '供应商名称',
    type: 'input',
    props: { placeholder: '请输入供应商名称' }
  },
  {
    field: 'providerType',
    label: '供应商类型',
    type: 'select',
    props: { placeholder: '请选择类型', options: providerTypeOptions }
  },
  {
    field: 'status',
    label: '状态',
    type: 'select',
    props: { placeholder: '请选择状态', options: statusOptions }
  }
]

// 表格列配置
const tableColumns = computed(() => [
  {
    prop: 'providerName',
    label: '供应商名称',
    width: 160
  },
  {
    prop: 'providerType',
    label: '类型',
    width: 120,
    render: (row) => {
      const opt = providerTypeOptions.find(o => o.value === row.providerType)
      return opt ? opt.label : row.providerType || '-'
    }
  },
  {
    prop: 'baseUrl',
    label: 'Base URL',
    minWidth: 200
  },
  {
    prop: 'defaultModel',
    label: '默认模型',
    width: 150
  },
  {
    prop: 'isDefault',
    label: '默认供应商',
    width: 100,
    render: (row) => {
      return h(NTag,
        { type: row.isDefault === '1' ? 'success' : 'default', size: 'small' },
        { default: () => row.isDefault === '1' ? '是' : '否' }
      )
    }
  },
  {
    prop: 'status',
    label: '状态',
    width: 80,
    render: (row) => {
      return h(NTag,
        { type: row.status === '0' ? 'success' : 'error', size: 'small' },
        { default: () => row.status === '0' ? '正常' : '停用' }
      )
    }
  },
  {
    prop: 'action',
    label: '操作',
    width: 220,
    fixed: 'right',
    actions: [
      { label: '编辑', key: 'edit', onClick: handleEdit },
      { label: '测试连接', key: 'test', onClick: handleTestConnection },
      { label: '设为默认', key: 'setDefault', onClick: handleSetDefault, visible: (row) => row.isDefault !== '1' },
      { label: '删除', key: 'delete', type: 'error', onClick: handleDelete }
    ]
  }
])

// 编辑表单配置
const editSchema = [
  {
    field: 'providerName',
    label: '供应商名称',
    type: 'input',
    rules: [{ required: true, message: '请输入供应商名称', trigger: 'blur' }],
    props: { placeholder: '请输入供应商名称' }
  },
  {
    field: 'providerType',
    label: '供应商类型',
    type: 'select',
    rules: [{ required: true, message: '请选择供应商类型', trigger: 'change' }],
    props: { placeholder: '请选择供应商类型', options: providerTypeOptions }
  },
  {
    field: 'baseUrl',
    label: 'Base URL',
    type: 'input',
    span: 2,
    rules: [{ required: true, message: '请输入 Base URL', trigger: 'blur' }],
    props: { placeholder: '如 https://api.openai.com' }
  },
  {
    field: 'apiKey',
    label: 'API Key',
    type: 'input',
    span: 2,
    rules: [{ required: true, message: '请输入 API Key', trigger: 'blur' }],
    props: { placeholder: '请输入 API Key', type: 'password', showPasswordOn: 'click' }
  },
  {
    type: 'divider',
    label: '其他配置',
    props: { titlePlacement: 'left' },
    span: 2
  },
  {
    field: 'status',
    label: '状态',
    type: 'radio',
    defaultValue: '0',
    props: { options: statusOptions }
  },
  {
    field: 'remark',
    label: '备注',
    type: 'textarea',
    span: 2,
    props: { placeholder: '请输入备注', rows: 3 }
  }
]

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
      providerName: row.providerName
    })
    if (res.code === 200) {
      window.$message.success('连接成功！')
    } else {
      window.$message.error(res.msg || '连接失败')
    }
  } catch (error) {
    window.$message.error('连接失败: ' + (error.message || '未知错误'))
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
  } catch (error) {
    window.$message.error('设置失败')
  }
}
</script>

<style scoped>
.ai-provider-page {
  height: 100%;
}
</style>
