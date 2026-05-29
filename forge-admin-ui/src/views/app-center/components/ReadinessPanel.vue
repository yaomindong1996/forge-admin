<template>
  <div class="readiness-panel">
    <NSpin :show="loading">
      <div v-if="readiness" class="readiness-content">
        <div class="readiness-status" :class="`status-${statusTone}`">
          <div class="status-copy">
            <span class="status-kicker">对象就绪度</span>
            <div class="status-title-row">
              <h3>{{ readiness.objectName }}</h3>
              <NTag :type="statusTagType" size="small" round>
                {{ statusLabel }}
              </NTag>
            </div>
            <p>{{ statusMessage }}</p>
          </div>

          <div class="status-meter">
            <span class="meter-number">{{ normalizedScore }}<small>%</small></span>
            <NProgress
              type="line"
              :percentage="normalizedScore"
              :status="progressStatus"
              :height="8"
              :show-indicator="false"
            />
          </div>

          <NButton
            v-if="readiness.nextAction"
            type="primary"
            size="small"
            class="status-action"
            @click="handleNextAction"
          >
            {{ readiness.nextActionLabel || '处理下一步' }}
          </NButton>
        </div>

        <div class="readiness-metrics">
          <div>
            <strong>{{ runnableCount }}</strong>
            <span>已就绪</span>
          </div>
          <div>
            <strong>{{ pendingCount }}</strong>
            <span>待完善</span>
          </div>
          <div>
            <strong>{{ issueCount }}</strong>
            <span>阻塞项</span>
          </div>
        </div>

        <div class="readiness-items-grid">
          <div
            v-for="item in readiness.items"
            :key="item.itemCode"
            class="readiness-item"
            :class="itemStatusClass(item)"
            @click="handleItemAction(item)"
          >
            <div class="item-icon">
              <NIcon :size="18">
                <CheckmarkCircleOutline v-if="item.status === 'RUNNABLE'" />
                <WarningOutline v-else-if="item.status === 'CONFIGURED' || item.status === 'REGISTERED'" />
                <CloseCircleOutline v-else />
              </NIcon>
            </div>
            <div class="item-info">
              <div class="item-title">
                <span>{{ item.itemName }}</span>
                <NTag :type="itemTagType(item.status)" size="small" :bordered="false">
                  {{ item.statusLabel }}
                </NTag>
              </div>
              <p>{{ item.message || item.statusLabel }}</p>
              <NButton
                v-if="item.nextAction"
                text
                type="primary"
                size="tiny"
                class="item-action"
                @click.stop="handleItemAction(item)"
              >
                {{ item.nextActionLabel || '去处理' }}
              </NButton>
            </div>
          </div>
        </div>
      </div>
      <NEmpty v-else-if="!loading" description="暂无就绪度数据" />
    </NSpin>
  </div>
</template>

<script setup>
import {
  CheckmarkCircleOutline,
  CloseCircleOutline,
  WarningOutline,
} from '@vicons/ionicons5'
import { NButton, NEmpty, NIcon, NProgress, NSpin, NTag } from 'naive-ui'
import { computed, ref, watch } from 'vue'
import { businessObjectReadiness } from '@/api/business-app'

const props = defineProps({
  objectId: {
    type: [Number, String],
    default: null,
  },
})

const emit = defineEmits(['action'])

const loading = ref(false)
const readiness = ref(null)

const normalizedScore = computed(() => {
  const score = Number(readiness.value?.score || 0)
  return Math.max(0, Math.min(100, score))
})

const progressStatus = computed(() => {
  if (!readiness.value)
    return 'default'
  const status = readiness.value.overallStatus
  if (status === 'RUNNABLE')
    return 'success'
  if (status === 'ERROR' || status === 'MISSING')
    return 'error'
  return 'warning'
})

const statusTone = computed(() => {
  if (!readiness.value)
    return 'pending'
  const status = readiness.value.overallStatus
  if (status === 'RUNNABLE')
    return 'success'
  if (status === 'ERROR' || status === 'MISSING')
    return 'error'
  return 'warning'
})

const statusTagType = computed(() => {
  if (statusTone.value === 'success')
    return 'success'
  if (statusTone.value === 'error')
    return 'error'
  return 'warning'
})

const statusLabel = computed(() => {
  if (!readiness.value)
    return '检查中'
  switch (readiness.value.overallStatus) {
    case 'RUNNABLE':
      return '可运行'
    case 'CONFIGURED':
      return '待验证'
    case 'REGISTERED':
      return '待配置'
    case 'MISSING':
      return '缺配置'
    case 'ERROR':
      return '有异常'
    default:
      return '检查中'
  }
})

const statusMessage = computed(() => {
  if (!readiness.value)
    return ''
  const status = readiness.value.overallStatus
  switch (status) {
    case 'RUNNABLE':
      return '对象设计、发布和导入导出能力已就绪，可以进入业务列表继续操作。'
    case 'CONFIGURED':
      return '核心配置已经完成，仍有增强能力或交付项需要验证。'
    case 'REGISTERED':
      return '对象档案已登记，请进入设计器补齐字段、页面、发布或能力配置。'
    case 'MISSING':
      return '存在阻塞运行的缺口，先处理标红检查项后再打开应用。'
    case 'ERROR':
      return '配置链路存在异常，优先处理错误项再继续交付验收。'
    default:
      return '正在检查业务对象是否具备运行条件。'
  }
})

const readinessItems = computed(() => readiness.value?.items || [])
const runnableCount = computed(() => readinessItems.value.filter(item => item.status === 'RUNNABLE').length)
const pendingCount = computed(() => readinessItems.value.filter(item => ['CONFIGURED', 'REGISTERED'].includes(item.status)).length)
const issueCount = computed(() => readinessItems.value.filter(item => ['MISSING', 'ERROR'].includes(item.status)).length)

watch(() => props.objectId, (newVal) => {
  if (newVal)
    loadReadiness()
  else
    readiness.value = null
}, { immediate: true })

async function loadReadiness() {
  if (!props.objectId)
    return

  loading.value = true
  try {
    const res = await businessObjectReadiness(props.objectId)
    readiness.value = res.data || null
  }
  catch (error) {
    console.error('加载就绪度失败:', error)
    readiness.value = null
  }
  finally {
    loading.value = false
  }
}

function itemStatusClass(item) {
  if (item.status === 'RUNNABLE')
    return 'item-success'
  if (item.status === 'ERROR' || item.status === 'MISSING')
    return 'item-error'
  return 'item-warning'
}

function itemTagType(status) {
  if (status === 'RUNNABLE')
    return 'success'
  if (status === 'ERROR' || status === 'MISSING')
    return 'error'
  return 'warning'
}

function handleNextAction() {
  if (readiness.value?.nextAction)
    emit('action', readiness.value.nextAction, readiness.value)
}

function handleItemAction(item) {
  if (item.nextAction)
    emit('action', item.nextAction, item)
}
</script>

<style scoped>
.readiness-panel {
  padding: 16px;
}

.readiness-content {
  display: grid;
  gap: 14px;
}

.readiness-status {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(160px, 220px) auto;
  gap: 16px;
  align-items: center;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #f8fbff;
  padding: 16px;
}

.readiness-status.status-success {
  border-color: #bbf7d0;
  background: #f7fdf9;
}

.readiness-status.status-warning {
  border-color: #fde68a;
  background: #fffdf4;
}

.readiness-status.status-error {
  border-color: #fecaca;
  background: #fff7f7;
}

.status-kicker {
  display: block;
  margin-bottom: 6px;
  color: #64748b;
  font-size: 12px;
}

.status-title-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.status-title-row h3 {
  margin: 0;
  color: #111827;
  font-size: 18px;
  line-height: 1.35;
}

.status-copy p {
  margin: 8px 0 0;
  color: #475569;
  font-size: 13px;
  line-height: 1.6;
}

.status-meter {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.meter-number {
  color: #111827;
  font-size: 28px;
  font-weight: 700;
  line-height: 1;
}

.meter-number small {
  margin-left: 2px;
  color: #64748b;
  font-size: 13px;
  font-weight: 500;
}

.status-action {
  justify-self: end;
  min-width: 96px;
}

.readiness-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.readiness-metrics div {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 10px 12px;
}

.readiness-metrics strong,
.readiness-metrics span {
  display: block;
}

.readiness-metrics strong {
  color: #111827;
  font-size: 20px;
  line-height: 1.2;
}

.readiness-metrics span {
  margin-top: 3px;
  color: #64748b;
  font-size: 12px;
}

.readiness-items-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 10px;
}

.readiness-item {
  display: grid;
  grid-template-columns: 32px minmax(0, 1fr);
  gap: 10px;
  min-width: 0;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 12px;
  transition:
    border-color 0.2s,
    box-shadow 0.2s,
    transform 0.2s;
}

.readiness-item:hover {
  border-color: #bfdbfe;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.06);
  transform: translateY(-1px);
}

.readiness-item.item-success .item-icon {
  background: #dcfce7;
  color: #16a34a;
}

.readiness-item.item-warning .item-icon {
  background: #fef3c7;
  color: #d97706;
}

.readiness-item.item-error .item-icon {
  background: #fee2e2;
  color: #dc2626;
}

.item-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
}

.item-info {
  min-width: 0;
}

.item-title {
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: space-between;
  min-width: 0;
}

.item-title span {
  overflow: hidden;
  color: #111827;
  font-size: 13px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-info p {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

.item-action {
  margin-top: 6px;
}

@media (max-width: 760px) {
  .readiness-status {
    grid-template-columns: 1fr;
  }

  .status-action {
    justify-self: stretch;
  }

  .readiness-metrics {
    grid-template-columns: 1fr;
  }
}
</style>
