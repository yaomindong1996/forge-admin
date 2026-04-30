<template>
  <div class="system-org-page">
    <!-- 组织列表 -->
    <div class="org-content">
      <AiCrudPage
        ref="crudRef"
        api="/system/org"
        :api-config="{
          list: 'get@/system/org/tree',
          detail: 'post@/system/org/getById',
          add: 'post@/system/org/add',
          update: 'post@/system/org/edit',
          delete: 'post@/system/org/remove',
        }"
        :search-schema="searchSchema"
        :columns="tableColumns"
        :edit-schema="editSchema"
        :before-render-list="beforeRenderList"
        :before-submit="beforeSubmit"
        row-key="id"
        :edit-grid-cols="2"
        modal-width="900px"
        :show-pagination="false"
        :lazy="false"
        add-button-text="新增组织"
        :table-props="{
          expandedRowKeys: expandedKeys,
          onUpdateExpandedRowKeys: handleExpandedKeysUpdate,
        }"
      >
        <!-- 自定义工具栏 -->
        <template #toolbar-end>
          <n-button @click="toggleExpandAll">
            <template #icon>
              <i :class="expandAll ? 'i-material-symbols:unfold-less' : 'i-material-symbols:unfold-more'" />
            </template>
            {{ expandAll ? '折叠全部' : '展开全部' }}
          </n-button>
        </template>
      </AiCrudPage>
    </div>
  </div>
</template>

<script setup>
import { NTag } from 'naive-ui'
import { computed, h, nextTick, onMounted, ref } from 'vue'
import { AiCrudPage } from '@/components/ai-form'
import { request } from '@/utils'

defineOptions({ name: 'SystemOrg' })

const crudRef = ref(null)
const expandAll = ref(true)
const expandedKeys = ref([])
const parentOrgOptions = ref([{ label: '顶级组织', value: 0, key: 0 }])
const searchRegionOptions = ref([])
const editRegionOptions = ref([])

// 组织类型选项
const orgTypeOptions = [
  { label: '公司', value: 1 },
  { label: '部门', value: 2 },
  { label: '小组', value: 3 },
]

// 组织状态选项
const orgStatusOptions = [
  { label: '正常', value: 1 },
  { label: '禁用', value: 0 },
]

// 搜索表单配置
const searchSchema = [
  {
    field: 'orgName',
    label: '组织名称',
    type: 'input',
    props: {
      placeholder: '请输入组织名称',
    },
  },
  {
    field: 'regionCode',
    label: '行政区划',
    type: 'treeSelect',
    props: {
      placeholder: '请选择行政区划',
      clearable: true,
      filterable: true,
    },
    options: () => searchRegionOptions.value,
  },
  {
    field: 'orgType',
    label: '组织类型',
    type: 'select',
    props: {
      placeholder: '请选择组织类型',
      options: orgTypeOptions,
    },
  },
  {
    field: 'orgStatus',
    label: '状态',
    type: 'select',
    props: {
      placeholder: '请选择状态',
      options: orgStatusOptions,
    },
  },
]

// 表格列配置
const tableColumns = computed(() => [
  {
    prop: 'orgName',
    label: '组织名称',
    width: 200,
  },
  {
    prop: 'orgType',
    label: '组织类型',
    width: 100,
    render: (row) => {
      const typeMap = {
        1: { text: '公司', type: 'success' },
        2: { text: '部门', type: 'info' },
        3: { text: '小组', type: 'warning' },
      }
      const config = typeMap[row.orgType] || { text: '未知', type: 'default' }
      return h(NTag, { type: config.type, size: 'small' }, { default: () => config.text })
    },
  },
  {
    prop: 'leaderName',
    label: '负责人',
    width: 120,
  },
  {
    prop: 'phone',
    label: '联系电话',
    width: 130,
  },
  {
    prop: 'address',
    label: '地址',
    width: 200,
  },
  {
    prop: 'regionCode',
    label: '行政区划',
    width: 150,
    render: (row) => {
      if (!row.regionCode)
        return '-'
      const name = findRegionName(searchRegionOptions.value, row.regionCode)
      return name || row.regionCode
    },
  },
  {
    prop: 'sort',
    label: '排序',
    width: 80,
  },
  {
    prop: 'orgStatus',
    label: '状态',
    width: 80,
    render: (row) => {
      return h(NTag, { type: row.orgStatus === 1 ? 'success' : 'error', size: 'small' }, { default: () => row.orgStatus === 1 ? '正常' : '禁用' },
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
    width: 150,
    fixed: 'right',
    actions: [
      { label: '新增', key: 'add', type: 'primary', onClick: handleAdd },
      { label: '编辑', key: 'edit', type: 'primary', onClick: handleEdit },
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
    field: 'parentId',
    label: '上级组织',
    type: 'treeSelect',
    defaultValue: 0,
    props: {
      placeholder: '请选择上级组织',
      clearable: true,
      filterable: true,
      defaultExpandAll: true,
    },
    options: () => parentOrgOptions.value,
  },
  {
    field: 'orgName',
    label: '组织名称',
    type: 'input',
    rules: [{ required: true, message: '请输入组织名称', trigger: 'blur' }],
    props: {
      placeholder: '请输入组织名称',
    },
  },
  {
    field: 'orgType',
    label: '组织类型',
    type: 'radio',
    defaultValue: 2,
    rules: [{ required: true, type: 'number', message: '请选择组织类型', trigger: 'change' }],
    props: {
      options: orgTypeOptions,
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

  // 负责人信息
  {
    type: 'divider',
    label: '负责人信息',
    props: {
      titlePlacement: 'left',
    },
    span: 2,
  },
  {
    field: 'leaderName',
    label: '负责人',
    type: 'input',
    props: {
      placeholder: '请输入负责人姓名',
    },
  },
  {
    field: 'phone',
    label: '联系电话',
    type: 'input',
    rules: [
      { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' },
    ],
    props: {
      placeholder: '请输入联系电话',
    },
  },
  {
    field: 'address',
    label: '地址',
    type: 'input',
    span: 2,
    props: {
      placeholder: '请输入组织地址',
    },
  },
  {
    field: 'regionCode',
    label: '行政区划',
    type: 'treeSelect',
    props: {
      placeholder: '请选择行政区划',
      clearable: true,
      filterable: true,
    },
    options: () => editRegionOptions.value,
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
    field: 'orgStatus',
    label: '组织状态',
    type: 'radio',
    defaultValue: 1,
    props: {
      options: orgStatusOptions,
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

// 组件挂载时加载上级组织选项
onMounted(() => {
  loadParentOrgOptions()
})

// 加载上级组织选项
async function loadParentOrgOptions() {
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
      parentOrgOptions.value = [
        { label: '顶级组织', value: 0, key: 0 },
        ...convertToTreeSelect(res.data || []),
      ]

      // 同时加载行政区划选项
      await loadRegionOptions()
    }
  }
  catch (error) {
    console.error('加载上级组织选项失败:', error)
  }
}

// 加载行政区划选项（一次性加载内蒙完整区划树，含虚拟组织）
async function loadRegionOptions() {
  try {
    const res = await request.get('/system/region/treeAll', { params: { rootCode: '150000', dataRight: true } })
    if (res.code === 200) {
      const data = res.data || []
      // 搜索场景：虚拟节点可选（代表"该区域下所有"）
      searchRegionOptions.value = convertRegionToTreeSelect(data, false)
      // 编辑场景：虚拟节点不可选（避免存入ALL后缀编码）
      editRegionOptions.value = convertRegionToTreeSelect(data, true)
    }
  }
  catch (error) {
    console.error('加载行政区划选项失败:', error)
  }
}

// 将后端返回的树形数据转换为TreeSelect组件需要的格式
// virtualDisabled: true=编辑表单（虚拟节点不可选），false=搜索筛选（虚拟节点可选）
function convertRegionToTreeSelect(list, virtualDisabled = true) {
  return list.map((item) => {
    const node = {
      label: item.name,
      value: item.code,
      key: item.code,
    }
    if (virtualDisabled && item.code && item.code.endsWith('ALL')) {
      node.disabled = true
    }
    if (item.children && item.children.length > 0) {
      node.children = convertRegionToTreeSelect(item.children, virtualDisabled)
    }
    return node
  })
}

// 在区划树中根据code查找名称
function findRegionName(options, code) {
  for (const item of options) {
    if (item.value === code)
      return item.label
    if (item.children) {
      const name = findRegionName(item.children, code)
      if (name)
        return name
    }
  }
  return null
}

// 获取所有节点的 key
function getAllKeys(list, keys = []) {
  list.forEach((item) => {
    keys.push(item.id)
    if (item.children && item.children.length > 0) {
      getAllKeys(item.children, keys)
    }
  })
  return keys
}

// 列表数据渲染前处理
function beforeRenderList(list) {
  console.log('加载的树形数据:', list)

  // 如果默认展开，收集所有节点的 key
  if (expandAll.value) {
    expandedKeys.value = getAllKeys(list)
  }

  return list
}

// 表单提交前处理
function beforeSubmit(formData) {
  console.log('提交的表单数据:', formData)
  return formData
}

// 处理表格展开状态更新
function handleExpandedKeysUpdate(keys) {
  expandedKeys.value = keys

  // 根据当前展开的 key 数量判断是否全部展开
  const tableData = crudRef.value?.getTableData() || []
  const allKeys = getAllKeys(tableData)
  expandAll.value = keys.length === allKeys.length
}

// 展开/折叠所有
function toggleExpandAll() {
  expandAll.value = !expandAll.value

  if (expandAll.value) {
    // 展开所有：获取当前表格数据的所有 key
    const tableData = crudRef.value?.getTableData() || []
    expandedKeys.value = getAllKeys(tableData)
  }
  else {
    // 收起所有：清空展开的 key
    expandedKeys.value = []
  }
}

// 新增子组织
async function handleAdd(row) {
  await loadParentOrgOptions()

  const parentIdField = editSchema.value.find(f => f.field === 'parentId')
  const originalDefaultValue = parentIdField?.defaultValue

  if (row && parentIdField) {
    parentIdField.defaultValue = row.id
  }

  crudRef.value?.showAdd()

  await nextTick()
  await nextTick()

  if (parentIdField && originalDefaultValue !== undefined) {
    parentIdField.defaultValue = originalDefaultValue
  }
}

// 编辑
async function handleEdit(row) {
  // 加载上级组织选项
  await loadParentOrgOptions()

  crudRef.value?.showEdit(row)
}

// 删除
function handleDelete(row) {
  window.$dialog.warning({
    title: '确认删除',
    content: `确定要删除组织"${row.orgName}"吗？删除后将无法恢复！`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await request.post('/system/org/remove', null, { params: { id: row.id } })
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
</script>

<style scoped>
.system-org-page {
  height: 100%;
  padding: 20px;
  display: flex;
  flex-direction: column;
}

/* 组织列表内容 */
.org-content {
  flex: 1;
  min-height: 0;
  background: #fff;
  border-radius: 12px;
  padding: 16px 20px;
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.04);
}

.org-content :deep(.ai-crud-page) {
  height: 100%;
}

/* 深色模式 */
.dark .org-content {
  background: #0f172a !important;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
}
</style>
