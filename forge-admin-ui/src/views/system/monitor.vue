<template>
  <div class="monitor-page" :class="{ 'is-loading': loading }">
    <div class="monitor-ambient" aria-hidden="true" />

    <header class="monitor-header">
      <div class="monitor-heading">
        <div class="monitor-icon" aria-hidden="true">
          <i class="i-lucide:monitor" />
        </div>
        <div class="monitor-copy">
          <h2 class="monitor-title">
            服务监控
          </h2>
          <p class="monitor-subtitle">
            实时掌握服务器运行状态与性能指标
          </p>
        </div>
      </div>
      <n-button
        class="refresh-btn"
        round
        type="primary"
        :loading="loading"
        @click="loadAllData"
      >
        <template #icon>
          <i class="i-lucide:refresh-cw" />
        </template>
        刷新数据
      </n-button>
    </header>

    <section class="metrics-section">
      <!-- CPU -->
      <article class="metric-card theme-cpu">
        <div class="metric-card-glow" aria-hidden="true" />
        <div class="metric-card-art" aria-hidden="true">
          <i class="i-lucide:cpu" />
        </div>
        <div class="metric-card-head">
          <span class="metric-chip">
            <i class="i-lucide:cpu" />
          </span>
          <h3>CPU</h3>
        </div>
        <div class="metric-card-body">
          <div class="metric-row">
            <span>核心数</span>
            <strong>{{ systemInfo.cpu?.cores || '-' }}</strong>
          </div>
          <div class="metric-row">
            <span>系统负载</span>
            <strong>{{ formatLoad(systemInfo.cpu?.systemLoadAverage) }}</strong>
          </div>
          <div class="metric-row">
            <span>系统使用率</span>
            <strong :class="getUsageClass(systemInfo.cpu?.systemCpuLoad)">
              {{ systemInfo.cpu?.systemCpuLoad || 'N/A' }}
            </strong>
          </div>
          <div class="metric-row">
            <span>进程使用率</span>
            <strong :class="getUsageClass(systemInfo.cpu?.processCpuLoad)">
              {{ systemInfo.cpu?.processCpuLoad || 'N/A' }}
            </strong>
          </div>
        </div>
        <div class="metric-card-chart" aria-hidden="true">
          <svg viewBox="0 0 200 48" preserveAspectRatio="none">
            <defs>
              <linearGradient id="cpuArea" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stop-color="#3b82f6" stop-opacity="0.35" />
                <stop offset="100%" stop-color="#3b82f6" stop-opacity="0" />
              </linearGradient>
            </defs>
            <path :d="cpuAreaPath" fill="url(#cpuArea)" />
            <path :d="cpuLinePath" fill="none" stroke="#3b82f6" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </div>
      </article>

      <!-- 内存 -->
      <article class="metric-card theme-memory">
        <div class="metric-card-glow" aria-hidden="true" />
        <div class="metric-card-art" aria-hidden="true">
          <i class="i-lucide:memory-stick" />
        </div>
        <div class="metric-card-head">
          <span class="metric-chip">
            <i class="i-lucide:memory-stick" />
          </span>
          <h3>内存</h3>
        </div>
        <div class="metric-card-body">
          <div class="metric-row">
            <span>物理内存</span>
            <strong :class="getUsageClass(systemInfo.memory?.physicalUsedPercent)">
              {{ systemInfo.memory?.physicalUsedPercent || '-' }}
            </strong>
          </div>
          <div class="metric-row">
            <span>已用 / 总量</span>
            <strong class="metric-muted">
              {{ systemInfo.memory?.physicalUsed || '-' }} / {{ systemInfo.memory?.physicalTotal || '-' }}
            </strong>
          </div>
          <div class="metric-row">
            <span>JVM 堆内存</span>
            <strong :class="getUsageClass(systemInfo.memory?.heapUsedPercent)">
              {{ systemInfo.memory?.heapUsedPercent || '-' }}
            </strong>
          </div>
          <div class="metric-row">
            <span>JVM 已用 / 最大</span>
            <strong class="metric-muted">
              {{ systemInfo.memory?.heapUsed || '-' }} / {{ systemInfo.memory?.heapMax || '-' }}
            </strong>
          </div>
        </div>
        <div class="metric-progress">
          <div
            class="metric-progress-track"
            :class="getProgressTone(memoryPercent)"
          >
            <div class="metric-progress-fill" :style="{ width: `${memoryPercent}%` }" />
            <span class="metric-progress-text">{{ memoryPercent }}%</span>
          </div>
        </div>
      </article>

      <!-- JVM -->
      <article class="metric-card theme-jvm">
        <div class="metric-card-glow" aria-hidden="true" />
        <div class="metric-card-art" aria-hidden="true">
          <i class="i-lucide:coffee" />
        </div>
        <div class="metric-card-head">
          <span class="metric-chip">
            <i class="i-lucide:coffee" />
          </span>
          <h3>JVM</h3>
        </div>
        <div class="metric-card-body">
          <div class="metric-row">
            <span>版本</span>
            <strong>{{ systemInfo.jvm?.javaVersion || '-' }}</strong>
          </div>
          <div class="metric-row">
            <span>运行时间</span>
            <strong>{{ systemInfo.jvm?.uptime || '-' }}</strong>
          </div>
          <div class="metric-row">
            <span>线程数</span>
            <strong>{{ systemInfo.jvm?.threadCount || '0' }} <em>(峰值: {{ systemInfo.jvm?.peakThreadCount || '0' }})</em></strong>
          </div>
          <div class="metric-row">
            <span>已加载类</span>
            <strong>{{ formatNumber(systemInfo.jvm?.loadedClassCount) }}</strong>
          </div>
        </div>
        <div class="metric-card-bars" aria-hidden="true">
          <span v-for="(h, i) in jvmBars" :key="i" :style="{ height: `${h}%` }" />
        </div>
      </article>

      <!-- 服务器 -->
      <article class="metric-card theme-server">
        <div class="metric-card-glow" aria-hidden="true" />
        <div class="metric-card-art" aria-hidden="true">
          <i class="i-lucide:server" />
        </div>
        <div class="metric-card-head">
          <span class="metric-chip">
            <i class="i-lucide:server" />
          </span>
          <h3>服务器</h3>
        </div>
        <div class="metric-card-body">
          <div class="metric-row">
            <span>主机名</span>
            <strong class="metric-ellipsis" :title="systemInfo.server?.hostName">
              {{ systemInfo.server?.hostName || '-' }}
            </strong>
          </div>
          <div class="metric-row">
            <span>IP 地址</span>
            <strong>{{ systemInfo.server?.hostAddress || '-' }}</strong>
          </div>
          <div class="metric-row">
            <span>操作系统</span>
            <strong>{{ systemInfo.server?.osName || '-' }}</strong>
          </div>
          <div class="metric-row">
            <span>架构</span>
            <strong>{{ systemInfo.server?.osArch || '-' }}</strong>
          </div>
        </div>
        <div class="metric-card-waves" aria-hidden="true">
          <svg viewBox="0 0 200 48" preserveAspectRatio="none">
            <path d="M0 30 C30 18 50 42 80 28 C110 14 130 36 160 24 C180 16 190 28 200 22 L200 48 L0 48 Z" fill="rgba(139,92,246,0.16)" />
            <path d="M0 34 C40 22 60 40 100 30 C140 20 160 38 200 28" fill="none" stroke="rgba(139,92,246,0.45)" stroke-width="1.5" />
          </svg>
        </div>
      </article>
    </section>

    <!-- 磁盘信息 -->
    <section class="panel-card disk-section">
      <div class="panel-head">
        <div class="panel-title-wrap">
          <span class="panel-icon">
            <i class="i-lucide:hard-drive" />
          </span>
          <h3>磁盘信息</h3>
        </div>
      </div>
      <div class="disk-list">
        <div
          v-for="(disk, index) in systemInfo.disk"
          :key="index"
          class="disk-item"
        >
          <div class="disk-item-top">
            <div class="disk-path-wrap">
              <span class="disk-folder">
                <i class="i-lucide:folder-open" />
              </span>
              <span class="disk-path">{{ disk.path }}</span>
            </div>
            <div class="disk-stats">
              <div class="disk-stat">
                <span>已用空间</span>
                <strong>{{ disk.usedSpace }}</strong>
              </div>
              <div class="disk-stat">
                <span>可用空间</span>
                <strong>{{ disk.freeSpace }}</strong>
              </div>
              <div class="disk-stat">
                <span>总空间</span>
                <strong>{{ disk.totalSpace }}</strong>
              </div>
            </div>
          </div>
          <div class="disk-bar">
            <div
              class="disk-bar-fill"
              :style="{ width: `${clampPercent(disk.usedPercent)}%` }"
            >
              <span class="disk-bar-label">{{ disk.usedPercent }}</span>
            </div>
          </div>
        </div>
        <div v-if="!systemInfo.disk?.length" class="empty-hint">
          暂无磁盘信息
        </div>
      </div>
    </section>

    <!-- JVM 内存池 -->
    <section v-if="systemInfo.memory?.pools?.length" class="panel-card pools-section">
      <div class="panel-head">
        <div class="panel-title-wrap">
          <span class="panel-icon">
            <i class="i-lucide:layers" />
          </span>
          <h3>JVM 内存池</h3>
        </div>
      </div>
      <div class="pools-table">
        <n-data-table
          :columns="memoryPoolColumns"
          :data="systemInfo.memory.pools"
          :pagination="false"
          size="small"
          :bordered="false"
          :single-line="false"
        />
      </div>
    </section>

    <!-- 垃圾收集器 -->
    <section v-if="systemInfo.jvm?.garbageCollectors?.length" class="panel-card gc-section">
      <div class="panel-head">
        <div class="panel-title-wrap">
          <span class="panel-icon">
            <i class="i-lucide:recycle" />
          </span>
          <h3>垃圾收集器</h3>
        </div>
      </div>
      <div class="gc-grid">
        <div
          v-for="(gc, index) in systemInfo.jvm.garbageCollectors"
          :key="index"
          class="gc-card"
        >
          <div class="gc-name">
            {{ gc.name }}
          </div>
          <div class="gc-stats">
            <div class="gc-stat">
              <span>收集次数</span>
              <strong>{{ formatNumber(gc.collectionCount) }}</strong>
            </div>
            <div class="gc-stat">
              <span>收集时间</span>
              <strong>{{ gc.collectionTime }}</strong>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { request } from '@/utils'

defineOptions({ name: 'SystemMonitor' })

const systemInfo = ref({
  cpu: {},
  memory: {},
  jvm: {},
  disk: [],
  server: {},
})

const loading = ref(false)
const cpuHistory = ref([18, 24, 20, 32, 28, 36, 30, 42, 38, 44, 40, 48])

let refreshTimer = null

const memoryPoolColumns = [
  { title: '名称', key: 'name', minWidth: 160 },
  { title: '类型', key: 'type', width: 120 },
  { title: '已使用', key: 'used', minWidth: 120 },
  { title: '已提交', key: 'committed', minWidth: 120 },
  { title: '最大值', key: 'max', minWidth: 120 },
]

const memoryPercent = computed(() => getMemoryPercent(systemInfo.value.memory?.physicalUsedPercent))

const cpuLinePath = computed(() => buildSparkline(cpuHistory.value, false))
const cpuAreaPath = computed(() => buildSparkline(cpuHistory.value, true))

const jvmBars = computed(() => {
  const threads = Number(systemInfo.value.jvm?.threadCount) || 40
  const peak = Number(systemInfo.value.jvm?.peakThreadCount) || threads
  const base = Math.max(12, Math.min(92, Math.round((threads / Math.max(peak, 1)) * 70)))
  return [38, 52, 44, base - 8, 60, base, 48, 72, base - 4, 55, 68, 46].map(v => Math.max(16, Math.min(92, v)))
})

onMounted(() => {
  loadAllData()
  refreshTimer = setInterval(loadAllData, 30000)
})

onBeforeUnmount(() => {
  if (refreshTimer)
    clearInterval(refreshTimer)
})

async function loadAllData() {
  loading.value = true
  try {
    const res = await request.get('/system/monitor/info')
    if (res.code === 200 && res.data) {
      systemInfo.value = res.data
      pushCpuSample(res.data?.cpu?.systemCpuLoad)
    }
  }
  catch (error) {
    console.error('加载监控数据失败:', error)
    window.$message.error('加载监控数据失败')
  }
  finally {
    loading.value = false
  }
}

function pushCpuSample(usage) {
  const value = parseUsage(usage)
  if (value == null)
    return
  const next = [...cpuHistory.value, value]
  cpuHistory.value = next.slice(-12)
}

function parseUsage(usage) {
  if (usage == null || usage === '')
    return null
  const percent = Number.parseFloat(String(usage).replace('%', ''))
  return Number.isFinite(percent) ? Math.max(0, Math.min(100, percent)) : null
}

function buildSparkline(values, closed) {
  const list = values?.length ? values : [20, 30, 25, 40]
  const max = Math.max(...list, 1)
  const min = Math.min(...list, 0)
  const span = Math.max(max - min, 1)
  const w = 200
  const h = 48
  const step = w / Math.max(list.length - 1, 1)
  const points = list.map((v, i) => {
    const x = i * step
    const y = h - 6 - ((v - min) / span) * (h - 14)
    return [x, y]
  })
  let d = `M ${points[0][0]} ${points[0][1]}`
  for (let i = 1; i < points.length; i++)
    d += ` L ${points[i][0]} ${points[i][1]}`
  if (closed)
    d += ` L ${w} ${h} L 0 ${h} Z`
  return d
}

function formatLoad(load) {
  if (load === undefined || load === null || load < 0)
    return 'N/A'
  return Number(load).toFixed(2)
}

function formatNumber(num) {
  if (!num && num !== 0)
    return '0'
  const n = Number.parseInt(num, 10)
  if (n >= 1000000)
    return `${(n / 1000000).toFixed(1)}M`
  if (n >= 1000)
    return `${(n / 1000).toFixed(1)}K`
  return String(n)
}

function getUsageClass(usage) {
  const percent = parseUsage(usage)
  if (percent == null)
    return ''
  if (percent >= 80)
    return 'is-danger'
  if (percent >= 60)
    return 'is-warning'
  return 'is-success'
}

function getMemoryPercent(percentStr) {
  const percent = parseUsage(percentStr)
  return percent == null ? 0 : Math.round(percent)
}

function clampPercent(percentStr) {
  return getMemoryPercent(percentStr)
}

function getProgressTone(percent) {
  if (percent >= 80)
    return 'tone-danger'
  if (percent >= 60)
    return 'tone-warning'
  return 'tone-ok'
}
</script>

<style scoped>
.monitor-page {
  --monitor-card-bg: color-mix(in srgb, #ffffff 78%, transparent);
  --monitor-card-border: color-mix(in srgb, #ffffff 70%, #dbe4f5);
  --monitor-shadow: 0 10px 30px color-mix(in srgb, #1e3a8a 8%, transparent);
  --monitor-art-size: 40px;
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 100%;
  padding: 12px 10px 16px;
  overflow: auto;
  border-radius: 16px;
  background: transparent;
}

.monitor-ambient {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  background:
    radial-gradient(48% 42% at 8% 0%, rgba(147, 197, 253, 0.55), transparent 60%),
    radial-gradient(42% 38% at 92% 8%, rgba(196, 181, 253, 0.42), transparent 58%),
    radial-gradient(36% 30% at 70% 88%, rgba(165, 243, 252, 0.28), transparent 62%),
    linear-gradient(180deg, #eef4ff 0%, #f5f7fb 42%, #f7f8fb 100%);
  border-radius: 16px;
}

.monitor-page > *:not(.monitor-ambient) {
  position: relative;
  z-index: 1;
}

/* 避开 global.css .page-header 的白底直角条 */
.monitor-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-width: 0;
  padding: 4px 2px;
  background: transparent;
  border: none;
  border-radius: 0;
}

.monitor-heading {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.monitor-icon {
  display: inline-flex;
  flex: none;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: 12px;
  background: linear-gradient(145deg, #4f8cff, #2563eb);
  color: #fff;
  font-size: 20px;
  box-shadow: 0 8px 18px color-mix(in srgb, #2563eb 28%, transparent);
}

.monitor-copy {
  min-width: 0;
}

.monitor-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  line-height: 1.25;
  color: var(--text-primary, #111827);
}

.monitor-subtitle {
  margin: 4px 0 0;
  color: var(--text-tertiary, #6b7280);
  font-size: 12px;
  line-height: 1.4;
}

.refresh-btn {
  flex: none;
  border-radius: 999px !important;
  --n-border-radius: 999px !important;
  box-shadow: 0 6px 16px color-mix(in srgb, var(--primary-color, #2563eb) 28%, transparent);
}

.refresh-btn :deep(.n-button__border),
.refresh-btn :deep(.n-button__state-border) {
  border-radius: 999px !important;
}

.metrics-section {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.metric-card {
  position: relative;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 236px;
  padding: 16px 16px 12px;
  overflow: hidden;
  border: 1px solid var(--monitor-card-border);
  border-radius: 18px;
  background: var(--monitor-card-bg);
  box-shadow: var(--monitor-shadow);
  backdrop-filter: blur(18px);
}

.metric-card-glow {
  position: absolute;
  inset: 0 auto auto 0;
  width: 100%;
  height: 72px;
  pointer-events: none;
  opacity: 0.9;
  border-radius: 18px 18px 0 0;
}

.theme-cpu .metric-card-glow {
  background: linear-gradient(180deg, rgba(59, 130, 246, 0.16), transparent);
}

.theme-memory .metric-card-glow {
  background: linear-gradient(180deg, rgba(34, 197, 94, 0.14), transparent);
}

.theme-jvm .metric-card-glow {
  background: linear-gradient(180deg, rgba(249, 115, 22, 0.14), transparent);
}

.theme-server .metric-card-glow {
  background: linear-gradient(180deg, rgba(139, 92, 246, 0.14), transparent);
}

.metric-card-art {
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 0;
  display: grid;
  place-items: center;
  width: var(--monitor-art-size);
  height: var(--monitor-art-size);
  border-radius: 12px;
  font-size: 20px;
  opacity: 0.88;
  transform: rotate(6deg);
  pointer-events: none;
}

.theme-cpu .metric-card-art {
  color: #3b82f6;
  background: linear-gradient(145deg, rgba(219, 234, 254, 0.95), rgba(191, 219, 254, 0.55));
  box-shadow: 0 10px 20px rgba(59, 130, 246, 0.18);
}

.theme-memory .metric-card-art {
  color: #16a34a;
  background: linear-gradient(145deg, rgba(220, 252, 231, 0.95), rgba(187, 247, 208, 0.55));
  box-shadow: 0 10px 20px rgba(22, 163, 74, 0.16);
}

.theme-jvm .metric-card-art {
  color: #ea580c;
  background: linear-gradient(145deg, rgba(255, 237, 213, 0.95), rgba(254, 215, 170, 0.55));
  box-shadow: 0 10px 20px rgba(234, 88, 12, 0.16);
}

.theme-server .metric-card-art {
  color: #7c3aed;
  background: linear-gradient(145deg, rgba(237, 233, 254, 0.95), rgba(221, 214, 254, 0.55));
  box-shadow: 0 10px 20px rgba(124, 58, 237, 0.16);
}

.metric-card-head {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
  padding-right: calc(var(--monitor-art-size) + 10px);
}

.metric-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  font-size: 15px;
}

.theme-cpu .metric-chip {
  color: #2563eb;
  background: rgba(59, 130, 246, 0.12);
}

.theme-memory .metric-chip {
  color: #16a34a;
  background: rgba(34, 197, 94, 0.12);
}

.theme-jvm .metric-chip {
  color: #ea580c;
  background: rgba(249, 115, 22, 0.12);
}

.theme-server .metric-chip {
  color: #7c3aed;
  background: rgba(139, 92, 246, 0.12);
}

.metric-card-head h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary, #111827);
}

.metric-card-body {
  position: relative;
  z-index: 1;
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 10px;
  min-width: 0;
  min-height: 0;
}

.metric-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  min-width: 0;
}

.metric-row > span {
  flex: none;
  color: var(--text-tertiary, #6b7280);
  font-size: 12px;
}

.metric-row > strong {
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
  color: var(--text-primary, #111827);
  font-size: 13px;
  font-weight: 650;
  text-align: right;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 主机名等超长字段允许在值侧换行，避免被裁切或挡住图标 */
.metric-row > strong.metric-ellipsis {
  white-space: normal;
  word-break: break-all;
  line-height: 1.35;
}

.metric-row > strong em {
  margin-left: 2px;
  color: var(--text-tertiary, #6b7280);
  font-style: normal;
  font-weight: 500;
}

.metric-row > strong.metric-muted {
  color: var(--text-secondary, #4b5563);
  font-weight: 550;
}

.metric-row > strong.is-success {
  color: #16a34a;
}

.metric-row > strong.is-warning {
  color: #d97706;
}

.metric-row > strong.is-danger {
  color: #dc2626;
}

.metric-card-chart,
.metric-card-waves {
  position: relative;
  z-index: 1;
  margin: 10px -4px -4px;
  height: 48px;
}

.metric-card-chart svg,
.metric-card-waves svg {
  display: block;
  width: 100%;
  height: 100%;
}

.metric-progress {
  position: relative;
  z-index: 1;
  margin-top: 12px;
}

.metric-progress-track {
  position: relative;
  height: 18px;
  overflow: hidden;
  border-radius: 999px;
  background: color-mix(in srgb, #e5e7eb 70%, #fff);
}

.metric-progress-fill {
  height: 100%;
  border-radius: inherit;
  transition: width 0.35s ease;
}

.tone-ok .metric-progress-fill {
  background: linear-gradient(90deg, #4ade80, #22c55e);
}

.tone-warning .metric-progress-fill {
  background: linear-gradient(90deg, #fbbf24, #f59e0b);
}

.tone-danger .metric-progress-fill {
  background: linear-gradient(90deg, #fb7185, #ef4444);
}

.metric-progress-text {
  position: absolute;
  inset: 0 8px 0 auto;
  display: inline-flex;
  align-items: center;
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.25);
}

.metric-card-bars {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: flex-end;
  gap: 4px;
  height: 42px;
  margin-top: 10px;
  padding: 0 2px;
}

.metric-card-bars span {
  flex: 1;
  min-width: 0;
  border-radius: 4px 4px 2px 2px;
  background: linear-gradient(180deg, rgba(249, 115, 22, 0.75), rgba(249, 115, 22, 0.18));
}

.panel-card {
  padding: 16px 18px 18px;
  border: 1px solid var(--monitor-card-border);
  border-radius: 18px;
  background: var(--monitor-card-bg);
  box-shadow: var(--monitor-shadow);
  backdrop-filter: blur(18px);
}

.panel-head {
  margin-bottom: 14px;
}

.panel-title-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}

.panel-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: color-mix(in srgb, var(--primary-color, #2563eb) 12%, #fff);
  color: var(--primary-color, #2563eb);
  font-size: 15px;
}

.panel-title-wrap h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary, #111827);
}

.disk-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.disk-item {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 14px;
  border: 1px solid color-mix(in srgb, var(--border-light, #e5e7eb) 80%, transparent);
  border-radius: 14px;
  background: color-mix(in srgb, #fff 72%, #f8fafc);
}

.disk-item-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-width: 0;
}

.disk-path-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.disk-folder {
  display: inline-flex;
  flex: none;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 9px;
  background: color-mix(in srgb, var(--primary-color, #2563eb) 10%, #fff);
  color: var(--primary-color, #2563eb);
  font-size: 15px;
}

.disk-path {
  overflow: hidden;
  color: var(--text-primary, #111827);
  font-size: 14px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.disk-stats {
  display: flex;
  flex: none;
  gap: 22px;
}

.disk-stat {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 72px;
}

.disk-stat span {
  color: var(--text-tertiary, #6b7280);
  font-size: 11px;
}

.disk-stat strong {
  color: var(--text-primary, #111827);
  font-size: 13px;
  font-weight: 650;
}

.disk-bar {
  height: 22px;
  overflow: hidden;
  border-radius: 999px;
  background: #e8edf5;
}

.disk-bar-fill {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  min-width: 52px;
  height: 100%;
  padding: 0 10px;
  border-radius: inherit;
  background: linear-gradient(90deg, #6366f1 0%, #3b82f6 55%, #22d3ee 100%);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.28);
  transition: width 0.35s ease;
}

.disk-bar-label {
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.02em;
  text-shadow: 0 1px 2px rgba(15, 23, 42, 0.25);
}

.empty-hint {
  padding: 20px;
  color: var(--text-tertiary, #6b7280);
  font-size: 13px;
  text-align: center;
}

.pools-table :deep(.n-data-table) {
  --n-th-color: color-mix(in srgb, #dbeafe 55%, #fff);
  --n-th-text-color: #334155;
  --n-td-color: transparent;
  --n-border-color: color-mix(in srgb, #e2e8f0 80%, transparent);
  --n-merged-border-color: color-mix(in srgb, #e2e8f0 80%, transparent);
  background: transparent;
}

.pools-table :deep(.n-data-table-th) {
  font-weight: 650;
}

.pools-table :deep(.n-data-table-wrapper),
.pools-table :deep(.n-data-table-base-table),
.pools-table :deep(.n-data-table-base-table-body) {
  background: transparent;
}

.gc-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
}

.gc-card {
  padding: 14px;
  border: 1px solid color-mix(in srgb, var(--border-light, #e5e7eb) 80%, transparent);
  border-radius: 14px;
  background: color-mix(in srgb, #fff 72%, #f8fafc);
}

.gc-name {
  margin-bottom: 10px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border-light, #e5e7eb);
  color: var(--text-primary, #111827);
  font-size: 13px;
  font-weight: 650;
}

.gc-stats {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.gc-stat {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.gc-stat span {
  color: var(--text-tertiary, #6b7280);
  font-size: 12px;
}

.gc-stat strong {
  color: var(--text-primary, #111827);
  font-size: 13px;
  font-weight: 650;
}

:global(.dark) .monitor-page {
  --monitor-card-bg: color-mix(in srgb, var(--bg-primary) 82%, transparent);
  --monitor-card-border: color-mix(in srgb, var(--border-light) 80%, transparent);
  --monitor-shadow: 0 10px 28px rgba(0, 0, 0, 0.28);
}

:global(.dark) .monitor-ambient {
  background:
    radial-gradient(48% 42% at 8% 0%, color-mix(in srgb, var(--primary-color, #2563eb) 28%, transparent), transparent 60%),
    radial-gradient(42% 38% at 92% 8%, color-mix(in srgb, #8b5cf6 22%, transparent), transparent 58%),
    linear-gradient(180deg, color-mix(in srgb, var(--primary-color, #2563eb) 10%, var(--bg-secondary)) 0%, var(--bg-secondary) 48%);
}

:global(.dark) .disk-item,
:global(.dark) .gc-card {
  background: color-mix(in srgb, var(--bg-secondary) 70%, transparent);
}

:global(.dark) .disk-bar {
  background: color-mix(in srgb, var(--bg-secondary) 80%, #000);
}

:global(.dark) .pools-table :deep(.n-data-table) {
  --n-th-color: color-mix(in srgb, var(--primary-color, #2563eb) 18%, var(--bg-secondary));
}

@media (max-width: 1200px) {
  .metrics-section {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .monitor-header {
    flex-direction: column;
    align-items: stretch;
  }

  .refresh-btn {
    width: 100%;
  }

  .metrics-section {
    grid-template-columns: 1fr;
  }

  .disk-item-top {
    flex-direction: column;
    align-items: stretch;
  }

  .disk-stats {
    justify-content: space-between;
    gap: 10px;
  }
}
</style>
