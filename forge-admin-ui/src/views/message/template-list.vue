<template>
  <AiCrudPage
    ref="crudRef"
    api="/api/message/template"
    :api-config="{
      list: 'get@/api/message/template/page',
      detail: 'get@/api/message/template/:id',
      add: 'post@/api/message/template',
      update: 'put@/api/message/template',
      delete: 'delete@/api/message/template/:id',
    }"
    :search-schema="searchSchema"
    :columns="tableColumns"
    :edit-schema="editSchema"
    row-key="id"
    add-button-text="新增模板"
    :load-detail-on-edit="true"
  />
</template>

<script setup>
import { NTag } from 'naive-ui'
import { computed, h, ref } from 'vue'
import { AiCrudPage } from '@/components/ai-form'

defineOptions({ name: 'MessageTemplate' })

const crudRef = ref(null)

// 消息类型选项
const messageTypeOptions = [
  { label: '系统消息', value: 'SYSTEM' },
  { label: '短信', value: 'SMS' },
  { label: '邮件', value: 'EMAIL' },
  { label: '自定义', value: 'CUSTOM' },
]

// 发送渠道选项
const channelOptions = [
  { label: '站内信', value: 'WEB' },
  { label: '短信', value: 'SMS' },
  { label: '邮件', value: 'EMAIL' },
  { label: '推送', value: 'PUSH' },
]

// 搜索表单配置
const searchSchema = [
  {
    field: 'type',
    label: '消息类型',
    type: 'select',
    props: {
      placeholder: '请选择消息类型',
      options: messageTypeOptions,
    },
  },
  {
    field: 'keyword',
    label: '关键词',
    type: 'input',
    props: {
      placeholder: '请输入模板编码或名称',
    },
  },
]

// 表格列配置
const tableColumns = computed(() => [
  {
    prop: 'templateCode',
    label: '模板编码',
    width: 150,
  },
  {
    prop: 'templateName',
    label: '模板名称',
    width: 150,
  },
  {
    prop: 'type',
    label: '消息类型',
    width: 100,
    render: (row) => {
      const typeMap = {
        SYSTEM: { label: '系统消息', type: 'info' },
        SMS: { label: '短信', type: 'warning' },
        EMAIL: { label: '邮件', type: 'success' },
        CUSTOM: { label: '自定义', type: 'default' },
      }
      const config = typeMap[row.type] || { label: row.type, type: 'default' }
      return h(NTag, { type: config.type, size: 'small' }, { default: () => config.label })
    },
  },
  {
    prop: 'titleTemplate',
    label: '标题模板',
    width: 200,
  },
  {
    prop: 'defaultChannel',
    label: '默认渠道',
    width: 100,
    render: (row) => {
      const channelMap = {
        WEB: '站内信',
        SMS: '短信',
        EMAIL: '邮件',
        PUSH: '推送',
      }
      return channelMap[row.defaultChannel] || row.defaultChannel
    },
  },
  {
    prop: 'enabled',
    label: '状态',
    width: 80,
    render: (row) => {
      return h(NTag, {
        type: row.enabled === 1 ? 'success' : 'error',
        size: 'small',
      }, { default: () => row.enabled === 1 ? '启用' : '禁用' })
    },
  },
  {
    prop: 'createTime',
    label: '创建时间',
    width: 180,
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
const editSchema = [
  {
    type: 'divider',
    label: '基础信息',
    props: {
      titlePlacement: 'left',
    },
    span: 2,
  },
  {
    field: 'templateCode',
    label: '模板编码',
    type: 'input',
    rules: [{ required: true, message: '请输入模板编码', trigger: 'blur' }],
    props: {
      placeholder: '请输入模板编码，全局唯一',
    },
    editDisabled: true,
  },
  {
    field: 'templateName',
    label: '模板名称',
    type: 'input',
    rules: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
    props: {
      placeholder: '请输入模板名称',
    },
  },
  {
    field: 'type',
    label: '消息类型',
    type: 'select',
    defaultValue: 'SYSTEM',
    rules: [{ required: true, message: '请选择消息类型', trigger: 'change' }],
    props: {
      options: messageTypeOptions,
    },
  },
  {
    field: 'defaultChannel',
    label: '默认渠道',
    type: 'select',
    defaultValue: 'WEB',
    props: {
      options: channelOptions,
    },
  },
  {
    field: 'titleTemplate',
    label: '标题模板',
    type: 'textarea',
    span: 2,
    rules: [{ required: true, message: '请输入标题模板', trigger: 'blur' }],
    props: {
      placeholder: '请输入标题模板，支持${变量}占位符',
      rows: 2,
    },
  },
  {
    field: 'contentTemplate',
    label: '内容模板',
    type: 'textarea',
    span: 2,
    rules: [{ required: true, message: '请输入内容模板', trigger: 'blur' }],
    props: {
      placeholder: '请输入内容模板，支持${变量}占位符',
      rows: 6,
    },
  },
  {
    field: 'enabled',
    label: '是否启用',
    type: 'radio',
    defaultValue: 1,
    props: {
      options: [
        { label: '启用', value: 1 },
        { label: '禁用', value: 0 },
      ],
    },
  },
  {
    field: 'remark',
    label: '备注说明',
    type: 'textarea',
    span: 2,
    props: {
      placeholder: '请输入备注说明',
      rows: 3,
    },
  },
]

// 编辑
function handleEdit(row) {
  crudRef.value?.handleEdit(row)
}

// 删除
function handleDelete(row) {
  crudRef.value?.handleDelete(row)
}
</script>

<style scoped>
:deep(.n-form-item-label) {
  font-weight: 500;
}
</style>
