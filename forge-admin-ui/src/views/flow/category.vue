<template>
  <div class="flow-page">
    <!-- 统计卡片 -->
    <FlowCategoryStats
      :total-count="totalCount"
      :enabled-count="enabledCount"
      :disabled-count="disabledCount"
      @filter="handleFilter"
    />

    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <div class="title-row">
          <div class="title-icon">
            <i class="i-material-symbols:category-outline" />
          </div>
          <h2 class="page-title">
            流程分类
          </h2>
        </div>
      </div>
      <div class="header-right">
        <n-input
          v-model:value="queryParams.categoryName"
          placeholder="搜索分类名称"
          clearable
          class="search-input"
          @keydown.enter="handleSearch"
        >
          <template #prefix>
            <i class="i-material-symbols:search" />
          </template>
        </n-input>
        <n-input
          v-model:value="queryParams.categoryCode"
          placeholder="搜索分类编码"
          clearable
          class="code-input"
          @keydown.enter="handleSearch"
        >
          <template #prefix>
            <i class="i-material-symbols:code" />
          </template>
        </n-input>
        <n-select
          v-model:value="queryParams.status"
          placeholder="状态"
          clearable
          class="status-select"
          :options="statusOptions"
        />
        <NButton type="primary" class="search-btn" @click="handleSearch">
          <i class="i-material-symbols:search mr-2" />
          搜索
        </NButton>
        <NButton class="reset-btn" @click="handleReset">
          <i class="i-material-symbols:refresh mr-2" />
          重置
        </NButton>
        <NButton type="primary" class="add-btn" @click="handleAdd">
          <i class="i-material-symbols:add mr-2" />
          新增分类
        </NButton>
      </div>
    </div>

    <!-- 数据表格（树形） -->
    <div class="table-container">
      <n-data-table
        :columns="columns"
        :data="dataSource"
        :loading="loading"
        :row-key="row => row.id"
        :expandable="{
          expandedRowKeys,
          onUpdateExpandedRowKeys: handleExpandedChange,
        }"
        striped
      />
    </div>

    <!-- 新增/编辑弹窗 -->
    <Teleport to="body">
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
              placeholder="请选择父分类（不选则为顶级）"
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
    </Teleport>
  </div>
</template>

<script setup>
import { NButton, NIcon, NSpace, NTreeSelect } from 'naive-ui'
import { h, onMounted, reactive, ref } from 'vue'
import flowApi from '@/api/flow'
import FlowCategoryStats from '@/components/flow/FlowCategoryStats.vue'

const statusOptions = [
  { label: '启用', value: 1 },
  { label: '禁用', value: 0 },
]

const queryParams = reactive({
  categoryName: '',
  categoryCode: '',
  status: null,
})

const totalCount = ref(0)
const enabledCount = ref(0)
const disabledCount = ref(0)

const expandedRowKeys = ref([])

const categoryTreeOptions = ref([])

function buildTreeSelectOptions(treeData, level = 0) {
  return treeData.map(item => ({
    label: item.categoryName,
    value: item.id,
    key: item.id,
    children: item.children && item.children.length > 0 ? buildTreeSelectOptions(item.children, level + 1) : undefined,
  }))
}

const columns = [
  {
    title: '分类名称',
    key: 'categoryName',
    width: 100,
    render: (row) => {
      return h('span', { class: 'category-name' }, row.categoryName)
    },
  },
  {
    title: '分类编码',
    key: 'categoryCode',
    width: 150,
    render: row => h('span', { class: 'category-code' }, row.categoryCode),
  },
  {
    title: '层级',
    key: 'level',
    width: 80,
    render: row => h('span', { class: 'level-tag' }, `L${row.level || 1}`),
  },
  {
    title: '排序',
    key: 'sortOrder',
    width: 80,
  },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render: row => h('span', {
      class: ['status-tag', row.status === 1 ? 'enabled' : 'disabled'],
      onClick: (e) => {
        e.stopPropagation()
        handleStatusChange(row, row.status === 1 ? 0 : 1)
      },
    }, row.status === 1 ? '启用' : '禁用'),
  },
  {
    title: '创建时间',
    key: 'createTime',
    width: 180,
  },
  {
    title: '操作',
    key: 'actions',
    width: 160,
    render: row => h(NSpace, { size: 12 }, {
      default: () => [
        h(NButton, {
          size: 'small',
          text: true,
          type: 'primary',
          title: '编辑',
          onClick: () => handleEdit(row),
        }, { default: () => h(NIcon, { size: 18 }, { default: () => h('i', { class: 'i-material-symbols:edit-outline' }) }) }),
        h(NButton, {
          size: 'small',
          text: true,
          type: 'info',
          title: '添加子级',
          onClick: () => handleAddChild(row),
        }, { default: () => h(NIcon, { size: 18 }, { default: () => h('i', { class: 'i-material-symbols:add-circle-outline' }) }) }),
        h(NButton, {
          size: 'small',
          text: true,
          type: 'error',
          title: '删除',
          onClick: () => handleDelete(row),
        }, { default: () => h(NIcon, { size: 18 }, { default: () => h('i', { class: 'i-material-symbols:delete-outline' }) }) }),
      ],
    }),
  },
]

const dataSource = ref([])
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

async function fetchData() {
  loading.value = true
  try {
    const res = await flowApi.getCategoryTree()
    if (res.code === 200) {
      dataSource.value = res.data || []
      totalCount.value = dataSource.value.length
      enabledCount.value = dataSource.value.filter(r => r.status === 1).length
      disabledCount.value = dataSource.value.filter(r => r.status === 0).length
    }
    const treeRes = await flowApi.getCategoryTreeSelect(false)
    if (treeRes.code === 200) {
      categoryTreeOptions.value = buildTreeSelectOptions(treeRes.data || [])
    }
  }
  catch {
    console.error('获取分类列表失败')
  }
  finally {
    loading.value = false
  }
}

function handleSearch() {
  fetchData()
}

function handleReset() {
  queryParams.categoryName = ''
  queryParams.categoryCode = ''
  queryParams.status = null
  fetchData()
}

function handleFilter(status) {
  if (status === 'all') {
    queryParams.status = null
  }
  else {
    queryParams.status = status
  }
  fetchData()
}

function toggleExpand(key) {
  const index = expandedRowKeys.value.indexOf(key)
  if (index > -1) {
    expandedRowKeys.value.splice(index, 1)
  }
  else {
    expandedRowKeys.value.push(key)
  }
}

function handleExpandedChange(keys) {
  expandedRowKeys.value = keys
}

function handleAdd() {
  isEdit.value = false
  modalTitle.value = '新增分类'
  Object.assign(formData, {
    id: '',
    parentId: null,
    categoryName: '',
    categoryCode: '',
    sortOrder: 0,
    status: 1,
    description: '',
  })
  showModal.value = true
}

function handleAddChild(row) {
  isEdit.value = false
  modalTitle.value = '新增子分类'
  Object.assign(formData, {
    id: '',
    parentId: row.id,
    categoryName: '',
    categoryCode: '',
    sortOrder: 0,
    status: 1,
    description: '',
  })
  showModal.value = true
}

function handleEdit(row) {
  isEdit.value = true
  modalTitle.value = '编辑分类'
  Object.assign(formData, {
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
    console.error('提交失败')
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
      row.status = val
      fetchData()
    }
  }
  catch {
    console.error('状态变更失败')
  }
}

async function handleDelete(row) {
  window.$dialog?.warning({
    title: '确认删除',
    content: `确定要删除分类"${row.categoryName}"吗？`,
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
        console.error('删除失败')
      }
    },
  })
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.flow-page {
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 0;
}

.page-header {
  background: #fff;
  border-radius: 12px;
  padding: 16px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.04);
  margin-bottom: 16px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.title-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: linear-gradient(135deg, #a78bfa 0%, #8b5cf6 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
}

.page-title {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.search-input {
  width: 200px;
}

.code-input {
  width: 150px;
}

.status-select {
  width: 120px;
}

.table-container {
  background: #fff;
  border-radius: 12px;
  padding: 16px 20px;
  flex: 1;
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.04);
}

:deep(.category-name) {
  font-weight: 600;
  color: #0f172a;
}

:deep(.category-code) {
  font-family: monospace;
  color: #64748b;
  letter-spacing: 0.3px;
}

:deep(.status-tag) {
  padding: 4px 12px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

:deep(.status-tag:hover) {
  opacity: 0.8;
}

:deep(.status-tag.enabled) {
  background: #dcfce7;
  color: #15803d;
}

:deep(.status-tag.disabled) {
  background: #fee2e2;
  color: #b91c1c;
}

:deep(.level-tag) {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
  background: #f1f5f9;
  color: #64748b;
}

:deep(.n-data-table-th) {
  font-weight: 600;
}

:deep(.n-data-table-td) {
  padding: 12px 16px;
}

:deep(.n-data-table-expand-icon) {
  margin-right: 8px;
  font-size: 16px;
  color: #64748b;
  transition: transform 0.2s ease;
}

:deep(.n-data-table-expand-icon:hover) {
  color: #2563eb;
}

:deep(.n-data-table-row--expanded .n-data-table-expand-icon) {
  transform: rotate(90deg);
  color: #2563eb;
}

:deep(.n-button) {
  padding: 4px;
}

:deep(.n-icon) {
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
