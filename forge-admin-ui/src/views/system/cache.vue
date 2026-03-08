<template>
  <div class="cache-management-page">
    <!-- 监控指标卡片 -->
    <div class="metrics-section">
      <div class="metrics-grid">
        <!-- 内存使用 -->
        <div class="metric-card">
          <div class="metric-icon memory">
            <i class="i-mdi:memory"></i>
          </div>
          <div class="metric-content">
            <div class="metric-label">内存使用</div>
            <div class="metric-value">{{ cacheMetrics.memory.usedMemoryHuman || '-' }}</div>
            <div class="metric-sub">峰值: {{ cacheMetrics.memory.usedMemoryRssHuman || '-' }}</div>
          </div>
        </div>

        <!-- QPS -->
        <div class="metric-card">
          <div class="metric-icon qps">
            <i class="i-mdi:chart-line"></i>
          </div>
          <div class="metric-content">
            <div class="metric-label">QPS</div>
            <div class="metric-value">{{ cacheMetrics.stats.instantaneousOpsPerSec || '0' }}</div>
            <div class="metric-sub">次/秒</div>
          </div>
        </div>

        <!-- 连接数 -->
        <div class="metric-card">
          <div class="metric-icon connections">
            <i class="i-mdi:connection"></i>
          </div>
          <div class="metric-content">
            <div class="metric-label">总连接数</div>
            <div class="metric-value">{{ formatNumber(cacheMetrics.stats.totalConnectionsReceived) }}</div>
            <div class="metric-sub">已处理命令: {{ formatNumber(cacheMetrics.stats.totalCommandsProcessed) }}</div>
          </div>
        </div>

        <!-- 命中率 -->
        <div class="metric-card">
          <div class="metric-icon hitrate">
            <i class="i-mdi:target"></i>
          </div>
          <div class="metric-content">
            <div class="metric-label">命中率</div>
            <div class="metric-value">{{ cacheMetrics.stats.hitRate || '0%' }}</div>
            <div class="metric-sub">命中: {{ formatNumber(cacheMetrics.stats.keyspaceHits) }} / 未命中: {{ formatNumber(cacheMetrics.stats.keyspaceMisses) }}</div>
          </div>
        </div>

        <!-- 过期Key -->
        <div class="metric-card">
          <div class="metric-icon expired">
            <i class="i-mdi:clock-remove-outline"></i>
          </div>
          <div class="metric-content">
            <div class="metric-label">过期Key</div>
            <div class="metric-value">{{ formatNumber(cacheMetrics.stats.expiredKeys) }}</div>
            <div class="metric-sub">驱逐: {{ formatNumber(cacheMetrics.stats.evictedKeys) }}</div>
          </div>
        </div>

        <!-- Redis版本 -->
        <div class="metric-card">
          <div class="metric-icon version">
            <i class="i-mdi:information-outline"></i>
          </div>
          <div class="metric-content">
            <div class="metric-label">Redis版本</div>
            <div class="metric-value">{{ cacheMetrics.server.redisVersion || '-' }}</div>
            <div class="metric-sub">运行时间: {{ formatUptime(cacheMetrics.server.uptimeInSeconds) }}</div>
          </div>
        </div>
      </div>
    </div>

    <div class="cache-container">
      <!-- 左侧Key树 -->
      <div class="left-panel" :style="{ width: leftPanelWidth + 'px' }">
        <div class="panel-header">
          <n-input
            v-model:value="searchPattern"
            placeholder="搜索Key (支持通配符*)"
            clearable
            @keyup.enter="loadKeys"
          >
            <template #prefix>
              <i class="i-mdi:magnify"></i>
            </template>
          </n-input>
          <div class="header-actions">
            <n-button size="small" @click="loadKeys" class="mr-2">
              <template #icon>
                <i class="i-mdi:refresh"></i>
              </template>
              刷新
            </n-button>
            <n-button type="error" size="small" @click="handleClearAll">
              <template #icon>
                <i class="i-mdi:delete-sweep"></i>
              </template>
              清空
            </n-button>
          </div>
        </div>
        
        <div class="panel-body">
          <n-spin :show="treeLoading" class="tree-spin-wrapper">
            <div class="tree-wrapper">
              <n-tree
                block-line
                :data="treeData"
                :pattern="treePattern"
                :show-irrelevant-nodes="false"
                :selected-keys="selectedTreeKeys"
                selectable
                @update:selected-keys="handleTreeSelect"
                :render-label="renderTreeLabel"
                :render-prefix="renderTreePrefix"
              />
              <n-empty v-if="!treeLoading && treeData.length === 0" description="暂无数据" class="mt-8" />
            </div>
          </n-spin>
        </div>

        <div class="panel-footer">
          <n-text depth="3" style="font-size: 12px">
            <template v-if="totalKeys > 200">
              显示 200 个 / 共 {{ totalKeys }} 个Key
            </template>
            <template v-else>
              共 {{ totalKeys }} 个Key
            </template>
          </n-text>
        </div>
      </div>

      <!-- 拖拽分隔条 -->
      <div 
        class="resize-handle"
        @mousedown="startResize"
      >
        <div class="resize-line"></div>
      </div>

      <!-- 右侧详情 -->
      <div class="right-panel">
        <div v-if="!currentKey" class="empty-state">
          <i class="i-mdi:database-outline empty-icon"></i>
          <div class="empty-text">请选择左侧的Key查看详情</div>
        </div>

        <div v-else class="detail-container">
          <!-- 头部 -->
          <div class="detail-header">
            <div class="header-info">
              <div class="key-name">
                <i class="i-mdi:key-variant"></i>
                <span>{{ currentKey }}</span>
              </div>
              <div class="key-meta">
                <n-tag :type="getTypeTag(currentCache?.type).type" size="small" class="mr-2">
                  {{ currentCache?.type }}
                </n-tag>
                <n-tag type="info" size="small">
                  <template #icon>
                    <i class="i-mdi:clock-outline"></i>
                  </template>
                  {{ currentCache?.ttlDesc }}
                </n-tag>
              </div>
            </div>
            <div class="header-actions">
              <n-button size="small" @click="refreshCurrentKey" class="mr-2">
                <template #icon>
                  <i class="i-mdi:refresh"></i>
                </template>
              </n-button>
              <n-button type="error" size="small" @click="handleDeleteCurrent">
                <template #icon>
                  <i class="i-mdi:delete"></i>
                </template>
                删除
              </n-button>
            </div>
          </div>

          <!-- 内容区 -->
          <div class="detail-body">
            <n-spin :show="detailLoading">
              <div v-if="currentCache" class="cache-content">
                <!-- String类型 -->
                <div v-if="currentCache.type === 'STRING'" class="value-display">
                  <div class="content-toolbar">
                    <n-text strong>Value:</n-text>
                    <n-button 
                      v-if="isJsonString(currentCache.value)" 
                      text 
                      size="tiny"
                      @click="jsonFormatted = !jsonFormatted"
                    >
                      {{ jsonFormatted ? '原始格式' : 'JSON格式' }}
                    </n-button>
                  </div>
                  <pre v-if="isJsonString(currentCache.value) && jsonFormatted" class="json-content">{{ formatJson(currentCache.value) }}</pre>
                  <pre v-else class="text-content">{{ currentCache.value }}</pre>
                </div>

                <!-- Hash类型 -->
                <div v-else-if="currentCache.type === 'HASH'" class="value-display">
                  <div class="content-toolbar">
                    <n-text strong>Hash Fields ({{ Object.keys(currentCache.value || {}).length }}):</n-text>
                  </div>
                  <n-data-table
                    :columns="hashColumns"
                    :data="formatHashData(currentCache.value)"
                    :pagination="false"
                    :max-height="'calc(100vh - 320px)'"
                    size="small"
                    striped
                  />
                </div>

                <!-- Set类型 -->
                <div v-else-if="currentCache.type === 'SET'" class="value-display">
                  <div class="content-toolbar">
                    <n-text strong>Set Members ({{ (currentCache.value || []).length }}):</n-text>
                  </div>
                  <div class="set-list">
                    <div v-for="(item, index) in currentCache.value" :key="index" class="set-item">
                      {{ formatValue(item) }}
                    </div>
                  </div>
                </div>

                <!-- List类型 -->
                <div v-else-if="currentCache.type === 'LIST'" class="value-display">
                  <div class="content-toolbar">
                    <n-text strong>List Elements ({{ (currentCache.value || []).length }}):</n-text>
                  </div>
                  <n-data-table
                    :columns="listColumns"
                    :data="formatListData(currentCache.value)"
                    :pagination="false"
                    :max-height="'calc(100vh - 320px)'"
                    size="small"
                    striped
                  />
                </div>

                <!-- 其他类型 -->
                <div v-else class="value-display">
                  <div class="content-toolbar">
                    <n-text strong>Value:</n-text>
                  </div>
                  <pre class="text-content">{{ JSON.stringify(currentCache.value, null, 2) }}</pre>
                </div>
              </div>
            </n-spin>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, h, onMounted, onBeforeUnmount } from 'vue'
import { NTag, NIcon } from 'naive-ui'
import { request } from '@/utils'

defineOptions({ name: 'CacheManagement' })

// 左侧面板宽度
const leftPanelWidth = ref(320)
const isResizing = ref(false)

// 搜索和树形数据
const searchPattern = ref('*')
const treePattern = ref('')
const treeData = ref([])
const treeLoading = ref(false)
const totalKeys = ref(0)
const selectedTreeKeys = ref([])

// 当前选中的Key
const currentKey = ref(null)
const currentCache = ref(null)
const detailLoading = ref(false)
const jsonFormatted = ref(true)

// 缓存监控指标
const cacheMetrics = ref({
  memory: {},
  stats: {},
  server: {}
})

// Hash表格列
const hashColumns = [
  { title: '键', key: 'key', ellipsis: { tooltip: true }, width: 200 },
  { title: '值', key: 'value', ellipsis: { tooltip: true } }
]

// List表格列
const listColumns = [
  { title: '索引', key: 'index', width: 80 },
  { title: '值', key: 'value', ellipsis: { tooltip: true } }
]

// 生命周期
onMounted(() => {
  loadKeys()
  loadMetrics()
  // 添加全局事件监听
  document.addEventListener('mousemove', handleMouseMove)
  document.addEventListener('mouseup', handleMouseUp)
  
  // 定时刷新监控指标（每30秒）
  setInterval(loadMetrics, 30000)
})

onBeforeUnmount(() => {
  // 移除全局事件监听
  document.removeEventListener('mousemove', handleMouseMove)
  document.removeEventListener('mouseup', handleMouseUp)
})

// 加载监控指标
async function loadMetrics() {
  try {
    const res = await request.get('/system/cache/metrics')
    if (res.code === 200 && res.data) {
      cacheMetrics.value = res.data
    }
  } catch (error) {
    console.error('加载缓存监控指标失败:', error)
  }
}

// 格式化数字
function formatNumber(num) {
  if (!num) return '0'
  const n = parseInt(num)
  if (n >= 1000000) {
    return (n / 1000000).toFixed(1) + 'M'
  } else if (n >= 1000) {
    return (n / 1000).toFixed(1) + 'K'
  }
  return n.toString()
}

// 格式化运行时间
function formatUptime(seconds) {
  if (!seconds) return '-'
  const s = parseInt(seconds)
  const days = Math.floor(s / 86400)
  const hours = Math.floor((s % 86400) / 3600)
  const minutes = Math.floor((s % 3600) / 60)
  
  if (days > 0) {
    return `${days}天${hours}小时`
  } else if (hours > 0) {
    return `${hours}小时${minutes}分钟`
  } else {
    return `${minutes}分钟`
  }
}

// 拖拽调整宽度
function startResize(e) {
  isResizing.value = true
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
}

function handleMouseMove(e) {
  if (!isResizing.value) return
  
  const newWidth = e.clientX
  // 限制最小200px,最大600px
  if (newWidth >= 200 && newWidth <= 600) {
    leftPanelWidth.value = newWidth
  }
}

function handleMouseUp() {
  if (isResizing.value) {
    isResizing.value = false
    document.body.style.cursor = ''
    document.body.style.userSelect = ''
  }
}

// 加载Keys并构建树
async function loadKeys() {
  try {
    treeLoading.value = true
    const res = await request.get('/system/cache/page', {
      params: {
        pattern: searchPattern.value || '*',
        page: 1,
        pageSize: 200 // 限制最多加载200个key
      }
    })
    
    if (res.code === 200) {
      const keys = res.data.records.map(item => item.key)
      totalKeys.value = res.data.total
      treeData.value = buildTree(keys)
      
      // 如果总数超过200,提示用户使用搜索
      if (res.data.total > 200) {
        window.$message.warning(`共有 ${res.data.total} 个Key，当前仅显示前200个。请使用搜索功能精确查找。`)
      }
    }
  } catch (error) {
    window.$message.error('加载Keys失败')
  } finally {
    treeLoading.value = false
  }
}

// 构建树形结构
function buildTree(keys) {
  const tree = []
  const map = new Map()
  
  // 主分隔符优先级: 冒号(:)优先级最高
  const primarySeparator = ':'
  
  keys.forEach(key => {
    // 只使用冒号作为分隔符
    if (key.includes(primarySeparator)) {
      // 有冒号,按层级展示
      const parts = key.split(primarySeparator)
      let currentLevel = tree
      let currentPath = ''
      
      for (let i = 0; i < parts.length; i++) {
        const part = parts[i]
        currentPath = currentPath ? currentPath + primarySeparator + part : part
        
        if (i === parts.length - 1) {
          // 最后一层,是实际的Key
          currentLevel.push({
            key: key,
            label: part,
            isLeaf: true,
            fullKey: key
          })
        } else {
          // 中间层,是分组
          let folder = currentLevel.find(item => item.label === part && !item.isLeaf)
          if (!folder) {
            folder = {
              key: currentPath,
              label: part,
              isLeaf: false,
              children: []
            }
            currentLevel.push(folder)
          }
          currentLevel = folder.children
        }
      }
    } else {
      // 没有冒号,直接作为根节点
      tree.push({
        key: key,
        label: key,
        isLeaf: true,
        fullKey: key
      })
    }
  })
  
  return tree
}

// 树形节点标签渲染
function renderTreeLabel({ option }) {
  if (option.isLeaf) {
    return h('span', { class: 'tree-leaf-label' }, option.label)
  }
  return h('span', { class: 'tree-folder-label' }, [
    option.label,
    h('span', { class: 'tree-count' }, ` (${countLeaves(option)})`)
  ])
}

// 树形节点图标渲染
function renderTreePrefix({ option }) {
  if (option.isLeaf) {
    return h(NIcon, { size: 16 }, {
      default: () => h('i', { class: 'i-mdi:key' })
    })
  }
  return h(NIcon, { size: 16 }, {
    default: () => h('i', { class: 'i-mdi:folder' })
  })
}

// 计算文件夹下的叶子节点数量
function countLeaves(node) {
  if (node.isLeaf) return 1
  if (!node.children) return 0
  return node.children.reduce((sum, child) => sum + countLeaves(child), 0)
}

// 树节点选择
function handleTreeSelect(keys, option) {
  if (option.length === 0) return
  
  const selectedNode = option[0]
  if (selectedNode.isLeaf) {
    selectedTreeKeys.value = keys
    loadKeyDetail(selectedNode.fullKey)
  }
}

// 加载Key详情
async function loadKeyDetail(key) {
  try {
    currentKey.value = key
    detailLoading.value = true
    
    const res = await request.post('/system/cache/getInfo', null, {
      params: { key }
    })
    
    if (res.code === 200) {
      currentCache.value = res.data
    }
  } catch (error) {
    window.$message.error('获取缓存详情失败')
  } finally {
    detailLoading.value = false
  }
}

// 刷新当前Key
async function refreshCurrentKey() {
  if (currentKey.value) {
    await loadKeyDetail(currentKey.value)
    window.$message.success('刷新成功')
  }
}

// 删除当前Key
function handleDeleteCurrent() {
  if (!currentKey.value) return
  
  window.$dialog.warning({
    title: '确认删除',
    content: `确定要删除缓存键"${currentKey.value}"吗？删除后将无法恢复！`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await request.post('/system/cache/remove', null, {
          params: { key: currentKey.value }
        })
        if (res.code === 200) {
          window.$message.success('删除成功')
          currentKey.value = null
          currentCache.value = null
          selectedTreeKeys.value = []
          await loadKeys()
        }
      } catch (error) {
        window.$message.error('删除失败')
      }
    }
  })
}

// 清空所有缓存
function handleClearAll() {
  window.$dialog.error({
    title: '危险操作',
    content: `确定要清空所有匹配"${searchPattern.value}"的缓存吗？此操作非常危险，将删除匹配的所有数据，无法恢复！`,
    positiveText: '确定清空',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        const res = await request.post('/system/cache/clear', null, {
          params: { pattern: searchPattern.value || '*' }
        })
        if (res.code === 200) {
          window.$message.success(res.message || '清空成功')
          currentKey.value = null
          currentCache.value = null
          selectedTreeKeys.value = []
          await loadKeys()
        }
      } catch (error) {
        window.$message.error('清空失败')
      }
    }
  })
}

// 获取类型标签配置
function getTypeTag(type) {
  const typeMap = {
    'STRING': { type: 'info' },
    'HASH': { type: 'success' },
    'SET': { type: 'warning' },
    'LIST': { type: 'primary' },
    'NONE': { type: 'error' }
  }
  return typeMap[type] || { type: 'default' }
}

// 判断是否为JSON字符串
function isJsonString(str) {
  if (typeof str !== 'string') return false
  try {
    const obj = JSON.parse(str)
    return typeof obj === 'object' && obj !== null
  } catch (e) {
    return false
  }
}

// 格式化JSON
function formatJson(str) {
  try {
    const obj = JSON.parse(str)
    return JSON.stringify(obj, null, 2)
  } catch (e) {
    return str
  }
}

// 格式化Hash数据
function formatHashData(hashMap) {
  if (!hashMap || typeof hashMap !== 'object') return []
  return Object.entries(hashMap).map(([key, value]) => ({
    key,
    value: formatValue(value)
  }))
}

// 格式化List数据
function formatListData(list) {
  if (!Array.isArray(list)) return []
  return list.map((value, index) => ({
    index,
    value: formatValue(value)
  }))
}

// 格式化值
function formatValue(value) {
  if (value === null || value === undefined) return ''
  if (typeof value === 'object') {
    return JSON.stringify(value)
  }
  return String(value)
}
</script>

<style scoped>
.cache-management-page {
  height: 100%;
  width: 100%;
  overflow: hidden;
  background: #fff;
  display: flex;
  flex-direction: column;
}

/* 监控指标区域 */
.metrics-section {
  padding: 16px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
  flex-shrink: 0;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
}

.metric-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  border: 1px solid #e2e8f0;
}

.metric-icon {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
}

.metric-icon.memory {
  background: #dbeafe;
  color: #2563eb;
}

.metric-icon.qps {
  background: #dcfce7;
  color: #16a34a;
}

.metric-icon.connections {
  background: #fef3c7;
  color: #d97706;
}

.metric-icon.hitrate {
  background: #fce7f3;
  color: #db2777;
}

.metric-icon.expired {
  background: #fee2e2;
  color: #dc2626;
}

.metric-icon.version {
  background: #e0e7ff;
  color: #4f46e5;
}

.metric-content {
  flex: 1;
  min-width: 0;
}

.metric-label {
  font-size: 12px;
  color: #64748b;
  margin-bottom: 4px;
}

.metric-value {
  font-size: 20px;
  font-weight: 600;
  color: #1e293b;
  line-height: 1.2;
}

.metric-sub {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 2px;
}

/* 缓存容器 */
.cache-container {
  display: flex;
  flex: 1;
  overflow: hidden;
  gap: 1px;
  background: #e5e7eb;
}

/* 左侧面板 */
.left-panel {
  min-width: 200px;
  max-width: 600px;
  background: #fff;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #e5e7eb;
  height: 100%;
  overflow: hidden;
  flex-shrink: 0;
}

/* 拖拽分隔条 */
.resize-handle {
  width: 4px;
  background: #e5e7eb;
  cursor: col-resize;
  position: relative;
  flex-shrink: 0;
  transition: background-color 0.2s;
}

.resize-handle:hover {
  background: #3b82f6;
}

.resize-handle:hover .resize-line {
  opacity: 1;
}

.resize-line {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 2px;
  height: 40px;
  background: #fff;
  border-radius: 1px;
  opacity: 0;
  transition: opacity 0.2s;
}

.panel-header {
  padding: 16px;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
}

.header-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}

.panel-body {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 8px;
  min-height: 0;
  height: 0;
  position: relative;
}

.panel-body::-webkit-scrollbar {
  width: 6px;
}

.panel-body::-webkit-scrollbar-track {
  background: #f1f1f1;
}

.panel-body::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.panel-body::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

/* Spin包裹器 */
.tree-spin-wrapper {
  width: 100%;
  min-height: 100%;
}

:deep(.tree-spin-wrapper .n-spin-content) {
  height: 100%;
}

.tree-wrapper {
  min-height: 100%;
}

.panel-footer {
  padding: 12px 16px;
  border-top: 1px solid #e5e7eb;
  background: #f9fafb;
  flex-shrink: 0;
}

/* 树形样式 */
:deep(.n-tree) {
  font-size: 13px;
}

:deep(.n-tree-node) {
  padding: 2px 0;
}

:deep(.n-tree-node-content) {
  padding: 4px 8px;
  border-radius: 4px;
  transition: background-color 0.2s;
}

:deep(.n-tree-node-content:hover) {
  background-color: #f3f4f6;
}

:deep(.n-tree-node-content.n-tree-node-content--selected) {
  background-color: #e0e7ff;
  font-weight: 500;
}

:deep(.n-tree-node-switcher) {
  width: 20px;
  height: 20px;
}

:deep(.n-tree-node-indent) {
  width: 16px;
}

.tree-leaf-label {
  color: #374151;
  font-size: 13px;
  word-break: break-all;
  line-height: 1.4;
}

.tree-folder-label {
  color: #1f2937;
  font-weight: 500;
  font-size: 13px;
  word-break: break-all;
  line-height: 1.4;
}

.tree-count {
  color: #9ca3af;
  font-size: 12px;
  font-weight: normal;
  margin-left: 4px;
}

/* 右侧面板 */
.right-panel {
  flex: 1;
  background: #fff;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  height: 100%;
  min-width: 0;
}

.empty-state {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #9ca3af;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
  opacity: 0.3;
}

.empty-text {
  font-size: 14px;
}

/* 详情容器 */
.detail-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.detail-header {
  padding: 16px 20px;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #f9fafb;
  flex-shrink: 0;
}

.header-info {
  flex: 1;
  min-width: 0;
}

.key-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 8px;
  word-break: break-all;
}

.key-name i {
  font-size: 20px;
  color: #6366f1;
  flex-shrink: 0;
}

.key-meta {
  display: flex;
  gap: 8px;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 8px;
  margin-left: 16px;
  flex-shrink: 0;
}

.detail-body {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 20px;
  min-height: 0;
  height: 0;
}

.detail-body::-webkit-scrollbar {
  width: 8px;
}

.detail-body::-webkit-scrollbar-track {
  background: #f1f1f1;
}

.detail-body::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 4px;
}

.detail-body::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

/* 内容显示 */
.value-display {
  height: 100%;
}

.content-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #e5e7eb;
}

.json-content,
.text-content {
  font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
  font-size: 13px;
  line-height: 1.6;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  padding: 16px;
  margin: 0;
  white-space: pre-wrap;
  word-wrap: break-word;
  color: #374151;
  overflow: auto;
}

.json-content {
  color: #0066cc;
}

/* Set列表 */
.set-list {
  overflow-y: auto;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #f9fafb;
}

.set-item {
  padding: 10px 16px;
  border-bottom: 1px solid #e5e7eb;
  font-size: 13px;
  font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
  color: #374151;
}

.set-item:last-child {
  border-bottom: none;
}

.set-item:hover {
  background: #f3f4f6;
}

/* 表格样式 */
:deep(.n-data-table) {
  font-size: 13px;
}

:deep(.n-data-table .n-data-table-td) {
  font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
}

/* 响应式 */
@media (max-width: 768px) {
  .metrics-grid {
    grid-template-columns: 1fr;
  }
  
  .cache-container {
    flex-direction: column;
  }
  
  .left-panel {
    width: 100% !important;
    max-width: 100%;
    height: 300px;
  }
  
  .resize-handle {
    display: none;
  }
}
</style>
