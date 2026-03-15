<template>
  <div class="p-16">
    <div class="bg-white rounded p-16">
      <h2 class="text-18 font-bold mb-16">我的已办</h2>
      
      <!-- 搜索栏 -->
      <n-space class="mb-16" :vertical="false">
        <n-input
          v-model:value="queryParams.title"
          placeholder="搜索任务标题"
          clearable
          style="width: 200px"
        >
          <template #prefix>
            <i class="i-material-symbols:search" />
          </template>
        </n-input>
        <n-select
          v-model:value="queryParams.category"
          placeholder="流程分类"
          clearable
          style="width: 150px"
          :options="categoryOptions"
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
      </n-space>

      <!-- 任务列表 -->
      <n-data-table
        :columns="columns"
        :data="dataSource"
        :loading="loading"
        :pagination="pagination"
        :row-key="row => row.id"
      />
    </div>

    <!-- 流程图弹窗 -->
    <Teleport to="body">
      <n-modal v-model:show="showDiagramModal" preset="card" title="流程图" style="width: 800px">
        <div class="flex justify-center">
          <img v-if="diagramUrl" :src="diagramUrl" alt="流程图" style="max-width: 100%" />
          <n-empty v-else description="暂无流程图" />
        </div>
      </n-modal>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, reactive, h, onMounted } from 'vue'
import { NTag, NButton, NSpace } from 'naive-ui'
import flowApi from '@/api/flow'
import { useUserStore } from '@/store'

const userStore = useUserStore()
const loading = ref(false)
const dataSource = ref([])
const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  onChange: (page) => {
    pagination.page = page
    loadData()
  }
})

const queryParams = reactive({
  title: '',
  category: ''
})

const categoryOptions = ref([])
const showDiagramModal = ref(false)
const diagramUrl = ref('')

// 状态映射
const statusMap = {
  2: { text: '已通过', type: 'success' },
  3: { text: '已驳回', type: 'error' },
  4: { text: '已转办', type: 'warning' },
  5: { text: '已委派', type: 'info' },
  6: { text: '已撤回', type: 'default' }
}

// 表格列
const columns = [
  {
    title: '任务标题',
    key: 'title',
    ellipsis: { tooltip: true }
  },
  {
    title: '任务名称',
    key: 'taskName',
    width: 120
  },
  {
    title: '发起人',
    key: 'startUserName',
    width: 100
  },
  {
    title: '发起部门',
    key: 'startDeptName',
    width: 120
  },
  {
    title: '审批结果',
    key: 'status',
    width: 80,
    render: (row) => {
      const status = statusMap[row.status] || { text: '未知', type: 'default' }
      return h(NTag, { type: status.type, size: 'small' }, () => status.text)
    }
  },
  {
    title: '审批意见',
    key: 'comment',
    width: 150,
    ellipsis: { tooltip: true }
  },
  {
    title: '完成时间',
    key: 'completeTime',
    width: 160
  },
  {
    title: '操作',
    key: 'actions',
    width: 100,
    render: (row) => {
      return h(NButton, {
        size: 'small',
        onClick: () => handleViewDiagram(row)
      }, () => '流程图')
    }
  }
]

// 加载数据
async function loadData() {
  loading.value = true
  try {
    const res = await flowApi.getDoneTasks({
      pageNum: pagination.page,
      pageSize: pagination.pageSize,
      userId: userStore.userId,
      title: queryParams.title || undefined,
      category: queryParams.category || undefined
    })
    if (res.code === 200 && res.data) {
      dataSource.value = res.data.records || []
      pagination.itemCount = res.data.total || 0
    }
  } catch (error) {
    console.error('加载已办任务失败:', error)
  } finally {
    loading.value = false
  }
}

// 加载分类
async function loadCategories() {
  try {
    const res = await flowApi.getEnabledCategories()
    if (res.code === 200 && res.data) {
      categoryOptions.value = res.data.map(item => ({
        label: item.categoryName,
        value: item.categoryCode
      }))
    }
  } catch (error) {
    console.error('加载分类失败:', error)
  }
}

// 搜索
function handleSearch() {
  pagination.page = 1
  loadData()
}

// 重置
function handleReset() {
  queryParams.title = ''
  queryParams.category = ''
  pagination.page = 1
  loadData()
}

// 查看流程图
async function handleViewDiagram(row) {
  if (!row.processInstanceId) {
    window.$message.warning('无法获取流程实例')
    return
  }
  
  try {
    const res = await flowApi.getProcessDiagram(row.processInstanceId)
    if (res) {
      diagramUrl.value = URL.createObjectURL(res)
      showDiagramModal.value = true
    }
  } catch (error) {
    window.$message.error('获取流程图失败')
  }
}

onMounted(() => {
  loadCategories()
  loadData()
})
</script>