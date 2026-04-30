<template>
  <div class="flow-page">
    <!-- 统计卡片 -->
    <FlowStats
      :todo-count="todoCount"
      :done-count="doneCount"
      :started-count="startedCount"
      :cc-count="ccCount"
      :unread-cc="unreadCc"
      active-tab="cc"
      @switch="handleSwitch"
    />

    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <div class="title-row">
          <div class="title-icon cc">
            <i class="i-material-symbols:content-copy" />
          </div>
          <h2 class="page-title">
            我的抄送
          </h2>
        </div>
      </div>
      <div class="header-right">
        <n-select v-model:value="queryParams.isRead" placeholder="阅读状态" clearable class="read-select" :options="readOptions" />
        <NButton type="primary" class="search-btn" @click="handleSearch">
          <i class="i-material-symbols:search mr-2" />搜索
        </NButton>
        <NButton class="reset-btn" @click="handleReset">
          <i class="i-material-symbols:refresh mr-2" />重置
        </NButton>
        <NButton v-if="selectedRowKeys.length > 0" class="batch-btn" @click="handleBatchMarkRead">
          <i class="i-material-symbols:mark-email-read-outline mr-2" />批量已读
        </NButton>
        <NButton class="all-read-btn" @click="handleMarkAllRead">
          <i class="i-material-symbols:done-all mr-2" />全部已读
        </NButton>
      </div>
    </div>

    <!-- 数据标签页 -->
    <div class="tabs-container">
      <n-tabs v-model:value="activeTab" type="line" @update:value="handleTabChange">
        <n-tab name="received">
          <div class="tab-content">
            <i class="i-material-symbols:inbox" />
            抄送给我的
            <span v-if="unreadCc > 0" class="tab-badge">{{ unreadCc > 99 ? '99+' : unreadCc }}未读</span>
          </div>
        </n-tab>
        <n-tab name="sent">
          <div class="tab-content">
            <i class="i-material-symbols:outbox" />
            我发送的
          </div>
        </n-tab>
      </n-tabs>
    </div>

    <!-- 抄送列表 -->
    <div class="table-container">
      <n-data-table :columns="columns" :data="dataSource" :loading="loading" :pagination="pagination" :row-key="row => row.id" @update:checked-row-keys="handleCheck" />
    </div>
  </div>
</template>

<script setup>
import { NButton } from 'naive-ui'
import { computed, h, onMounted, reactive, ref } from 'vue'
import flowApi from '@/api/flow'
import UserAvatar from '@/components/common/UserAvatar.vue'
import FlowStats from '@/components/flow/FlowStats.vue'
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

const queryParams = reactive({ isRead: null })

const readOptions = [{ label: '未读', value: 0 }, { label: '已读', value: 1 }]

const todoCount = ref(0)
const doneCount = ref(0)
const startedCount = ref(0)
const ccCount = ref(0)
const unreadCc = ref(0)

const columns = computed(() => {
  const baseColumns = [
    { type: 'selection' },
    { title: '标题', key: 'title', minWidth: 180, ellipsis: { tooltip: true }, render: row => h('span', { class: 'cc-title' }, row.title) },
    { title: '内容摘要', key: 'content', width: 200, ellipsis: { tooltip: true } },
  ]

  if (activeTab.value === 'received') {
    baseColumns.push({
      title: '发送人',
      key: 'sendUserName',
      width: 120,
      render: row => h('div', { class: 'table-user' }, [h(UserAvatar, { name: row.sendUserName || '未知', size: 24 }), h('span', { class: 'user-name-text' }, row.sendUserName || '-')]),
    })
  }
  else {
    baseColumns.push({
      title: '抄送人',
      key: 'ccUserName',
      width: 120,
      render: row => h('div', { class: 'table-user' }, [h(UserAvatar, { name: row.ccUserName || '未知', size: 24 }), h('span', { class: 'user-name-text' }, row.ccUserName || '-')]),
    })
  }

  baseColumns.push(
    { title: '阅读状态', key: 'isRead', width: 90, render: row => h('span', { class: ['read-tag', row.isRead === 1 ? 'read' : 'unread'] }, row.isRead === 1 ? '已读' : '未读') },
    { title: '抄送时间', key: 'ccTime', width: 160 },
    { title: '操作', key: 'actions', width: 100, render: row => activeTab.value === 'received' && row.isRead === 0 ? h(NButton, { size: 'small', type: 'primary', onClick: () => handleMarkRead(row.id) }, () => '标记已读') : null },
  )

  return baseColumns
})

async function loadData() {
  loading.value = true
  try {
    const params = { pageNum: pagination.page, pageSize: pagination.pageSize, userId: userStore.userId }
    let res
    if (activeTab.value === 'received') {
      if (queryParams.isRead !== null)
        params.isRead = queryParams.isRead
      res = await flowApi.getMyCc(params)
    }
    else {
      res = await flowApi.getSentCc(params)
    }
    if (res.code === 200 && res.data) {
      dataSource.value = res.data.records || []
      pagination.itemCount = res.data.total || 0
      ccCount.value = res.data.total || 0
      if (activeTab.value === 'received')
        unreadCc.value = dataSource.value.filter(r => r.isRead === 0).length
    }
  }
  catch (error) { console.error('加载抄送列表失败:', error) }
  finally { loading.value = false }
}

async function loadStats() {
  try {
    const [todoRes, doneRes, startedRes] = await Promise.all([
      flowApi.getTodoTasks({ pageNum: 1, pageSize: 1, userId: userStore.userId }),
      flowApi.getDoneTasks({ pageNum: 1, pageSize: 1, userId: userStore.userId }),
      flowApi.getStartedTasks({ pageNum: 1, pageSize: 1, userId: userStore.userId }),
    ])
    todoCount.value = todoRes.code === 200 ? todoRes.data?.total || 0 : 0
    doneCount.value = doneRes.code === 200 ? doneRes.data?.total || 0 : 0
    startedCount.value = startedRes.code === 200 ? startedRes.data?.total || 0 : 0
  }
  catch (e) {
    console.error('加载统计数据失败', e)
  }
}

function handleSearch() {
  pagination.page = 1
  loadData()
}

function handleReset() {
  queryParams.isRead = null
  pagination.page = 1
  loadData()
}

function handleTabChange() {
  pagination.page = 1
  selectedRowKeys.value = []
  loadData()
}

function handleCheck(keys) {
  selectedRowKeys.value = keys
}

async function handleMarkRead(id) {
  try {
    const res = await flowApi.markCcRead(id)
    if (res.code === 200) {
      window.$message.success('已标记已读')
      loadData()
    }
  }
  catch {
    window.$message.error('操作失败')
  }
}

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
  catch {
    window.$message.error('操作失败')
  }
}

function handleMarkAllRead() {
  window.$dialog.warning({
    title: '确认',
    content: '确定将所有未读抄送标记为已读吗？',
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const unreadItems = dataSource.value.filter(item => item.isRead === 0)
        const ids = unreadItems.map(item => item.id)
        if (ids.length > 0) {
          await flowApi.batchMarkCcRead(ids)
          window.$message.success('已全部标记已读')
          loadData()
        }
      }
      catch {
        window.$message.error('操作失败')
      }
    },
  })
}

function handleSwitch(tab) {
  const routes = { todo: '/flow/todo', done: '/flow/done', started: '/flow/started', cc: '/flow/cc' }
  if (routes[tab])
    window.$router?.push(routes[tab])
}

onMounted(() => {
  loadStats()
  loadData()
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
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
}
.title-icon.cc {
  background: linear-gradient(135deg, #a78bfa 0%, #8b5cf6 100%);
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
.read-select {
  width: 120px;
}
.tabs-container {
  background: #fff;
  border-radius: 12px;
  padding: 12px 20px 0;
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.04);
  margin-bottom: 16px;
}
.tab-content {
  display: flex;
  align-items: center;
  gap: 6px;
}
.tab-badge {
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
  color: #fff;
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 10px;
  font-weight: 600;
}
.table-container {
  background: #fff;
  border-radius: 12px;
  padding: 16px 20px;
  flex: 1;
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.04);
}
:deep(.cc-title) {
  font-weight: 500;
  color: #0f172a;
}
:deep(.table-user) {
  display: flex;
  align-items: center;
  gap: 8px;
}
:deep(.user-name-text) {
  font-weight: 500;
  color: #0f172a;
}
:deep(.read-tag) {
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
}
:deep(.read-tag.read) {
  background: #dcfce7;
  color: #15803d;
}
:deep(.read-tag.unread) {
  background: #fee2e2;
  color: #b91c1c;
}
</style>
