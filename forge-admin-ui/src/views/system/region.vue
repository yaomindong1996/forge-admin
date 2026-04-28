<template>
  <div class="system-region-page">
    <AiCrudPage
      ref="crudRef"
      api="/system/region"
      :api-config="{
        list: 'get@/system/region/tree',
        detail: 'get@/system/region/{code}',
        add: 'post@/system/region/',
        update: 'put@/system/region/',
        delete: 'delete@/system/region/{code}',
      }"
      :search-schema="searchSchema"
      :columns="tableColumns"
      :edit-schema="editSchema"
      :before-render-list="beforeRenderList"
      row-key="code"
      :show-pagination="false"
      add-button-text="新增行政区划"
      :table-props="{
        expandedRowKeys: expandedKeys,
        onUpdateExpandedRowKeys: handleExpandedKeysUpdate,
        onExpand: handleExpand,
      }"
    >
      <!-- 自定义工具栏 -->
      <template #toolbar-end>
        <n-button @click="toggleExpandLoaded">
          <template #icon>
            <i :class="expandLoaded ? 'i-material-symbols:unfold-less' : 'i-material-symbols:unfold-more'" />
          </template>
          {{ expandLoaded ? '折叠已加载' : '展开已加载' }}
        </n-button>
      </template>
    </AiCrudPage>
  </div>
</template>

<script setup>
import { NTag } from 'naive-ui'
import { computed, h, onMounted, ref } from 'vue'
import { AiCrudPage } from '@/components/ai-form'
import { request } from '@/utils'

defineOptions({ name: 'SystemRegion' })

const crudRef = ref(null)
const expandLoaded = ref(false)
const expandedKeys = ref([])
const parentRegionOptions = ref([{ label: '顶级区域', value: '', key: '' }])

const levelOptions = [
  { label: '省', value: 1 },
  { label: '市', value: 2 },
  { label: '区/县', value: 3 },
  { label: '街道', value: 4 },
]

const searchSchema = [
  {
    field: 'name',
    label: '名称',
    type: 'input',
    props: {
      placeholder: '请输入行政区划名称',
    },
  },
]

const tableColumns = computed(() => [
  {
    prop: 'name',
    label: '名称',
    width: 200,
  },
  {
    prop: 'code',
    label: '编码',
    width: 120,
  },
  {
    prop: 'level',
    label: '级别',
    width: 100,
    render: (row) => {
      const levelMap = {
        1: { text: '省', type: 'success' },
        2: { text: '市', type: 'info' },
        3: { text: '区/县', type: 'warning' },
        4: { text: '街道', type: 'default' },
      }
      const config = levelMap[row.level] || { text: '未知', type: 'default' }
      return h(NTag, { type: config.type, size: 'small' }, { default: () => config.text })
    },
  },
  {
    prop: 'fullName',
    label: '全名',
    minWidth: 200,
  },
  {
    prop: 'cityCode',
    label: '地市编码',
    width: 120,
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

const editSchema = ref([
  {
    field: 'parentCode',
    label: '上级区域',
    type: 'treeSelect',
    defaultValue: '',
    props: {
      placeholder: '请选择上级区域',
      clearable: true,
      filterable: true,
      defaultExpandAll: true,
    },
    options: () => parentRegionOptions.value,
  },
  {
    field: 'code',
    label: '编码',
    type: 'input',
    rules: [{ required: true, message: '请输入行政区划编码', trigger: 'blur' }],
    props: {
      placeholder: '请输入行政区划编码',
    },
  },
  {
    field: 'name',
    label: '名称',
    type: 'input',
    rules: [{ required: true, message: '请输入行政区划名称', trigger: 'blur' }],
    props: {
      placeholder: '请输入行政区划名称',
    },
  },
  {
    field: 'level',
    label: '级别',
    type: 'select',
    defaultValue: 1,
    rules: [{ required: true, type: 'number', message: '请选择行政级别', trigger: 'change' }],
    props: {
      placeholder: '请选择行政级别',
      options: levelOptions,
    },
  },
  {
    field: 'fullName',
    label: '全名',
    type: 'input',
    span: 2,
    props: {
      placeholder: '请输入行政区划全名',
    },
  },
  {
    field: 'cityCode',
    label: '地市编码',
    type: 'input',
    props: {
      placeholder: '请输入地市编码',
    },
  },
])

async function loadParentRegionOptions() {
  try {
    const res = await request.get('/system/region/tree')
    if (res.code === 200) {
      const convertToTreeSelect = (list) => {
        return list.map(item => ({
          label: item.name,
          value: item.code,
          key: item.code,
          children: item.children && item.children.length > 0
            ? convertToTreeSelect(item.children)
            : undefined,
        }))
      }
      parentRegionOptions.value = [
        { label: '顶级区域', value: '', key: '' },
        ...convertToTreeSelect(res.data || []),
      ]
    }
  }
  catch (error) {
    console.error('加载上级区域选项失败:', error)
  }
}

function getAllKeys(list, keys = []) {
  list.forEach((item) => {
    keys.push(item.code)
    if (item.children && item.children.length > 0) {
      getAllKeys(item.children, keys)
    }
  })
  return keys
}

function beforeRenderList(list) {
  // 为每个节点添加children数组，用于树形结构
  return list.map(item => ({
    ...item,
    children: item.hasChildren ? [] : undefined,
  }))
}

// 处理展开事件，懒加载子节点
async function handleExpand(expandedKeys, { node, expanded }) {
  if (expanded && node.hasChildren && (!node.children || node.children.length === 0)) {
    // 展开且需要加载子节点
    try {
      const res = await request.get(`/system/region/childrenVO/${node.code}`)
      if (res.code === 200) {
        // 更新节点的children
        const children = (res.data || []).map(item => ({
          ...item,
          children: item.hasChildren ? [] : undefined,
        }))
        node.children = children
      }
    }
    catch (error) {
      console.error('加载子节点失败:', error)
    }
  }
}

function handleExpandedKeysUpdate(keys) {
  expandedKeys.value = keys
}

// 展开/折叠已加载的节点
function toggleExpandLoaded() {
  expandLoaded.value = !expandLoaded.value
  if (expandLoaded.value) {
    // 只展开当前已加载的节点
    const tableData = crudRef.value?.getTableData() || []
    expandedKeys.value = tableData.map(item => item.code)
  }
  else {
    expandedKeys.value = []
  }
}

async function handleAdd(row) {
  await loadParentRegionOptions()
  const parentCodeField = editSchema.value.find(f => f.field === 'parentCode')
  if (row && parentCodeField) {
    parentCodeField.defaultValue = row.code
  }
  crudRef.value?.showAdd()
}

async function handleEdit(row) {
  await loadParentRegionOptions()
  crudRef.value?.showEdit(row)
}

function handleDelete(row) {
  window.$dialog.warning({
    title: '确认删除',
    content: `确定要删除"${row.name}"吗？删除后将无法恢复！`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await request.delete(`/system/region/${row.code}`)
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

onMounted(() => {
  loadParentRegionOptions()
})
</script>

<style scoped>
.system-region-page {
  height: 100%;
}
</style>