<template>
  <div class="p-16">
    <div class="bg-white rounded p-16">
      <h2 class="text-18 font-bold mb-16">流程分类</h2>
      
      <!-- 搜索栏 -->
      <n-space class="mb-16" :vertical="false">
        <n-input
          v-model:value="queryParams.categoryName"
          placeholder="搜索分类名称"
          clearable
          style="width: 200px"
        >
          <template #prefix>
            <i class="i-material-symbols:search" />
          </template>
        </n-input>
        <n-input
          v-model:value="queryParams.categoryCode"
          placeholder="搜索分类编码"
          clearable
          style="width: 150px"
        >
          <template #prefix>
            <i class="i-material-symbols:search" />
          </template>
        </n-input>
        <n-select
          v-model:value="queryParams.status"
          placeholder="状态"
          clearable
          style="width: 120px"
          :options="statusOptions"
        />
        <n-button type="primary" @click="handleSearch">
          <template #icon>
            <i class="i-material-symbols:search" />
          </template>
          搜索
        </n-button>
        <n-button @click="handleReset">
          <template #icon>
            <i class="i-material-symbols:refresh" />
          </template>
          重置
        </n-button>
        <n-button type="primary" @click="handleAdd">
          <template #icon>
            <i class="i-material-symbols:add" />
          </template>
          新增分类
        </n-button>
      </n-space>

      <!-- 数据表格 -->
      <n-data-table
        :columns="columns"
        :data="dataSource"
        :loading="loading"
        :pagination="pagination"
        :row-key="row => row.id"
      />
    </div>

    <!-- 新增/编辑弹窗 -->
    <Teleport to="body">
      <n-modal
        v-model:show="showModal"
        preset="dialog"
        :title="modalTitle"
        positive-text="确定"
        negative-text="取消"
        :loading="submitLoading"
        @positive-click="handleSubmit"
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
              <template #checked>启用</template>
              <template #unchecked>禁用</template>
            </n-switch>
          </n-form-item>
          <n-form-item label="备注" path="remark">
            <n-input
              v-model:value="formData.remark"
              type="textarea"
              placeholder="请输入备注"
              :rows="3"
            />
          </n-form-item>
        </n-form>
      </n-modal>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, reactive, h, onMounted } from 'vue'
import { NTag, NButton, NSpace, NSwitch } from 'naive-ui'
import flowApi from '@/api/flow'

// 状态选项
const statusOptions = [
  { label: '启用', value: 1 },
  { label: '禁用', value: 0 },
]

// 查询参数
const queryParams = reactive({
  categoryName: '',
  categoryCode: '',
  status: null,
})

// 表格列
const columns = [
  {
    title: '分类名称',
    key: 'categoryName',
  },
  {
    title: '分类编码',
    key: 'categoryCode',
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
    render: (row) => {
      return h(NSwitch, {
        value: row.status,
        checkedValue: 1,
        uncheckedValue: 0,
        onUpdateValue: (val) => handleStatusChange(row, val),
      }, {
        checked: () => '启用',
        unchecked: () => '禁用',
      })
    },
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
    render: (row) => {
      return h(NSpace, null, {
        default: () => [
          h(NButton, {
            size: 'small',
            onClick: () => handleEdit(row),
          }, { default: () => '编辑' }),
          h(NButton, {
            size: 'small',
            type: 'error',
            onClick: () => handleDelete(row),
          }, { default: () => '删除' }),
        ],
      })
    },
  },
]

// 数据
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

// 弹窗
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

// 获取数据
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
    }
  } catch (error) {
    console.error('获取分类列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 搜索
function handleSearch() {
  pagination.page = 1
  fetchData()
}

// 重置
function handleReset() {
  queryParams.categoryName = ''
  queryParams.categoryCode = ''
  queryParams.status = null
  pagination.page = 1
  fetchData()
}

// 新增
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

// 编辑
function handleEdit(row) {
  isEdit.value = true
  modalTitle.value = '编辑分类'
  Object.assign(formData, row)
  showModal.value = true
}

// 提交
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
    } else {
      window.$message?.error(res.message || '操作失败')
    }
  } catch (error) {
    console.error('提交失败:', error)
  } finally {
    submitLoading.value = false
  }
}

// 状态变更
async function handleStatusChange(row, val) {
  try {
    const api = val === 1 ? flowApi.enableCategory : flowApi.disableCategory
    // 需要在 flowApi 中添加这两个方法
    const res = await api(row.id)
    if (res.code === 200) {
      window.$message?.success(val === 1 ? '启用成功' : '禁用成功')
      row.status = val
    }
  } catch (error) {
    console.error('状态变更失败:', error)
  }
}

// 删除
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
        } else {
          window.$message?.error(res.message || '删除失败')
        }
      } catch (error) {
        console.error('删除失败:', error)
      }
    },
  })
}

// 初始化
onMounted(() => {
  fetchData()
})
</script>