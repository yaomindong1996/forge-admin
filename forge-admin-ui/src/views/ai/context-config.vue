<template>
  <div class="ai-context-config-page">
    <AiCrudPage
      ref="crudRef"
      :api-config="{
        list: 'get@/ai/context/list',
        add: 'post@/ai/context/add',
        update: 'put@/ai/context/update',
        delete: 'delete@/ai/context/:id',
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
import { computed, h, ref } from 'vue'
import { AiCrudPage } from '@/components/ai-form'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'

defineOptions({ name: 'AiContextConfig' })

const { dict } = useDict('ai_context_type', 'sys_normal_disable')

const crudRef = ref(null)

const configTypeOptions = computed(() => dict.value.ai_context_type || [])
const statusOptions = computed(() => dict.value.sys_normal_disable || [])

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
  { prop: 'configType', label: '类型', width: 80, render: (row) => {
    return h(DictTag, { dictType: 'ai_context_type', value: row.configType, size: 'small' })
  } },
  { prop: 'configContent', label: '内容', minWidth: 200, ellipsis: true },
  { prop: 'sort', label: '排序', width: 70 },
  { prop: 'status', label: '状态', width: 70, render: (row) => {
    return h(DictTag, { dictType: 'sys_normal_disable', value: row.status, size: 'small' })
  } },
])

const editSchema = computed(() => [
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
    props: { options: configTypeOptions.value, placeholder: '请选择类型' },
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
    defaultValue: '1',
    props: { options: statusOptions.value, placeholder: '请选择状态' },
  },
])
</script>
