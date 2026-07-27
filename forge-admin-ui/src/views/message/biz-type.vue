<template>
  <div class="biz-type-page">
    <AiCrudPage
      ref="crudRef"
      :api-config="apiConfig"
      :search-schema="searchSchema"
      :columns="tableColumns"
      row-key="id"
      :edit-schema="editSchema"
      :load-detail-on-edit="true"
    />
  </div>
</template>

<script setup>
import { computed, h } from 'vue'
import { AiCrudPage } from '@/components/ai-form'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'
import { toNumberDictOptions } from '@/utils/dict-options'

defineOptions({ name: 'MessageBizType' })

const crudRef = ref(null)
const { dict } = useDict('sys_enable_disable', 'sys_link_open_target')
const statusOptions = computed(() => toNumberDictOptions(dict.value.sys_enable_disable))
const jumpTargetOptions = computed(() => dict.value.sys_link_open_target || [])

const apiConfig = {
  list: 'get@/api/message/bizType/page',
  detail: 'get@/api/message/bizType/:id',
  add: 'post@/api/message/bizType',
  update: 'put@/api/message/bizType',
  delete: 'delete@/api/message/bizType/:id',
}

const searchSchema = computed(() => [
  {
    field: 'bizType',
    label: '业务类型编码',
    type: 'input',
    props: {
      placeholder: '请输入业务类型编码',
    },
  },
  {
    field: 'bizName',
    label: '业务类型名称',
    type: 'input',
    props: {
      placeholder: '请输入业务类型名称',
    },
  },
  {
    field: 'enabled',
    label: '状态',
    type: 'select',
    props: {
      placeholder: '请选择状态',
      clearable: true,
      options: statusOptions.value,
    },
  },
])

const tableColumns = computed(() => [
  {
    prop: 'bizType',
    label: '业务类型编码',
    width: 150,
  },
  {
    prop: 'bizName',
    label: '业务类型名称',
    width: 150,
  },
  {
    prop: 'jumpUrl',
    label: '跳转URL模板',
    ellipsis: { tooltip: true },
  },
  {
    prop: 'jumpTarget',
    label: '跳转方式',
    width: 100,
    render: row => h(DictTag, { options: jumpTargetOptions.value, value: row.jumpTarget, forceTag: true }),
  },
  {
    prop: 'icon',
    label: '图标',
    width: 100,
  },
  {
    prop: 'sort',
    label: '排序',
    width: 80,
  },
  {
    prop: 'enabled',
    label: '状态',
    width: 80,
    render: row => h(DictTag, { options: statusOptions.value, value: row.enabled, forceTag: true }),
  },
  {
    prop: 'remark',
    label: '备注',
    width: 150,
    ellipsis: { tooltip: true },
  },
])

const editSchema = computed(() => [
  {
    field: 'bizType',
    label: '业务类型编码',
    type: 'input',
    rules: [{ required: true, message: '请输入业务类型编码', trigger: 'blur' }],
    props: {
      placeholder: '请输入业务类型编码（如：ORDER、APPROVAL）',
    },
  },
  {
    field: 'bizName',
    label: '业务类型名称',
    type: 'input',
    rules: [{ required: true, message: '请输入业务类型名称', trigger: 'blur' }],
    props: {
      placeholder: '请输入业务类型名称',
    },
  },
  {
    field: 'jumpUrl',
    label: '跳转URL模板',
    type: 'input',
    props: {
      placeholder: `支持变量：\${bizKey}、\${messageId}，如：/order/detail?id=\${bizKey}`,
    },
  },
  {
    field: 'jumpTarget',
    label: '跳转方式',
    type: 'select',
    defaultValue: '_self',
    props: {
      options: jumpTargetOptions.value,
    },
  },
  {
    field: 'icon',
    label: '图标',
    type: 'input',
    props: {
      placeholder: '请输入图标名称',
    },
  },
  {
    field: 'sort',
    label: '排序',
    type: 'number',
    defaultValue: 0,
    props: {
      min: 0,
      placeholder: '数值越小越靠前',
    },
  },
  {
    field: 'enabled',
    label: '状态',
    type: 'switch',
    defaultValue: 1,
    props: {
      checkedValue: 1,
      uncheckedValue: 0,
    },
  },
  {
    field: 'remark',
    label: '备注',
    type: 'textarea',
    props: {
      placeholder: '请输入备注说明',
      rows: 3,
    },
  },
])
</script>

<style scoped>
.biz-type-page {
  height: 100%;
}
</style>
