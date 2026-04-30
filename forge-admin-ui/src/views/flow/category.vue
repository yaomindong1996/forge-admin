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

    <!-- 数据表格 -->
    <div class="table-container">
      <n-data-table
        :columns="columns"
        :data="dataSource"
        :loading="loading"
        :pagination="pagination"
        :row-key="row => row.id"
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
          <n-form-item label="分类名称" path="categoryName">
            <n-input v-model:value="formData.categoryName" placeholder="请输入分类名称" />
          </n-form-item>
          <n-form-item label="分类编码" path="categoryCode">
            <n-input v-model:value="formData.categoryCode" placeholder="请输入分类编码" :disabled="isEdit" />
          </n-form-item>
          <n-form-item label="排序" path="sort">
            <n-input-number v-model:value="formData.sort" :min="0" placeholder="请输入排序" style="width: 100%" />
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
          <n-form-item label="备注" path="remark">
            <n-input v-model:value="formData.remark" type="textarea" placeholder="请输入备注" :rows="3" />
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
import { NButton, NSpace } from 'naive-ui'
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

const columns = [
  {
    title: '分类名称',
    key: 'categoryName',
    render: row => h('span', { class: 'category-name' }, row.categoryName),
  },
  {
    title: '分类编码',
    key: 'categoryCode',
    render: row => h('span', { class: 'category-code' }, row.categoryCode),
  },
  {
    title: '排序',
    key: 'sort',
    width: 80,
  },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render: row => h('span', {
      class: ['status-tag', row.status === 1 ? 'enabled' : 'disabled'],
      onClick: () => handleStatusChange(row, row.status === 1 ? 0 : 1),
    }, row.status === 1 ? '启用' : '禁用'),
  },
  {
    title: '备注',
    key: 'remark',
    ellipsis: { tooltip: true },
  },
  {
    title: '创建时间',
    key: 'createTime',
    width: 180,
  },
  {
    title: '操作',
    key: 'actions',
    width: 150,
    render: row => h(NSpace, { size: 4 }, {
      default: () => [
        h(NButton, {
          size: 'small',
          type: 'primary',
          onClick: () => handleEdit(row),
        }, { default: () => '编辑' }),
        h(NButton, {
          size: 'small',
          type: 'error',
          onClick: () => handleDelete(row),
        }, { default: () => '删除' }),
      ],
    }),
  },
]

const dataSource = ref([])
const loading = ref(false)
const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50],
  onChange: (page) => {
    pagination.page = page
    fetchData()
  },
  onUpdatePageSize: (pageSize) => {
    pagination.pageSize = pageSize
    pagination.page = 1
    fetchData()
  },
})

const showModal = ref(false)
const modalTitle = ref('新增分类')
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref(null)
const formData = reactive({
  id: '',
  categoryName: '',
  categoryCode: '',
  sort: 0,
  status: 1,
  remark: '',
})

const rules = {
  categoryName: { required: true, message: '请输入分类名称', trigger: 'blur' },
  categoryCode: { required: true, message: '请输入分类编码', trigger: 'blur' },
}

async function fetchData() {
  loading.value = true
  try {
    const res = await flowApi.getCategoryPage({
      pageNum: pagination.page,
      pageSize: pagination.pageSize,
      ...queryParams,
    })
    if (res.code === 200) {
      dataSource.value = res.data?.records || []
      pagination.itemCount = res.data?.total || 0
      totalCount.value = res.data?.total || 0
      enabledCount.value = dataSource.value.filter(r => r.status === 1).length
      disabledCount.value = dataSource.value.filter(r => r.status === 0).length
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
  pagination.page = 1
  fetchData()
}

function handleReset() {
  queryParams.categoryName = ''
  queryParams.categoryCode = ''
  queryParams.status = null
  pagination.page = 1
  fetchData()
}

function handleFilter(status) {
  if (status === 'all') {
    queryParams.status = null
  }
  else {
    queryParams.status = status
  }
  pagination.page = 1
  fetchData()
}

function handleAdd() {
  isEdit.value = false
  modalTitle.value = '新增分类'
  Object.assign(formData, {
    id: '',
    categoryName: '',
    categoryCode: '',
    sort: 0,
    status: 1,
    remark: '',
  })
  showModal.value = true
}

function handleEdit(row) {
  isEdit.value = true
  modalTitle.value = '编辑分类'
  Object.assign(formData, row)
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
</style>
