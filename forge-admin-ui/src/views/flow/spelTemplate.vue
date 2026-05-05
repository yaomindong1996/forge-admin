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
import { NTag } from 'naive-ui'
import { h } from 'vue'
import { AiCrudPage } from '@/components/ai-form'

defineOptions({ name: 'FlowSpelTemplate' })

const categoryOptions = [
  { label: '通用', value: 'general' },
  { label: '部门', value: 'dept' },
  { label: '角色', value: 'role' },
  { label: '行政区划', value: 'region' },
  { label: '自定义', value: 'custom' },
]

const statusOptions = [
  { label: '启用', value: 1 },
  { label: '禁用', value: 0 },
]

const searchSchema = [
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
    props: { options: categoryOptions, clearable: true, placeholder: '请选择分类' },
  },
  {
    field: 'status',
    label: '状态',
    type: 'select',
    props: { options: statusOptions, clearable: true, placeholder: '请选择状态' },
  },
]

const tableColumns = [
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
    render: (row) => {
      const item = categoryOptions.find(o => o.value === row.category)
      return item ? item.label : row.category
    },
  },
  {
    prop: 'status',
    label: '状态',
    width: 80,
    render: row =>
      h(NTag, { type: row.status === 1 ? 'success' : 'warning', size: 'small' }, () => row.status === 1 ? '启用' : '禁用'),
  },
  { prop: 'sort', label: '排序', width: 80 },
  { prop: 'createTime', label: '创建时间', width: 160 },
]

const editSchema = [
  {
    field: 'templateName',
    label: '模板名称',
    type: 'input',
    rules: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
    props: { placeholder: '请输入模板名称' },
  },
  {
    field: 'templateCode',
    label: '模板编码',
    type: 'input',
    rules: [{ required: true, message: '请输入模板编码', trigger: 'blur' }],
    props: { placeholder: '请输入唯一编码，如：findDeptManager' },
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
    props: { options: categoryOptions, placeholder: '请选择分类' },
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
    type: 'input-number',
    defaultValue: 100,
    props: { min: 0, max: 999, placeholder: '数值越小优先级越高' },
  },
  {
    field: 'remark',
    label: '备注',
    type: 'textarea',
    props: { rows: 2, placeholder: '请输入备注' },
  },
]
</script>
