<template>
  <div class="code-rule-page">
    <AiCrudPage
      api="/ai/code-rule"
      :api-config="{
        list: 'get@/ai/code-rule/page',
        detail: 'get@/ai/code-rule/:id',
        add: 'post@/ai/code-rule',
        update: 'put@/ai/code-rule',
        delete: 'delete@/ai/code-rule/:id',
      }"
      :search-schema="searchSchema"
      :columns="tableColumns"
      :edit-schema="editSchema"
      row-key="id"
      :edit-grid-cols="2"
      modal-width="820px"
      add-button-text="新增规则"
    />
  </div>
</template>

<script setup>
import { NTag } from 'naive-ui'
import { computed, h } from 'vue'
import { AiCrudPage } from '@/components/ai-form'

defineOptions({ name: 'AppCenterCodeRules' })

const sceneOptions = [
  { label: '通用', value: 'COMMON' },
  { label: '单据', value: 'DOCUMENT' },
  { label: '物料', value: 'MATERIAL' },
  { label: '客户', value: 'CUSTOMER' },
  { label: '订单', value: 'ORDER' },
  { label: '合同', value: 'CONTRACT' },
]

const resetPolicyOptions = [
  { label: '按模板自动', value: 'AUTO' },
  { label: '不重置', value: 'NONE' },
  { label: '每年重置', value: 'YEAR' },
  { label: '每月重置', value: 'MONTH' },
  { label: '每天重置', value: 'DAY' },
  { label: '每小时重置', value: 'HOUR' },
  { label: '每分钟重置', value: 'MINUTE' },
  { label: '每秒重置', value: 'SECOND' },
]

const statusOptions = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 },
]
const codeTemplatePlaceholder = ['例如 WL', '$', '{yyyyMMddHHmmss}', '$', '{seq:3}'].join('')

const searchSchema = computed(() => [
  { field: 'ruleCode', label: '规则编码', type: 'input', props: { placeholder: '请输入规则编码' } },
  { field: 'ruleName', label: '规则名称', type: 'input', props: { placeholder: '请输入规则名称' } },
  { field: 'scene', label: '适用场景', type: 'select', props: { options: sceneOptions, clearable: true } },
  { field: 'status', label: '状态', type: 'select', props: { options: statusOptions, clearable: true } },
])

const tableColumns = computed(() => [
  { prop: 'ruleCode', label: '规则编码', minWidth: 150 },
  { prop: 'ruleName', label: '规则名称', minWidth: 160 },
  {
    prop: 'scene',
    label: '场景',
    width: 110,
    render: row => sceneOptions.find(item => item.value === row.scene)?.label || row.scene || '-',
  },
  { prop: 'template', label: '编码模板', minWidth: 260, ellipsis: { tooltip: true } },
  {
    prop: 'resetPolicy',
    label: '重置周期',
    width: 120,
    render: row => resetPolicyOptions.find(item => item.value === row.resetPolicy)?.label || row.resetPolicy || '-',
  },
  { prop: 'seqLength', label: '流水长度', width: 96 },
  {
    prop: 'status',
    label: '状态',
    width: 90,
    render: row => h(NTag, {
      type: Number(row.status) === 1 ? 'success' : 'default',
      bordered: false,
      size: 'small',
    }, () => Number(row.status) === 1 ? '启用' : '停用'),
  },
  {
    prop: 'builtin',
    label: '来源',
    width: 90,
    render: row => Number(row.builtin) === 1 ? '内置' : '自定义',
  },
  { prop: 'remark', label: '备注', minWidth: 180, ellipsis: { tooltip: true } },
])

const editSchema = computed(() => [
  {
    field: 'ruleCode',
    label: '规则编码',
    type: 'input',
    required: true,
    props: { placeholder: '如 material_code' },
  },
  {
    field: 'ruleName',
    label: '规则名称',
    type: 'input',
    required: true,
    props: { placeholder: '如 物料编码' },
  },
  {
    field: 'scene',
    label: '适用场景',
    type: 'select',
    defaultValue: 'COMMON',
    props: { options: sceneOptions },
  },
  {
    field: 'status',
    label: '状态',
    type: 'select',
    defaultValue: 1,
    props: { options: statusOptions },
  },
  {
    field: 'template',
    label: '编码模板',
    type: 'input',
    required: true,
    props: { placeholder: codeTemplatePlaceholder },
    span: 2,
  },
  {
    field: 'resetPolicy',
    label: '流水重置',
    type: 'select',
    defaultValue: 'AUTO',
    props: { options: resetPolicyOptions },
  },
  {
    field: 'seqLength',
    label: '默认流水长度',
    type: 'input-number',
    defaultValue: 4,
    props: { min: 1, max: 16 },
  },
  {
    field: 'remark',
    label: '备注',
    type: 'textarea',
    props: { placeholder: '说明适用场景、示例或业务规则' },
    span: 2,
  },
])
</script>

<style scoped>
.code-rule-page {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  padding: 16px;
  background: #f5f7fb;
}

.code-rule-page :deep(.ai-crud-page) {
  flex: 1;
  min-height: 0;
}
</style>
