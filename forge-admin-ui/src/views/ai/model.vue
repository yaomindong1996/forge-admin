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
        delete: 'delete@/ai/model/{id}'
      }"
      :search-schema="searchSchema"
      :columns="tableColumns"
      :edit-schema="editSchema"
      :before-submit="beforeSubmit"
      :before-render-form="beforeRenderForm"
      row-key="id"
      :edit-grid-cols="2"
      :modal-width="'800px'"
      add-button-text="新增模型"
    />
  </div>
</template>

<script setup>
import { ref, h, computed, onMounted } from 'vue'
import { NTag } from 'naive-ui'
import { AiCrudPage } from '@/components/ai-form'
import { providerPage } from '@/api/ai'

defineOptions({ name: 'AiModel' })

const crudRef = ref(null)

// 供应商列表（下拉选择用）
const providerList = ref([])

// 模型类型选项
const modelTypeOptions = [
  { label: '对话', value: 'chat' },
  { label: '向量化', value: 'embedding' },
  { label: '图像生成', value: 'image' },
  { label: '语音', value: 'audio' }
]

// 模型类型颜色映射
const modelTypeColorMap = {
  chat: 'success',
  embedding: 'info',
  image: 'warning',
  audio: 'default'
}

// 状态选项
const statusOptions = [
  { label: '正常', value: '0' },
  { label: '停用', value: '1' }
]

// 加载供应商列表
async function loadProviders() {
  try {
    const res = await providerPage({ pageNum: 1, pageSize: 100 })
    if (res.code === 200) {
      providerList.value = (res.data.records || []).map(p => ({
        label: p.providerName,
        value: p.id
      }))
    }
  } catch (e) {
    console.error('加载供应商列表失败', e)
  }
}

onMounted(() => {
  loadProviders()
})

// 搜索表单配置
const searchSchema = [
  {
    field: 'modelName',
    label: '模型名称',
    type: 'input',
    props: { placeholder: '请输入模型名称' }
  },
  {
    field: 'providerId',
    label: '供应商',
    type: 'select',
    props: { placeholder: '请选择供应商', options: providerList }
  },
  {
    field: 'modelType',
    label: '模型类型',
    type: 'select',
    props: { placeholder: '请选择类型', options: modelTypeOptions }
  }
]

// 表格列配置
const tableColumns = computed(() => [
  {
    prop: 'modelName',
    label: '模型名称',
    width: 160
  },
  {
    prop: 'modelId',
    label: '模型标识',
    width: 180
  },
  {
    prop: 'providerId',
    label: '供应商',
    width: 140,
    render: (row) => {
      const provider = providerList.value.find(p => p.value === row.providerId)
      return provider ? provider.label : '-'
    }
  },
  {
    prop: 'modelType',
    label: '模型类型',
    width: 100,
    render: (row) => {
      const opt = modelTypeOptions.find(o => o.value === row.modelType)
      return h(NTag,
        { type: modelTypeColorMap[row.modelType] || 'default', size: 'small' },
        { default: () => opt ? opt.label : row.modelType || '-' }
      )
    }
  },
  {
    prop: 'isDefault',
    label: '默认模型',
    width: 90,
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
    prop: 'description',
    label: '描述',
    minWidth: 150
  },
  {
    prop: 'action',
    label: '操作',
    width: 120,
    fixed: 'right',
    actions: [
      { label: '编辑', key: 'edit', onClick: handleEdit },
      { label: '删除', key: 'delete', type: 'error', onClick: handleDelete }
    ]
  }
])

// 编辑表单配置
const editSchema = computed(() => [
  {
    field: 'providerId',
    label: '供应商',
    type: 'select',
    rules: [{ required: true, message: '请选择供应商', trigger: 'change' }],
    props: { placeholder: '请选择供应商', options: providerList.value }
  },
  {
    field: 'modelType',
    label: '模型类型',
    type: 'select',
    rules: [{ required: true, message: '请选择模型类型', trigger: 'change' }],
    props: { placeholder: '请选择模型类型', options: modelTypeOptions }
  },
  {
    field: 'modelId',
    label: '模型标识',
    type: 'input',
    rules: [{ required: true, message: '请输入模型标识', trigger: 'blur' }],
    props: { placeholder: '如 gpt-4o, text-embedding-3' }
  },
  {
    field: 'modelName',
    label: '模型名称',
    type: 'input',
    rules: [{ required: true, message: '请输入模型名称', trigger: 'blur' }],
    props: { placeholder: '如 GPT-4o, 文本向量化 v3' }
  },
  {
    field: 'isDefault',
    label: '默认模型',
    type: 'radio',
    defaultValue: '0',
    props: { options: statusOptions.map(s => ({ label: s.value === '0' ? '否' : '是', value: s.value })) }
  },
  {
    field: 'status',
    label: '状态',
    type: 'radio',
    defaultValue: '0',
    props: { options: statusOptions }
  },
  {
    field: 'sortOrder',
    label: '排序号',
    type: 'input-number',
    defaultValue: 0,
    props: { placeholder: '排序值', min: 0 }
  },
  {
    type: 'divider',
    label: '其他',
    props: { titlePlacement: 'left' },
    span: 2
  },
  {
    field: 'description',
    label: '描述',
    type: 'textarea',
    span: 2,
    props: { placeholder: '请输入模型描述', rows: 3 }
  },
  {
    field: 'remark',
    label: '备注',
    type: 'textarea',
    span: 2,
    props: { placeholder: '请输入备注', rows: 2 }
  }
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
