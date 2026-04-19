<template>
  <div class="p-16">
    <div class="rounded bg-white p-16">
      <h2 class="text-18 mb-16 font-bold">
        我的抄送
      </h2>

      <!-- 搜索栏 -->
      <NSpace class="mb-16" :vertical="false">
        <n-select
          v-model:value="queryParams.isRead"
          placeholder="阅读状态"
          clearable
          style="width: 120px"
          :options="readOptions"
        />
        <NButton type="primary" @click="handleSearch">
          <template #icon>
            <i class="i-material-symbols:search" />
          </template>
          搜索
        </NButton>
        <NButton @click="handleReset">
          <template #icon>
            <i class="i-material-symbols:refresh" />
          </template>
          重置
        </NButton>
        <NButton
          v-if="selectedRowKeys.length > 0"
          @click="handleBatchMarkRead"
        >
          <template #icon>
            <i class="i-material-symbols:mark-email-read-outline" />
          </template>
          批量标记已读
        </NButton>
        <NButton @click="handleMarkAllRead">
          <template #icon>
            <i class="i-material-symbols:done-all" />
          </template>
          全部已读
        </NButton>
      </NSpace>

      <!-- 数据标签页 -->
      <n-tabs v-model:value="activeTab" type="line" class="mb-16" @update:value="handleTabChange">
        <n-tab name="received">
          抄送给我的
        </n-tab>
        <n-tab name="sent">
          我发送的
        </n-tab>
      </n-tabs>

      <!-- 抄送列表 -->
      <n-data-table
        :columns="columns"
        :data="dataSource"
        :loading="loading"
        :pagination="pagination"
        :row-key="row => row.id"
        @update:checked-row-keys="handleCheck"
      />
    </div>
  </div>
</template>

<script setup>
import { NButton, NSpace, NTag } from 'naive-ui'
import { computed, h, onMounted, reactive, ref } from 'vue'
import flowApi from '@/api/flow'
import { useUserStore } from '@/store'

const userStore = useUserStore()
const loading = ref(false)
const dataSource = ref([])
const activeTab = ref('received')
const selectedRowKeys = ref([])

const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  onChange: (page) => {
    pagination.page = page
    loadData()
  },
})

const queryParams = reactive({
  isRead: null,
})

const readOptions = [
  { label: '未读', value: 0 },
  { label: '已读', value: 1 },
]

// 表格列
const columns = computed(() => {
  const baseColumns = [
    {
      type: 'selection',
    },
    {
      title: '标题',
      key: 'title',
      ellipsis: { tooltip: true },
    },
    {
      title: '内容摘要',
      key: 'content',
      width: 200,
      ellipsis: { tooltip: true },
    },
  ]

  if (activeTab.value === 'received') {
    baseColumns.push({
      title: '发送人',
      key: 'sendUserName',
      width: 100,
    })
  }
  else {
    baseColumns.push({
      title: '抄送人',
      key: 'ccUserName',
      width: 100,
    })
  }

  baseColumns.push(
    {
      title: '阅读状态',
      key: 'isRead',
      width: 80,
      render: (row) => {
        return h(NTag, {
          type: row.isRead === 1 ? 'success' : 'warning',
          size: 'small',
        }, () => row.isRead === 1 ? '已读' : '未读')
      },
    },
    {
      title: '抄送时间',
      key: 'ccTime',
      width: 160,
    },
    {
      title: '操作',
      key: 'actions',
      width: 100,
      render: (row) => {
        if (activeTab.value === 'received' && row.isRead === 0) {
          return h(NButton, {
            size: 'small',
            type: 'primary',
            onClick: () => handleMarkRead(row.id),
          }, () => '标记已读')
        }
        return null
      },
    },
  )

  return baseColumns
})

// 加载数据
async function loadData() {
  loading.value = true
  try {
    const params = {
      pageNum: pagination.page,
      pageSize: pagination.pageSize,
      userId: userStore.userId,
    }

    let res
    if (activeTab.value === 'received') {
      if (queryParams.isRead !== null) {
        params.isRead = queryParams.isRead
      }
      res = await flowApi.getMyCc(params)
    }
    else {
      res = await flowApi.getSentCc(params)
    }

    if (res.code === 200 && res.data) {
      dataSource.value = res.data.records || []
      pagination.itemCount = res.data.total || 0
    }
  }
  catch (error) {
    console.error('加载抄送列表失败:', error)
  }
  finally {
    loading.value = false
  }
}

// 搜索
function handleSearch() {
  pagination.page = 1
  loadData()
}

// 重置
function handleReset() {
  queryParams.isRead = null
  pagination.page = 1
  loadData()
}

// 切换标签
function handleTabChange() {
  pagination.page = 1
  selectedRowKeys.value = []
  loadData()
}

// 选择行
function handleCheck(keys) {
  selectedRowKeys.value = keys
}

// 标记已读
async function handleMarkRead(id) {
  try {
    const res = await flowApi.markCcRead(id)
    if (res.code === 200) {
      window.$message.success('已标记已读')
      loadData()
    }
  }
  catch (error) {
    window.$message.error('操作失败')
  }
}

// 批量标记已读
async function handleBatchMarkRead() {
  if (selectedRowKeys.value.length === 0)
    return

  try {
    const res = await flowApi.batchMarkCcRead(selectedRowKeys.value)
    if (res.code === 200) {
      window.$message.success('已批量标记已读')
      selectedRowKeys.value = []
      loadData()
    }
  }
  catch (error) {
    window.$message.error('操作失败')
  }
}

// 全部标记已读
async function handleMarkAllRead() {
  window.$dialog.warning({
    title: '确认',
    content: '确定将所有未读抄送标记为已读吗？',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        // 获取所有未读ID
        const unreadItems = dataSource.value.filter(item => item.isRead === 0)
        const ids = unreadItems.map(item => item.id)
        if (ids.length > 0) {
          await flowApi.batchMarkCcRead(ids)
          window.$message.success('已全部标记已读')
          loadData()
        }
      }
      catch (error) {
        window.$message.error('操作失败')
      }
    },
  })
}

onMounted(() => {
  loadData()
})
</script>
