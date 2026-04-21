<script>
</script>

<template>
  <CommonPage show-footer>
    <n-card title="AiSearch + AiTable 组件示例">
      <!-- 搜索表单 -->
      <AiSearch
        ref="searchRef"
        v-model="searchParams"
        :schema="searchSchema"
        @search="handleSearch"
      >
        <template #extra-actions>
          <NButton @click="handleExport">
            <template #icon>
              <n-icon><DownloadOutline /></n-icon>
            </template>
            导出
          </NButton>
        </template>
      </AiSearch>

      <!-- 工具栏 -->
      <NSpace justify="space-between" style="margin-bottom: 16px">
        <NSpace>
          <NButton type="primary" @click="handleAdd">
            <template #icon>
              <n-icon><AddOutline /></n-icon>
            </template>
            新增
          </NButton>
          <NButton
            type="error"
            :disabled="selectedKeys.length === 0"
            @click="handleBatchDelete"
          >
            批量删除 ({{ selectedKeys.length }})
          </NButton>
        </NSpace>
        <NSpace>
          <NButton @click="loadData">
            <template #icon>
              <n-icon><RefreshOutline /></n-icon>
            </template>
            刷新
          </NButton>
        </NSpace>
      </NSpace>

      <!-- 数据表格 -->
      <AiTable
        ref="tableRef"
        v-model:checked-row-keys="selectedKeys"
        :columns="tableColumns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        @page-change="handlePageChange"
        @page-size-change="handlePageSizeChange"
      />
    </n-card>

    <!-- 表单数据查看 -->
    <n-card title="当前搜索参数" style="margin-top: 16px">
      <pre>{{ JSON.stringify(searchParams, null, 2) }}</pre>
    </n-card>

    <n-card title="选中的行" style="margin-top: 16px">
      <pre>{{ JSON.stringify(selectedKeys, null, 2) }}</pre>
    </n-card>
  </CommonPage>
</template>

<script setup>
import { AddOutline, DownloadOutline, RefreshOutline } from '@vicons/ionicons5'
import { NButton, NSpace, NTag } from 'naive-ui'
import { h, onMounted, ref } from 'vue'
import { AiSearch, AiTable, FieldFactory } from '@/components/ai-form'

// 使用全局的 message 和 dialog
const message = window.$message
const dialog = window.$dialog

// 搜索
const searchRef = ref(null)
const searchParams = ref({})

const searchSchema = [
  FieldFactory.input('keyword', '关键词', {
    placeholder: '请输入关键词搜索',
  }),
  FieldFactory.select('status', '状态', [
    { label: '全部', value: null },
    { label: '启用', value: 1 },
    { label: '禁用', value: 0 },
  ]),
  FieldFactory.select('category', '分类', [
    { label: '全部', value: null },
    { label: '技术', value: 'tech' },
    { label: '产品', value: 'product' },
    { label: '运营', value: 'operation' },
  ]),
  FieldFactory.date('createTime', '创建日期'),
  FieldFactory.input('creator', '创建人'),
]

// 表格
const tableRef = ref(null)
const tableData = ref([])
const loading = ref(false)
const selectedKeys = ref([])
const pagination = ref({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50, 100],
})

const tableColumns = [
  {
    prop: 'id',
    label: 'ID',
    width: 80,
    fixed: 'left',
  },
  {
    prop: 'name',
    label: '名称',
    width: 150,
  },
  {
    prop: 'category',
    label: '分类',
    width: 100,
    render: (row) => {
      const categoryMap = {
        tech: { text: '技术', type: 'info' },
        product: { text: '产品', type: 'success' },
        operation: { text: '运营', type: 'warning' },
      }
      const config = categoryMap[row.category] || { text: row.category, type: 'default' }
      return h(NTag, { type: config.type }, { default: () => config.text })
    },
  },
  {
    prop: 'status',
    label: '状态',
    width: 100,
    align: 'center',
    render: (row) => {
      return h(NTag, {
        type: row.status === 1 ? 'success' : 'error',
      }, {
        default: () => row.status === 1 ? '启用' : '禁用',
      })
    },
  },
  {
    prop: 'creator',
    label: '创建人',
    width: 120,
  },
  {
    prop: 'createTime',
    label: '创建时间',
    width: 180,
    formatter: (row, column, value) => {
      return value ? new Date(value).toLocaleString() : '-'
    },
  },
  {
    prop: 'description',
    label: '描述',
    width: 200,
  },
  {
    prop: 'action',
    label: '操作',
    width: 200,
    fixed: 'right',
    render: (row) => {
      return h(NSpace, null, {
        default: () => [
          h(NButton, {
            size: 'small',
            type: 'primary',
            onClick: () => handleEdit(row),
          }, { default: () => '编辑' }),
          h(NButton, {
            size: 'small',
            type: 'warning',
            onClick: () => handleToggleStatus(row),
          }, { default: () => row.status === 1 ? '禁用' : '启用' }),
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

// 生成模拟数据
function generateMockData(count = 50) {
  const categories = ['tech', 'product', 'operation']
  const names = ['项目A', '项目B', '项目C', '项目D', '项目E']
  const creators = ['张三', '李四', '王五', '赵六']

  return Array.from({ length: count }, (_, index) => ({
    id: index + 1,
    name: `${names[index % names.length]}${index + 1}`,
    category: categories[index % categories.length],
    status: Math.random() > 0.5 ? 1 : 0,
    creator: creators[index % creators.length],
    createTime: new Date(Date.now() - Math.random() * 30 * 24 * 60 * 60 * 1000).toISOString(),
    description: `这是第${index + 1}条数据的描述信息`,
  }))
}

const allData = generateMockData(50)

// 加载数据
function loadData() {
  loading.value = true

  // 模拟接口延迟
  setTimeout(() => {
    // 过滤数据
    let filtered = [...allData]

    if (searchParams.value.keyword) {
      filtered = filtered.filter(item =>
        item.name.includes(searchParams.value.keyword)
        || item.description.includes(searchParams.value.keyword),
      )
    }

    if (searchParams.value.status !== null && searchParams.value.status !== undefined) {
      filtered = filtered.filter(item => item.status === searchParams.value.status)
    }

    if (searchParams.value.category) {
      filtered = filtered.filter(item => item.category === searchParams.value.category)
    }

    if (searchParams.value.creator) {
      filtered = filtered.filter(item => item.creator.includes(searchParams.value.creator))
    }

    // 分页
    const start = (pagination.value.page - 1) * pagination.value.pageSize
    const end = start + pagination.value.pageSize

    tableData.value = filtered.slice(start, end)
    pagination.value.itemCount = filtered.length

    loading.value = false
    message.success('数据加载成功')
  }, 500)
}

// 搜索
function handleSearch(params) {
  console.log('搜索参数:', params)
  pagination.value.page = 1
  loadData()
}

// 翻页
function handlePageChange(page) {
  pagination.value.page = page
  loadData()
}

// 改变每页条数
function handlePageSizeChange(pageSize) {
  pagination.value.pageSize = pageSize
  pagination.value.page = 1
  loadData()
}

// 新增
function handleAdd() {
  message.info('点击了新增')
}

// 编辑
function handleEdit(row) {
  message.info(`编辑: ${row.name}`)
}

// 切换状态
function handleToggleStatus(row) {
  const newStatus = row.status === 1 ? 0 : 1
  const statusText = newStatus === 1 ? '启用' : '禁用'

  dialog.warning({
    title: '确认',
    content: `确定要${statusText}【${row.name}】吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: () => {
      row.status = newStatus
      message.success(`${statusText}成功`)
    },
  })
}

// 删除
function handleDelete(row) {
  dialog.error({
    title: '确认删除',
    content: `确定要删除【${row.name}】吗？此操作不可恢复！`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: () => {
      const index = allData.findIndex(item => item.id === row.id)
      if (index > -1) {
        allData.splice(index, 1)
        loadData()
        message.success('删除成功')
      }
    },
  })
}

// 批量删除
function handleBatchDelete() {
  const rows = tableRef.value?.getCheckedRows()

  dialog.error({
    title: '确认删除',
    content: `确定要删除选中的 ${rows.length} 条数据吗？此操作不可恢复！`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: () => {
      selectedKeys.value.forEach((id) => {
        const index = allData.findIndex(item => item.id === id)
        if (index > -1) {
          allData.splice(index, 1)
        }
      })
      selectedKeys.value = []
      loadData()
      message.success('批量删除成功')
    },
  })
}

// 导出
function handleExport() {
  message.success('导出功能开发中...')
  console.log('导出参数:', searchParams.value)
}

// 初始化
onMounted(() => {
  loadData()
})
</script>

<style scoped>
pre {
  background: #f5f5f5;
  padding: 12px;
  border-radius: 4px;
  overflow: auto;
  margin: 0;
}
</style>
