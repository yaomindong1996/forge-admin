<template>
  <div class="flow-category-page">
    <!-- 工具栏 -->
    <div class="panel-toolbar">
      <div class="toolbar-filters">
        <n-input
          v-model:value="queryParams.categoryName"
          placeholder="分类名称"
          clearable
          class="filter-name"
          @keydown.enter="handleSearch"
        >
          <template #prefix>
            <i class="i-material-symbols:search" />
          </template>
        </n-input>
        <n-input
          v-model:value="queryParams.categoryCode"
          placeholder="分类编码"
          clearable
          class="filter-code"
          @keydown.enter="handleSearch"
        />
        <n-select
          v-model:value="queryParams.status"
          placeholder="状态"
          clearable
          class="filter-status"
          :options="statusOptions"
          @update:value="handleSearch"
        />
        <NButton secondary @click="handleSearch">
          查询
        </NButton>
        <NButton quaternary @click="handleReset">
          重置
        </NButton>
      </div>
      <div class="toolbar-actions">
        <span class="result-summary">{{ resultSummary }}</span>
        <NButton type="primary" @click="handleAdd">
          <template #icon>
            <i class="i-material-symbols:add" />
          </template>
          新增分类
        </NButton>
      </div>
    </div>

    <!-- 表格 -->
    <div class="table-region">
      <n-data-table
        size="medium"
        :columns="columns"
        :data="displayTree"
        :loading="loading"
        :row-key="row => row.id"
        :default-expand-all="true"
        flex-height
        class="category-table"
      />
    </div>

    <!-- 新增/编辑弹窗 -->
    <n-modal
      v-model:show="showModal"
      preset="card"
      :title="modalTitle"
      style="width: 480px"
      :mask-closable="false"
    >
      <n-form ref="formRef" :model="formData" :rules="rules" label-placement="left" label-width="80">
        <n-form-item label="父分类" path="parentId">
          <NTreeSelect
            v-model:value="formData.parentId"
            :options="categoryTreeOptions"
            placeholder="不选则为顶级分类"
            clearable
            :default-expand-all="true"
          />
        </n-form-item>
        <n-form-item label="分类名称" path="categoryName">
          <n-input v-model:value="formData.categoryName" placeholder="请输入分类名称" />
        </n-form-item>
        <n-form-item label="分类编码" path="categoryCode">
          <n-input v-model:value="formData.categoryCode" placeholder="请输入分类编码" :disabled="isEdit" />
        </n-form-item>
        <n-form-item label="排序" path="sortOrder">
          <n-input-number v-model:value="formData.sortOrder" :min="0" placeholder="请输入排序" style="width: 100%" />
        </n-form-item>
        <n-form-item label="状态" path="status">
          <n-switch v-model:value="formData.status" :checked-value="1" :unchecked-value="0">
            <template #checked>
              启用
            </template>
            <template #unchecked>
              禁用
            </template>
          </n-switch>
        </n-form-item>
        <n-form-item label="描述" path="description">
          <n-input v-model:value="formData.description" type="textarea" placeholder="请输入描述" :rows="3" />
        </n-form-item>
      </n-form>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="showModal = false">
            取消
          </NButton>
          <NButton type="primary" :loading="submitLoading" @click="handleSubmit">
            确定
          </NButton>
        </NSpace>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { NButton, NDropdown, NSpace, NTreeSelect } from 'naive-ui'
import { computed, h, onMounted, reactive, ref } from 'vue'
import flowApi from '@/api/flow'
import SystemTableCell from '@/components/common/SystemTableCell.vue'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables/useDict'
import { toNumberDictOptions } from '@/utils/dict-options'

defineOptions({ name: 'FlowCategory' })

const { dict } = useDict('sys_enable_disable')
const statusOptions = computed(() => toNumberDictOptions(dict.value.sys_enable_disable))

const queryParams = reactive({
  categoryName: '',
  categoryCode: '',
  status: null,
})

const rawTree = ref([])
const categoryTreeOptions = ref([])
const loading = ref(false)
const showModal = ref(false)
const modalTitle = ref('新增分类')
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref(null)
const formData = reactive({
  id: '',
  parentId: null,
  categoryName: '',
  categoryCode: '',
  sortOrder: 0,
  status: 1,
  description: '',
})

const rules = {
  categoryName: { required: true, message: '请输入分类名称', trigger: 'blur' },
  categoryCode: { required: true, message: '请输入分类编码', trigger: 'blur' },
}

const displayTree = computed(() => filterCategoryTree(rawTree.value, queryParams))
const resultSummary = computed(() => {
  const count = countTreeNodes(displayTree.value)
  return count ? `${count} 个分类` : '暂无分类'
})

const columns = [
  {
    title: '分类',
    key: 'categoryName',
    minWidth: 220,
    render: row => h(SystemTableCell, {
      title: row.categoryName || '-',
      subtitle: row.categoryCode || '未设置编码',
      interactive: true,
      tooltip: `编辑分类：${row.categoryName || row.categoryCode || '-'}`,
      onActivate: () => handleEdit(row),
    }),
  },
  {
    title: '层级',
    key: 'level',
    width: 72,
    render: row => `L${row.level || 1}`,
  },
  {
    title: '排序',
    key: 'sortOrder',
    width: 72,
  },
  {
    title: '状态',
    key: 'status',
    width: 96,
    render: row => h(DictTag, {
      dictType: 'sys_enable_disable',
      value: row.status,
      bordered: false,
    }),
  },
  {
    title: '创建时间',
    key: 'createTime',
    width: 168,
  },
  {
    title: '操作',
    key: 'actions',
    width: 168,
    fixed: 'right',
    render: row => renderRowActions(row),
  },
]

function renderRowActions(row) {
  const moreOptions = [
    {
      label: Number(row.status) === 1 ? '停用' : '启用',
      key: 'toggle',
    },
    {
      label: '删除',
      key: 'delete',
      props: { style: { color: 'var(--error-color, #ef4444)' } },
    },
  ]

  return h('div', {
    class: 'flow-category-row-actions',
    onClick: event => event.stopPropagation(),
  }, [
    h('a', {
      class: 'text-primary cursor-pointer hover:text-primary-hover',
      onClick: () => handleEdit(row),
    }, '编辑'),
    h('a', {
      class: 'text-primary cursor-pointer hover:text-primary-hover',
      onClick: () => handleAddChild(row),
    }, '添加子级'),
    h(NDropdown, {
      trigger: 'click',
      options: moreOptions,
      onSelect: (key) => {
        if (key === 'toggle')
          handleStatusChange(row, Number(row.status) === 1 ? 0 : 1)
        else if (key === 'delete')
          handleDelete(row)
      },
    }, {
      default: () => h(NButton, {
        'text': true,
        'size': 'tiny',
        'quaternary': true,
        'class': 'flow-category-more-btn',
        'aria-label': `${row.categoryName || '此分类'}的更多操作`,
        'title': '更多操作',
      }, {
        icon: () => h('i', { 'class': 'i-lucide:ellipsis', 'aria-hidden': true }),
      }),
    }),
  ])
}

function buildTreeSelectOptions(treeData) {
  return treeData.map(item => ({
    label: item.categoryName,
    value: item.id,
    key: item.id,
    children: item.children?.length ? buildTreeSelectOptions(item.children) : undefined,
  }))
}

function countTreeNodes(nodes = []) {
  return nodes.reduce((total, node) => total + 1 + countTreeNodes(node.children || []), 0)
}

function filterCategoryTree(nodes = [], query) {
  const name = String(query.categoryName || '').trim().toLowerCase()
  const code = String(query.categoryCode || '').trim().toLowerCase()
  const status = query.status

  return nodes
    .map((node) => {
      const children = filterCategoryTree(node.children || [], query)
      const nameMatched = !name || String(node.categoryName || '').toLowerCase().includes(name)
      const codeMatched = !code || String(node.categoryCode || '').toLowerCase().includes(code)
      const statusMatched = status === null || status === undefined || Number(node.status) === Number(status)
      if ((nameMatched && codeMatched && statusMatched) || children.length)
        return { ...node, children }
      return null
    })
    .filter(Boolean)
}

async function fetchData() {
  loading.value = true
  try {
    const res = await flowApi.getCategoryTree()
    if (res.code === 200)
      rawTree.value = res.data || []
    const treeRes = await flowApi.getCategoryTreeSelect(false)
    if (treeRes.code === 200)
      categoryTreeOptions.value = buildTreeSelectOptions(treeRes.data || [])
  }
  catch {
    window.$message?.error('获取分类列表失败')
  }
  finally {
    loading.value = false
  }
}

function handleSearch() {
  // displayTree 已按 queryParams 过滤
}

function handleReset() {
  queryParams.categoryName = ''
  queryParams.categoryCode = ''
  queryParams.status = null
}

function resetForm(overrides = {}) {
  Object.assign(formData, {
    id: '',
    parentId: null,
    categoryName: '',
    categoryCode: '',
    sortOrder: 0,
    status: 1,
    description: '',
    ...overrides,
  })
}

function handleAdd() {
  isEdit.value = false
  modalTitle.value = '新增分类'
  resetForm()
  showModal.value = true
}

function handleAddChild(row) {
  isEdit.value = false
  modalTitle.value = '新增子分类'
  resetForm({ parentId: row.id })
  showModal.value = true
}

function handleEdit(row) {
  isEdit.value = true
  modalTitle.value = '编辑分类'
  resetForm({
    id: row.id,
    parentId: row.parentId || null,
    categoryName: row.categoryName,
    categoryCode: row.categoryCode,
    sortOrder: row.sortOrder || 0,
    status: row.status,
    description: row.description || '',
  })
  showModal.value = true
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
    submitLoading.value = true
    const api = isEdit.value ? flowApi.updateCategory : flowApi.createCategory
    const res = await api(formData)
    if (res.code === 200) {
      window.$message?.success(isEdit.value ? '编辑成功' : '新增成功')
      showModal.value = false
      fetchData()
    }
    else {
      window.$message?.error(res.message || '操作失败')
    }
  }
  catch {
    // 表单校验失败时不提示
  }
  finally {
    submitLoading.value = false
  }
}

async function handleStatusChange(row, val) {
  try {
    const api = val === 1 ? flowApi.enableCategory : flowApi.disableCategory
    const res = await api(row.id)
    if (res.code === 200) {
      window.$message?.success(val === 1 ? '启用成功' : '禁用成功')
      fetchData()
    }
    else {
      window.$message?.error(res.message || '状态变更失败')
    }
  }
  catch {
    window.$message?.error('状态变更失败')
  }
}

async function handleDelete(row) {
  window.$dialog?.warning({
    title: '确认删除',
    content: `确定要删除分类「${row.categoryName}」吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await flowApi.deleteCategory(row.id)
        if (res.code === 200) {
          window.$message?.success('删除成功')
          fetchData()
        }
        else {
          window.$message?.error(res.message || '删除失败')
        }
      }
      catch {
        window.$message?.error('删除失败')
      }
    },
  })
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.flow-category-page {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  box-sizing: border-box;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  background: var(--bg-primary, #fff);
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 6px;
}

.panel-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 10px 12px;
  padding: 10px 12px;
  border-bottom: 1px solid var(--border-light, #e5e7eb);
}

.toolbar-filters,
.toolbar-actions {
  display: flex;
  min-width: 0;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.toolbar-actions {
  margin-left: auto;
}

.filter-name {
  width: 180px;
}

.filter-code {
  width: 140px;
}

.filter-status {
  width: 110px;
}

.result-summary {
  color: var(--text-tertiary, #6b7280);
  font-size: 12px;
}

.table-region {
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  padding: 8px 12px 12px;
}

.category-table {
  height: 100%;
}

/* 表格 render 节点拿不到 scoped 属性，必须用 :deep */
.flow-category-page :deep(.flow-category-row-actions) {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  white-space: nowrap;
}

.flow-category-page :deep(.flow-category-row-actions > a) {
  flex: 0 0 auto;
  font-size: 13px;
  line-height: 22px;
}

.flow-category-page :deep(.flow-category-more-btn) {
  width: 22px;
  height: 22px;
  color: var(--text-tertiary, #6b7280);
}

.flow-category-page :deep(.flow-category-more-btn:hover) {
  color: var(--primary-color, #165dff);
}

@media (max-width: 760px) {
  .filter-name,
  .filter-code,
  .filter-status {
    width: 100%;
    flex: 1 1 140px;
  }

  .toolbar-actions {
    width: 100%;
    justify-content: space-between;
  }
}
</style>
