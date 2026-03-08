<template>
  <div class="monitor-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2 class="page-title">
        <i class="i-mdi:monitor-dashboard"></i>
        服务监控
      </h2>
      <n-button size="small" @click="loadAllData">
        <template #icon>
          <i class="i-mdi:refresh"></i>
        </template>
        刷新数据
      </n-button>
    </div>

    <!-- 监控指标卡片 -->
    <div class="metrics-section">
      <!-- CPU -->
      <div class="metric-card">
        <div class="metric-header">
          <div class="metric-icon cpu">
            <i class="i-mdi:cpu-64-bit"></i>
          </div>
          <div class="metric-title">CPU</div>
        </div>
        <div class="metric-body">
          <div class="metric-value-row">
            <span class="metric-label">核心数</span>
            <span class="metric-value">{{ systemInfo.cpu?.cores || '-' }}</span>
          </div>
          <div class="metric-value-row">
            <span class="metric-label">系统负载</span>
            <span class="metric-value">{{ formatLoad(systemInfo.cpu?.systemLoadAverage) }}</span>
          </div>
          <div class="metric-value-row">
            <span class="metric-label">系统使用率</span>
            <span class="metric-value" :class="getUsageClass(systemInfo.cpu?.systemCpuLoad)">
              {{ systemInfo.cpu?.systemCpuLoad || 'N/A' }}
            </span>
          </div>
          <div class="metric-value-row">
            <span class="metric-label">进程使用率</span>
            <span class="metric-value" :class="getUsageClass(systemInfo.cpu?.processCpuLoad)">
              {{ systemInfo.cpu?.processCpuLoad || 'N/A' }}
            </span>
          </div>
        </div>
      </div>

      <!-- 内存 -->
      <div class="metric-card">
        <div class="metric-header">
          <div class="metric-icon memory">
            <i class="i-mdi:memory"></i>
          </div>
          <div class="metric-title">内存</div>
        </div>
        <div class="metric-body">
          <div class="metric-value-row">
            <span class="metric-label">物理内存</span>
            <span class="metric-value">{{ systemInfo.memory?.physicalUsedPercent || '-' }}</span>
          </div>
          <div class="metric-value-row">
            <span class="metric-label">已用 / 总量</span>
            <span class="metric-value">{{ systemInfo.memory?.physicalUsed || '-' }} / {{ systemInfo.memory?.physicalTotal || '-' }}</span>
          </div>
          <div class="metric-value-row">
            <span class="metric-label">JVM堆内存</span>
            <span class="metric-value">{{ systemInfo.memory?.heapUsedPercent || '-' }}</span>
          </div>
          <div class="metric-value-row">
            <span class="metric-label">JVM已用 / 最大</span>
            <span class="metric-value">{{ systemInfo.memory?.heapUsed || '-' }} / {{ systemInfo.memory?.heapMax || '-' }}</span>
          </div>
        </div>
        <!-- 内存进度条 -->
        <div class="progress-bar-container">
          <div class="progress-label">物理内存使用率</div>
          <n-progress 
            type="line" 
            :percentage="getMemoryPercent(systemInfo.memory?.physicalUsedPercent)" 
            :indicator-placement="'inside'"
            :status="getProgressStatus(getMemoryPercent(systemInfo.memory?.physicalUsedPercent))"
          />
        </div>
      </div>

      <!-- JVM -->
      <div class="metric-card">
        <div class="metric-header">
          <div class="metric-icon jvm">
            <i class="i-mdi:application-braces"></i>
          </div>
          <div class="metric-title">JVM</div>
        </div>
        <div class="metric-body">
          <div class="metric-value-row">
            <span class="metric-label">版本</span>
            <span class="metric-value">{{ systemInfo.jvm?.javaVersion || '-' }}</span>
          </div>
          <div class="metric-value-row">
            <span class="metric-label">运行时间</span>
            <span class="metric-value">{{ systemInfo.jvm?.uptime || '-' }}</span>
          </div>
          <div class="metric-value-row">
            <span class="metric-label">线程数</span>
            <span class="metric-value">{{ systemInfo.jvm?.threadCount || '0' }} (峰值: {{ systemInfo.jvm?.peakThreadCount || '0' }})</span>
          </div>
          <div class="metric-value-row">
            <span class="metric-label">已加载类</span>
            <span class="metric-value">{{ formatNumber(systemInfo.jvm?.loadedClassCount) }}</span>
          </div>
        </div>
      </div>

      <!-- 服务器信息 -->
      <div class="metric-card">
        <div class="metric-header">
          <div class="metric-icon server">
            <i class="i-mdi:server"></i>
          </div>
          <div class="metric-title">服务器</div>
        </div>
        <div class="metric-body">
          <div class="metric-value-row">
            <span class="metric-label">主机名</span>
            <span class="metric-value">{{ systemInfo.server?.hostName || '-' }}</span>
          </div>
          <div class="metric-value-row">
            <span class="metric-label">IP地址</span>
            <span class="metric-value">{{ systemInfo.server?.hostAddress || '-' }}</span>
          </div>
          <div class="metric-value-row">
            <span class="metric-label">操作系统</span>
            <span class="metric-value">{{ systemInfo.server?.osName || '-' }}</span>
          </div>
          <div class="metric-value-row">
            <span class="metric-label">架构</span>
            <span class="metric-value">{{ systemInfo.server?.osArch || '-' }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 磁盘信息 -->
    <div class="disk-section">
      <div class="section-header">
        <h3 class="section-title">
          <i class="i-mdi:harddisk"></i>
          磁盘信息
        </h3>
      </div>
      <div class="disk-grid">
        <div v-for="(disk, index) in systemInfo.disk" :key="index" class="disk-card">
          <div class="disk-header">
            <div class="disk-icon">
              <i class="i-mdi:folder-open-outline"></i>
            </div>
            <div class="disk-path">{{ disk.path }}</div>
          </div>
          <div class="disk-usage">
            <div class="usage-info">
              <span class="usage-label">已用空间</span>
              <span class="usage-value">{{ disk.usedSpace }}</span>
            </div>
            <div class="usage-info">
              <span class="usage-label">可用空间</span>
              <span class="usage-value">{{ disk.freeSpace }}</span>
            </div>
            <div class="usage-info">
              <span class="usage-label">总空间</span>
              <span class="usage-value">{{ disk.totalSpace }}</span>
            </div>
          </div>
          <div class="disk-progress">
            <n-progress 
              type="line" 
              :percentage="parseFloat(disk.usedPercent)" 
              :indicator-placement="'inside'"
              :status="getProgressStatus(parseFloat(disk.usedPercent))"
            />
            <span class="disk-percent">{{ disk.usedPercent }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- JVM内存池详情 -->
    <div class="memory-pools-section" v-if="systemInfo.memory?.pools?.length">
      <div class="section-header">
        <h3 class="section-title">
          <i class="i-mdi:memory"></i>
          JVM内存池
        </h3>
      </div>
      <div class="pools-table">
        <n-data-table
          :columns="memoryPoolColumns"
          :data="systemInfo.memory.pools"
          :pagination="false"
          size="small"
          striped
        />
      </div>
    </div>

    <!-- 垃圾收集器信息 -->
    <div class="gc-section" v-if="systemInfo.jvm?.garbageCollectors?.length">
      <div class="section-header">
        <h3 class="section-title">
          <i class="i-mdi:trash-can-outline"></i>
          垃圾收集器
        </h3>
      </div>
      <div class="gc-grid">
        <div v-for="(gc, index) in systemInfo.jvm.garbageCollectors" :key="index" class="gc-card">
          <div class="gc-name">{{ gc.name }}</div>
          <div class="gc-stats">
            <div class="gc-stat">
              <span class="gc-stat-label">收集次数</span>
              <span class="gc-stat-value">{{ formatNumber(gc.collectionCount) }}</span>
            </div>
            <div class="gc-stat">
              <span class="gc-stat-label">收集时间</span>
              <span class="gc-stat-value">{{ gc.collectionTime }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { request } from '@/utils'

defineOptions({ name: 'SystemMonitor' })

// 系统信息
const systemInfo = ref({
  cpu: {},
  memory: {},
  jvm: {},
  disk: [],
  server: {}
})

// 加载状态
const loading = ref(false)

// 定时器
let refreshTimer = null

// 内存池表格列
const memoryPoolColumns = [
  { title: '名称', key: 'name', width: 200 },
  { title: '类型', key: 'type', width: 100 },
  { title: '已使用', key: 'used' },
  { title: '已提交', key: 'committed' },
  { title: '最大值', key: 'max' }
]

// 生命周期
onMounted(() => {
  loadAllData()
  // 每30秒自动刷新
  refreshTimer = setInterval(loadAllData, 30000)
})

onBeforeUnmount(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
})

// 加载所有数据
async function loadAllData() {
  loading.value = true
  try {
    const res = await request.get('/system/monitor/info')
    if (res.code === 200 && res.data) {
      systemInfo.value = res.data
    }
  } catch (error) {
    console.error('加载监控数据失败:', error)
    window.$message.error('加载监控数据失败')
  } finally {
    loading.value = false
  }
}

// 格式化负载
function formatLoad(load) {
  if (load === undefined || load === null || load < 0) return 'N/A'
  return load.toFixed(2)
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

// 获取使用率样式类
function getUsageClass(usage) {
  if (!usage) return ''
  const percent = parseFloat(usage.replace('%', ''))
  if (percent >= 80) return 'danger'
  if (percent >= 60) return 'warning'
  return 'success'
}

// 获取内存使用百分比
function getMemoryPercent(percentStr) {
  if (!percentStr) return 0
  const percent = parseFloat(percentStr.replace('%', ''))
  return isNaN(percent) ? 0 : Math.round(percent)
}

// 获取进度条状态
function getProgressStatus(percent) {
  if (percent >= 80) return 'error'
  if (percent >= 60) return 'warning'
  return 'success'
}
</script>

<style scoped>
.monitor-page {
  padding: 20px;
  background: #f8fafc;
  min-height: 100%;
}

/* 页面头部 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.page-title i {
  font-size: 24px;
  color: #3b82f6;
}

/* 监控指标区域 */
.metrics-section {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.metric-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  border: 1px solid #e2e8f0;
}

.metric-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f1f5f9;
}

.metric-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
}

.metric-icon.cpu {
  background: #dbeafe;
  color: #2563eb;
}

.metric-icon.memory {
  background: #dcfce7;
  color: #16a34a;
}

.metric-icon.jvm {
  background: #fef3c7;
  color: #d97706;
}

.metric-icon.server {
  background: #e0e7ff;
  color: #4f46e5;
}

.metric-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}

.metric-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.metric-value-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.metric-label {
  font-size: 13px;
  color: #64748b;
}

.metric-value {
  font-size: 13px;
  font-weight: 500;
  color: #1e293b;
}

.metric-value.success {
  color: #16a34a;
}

.metric-value.warning {
  color: #d97706;
}

.metric-value.danger {
  color: #dc2626;
}

.progress-bar-container {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f1f5f9;
}

.progress-label {
  font-size: 12px;
  color: #64748b;
  margin-bottom: 8px;
}

/* 磁盘区域 */
.disk-section {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  border: 1px solid #e2e8f0;
  margin-bottom: 20px;
}

.section-header {
  margin-bottom: 16px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-title i {
  font-size: 20px;
  color: #3b82f6;
}

.disk-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 16px;
}

.disk-card {
  background: #f8fafc;
  border-radius: 8px;
  padding: 16px;
  border: 1px solid #e2e8f0;
}

.disk-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.disk-icon {
  font-size: 20px;
  color: #3b82f6;
}

.disk-path {
  font-size: 14px;
  font-weight: 500;
  color: #1e293b;
  word-break: break-all;
}

.disk-usage {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
  padding: 12px;
  background: #fff;
  border-radius: 6px;
}

.usage-info {
  text-align: center;
}

.usage-label {
  display: block;
  font-size: 11px;
  color: #64748b;
  margin-bottom: 4px;
}

.usage-value {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: #1e293b;
}

.disk-progress {
  display: flex;
  align-items: center;
  gap: 12px;
}

.disk-progress :deep(.n-progress) {
  flex: 1;
}

.disk-percent {
  font-size: 13px;
  font-weight: 500;
  color: #1e293b;
  min-width: 45px;
  text-align: right;
}

/* 内存池区域 */
.memory-pools-section {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  border: 1px solid #e2e8f0;
  margin-bottom: 20px;
}

.pools-table {
  overflow-x: auto;
}

/* GC区域 */
.gc-section {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  border: 1px solid #e2e8f0;
}

.gc-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
}

.gc-card {
  background: #f8fafc;
  border-radius: 8px;
  padding: 16px;
  border: 1px solid #e2e8f0;
}

.gc-name {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #e2e8f0;
}

.gc-stats {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.gc-stat {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.gc-stat-label {
  font-size: 12px;
  color: #64748b;
}

.gc-stat-value {
  font-size: 13px;
  font-weight: 500;
  color: #1e293b;
}

/* 响应式 */
@media (max-width: 768px) {
  .monitor-page {
    padding: 12px;
  }
  
  .metrics-section {
    grid-template-columns: 1fr;
  }
  
  .disk-grid {
    grid-template-columns: 1fr;
  }
  
  .disk-usage {
    flex-direction: column;
    gap: 8px;
  }
  
  .usage-info {
    display: flex;
    justify-content: space-between;
    text-align: left;
  }
}
</style>
