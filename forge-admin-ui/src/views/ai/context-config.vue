<template>
  <div class="ai-context-config-page">
    <AiCrudPage
      ref="crudRef"
      :api-config="{
        list: 'get@/ai/context/list',
        add: 'post@/ai/context/add',
        update: 'put@/ai/context/update',
        delete: 'delete@/ai/context/{id}',
      }"
      :search-schema="searchSchema"
      :columns="tableColumns"
      :edit-schema="editSchema"
      row-key="id"
      :edit-grid-cols="1"
      modal-width="700px"
      add-button-text="新增上下文"
    />
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { AiCrudPage } from '@/components/ai-form'

defineOptions({ name: 'AiContextConfig' })

const crudRef = ref(null)

const configTypeOptions = [
  { label: 'SPEC', value: 'SPEC' },
  { label: 'SAMPLE', value: 'SAMPLE' },
  { label: 'RULE', value: 'RULE' },
]

const statusOptions = [
  { label: '正常', value: '0' },
  { label: '停用', value: '1' },
]

const searchSchema = [
  {
    field: 'agentCode',
    label: 'Agent编码',
    type: 'input',
    props: {
      placeholder: '请输入Agent编码',
    },
  },
]

const tableColumns = computed(() => [
  { prop: 'agentCode', label: 'Agent编码', width: 150 },
  { prop: 'configName', label: '配置名称', width: 150 },
  { prop: 'configType', label: '类型', width: 80,
    render: row => {
      const opt = configTypeOptions.find(o => o.value === row.configType)
      return opt ? opt.label : row.configType
    },
  },
  { prop: 'configContent', label: '内容', minWidth: 200, ellipsis: true },
  { prop: 'sort', label: '排序', width: 70 },
  { prop: 'status', label: '状态', width: 70,
    render: row => {
      return row.status === '0' ? '正常' : '停用'
    },
  },
])

const editSchema = [
  {
    field: 'agentCode',
    label: 'Agent编码',
    type: 'input',
    rules: [{ required: true, message: '请输入Agent编码', trigger: 'blur' }],
    props: { placeholder: '如 codegen_column_advisor' },
  },
  {
    field: 'configName',
    label: '配置名称',
    type: 'input',
    rules: [{ required: true, message: '请输入配置名称', trigger: 'blur' }],
    props: { placeholder: '请输入配置名称' },
  },
  {
    field: 'configType',
    label: '类型',
    type: 'select',
    defaultValue: 'SPEC',
    props: { options: configTypeOptions, placeholder: '请选择类型' },
  },
  {
    field: 'configContent',
    label: '上下文内容',
    type: 'textarea',
    span: 2,
    rules: [{ required: true, message: '请输入上下文内容', trigger: 'blur' }],
    props: { placeholder: '请输入SPEC上下文内容', rows: 6 },
  },
  {
    field: 'sort',
    label: '排序',
    type: 'input-number',
    defaultValue: 0,
    props: { min: 0, placeholder: '排序号' },
  },
  {
    field: 'status',
    label: '状态',
    type: 'select',
    defaultValue: '0',
    props: { options: statusOptions, placeholder: '请选择状态' },
  },
]
</script>
