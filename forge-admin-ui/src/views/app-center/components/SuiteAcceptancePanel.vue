<template>
  <div class="suite-acceptance-panel">
    <NSpin :show="loading">
      <div v-if="acceptance" class="acceptance-content">
        <div class="acceptance-hero" :class="`hero-${statusTone}`">
          <div class="hero-copy">
            <span class="hero-kicker">交付验收</span>
            <div class="hero-title-row">
              <h3>{{ acceptance.suiteName }} 最小交付标准</h3>
              <NTag :type="statusTagType" size="small" round>
                {{ statusLabel }}
              </NTag>
            </div>
            <p>{{ statusMessage }}</p>
          </div>

          <div class="hero-meter">
            <span>{{ normalizedScore }}<small>%</small></span>
            <NProgress
              type="line"
              :percentage="normalizedScore"
              :status="progressStatus"
              :height="8"
              :show-indicator="false"
            />
          </div>

          <NButton
            v-if="acceptance.nextAction"
            type="primary"
            size="small"
            class="hero-action"
            @click="handleNextAction"
          >
            {{ acceptance.nextActionLabel || '处理下一步' }}
          </NButton>
        </div>

        <div class="acceptance-steps">
          <div
            v-for="step in acceptanceSteps"
            :key="step.key"
            class="acceptance-step"
            :class="`step-${step.tone}`"
            @click="handleStepClick(step)"
          >
            <NIcon :size="18">
              <CheckmarkCircleOutline v-if="step.tone === 'success'" />
              <WarningOutline v-else />
            </NIcon>
            <div>
              <strong>{{ step.title }}</strong>
              <span>{{ step.desc }}</span>
            </div>
          </div>
        </div>

        <section class="acceptance-block">
          <div class="block-head">
            <div>
              <h4>核心对象</h4>
              <p>{{ runnableObjectCount }}/{{ objects.length }} 个对象达到运行标准，点击对象进入设计器处理缺口。</p>
            </div>
          </div>

          <div class="acceptance-objects">
            <div
              v-for="obj in objects"
              :key="obj.objectCode"
              class="acceptance-object"
              :class="{ 'object-success': obj.runnable, 'object-warning': !obj.runnable }"
              @click="handleObjectClick(obj)"
            >
              <div class="object-icon">
                <NIcon :size="20">
                  <CheckmarkCircleOutline v-if="obj.runnable" />
                  <WarningOutline v-else />
                </NIcon>
              </div>
              <div class="object-info">
                <div class="object-title">
                  <span>{{ obj.objectName }}</span>
                  <NTag :type="obj.runnable ? 'success' : 'warning'" size="small" :bordered="false">
                    {{ obj.runnable ? '可运行' : '待处理' }}
                  </NTag>
                </div>
                <p>{{ obj.message || obj.statusLabel }}</p>
              </div>
            </div>
          </div>
        </section>

        <div class="acceptance-secondary">
          <section class="acceptance-block">
            <div class="block-head compact">
              <div>
                <h4>引擎能力</h4>
                <p>{{ engineRunnableTotal }}/{{ engineTotal }} 个能力可运行。</p>
              </div>
              <NButton text type="primary" size="small" @click="emit('action', 'OPEN_ENGINE_CENTER', acceptance)">
                去引擎中心
              </NButton>
            </div>
            <div class="compact-list">
              <button
                v-for="engine in engines"
                :key="engine.engineType"
                class="compact-row"
                type="button"
                @click="handleEngineClick(engine)"
              >
                <span>{{ engine.engineName }}</span>
                <small>{{ engine.runnableCount }}/{{ engine.totalCount }} 可运行</small>
                <NTag :type="statusType(engine.status)" size="small" :bordered="false">
                  {{ statusText(engine.status) }}
                </NTag>
              </button>
            </div>
          </section>

          <section class="acceptance-block">
            <div class="block-head compact">
              <div>
                <h4>渠道接入</h4>
                <p>{{ channelAvailableTotal }}/{{ channelTotal }} 个入口可打开。</p>
              </div>
              <NButton text type="primary" size="small" @click="emit('action', 'OPEN_CHANNEL_CENTER', acceptance)">
                去渠道配置
              </NButton>
            </div>
            <div class="compact-list">
              <button
                v-for="channel in channels"
                :key="channel.channelType"
                class="compact-row"
                type="button"
                @click="handleChannelClick(channel)"
              >
                <span>{{ channel.channelName }}</span>
                <small>{{ channel.availableCount }}/{{ channel.totalCount }} 可用</small>
                <NTag :type="statusType(channel.status)" size="small" :bordered="false">
                  {{ statusText(channel.status) }}
                </NTag>
              </button>
            </div>
          </section>
        </div>
      </div>
      <NEmpty v-else-if="!loading" description="暂无验收数据" />
    </NSpin>
  </div>
</template>

<script setup>
import {
  CheckmarkCircleOutline,
  WarningOutline,
} from '@vicons/ionicons5'
import { NButton, NEmpty, NIcon, NProgress, NSpin, NTag } from 'naive-ui'
import { computed, ref, watch } from 'vue'
import { businessSuiteAcceptance } from '@/api/business-app'

const props = defineProps({
  suiteCode: {
    type: String,
    default: null,
  },
})

const emit = defineEmits(['objectClick', 'action'])

const loading = ref(false)
const acceptance = ref(null)

const objects = computed(() => acceptance.value?.objects || [])
const engines = computed(() => acceptance.value?.engines || [])
const channels = computed(() => acceptance.value?.channels || [])
const normalizedScore = computed(() => Math.max(0, Math.min(100, Number(acceptance.value?.score || 0))))
const runnableObjectCount = computed(() => objects.value.filter(item => item.runnable).length)
const engineRunnableTotal = computed(() => engines.value.reduce((total, item) => total + Number(item.runnableCount || 0), 0))
const engineTotal = computed(() => engines.value.reduce((total, item) => total + Number(item.totalCount || 0), 0))
const channelAvailableTotal = computed(() => channels.value.reduce((total, item) => total + Number(item.availableCount || 0), 0))
const channelTotal = computed(() => channels.value.reduce((total, item) => total + Number(item.totalCount || 0), 0))

const progressStatus = computed(() => {
  if (!acceptance.value)
    return 'default'
  if (acceptance.value.overallStatus === 'PASSED')
    return 'success'
  if (acceptance.value.overallStatus === 'FAILED')
    return 'error'
  return 'warning'
})

const statusTone = computed(() => {
  if (!acceptance.value)
    return 'warning'
  if (acceptance.value.overallStatus === 'PASSED')
    return 'success'
  if (acceptance.value.overallStatus === 'FAILED')
    return 'error'
  return 'warning'
})

const statusTagType = computed(() => statusType(acceptance.value?.overallStatus))

const statusLabel = computed(() => {
  if (!acceptance.value)
    return '检查中'
  switch (acceptance.value.overallStatus) {
    case 'PASSED':
      return '可交付'
    case 'PARTIAL':
      return '部分通过'
    case 'FAILED':
      return '未通过'
    default:
      return '检查中'
  }
})

const statusMessage = computed(() => {
  if (!acceptance.value)
    return ''
  switch (acceptance.value.overallStatus) {
    case 'PASSED':
      return '核心对象已具备运行入口、发布配置和导入导出能力，可以进入场景入口做业务验证。'
    case 'PARTIAL':
      return '已有对象可以运行，仍需处理下方待完善项后再交付。'
    case 'FAILED':
      return '最小交付链路仍有阻塞项，请先从第一个待处理对象进入配置。'
    default:
      return '正在检查业务套件是否达到最小交付标准。'
  }
})

const acceptanceSteps = computed(() => [
  {
    key: 'objects',
    title: '核心对象',
    desc: `${runnableObjectCount.value}/${objects.value.length} 可运行`,
    tone: objects.value.length && runnableObjectCount.value === objects.value.length ? 'success' : 'warning',
    action: 'FIX_OBJECT',
  },
  {
    key: 'engines',
    title: '引擎能力',
    desc: engineTotal.value ? `${engineRunnableTotal.value}/${engineTotal.value} 可运行` : '待接入能力',
    tone: engineTotal.value && engineRunnableTotal.value === engineTotal.value ? 'success' : 'warning',
    action: 'OPEN_ENGINE_CENTER',
  },
  {
    key: 'channels',
    title: '渠道接入',
    desc: channelTotal.value ? `${channelAvailableTotal.value}/${channelTotal.value} 可用` : '待配置入口',
    tone: channelTotal.value && channelAvailableTotal.value === channelTotal.value ? 'success' : 'warning',
    action: 'OPEN_CHANNEL_CENTER',
  },
  {
    key: 'acceptance',
    title: '验收结论',
    desc: statusLabel.value,
    tone: acceptance.value?.overallStatus === 'PASSED' ? 'success' : 'warning',
    action: acceptance.value?.nextAction,
  },
])

watch(() => props.suiteCode, (newVal) => {
  if (newVal)
    loadAcceptance()
  else
    acceptance.value = null
}, { immediate: true })

async function loadAcceptance() {
  if (!props.suiteCode)
    return

  loading.value = true
  try {
    const res = await businessSuiteAcceptance(props.suiteCode)
    acceptance.value = res.data || null
  }
  catch (error) {
    console.error('加载验收状态失败:', error)
    acceptance.value = null
  }
  finally {
    loading.value = false
  }
}

function statusType(status) {
  switch (status) {
    case 'PASSED':
    case 'RUNNABLE':
      return 'success'
    case 'FAILED':
    case 'MISSING':
    case 'ERROR':
      return 'error'
    default:
      return 'warning'
  }
}

function statusText(status) {
  switch (status) {
    case 'RUNNABLE':
      return '已就绪'
    case 'PARTIAL':
      return '部分就绪'
    case 'MISSING':
      return '未配置'
    case 'ERROR':
      return '异常'
    default:
      return '待完善'
  }
}

function handleObjectClick(obj) {
  emit('objectClick', obj)
}

function handleNextAction() {
  if (acceptance.value?.nextAction)
    emit('action', acceptance.value.nextAction, acceptance.value)
}

function handleStepClick(step) {
  if (step.action)
    emit('action', step.action, acceptance.value)
}

function handleEngineClick(engine) {
  emit('action', 'OPEN_ENGINE_CENTER', engine)
}

function handleChannelClick(channel) {
  switch (channel.channelType) {
    case 'MOBILE':
      emit('action', 'OPEN_MOBILE_CENTER', channel)
      break
    case 'INTEGRATION':
      emit('action', 'OPEN_INTEGRATION_CENTER', channel)
      break
    default:
      emit('action', 'OPEN_CHANNEL_CENTER', channel)
      break
  }
}
</script>

<style scoped>
.suite-acceptance-panel {
  padding: 0;
}

.acceptance-content {
  display: grid;
  gap: 14px;
}

.acceptance-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(160px, 220px) auto;
  gap: 16px;
  align-items: center;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #f8fbff;
  padding: 16px;
}

.acceptance-hero.hero-success {
  border-color: #bbf7d0;
  background: #f7fdf9;
}

.acceptance-hero.hero-warning {
  border-color: #fde68a;
  background: #fffdf4;
}

.acceptance-hero.hero-error {
  border-color: #fecaca;
  background: #fff7f7;
}

.hero-kicker {
  display: block;
  margin-bottom: 6px;
  color: #64748b;
  font-size: 12px;
}

.hero-title-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.hero-title-row h3 {
  margin: 0;
  color: #111827;
  font-size: 18px;
  line-height: 1.35;
}

.hero-copy p {
  margin: 8px 0 0;
  color: #475569;
  font-size: 13px;
  line-height: 1.6;
}

.hero-meter {
  display: grid;
  gap: 8px;
}

.hero-meter span {
  color: #111827;
  font-size: 28px;
  font-weight: 700;
  line-height: 1;
}

.hero-meter small {
  margin-left: 2px;
  color: #64748b;
  font-size: 13px;
  font-weight: 500;
}

.hero-action {
  justify-self: end;
}

.acceptance-steps {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.acceptance-step {
  display: grid;
  grid-template-columns: 24px minmax(0, 1fr);
  gap: 8px;
  align-items: start;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 12px;
  cursor: pointer;
  transition:
    border-color 0.2s,
    box-shadow 0.2s,
    transform 0.2s;
}

.acceptance-step:hover {
  border-color: #93c5fd;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.06);
  transform: translateY(-1px);
}

.acceptance-step.step-success {
  color: #16a34a;
}

.acceptance-step.step-warning {
  color: #d97706;
}

.acceptance-step strong,
.acceptance-step span {
  display: block;
}

.acceptance-step strong {
  overflow: hidden;
  color: #111827;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.acceptance-step span {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
}

.acceptance-block {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 14px;
}

.block-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.block-head.compact {
  align-items: flex-start;
}

.block-head h4 {
  margin: 0;
  color: #111827;
  font-size: 15px;
}

.block-head p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

.acceptance-objects {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 10px;
}

.acceptance-object {
  display: grid;
  grid-template-columns: 36px minmax(0, 1fr);
  gap: 10px;
  min-width: 0;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 12px;
  cursor: pointer;
  transition:
    border-color 0.2s,
    box-shadow 0.2s,
    transform 0.2s;
}

.acceptance-object:hover {
  border-color: #93c5fd;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.06);
  transform: translateY(-1px);
}

.object-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 8px;
}

.acceptance-object.object-success .object-icon {
  background: #dcfce7;
  color: #16a34a;
}

.acceptance-object.object-warning .object-icon {
  background: #fef3c7;
  color: #d97706;
}

.object-info {
  min-width: 0;
}

.object-title {
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: space-between;
}

.object-title span {
  overflow: hidden;
  color: #111827;
  font-size: 13px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.object-info p {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

.acceptance-secondary {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.compact-list {
  display: grid;
  gap: 8px;
}

.compact-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  gap: 8px;
  align-items: center;
  width: 100%;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f8fafc;
  padding: 10px 12px;
  cursor: pointer;
  text-align: left;
  transition:
    border-color 0.2s,
    background 0.2s;
}

.compact-row:hover {
  border-color: #bfdbfe;
  background: #eff6ff;
}

.compact-row span {
  overflow: hidden;
  color: #111827;
  font-size: 13px;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.compact-row small {
  color: #64748b;
  font-size: 12px;
  white-space: nowrap;
}

@media (max-width: 900px) {
  .acceptance-hero,
  .acceptance-secondary {
    grid-template-columns: 1fr;
  }

  .hero-action {
    justify-self: stretch;
  }

  .acceptance-steps {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .acceptance-steps,
  .acceptance-objects {
    grid-template-columns: 1fr;
  }

  .compact-row {
    grid-template-columns: minmax(0, 1fr) auto;
  }

  .compact-row :deep(.n-tag) {
    grid-column: 1 / -1;
    justify-self: start;
  }
}
</style>
