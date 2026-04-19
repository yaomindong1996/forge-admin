<template>
  <div class="system-post-page">
    <AiCrudPage
      ref="crudRef"
      api="/system/post"
      :api-config="{
        list: 'get@/system/post/page',
        detail: 'post@/system/post/getById',
        add: 'post@/system/post/add',
        update: 'post@/system/post/edit',
        delete: 'post@/system/post/remove',
      }"
      :search-schema="searchSchema"
      :columns="tableColumns"
      :edit-schema="editSchema"
      row-key="id"
      :edit-grid-cols="2"
      modal-width="800px"
      add-button-text="新增岗位"
      :hide-selection="false"
      @selection-change="handleSelectionChange"
    >
      <!-- 批量删除按钮 -->
      <template #toolbar-end>
        <n-button
          type="error"
          :disabled="selectedKeys.length === 0"
          size="small"
          @click="handleBatchDelete"
        >
          <template #icon>
            <NIcon><TrashOutline /></NIcon>
          </template>
          批量删除
        </n-button>
      </template>
    </AiCrudPage>
  </div>
</template>

<script setup>
import { TrashOutline } from '@vicons/ionicons5'
import { NIcon, NTag } from 'naive-ui'
import { computed, h, onMounted, ref } from 'vue'
import { AiCrudPage } from '@/components/ai-form'
import { request } from '@/utils'

defineOptions({ name: 'SystemPost' })

const crudRef = ref(null)
const orgOptions = ref([])
const selectedKeys = ref([])

// 岗位类型选项
const postTypeOptions = [
  { label: '管理岗', value: 1 },
  { label: '技术岗', value: 2 },
  { label: '业务岗', value: 3 },
  { label: '其他', value: 4 },
]

// 岗位状态选项
const postStatusOptions = [
  { label: '正常', value: 1 },
  { label: '禁用', value: 0 },
]

// 搜索表单配置
const searchSchema = [
  {
    field: 'postName',
    label: '岗位名称',
    type: 'input',
    props: {
      placeholder: '请输入岗位名称',
    },
  },
  {
    field: 'postCode',
    label: '岗位编码',
    type: 'input',
    props: {
      placeholder: '请输入岗位编码',
    },
  },
  {
    field: 'postType',
    label: '岗位类型',
    type: 'select',
    props: {
      placeholder: '请选择岗位类型',
      options: postTypeOptions,
    },
  },
  {
    field: 'postStatus',
    label: '状态',
    type: 'select',
    props: {
      placeholder: '请选择状态',
      options: postStatusOptions,
    },
  },
]

// 表格列配置
const tableColumns = computed(() => [
  {
    prop: 'postCode',
    label: '岗位编码',
    width: 150,
  },
  {
    prop: 'postName',
    label: '岗位名称',
    width: 180,
  },
  {
    prop: 'postType',
    label: '岗位类型',
    width: 120,
    render: (row) => {
      const typeMap = {
        1: { text: '管理岗', type: 'success' },
        2: { text: '技术岗', type: 'info' },
        3: { text: '业务岗', type: 'warning' },
        4: { text: '其他', type: 'default' },
      }
      const config = typeMap[row.postType] || { text: '未知', type: 'default' }
      return h(NTag, { type: config.type, size: 'small' }, { default: () => config.text })
    },
  },
  {
    prop: 'sort',
    label: '排序',
    width: 80,
  },
  {
    prop: 'postStatus',
    label: '状态',
    width: 100,
    render: (row) => {
      return h(NTag, { type: row.postStatus === 1 ? 'success' : 'error', size: 'small' }, { default: () => row.postStatus === 1 ? '正常' : '禁用' },
      )
    },
  },
  {
    prop: 'remark',
    label: '备注',
    minWidth: 150,
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
const editSchema = ref([
  // 基础信息
  {
    type: 'divider',
    label: '基础信息',
    props: {
      titlePlacement: 'left',
    },
    span: 2,
  },
  {
    field: 'postCode',
    label: '岗位编码',
    type: 'input',
    rules: [{ required: true, message: '请输入岗位编码', trigger: 'blur' }],
    props: {
      placeholder: '请输入岗位编码',
    },
  },
  {
    field: 'postName',
    label: '岗位名称',
    type: 'input',
    rules: [{ required: true, message: '请输入岗位名称', trigger: 'blur' }],
    props: {
      placeholder: '请输入岗位名称',
    },
  },
  {
    field: 'orgId',
    label: '所属组织',
    type: 'treeSelect',
    props: {
      placeholder: '请选择所属组织',
      clearable: true,
      filterable: true,
      defaultExpandAll: true,
    },
    options: () => orgOptions.value,
  },
  {
    field: 'postType',
    label: '岗位类型',
    type: 'radio',
    defaultValue: 2,
    rules: [{ required: true, type: 'number', message: '请选择岗位类型', trigger: 'change' }],
    props: {
      options: postTypeOptions,
    },
  },
  {
    field: 'sort',
    label: '排序',
    type: 'input-number',
    defaultValue: 0,
    props: {
      placeholder: '排序值',
      min: 0,
    },
  },

  // 状态配置
  {
    type: 'divider',
    label: '状态配置',
    props: {
      titlePlacement: 'left',
    },
    span: 2,
  },
  {
    field: 'postStatus',
    label: '岗位状态',
    type: 'radio',
    defaultValue: 1,
    props: {
      options: postStatusOptions,
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
])

// 组件挂载时加载组织选项
onMounted(() => {
  loadOrgOptions()
})

// 加载组织选项
async function loadOrgOptions() {
  try {
    const res = await request.get('/system/org/tree')
    if (res.code === 200) {
      const convertToTreeSelect = (list) => {
        return list.map(item => ({
          label: item.orgName,
          value: item.id,
          key: item.id,
          children: item.children && item.children.length > 0
            ? convertToTreeSelect(item.children)
            : undefined,
        }))
      }
      orgOptions.value = convertToTreeSelect(res.data || [])
    }
  }
  catch (error) {
    console.error('加载组织选项失败:', error)
  }
}

// 编辑
async function handleEdit(row) {
  // 加载组织选项
  await loadOrgOptions()

  crudRef.value?.showEdit(row)
}

// 删除
function handleDelete(row) {
  window.$dialog.warning({
    title: '确认删除',
    content: `确定要删除岗位"${row.postName}"吗？删除后将无法恢复！`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await request.post('/system/post/remove', null, { params: { id: row.id } })
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

// 批量删除
function handleBatchDelete() {
  if (selectedKeys.value.length === 0) {
    window.$message.warning('请先选择要删除的岗位')
    return
  }

  window.$dialog.warning({
    title: '确认删除',
    content: `确定要删除选中的 ${selectedKeys.value.length} 个岗位吗？删除后将无法恢复！`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await request.post('/system/post/removeBatch', selectedKeys.value)
        if (res.code === 200) {
          window.$message.success('批量删除成功')
          selectedKeys.value = []
          crudRef.value?.refresh()
        }
      }
      catch (error) {
        window.$message.error('批量删除失败')
      }
    },
  })
}

// 监听选中项变化
function handleSelectionChange({ keys }) {
  selectedKeys.value = keys
}
</script>

<style scoped>
.system-post-page {
  height: 100%;
}
</style>
