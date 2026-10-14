<template>
  <div class="system-config-page">
    <AiCrudPage
      ref="crudRef"
      api="/system/config"
      :api-config="{
        list: 'get@/system/config/page',
        detail: 'post@/system/config/getById',
        add: 'post@/system/config/add',
        update: 'post@/system/config/edit',
        delete: 'post@/system/config/removeBatch',
      }"
      :search-schema="searchSchema"
      :columns="tableColumns"
      :edit-schema="editSchema"
      row-key="configId"
      add-button-text="新增配置"
      :load-detail-on-edit="true"
    />
  </div>
</template>

<script setup>
import { computed, h, ref } from 'vue'
import { AiCrudPage } from '@/components/ai-form'
import SystemTableCell from '@/components/common/SystemTableCell.vue'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables'
import { request } from '@/utils'

defineOptions({ name: 'SystemConfig' })

const { dict } = useDict('sys_config_type')

const configTypeOptions = computed(() => dict.value.sys_config_type || [])

const crudRef = ref(null)

// 搜索表单配置
const searchSchema = computed(() => [
  {
    field: 'configName',
    label: '参数名称',
    type: 'input',
    props: {
      placeholder: '请输入参数名称',
    },
  },
  {
    field: 'configKey',
    label: '参数键名',
    type: 'input',
    props: {
      placeholder: '请输入参数键名',
    },
  },
  {
    field: 'configType',
    label: '系统内置',
    type: 'select',
    props: {
      placeholder: '请选择',
      options: configTypeOptions.value,
    },
  },
])

// 表格列配置
const tableColumns = computed(() => [
  {
    prop: 'configName',
    label: '参数',
    minWidth: 240,
    render: row => h(SystemTableCell, {
      title: row.configName,
      subtitle: row.configKey,
      interactive: true,
      tooltip: `查看参数：${row.configName || row.configKey || '-'}`,
      onActivate: () => crudRef.value?.showDetail(row),
    }),
  },
  {
    prop: 'configValue',
    label: '参数键值',
    minWidth: 180,
  },
  {
    prop: 'configType',
    label: '系统内置',
    width: 100,
    render: (row) => {
      return h(DictTag, {
        options: configTypeOptions.value,
        value: row.configType,
        size: 'small',
      })
    },
  },
  {
    prop: 'sort',
    label: '排序',
    width: 100,
  },
  {
    prop: 'configDesc',
    label: '参数描述',
    minWidth: 200,
  },
  {
    prop: 'createTime',
    label: '创建时间',
    width: 172,
  },
  {
    prop: 'action',
    label: '操作',
    width: 140,
    fixed: 'right',
    actions: [
      { label: '编辑', key: 'edit', onClick: handleEdit },
      {
        label: '删除',
        key: 'delete',
        type: 'error',
        onClick: handleDelete,
        visible: row => String(row.configType || '').toUpperCase() !== 'Y',
      },
    ],
  },
])

// 编辑表单配置
const editSchema = computed(() => [
  {
    type: 'divider',
    label: '基础信息',
    props: {
      titlePlacement: 'left',
    },
    span: 2,
  },
  {
    field: 'configName',
    label: '参数名称',
    type: 'input',
    rules: [{ required: true, message: '请输入参数名称', trigger: 'blur' }],
    props: {
      placeholder: '请输入参数名称',
    },
  },
  {
    field: 'configKey',
    label: '参数键名',
    type: 'input',
    rules: [{ required: true, message: '请输入参数键名', trigger: 'blur' }],
    disabled: ({ formData, context }) => Boolean(context?.isEdit || formData?.configId)
      && String(formData?.configType || '').toUpperCase() === 'Y',
    props: {
      placeholder: '请输入参数键名，如：sys.login.timeout',
    },
  },
  {
    field: 'configValue',
    label: '参数键值',
    type: 'textarea',
    span: 2,
    rules: [{ required: true, message: '请输入参数键值', trigger: 'blur' }],
    props: {
      placeholder: '请输入参数键值',
      rows: 3,
    },
  },
  {
    field: 'configType',
    label: '系统内置',
    type: 'radio',
    defaultValue: 'N',
    rules: [{ required: true, message: '请选择是否系统内置', trigger: 'change' }],
    disabled: ({ formData, context }) => Boolean(context?.isEdit || formData?.configId)
      && String(formData?.configType || '').toUpperCase() === 'Y',
    props: {
      options: configTypeOptions.value,
    },
  },
  {
    field: 'sort',
    label: '排序',
    type: 'number',
    defaultValue: 0,
    props: {
      placeholder: '排序值',
      min: 0,
    },
  },
  {
    field: 'configDesc',
    label: '参数描述',
    type: 'textarea',
    span: 2,
    props: {
      placeholder: '请输入参数描述',
      rows: 3,
    },
  },
])

// 编辑
function handleEdit(row) {
  crudRef.value?.showEdit(row)
}

// 删除
function handleDelete(row) {
  window.$dialog.warning({
    title: '确认删除',
    content: '确定要删除该配置吗？删除后将无法恢复！',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await request.post('/system/config/remove', null, {
          params: { configId: row.configId },
        })
        if (res.code === 200) {
          window.$message.success('删除成功')
          crudRef.value?.refresh()
        }
      }
      catch {
        window.$message.error('删除失败')
      }
    },
  })
}
</script>

<style scoped>
.system-config-page {
  height: 100%;
}
</style>
