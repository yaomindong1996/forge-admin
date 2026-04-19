<template>
  <div class="ai-model-page">
    <AiCrudPage
      ref="crudRef"
      api="/ai/model"
      :api-config="{
        list: 'get@/ai/model/page',
        detail: 'get@/ai/model/{id}',
        add: 'post@/ai/model',
        update: 'put@/ai/model',
        delete: 'delete@/ai/model/{id}',
      }"
      :search-schema="searchSchema"
      :columns="tableColumns"
      :edit-schema="editSchema"
      :before-submit="beforeSubmit"
      :before-render-form="beforeRenderForm"
      row-key="id"
      :edit-grid-cols="2"
      modal-width="800px"
      add-button-text="新增模型"
    />
  </div>
</template>

<script setup>
import { NAvatar } from 'naive-ui'
import { computed, h, onMounted, ref } from 'vue'
import { providerPage } from '@/api/ai'
import { AiCrudPage } from '@/components/ai-form'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'
import { getFileUrl } from '@/utils/file'

defineOptions({ name: 'AiModel' })

const crudRef = ref(null)

// 供应商列表（下拉选择用）
const providerList = ref([])

// 加载字典
const { dict } = useDict('ai_model_type', 'ai_status', 'ai_is_default')

// 模型类型选项
const modelTypeOptions = computed(() => dict.value.ai_model_type || [])
// 状态选项
const statusOptions = computed(() => dict.value.ai_status || [])
// 是否默认选项
const isDefaultOptions = computed(() => dict.value.ai_is_default || [])

// 加载供应商列表
async function loadProviders() {
  try {
    const res = await providerPage({ pageNum: 1, pageSize: 100 })
    if (res.code === 200) {
      providerList.value = (res.data.records || []).map(p => ({
        label: p.providerName,
        value: p.id,
      }))
    }
  }
  catch (e) {
    console.error('加载供应商列表失败', e)
  }
}

onMounted(() => {
  loadProviders()
})

// 搜索表单配置
const searchSchema = computed(() => [
  {
    field: 'modelName',
    label: '模型名称',
    type: 'input',
    props: { placeholder: '请输入模型名称' },
  },
  {
    field: 'providerId',
    label: '供应商',
    type: 'select',
    props: { placeholder: '请选择供应商', options: providerList.value },
  },
  {
    field: 'modelType',
    label: '模型类型',
    type: 'select',
    props: { placeholder: '请选择类型', options: modelTypeOptions.value },
  },
])

// 表格列配置
const tableColumns = computed(() => [
  {
    prop: 'icon',
    label: '图标',
    width: 60,
    render: (row) => {
      if (row.icon) {
        return h(NAvatar, { src: getFileUrl(row.icon), size: 28, round: true })
      }
      return h(NAvatar, { size: 28, round: true }, { default: () => (row.modelName || '-').charAt(0) })
    },
  },
  {
    prop: 'modelName',
    label: '模型名称',
    width: 160,
  },
  {
    prop: 'modelId',
    label: '模型标识',
    width: 180,
  },
  {
    prop: 'providerId',
    label: '供应商',
    width: 140,
    render: (row) => {
      const provider = providerList.value.find(p => p.value === row.providerId)
      return provider ? provider.label : '-'
    },
  },
  {
    prop: 'modelType',
    label: '模型类型',
    width: 100,
    render: (row) => {
      return h(DictTag, { dictType: 'ai_model_type', value: row.modelType, size: 'small' })
    },
  },
  {
    prop: 'maxTokens',
    label: '最大Token',
    width: 100,
    render: (row) => {
      return row.maxTokens ? row.maxTokens.toLocaleString() : '-'
    },
  },
  {
    prop: 'isDefault',
    label: '默认模型',
    width: 90,
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
    prop: 'description',
    label: '描述',
    minWidth: 150,
  },
  {
    prop: 'action',
    label: '操作',
    width: 120,
    fixed: 'right',
    actions: [
      { label: '编辑', key: 'edit', onClick: handleEdit },
      { label: '删除', key: 'delete', type: 'error', onClick: handleDelete },
    ],
  },
])

// 编辑表单配置
const editSchema = computed(() => [
  {
    field: 'providerId',
    label: '供应商',
    type: 'select',
    rules: [{ required: true, message: '请选择供应商', trigger: 'change' }],
    props: { placeholder: '请选择供应商', options: providerList.value },
  },
  {
    field: 'modelType',
    label: '模型类型',
    type: 'select',
    rules: [{ required: true, message: '请选择模型类型', trigger: 'change' }],
    props: { placeholder: '请选择模型类型', options: modelTypeOptions.value },
  },
  {
    field: 'modelId',
    label: '模型标识',
    type: 'input',
    rules: [{ required: true, message: '请输入模型标识', trigger: 'blur' }],
    props: { placeholder: '如 gpt-4o, text-embedding-3' },
  },
  {
    field: 'modelName',
    label: '模型名称',
    type: 'input',
    rules: [{ required: true, message: '请输入模型名称', trigger: 'blur' }],
    props: { placeholder: '如 GPT-4o, 文本向量化 v3' },
  },
  {
    field: 'maxTokens',
    label: '最大Token数',
    type: 'input-number',
    props: { placeholder: '如 128000', min: 0, style: { width: '100%' } },
  },
  {
    field: 'icon',
    label: '模型图标',
    type: 'imageUpload',
    businessType: 'ai-model-icon',
    limit: 1,
    fileSize: 2,
    fileType: ['png', 'jpg', 'jpeg', 'svg', 'webp'],
    valueType: 'string',
    props: { showTip: true },
  },
  {
    field: 'isDefault',
    label: '默认模型',
    type: 'radio',
    defaultValue: '0',
    props: { options: isDefaultOptions.value },
  },
  {
    field: 'status',
    label: '状态',
    type: 'radio',
    defaultValue: '0',
    props: { options: statusOptions.value },
  },
  {
    field: 'sortOrder',
    label: '排序号',
    type: 'input-number',
    defaultValue: 0,
    props: { placeholder: '排序值', min: 0 },
  },
  {
    type: 'divider',
    label: '其他',
    props: { titlePlacement: 'left' },
    span: 2,
  },
  {
    field: 'description',
    label: '描述',
    type: 'textarea',
    span: 2,
    props: { placeholder: '请输入模型描述', rows: 3 },
  },
  {
    field: 'remark',
    label: '备注',
    type: 'textarea',
    span: 2,
    props: { placeholder: '请输入备注', rows: 2 },
  },
])

// 表单渲染前处理
function beforeRenderForm(formData) {
  return formData
}

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
</script>

<style scoped>
.ai-model-page {
  height: 100%;
}
</style>
