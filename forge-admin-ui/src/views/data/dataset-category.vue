<template>
  <div class="dataset-category-page">
    <div class="category-content">
      <AiCrudPage
        ref="crudRef"
        api="/data/dataset/category"
        :api-config="{
          list: 'get@/data/dataset/category/tree',
          add: 'post@/data/dataset/category',
          update: 'put@/data/dataset/category',
          delete: 'delete@/data/dataset/category/:id',
        }"
        :is-encrypt="true"
        :search-schema="searchSchema"
        :columns="tableColumns"
        :edit-schema="editSchema"
        :before-render-list="beforeRenderList"
        row-key="id"
        :edit-grid-cols="2"
        modal-width="720px"
        :show-pagination="false"
        add-button-text="新增根分类"
        :table-props="{
          expandedRowKeys: expandedKeys,
          onUpdateExpandedRowKeys: handleExpandedKeysUpdate,
        }"
      >
        <template #toolbar-end>
          <n-button @click="toggleExpandAll">
            <template #icon>
              <i :class="expandAll ? 'i-material-symbols:unfold-less' : 'i-material-symbols:unfold-more'" />
            </template>
            {{ expandAll ? '折叠全部' : '展开全部' }}
          </n-button>
          <n-button @click="goToDatasetPage">
            <template #icon>
              <i class="i-material-symbols:dataset-outline" />
            </template>
            查看数据集
          </n-button>
        </template>
      </AiCrudPage>
    </div>
  </div>
</template>

<script setup>
import { computed, h, nextTick, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { AiCrudPage } from '@/components/ai-form'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'
import { request } from '@/utils'

defineOptions({ name: 'DataDatasetCategory' })

const { dict } = useDict('sys_enable_disable')

const router = useRouter()
const crudRef = ref(null)
const expandAll = ref(true)
const expandedKeys = ref([])
const parentCategoryOptions = ref([{ label: '顶级分类', value: null, key: 0 }])
const defaultParentId = ref(null)

const searchSchema = computed(() => [
  {
    field: 'keyword',
    label: '关键字',
    type: 'input',
    props: {
      placeholder: '搜索分类名称或编码',
    },
  },
  {
    field: 'status',
    label: '状态',
    type: 'select',
    props: {
      placeholder: '请选择状态',
      clearable: true,
      options: dict.value.sys_enable_disable || [],
    },
  },
])

const tableColumns = computed(() => [
  {
    prop: 'categoryName',
    label: '分类名称',
    minWidth: 260,
  },
  {
    prop: 'categoryCode',
    label: '分类编码',
    width: 200,
    render: row => h('span', { style: 'font-family: ui-monospace, SFMono-Regular, Menlo, monospace; color: var(--text-secondary);' }, row.categoryCode),
  },
  {
    prop: 'sortOrder',
    label: '排序',
    width: 80,
  },
  {
    prop: 'status',
    label: '状态',
    width: 90,
    render: row => h(DictTag, { dictType: 'sys_enable_disable', value: row.status }),
  },
  {
    prop: 'description',
    label: '描述',
    minWidth: 200,
    render: row => row.description || '-',
  },
  {
    prop: 'updateTime',
    label: '更新时间',
    width: 170,
  },
  {
    prop: 'action',
    label: '操作',
    width: 200,
    fixed: 'right',
    actions: [
      { label: '新增子级', key: 'addChild', type: 'primary', onClick: handleAddChild },
      { label: '编辑', key: 'edit', type: 'primary', onClick: handleEdit },
      { label: '删除', key: 'delete', type: 'error', onClick: handleDelete },
    ],
  },
])

const editSchema = computed(() => [
  {
    field: 'parentId',
    label: '父分类',
    type: 'treeSelect',
    defaultValue: defaultParentId.value,
    span: 2,
    props: {
      placeholder: '不选择则创建为根分类',
      clearable: true,
      filterable: true,
      defaultExpandAll: true,
    },
    options: () => parentCategoryOptions.value,
  },
  {
    field: 'categoryCode',
    label: '分类编码',
    type: 'input',
    rules: [{ required: true, message: '请输入分类编码', trigger: 'blur' }],
    props: {
      placeholder: '如 sales_assets',
    },
  },
  {
    field: 'categoryName',
    label: '分类名称',
    type: 'input',
    rules: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
    props: {
      placeholder: '请输入分类名称',
    },
  },
  {
    field: 'sortOrder',
    label: '排序',
    type: 'number',
    defaultValue: 0,
    props: {
      placeholder: '排序值',
      min: 0,
    },
  },
  {
    field: 'status',
    label: '状态',
    type: 'radio',
    defaultValue: 1,
    valueType: 'number',
    props: {
      options: dict.value.sys_enable_disable || [],
    },
  },
  {
    field: 'description',
    label: '描述',
    type: 'textarea',
    span: 2,
    props: {
      placeholder: '描述当前分类的业务归属、适用范围或命名规则',
      rows: 3,
    },
  },
])

onMounted(() => {
  loadParentCategoryOptions()
})

async function loadParentCategoryOptions() {
  try {
    const res = await request.get('/data/dataset/category/tree')
    if (res.code === 200) {
      parentCategoryOptions.value = [
        { label: '顶级分类', value: null, key: 0 },
        ...convertToTreeSelect(res.data || []),
      ]
    }
  }
  catch (error) {
    console.error('加载父分类选项失败:', error)
  }
}

function convertToTreeSelect(list) {
  return list.map(item => ({
    label: item.status === 1 ? item.categoryName : `${item.categoryName}（停用）`,
    value: item.id,
    key: item.id,
    children: item.children && item.children.length > 0
      ? convertToTreeSelect(item.children)
      : undefined,
  }))
}

function beforeRenderList(list) {
  // 接口返回完整树，前端按搜索条件本地过滤
  const params = crudRef.value?.getSearchParams?.() || {}
  const filtered = filterTree(list, params.keyword, params.status)

  const hasStatusFilter = params.status !== undefined && params.status !== null && params.status !== ''
  if (expandAll.value || params.keyword || hasStatusFilter) {
    expandedKeys.value = collectAllKeys(filtered)
  }
  return filtered
}

function filterTree(tree, keyword, status) {
  const kw = String(keyword || '').trim().toLowerCase()
  const hasStatus = status !== undefined && status !== null && status !== ''

  return (tree || [])
    .map((item) => {
      const children = filterTree(item.children || [], keyword, status)
      const matchKeyword = !kw
        || (item.categoryName || '').toLowerCase().includes(kw)
        || (item.categoryCode || '').toLowerCase().includes(kw)
      const matchStatus = !hasStatus || item.status === Number(status)

      if (!matchKeyword || !matchStatus) {
        if (children.length === 0) {
          return null
        }
      }
      return { ...item, children }
    })
    .filter(Boolean)
}

function collectAllKeys(list, keys = []) {
  list.forEach((item) => {
    keys.push(item.id)
    if (item.children && item.children.length > 0) {
      collectAllKeys(item.children, keys)
    }
  })
  return keys
}

function handleExpandedKeysUpdate(keys) {
  expandedKeys.value = keys
  const tableData = crudRef.value?.getTableData() || []
  const allKeys = collectAllKeys(tableData)
  expandAll.value = allKeys.length > 0 && keys.length === allKeys.length
}

function toggleExpandAll() {
  expandAll.value = !expandAll.value
  const tableData = crudRef.value?.getTableData() || []
  expandedKeys.value = expandAll.value ? collectAllKeys(tableData) : []
}

function goToDatasetPage() {
  router.push('/data/dataset')
}

async function handleAddChild(row) {
  await loadParentCategoryOptions()
  defaultParentId.value = row ? row.id : null
  crudRef.value?.showAdd()
  await nextTick()
  await nextTick()
  defaultParentId.value = null
}

async function handleEdit(row) {
  await loadParentCategoryOptions()
  crudRef.value?.showEdit(row)
}

function handleDelete(row) {
  window.$dialog.warning({
    title: '确认删除分类',
    content: `确定删除分类“${row.categoryName}”吗？存在子分类或已关联数据集时将无法删除。`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await request.delete(`/data/dataset/category/${row.id}`)
        if (res.code === 200) {
          window.$message.success('分类删除成功')
          crudRef.value?.refresh()
        }
      }
      catch (error) {
        window.$message.error(error?.message || '分类删除失败')
      }
    },
  })
}
</script>

<style scoped>
.dataset-category-page {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.category-content {
  flex: 1;
  min-height: 0;
}

.category-content :deep(.ai-crud-page) {
  height: 100%;
}
</style>
