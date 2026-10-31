<template>
  <AiCrudPage
    :api-config="{
      list: 'get@/api/flow/spelTemplate/page',
      detail: 'get@/api/flow/spelTemplate/:id',
      add: 'post@/api/flow/spelTemplate',
      update: 'put@/api/flow/spelTemplate',
      delete: 'delete@/api/flow/spelTemplate/:id',
    }"
    :search-schema="searchSchema"
    :columns="tableColumns"
    :edit-schema="editSchema"
    row-key="id"
    :load-detail-on-edit="true"
  />
</template>

<script setup>
import { computed, defineAsyncComponent, h } from 'vue'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'
import { createAutoCodeSync } from '@/utils'
import DesignerAsyncLoader from '@/views/app-center/components/designer/DesignerAsyncLoader.vue'

defineOptions({ name: 'FlowSpelTemplate' })

const templateCodeAuto = createAutoCodeSync({
  scene: 'templateCode',
  targetField: 'templateCode',
})

const FlowSpelCrudLoader = {
  name: 'FlowSpelCrudLoader',
  setup() {
    return () => h(DesignerAsyncLoader, {
      title: '正在加载模板列表',
      description: '首次打开需要准备列表和表单组件资源',
    })
  },
}

const AiCrudPage = defineAsyncComponent({
  loader: () => import('@/components/ai-form').then(module => module.AiCrudPage),
  loadingComponent: FlowSpelCrudLoader,
  delay: 120,
  suspensible: false,
})

const { dict } = useDict('flow_spel_category', 'sys_enable_disable')

const categoryOptions = computed(() => dict.value.flow_spel_category || [])
const statusOptions = computed(() => toNumberOptions(dict.value.sys_enable_disable))

const searchSchema = computed(() => [
  {
    field: 'templateName',
    label: '模板名称',
    type: 'input',
    props: { placeholder: '请输入模板名称', clearable: true },
  },
  {
    field: 'category',
    label: '分类',
    type: 'select',
    props: { options: categoryOptions.value, clearable: true, placeholder: '请选择分类' },
  },
  {
    field: 'status',
    label: '状态',
    type: 'select',
    props: { options: statusOptions.value, clearable: true, placeholder: '请选择状态' },
  },
])

const tableColumns = computed(() => [
  { prop: 'templateName', label: '模板名称', width: 150 },
  { prop: 'templateCode', label: '模板编码', width: 120 },
  {
    prop: 'expression',
    label: '表达式',
    minWidth: 200,
    ellipsis: { tooltip: true },
  },
  {
    prop: 'category',
    label: '分类',
    width: 100,
    render: row => h(DictTag, { dictType: 'flow_spel_category', value: row.category }),
  },
  {
    prop: 'status',
    label: '状态',
    width: 80,
    render: row => h(DictTag, { dictType: 'sys_enable_disable', value: row.status }),
  },
  { prop: 'sort', label: '排序', width: 80 },
  { prop: 'createTime', label: '创建时间', width: 160 },
])

const editSchema = computed(() => [
  {
    field: 'templateName',
    label: '模板名称',
    type: 'input',
    rules: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
    props: { placeholder: '请输入模板名称' },
    onChange: templateCodeAuto.onSourceChange,
  },
  {
    field: 'templateCode',
    label: '模板编码',
    type: 'input',
    rules: [{ required: true, message: '请输入模板编码', trigger: 'blur' }],
    labelTip: '根据名称自动生成，可手动修改',
    props: { placeholder: '自动生成，如：find_dept_manager' },
    onChange: templateCodeAuto.onTargetChange,
  },
  {
    field: 'expression',
    label: 'SPEL表达式',
    type: 'textarea',
    rules: [{ required: true, message: '请输入表达式', trigger: 'blur' }],
    props: {
      rows: 3,
      placeholder: '输入 SPEL 表达式',
    },
  },
  {
    field: 'description',
    label: '描述说明',
    type: 'input',
    props: { placeholder: '简要描述模板用途' },
  },
  {
    field: 'category',
    label: '分类',
    type: 'select',
    props: { options: categoryOptions.value, placeholder: '请选择分类' },
  },
  {
    field: 'exampleParams',
    label: '示例参数',
    type: 'textarea',
    props: {
      rows: 2,
      placeholder: 'JSON格式示例参数，如：{"deptId": "123"}',
    },
  },
  {
    field: 'status',
    label: '状态',
    type: 'switch',
    defaultValue: 1,
    props: { checkedValue: 1, uncheckedValue: 0 },
  },
  {
    field: 'sort',
    label: '排序',
    type: 'number',
    defaultValue: 100,
    props: { min: 0, max: 999, placeholder: '数值越小优先级越高' },
  },
  {
    field: 'remark',
    label: '备注',
    type: 'textarea',
    props: { rows: 2, placeholder: '请输入备注' },
  },
])

function toNumberOptions(options = []) {
  return options.map(item => ({
    ...item,
    value: Number(item.value),
  }))
}
</script>
