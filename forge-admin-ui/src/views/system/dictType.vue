<template>
  <div class="dict-type-page">
    <AiCrudPage
      ref="crudRef"
      api="/system/dict/type"
      :api-config="{
        list: 'get@/system/dict/type/page',
        detail: 'post@/system/dict/type/getById',
        add: 'post@/system/dict/type/add',
        update: 'post@/system/dict/type/edit',
        delete: 'post@/system/dict/type/remove',
      }"
      :load-detail-on-edit="true"
      :search-schema="searchSchema"
      :columns="tableColumns"
      :edit-schema="editSchema"
      row-key="dictId"
      add-button-text="新增字典类型"
    />
  </div>
</template>

<script setup>
import { NTag } from 'naive-ui'
import { computed, h, ref } from 'vue'
import { useRouter } from 'vue-router'
import { AiCrudPage } from '@/components/ai-form'
import { closeAndOpen, request } from '@/utils'

defineOptions({ name: 'DictType' })

const router = useRouter()
const crudRef = ref(null)

// 字典状态选项
const statusOptions = [
  { label: '正常', value: 1 },
  { label: '禁用', value: 0 },
]

// 搜索表单配置
const searchSchema = [
  {
    field: 'dictName',
    label: '字典名称',
    type: 'input',
    props: {
      placeholder: '请输入字典名称',
    },
  },
  {
    field: 'dictType',
    label: '字典类型',
    type: 'input',
    props: {
      placeholder: '请输入字典类型',
    },
  },
  {
    field: 'dictStatus',
    label: '状态',
    type: 'select',
    props: {
      placeholder: '请选择状态',
      options: statusOptions,
    },
  },
]

// 管理字典数据
function handleManageData(row) {
  closeAndOpen(
    '/system/dictData',
    {
      path: '/system/dictData',
      query: {
        dictType: row.dictType,
        dictName: row.dictName,
      },
    },
  )
}

// 编辑
function handleEdit(row) {
  crudRef.value?.showEdit(row)
}

// 删除
function handleDelete(row) {
  window.$dialog.warning({
    title: '确认删除',
    content: '确定要删除该字典类型吗？删除后将无法恢复！',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await request.post('/system/dict/type/remove', null, {
          params: { dictId: row.dictId },
        })
        if (res.code === 200) {
          window.$message.success('删除成功')
          crudRef.value?.refresh()
        }
      }
      catch (error) {
        window.$message.error('删除失败')
      }
    },
  })
}

// 表格列配置
const tableColumns = computed(() => [
  {
    prop: 'dictId',
    label: '字典ID',
    width: 100,
  },
  {
    prop: 'dictName',
    label: '字典名称',
    width: 200,
  },
  {
    prop: 'dictType',
    label: '字典类型',
    width: 200,
  },
  {
    prop: 'dictStatus',
    label: '状态',
    width: 100,
    render: (row) => {
      return h(NTag, { type: row.dictStatus === 1 ? 'success' : 'error', size: 'small' }, { default: () => row.dictStatus === 1 ? '正常' : '禁用' },
      )
    },
  },
  {
    prop: 'remark',
    label: '备注',
    width: 200,
  },
  {
    prop: 'createTime',
    label: '创建时间',
    width: 180,
  },
  {
    prop: 'action',
    label: '操作',
    width: 150,
    fixed: 'right',
    actions: [
      { label: '字典数据', key: 'dictData', onClick: handleManageData },
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
    field: 'dictName',
    label: '字典名称',
    type: 'input',
    rules: [{ required: true, message: '请输入字典名称', trigger: 'blur' }],
    props: {
      placeholder: '请输入字典名称',
    },
  },
  {
    field: 'dictType',
    label: '字典类型',
    type: 'input',
    rules: [{ required: true, message: '请输入字典类型', trigger: 'blur' }],
    props: {
      placeholder: '请输入字典类型，如：sys_user_sex',
    },
  },
  {
    field: 'dictStatus',
    label: '状态',
    type: 'radio',
    defaultValue: 1,
    rules: [{ required: true, type: 'number', message: '请选择状态', trigger: 'change' }],
    props: {
      options: statusOptions,
    },
  },
  {
    field: 'remark',
    label: '备注',
    type: 'textarea',
    span: 2,
    props: {
      placeholder: '请输入备注',
      rows: 3,
    },
  },
]
</script>

<style scoped>
.dict-type-page {
  height: 100%;
}
</style>
