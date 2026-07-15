<template>
  <div class="publish-panel">
    <header class="panel-heading">
      <div>
        <h2>发布历史</h2>
        <p>历史记录轻量加载；发布时统一检查对象、数据库、入口、流程、扩展和权限。</p>
      </div>
      <div class="heading-actions">
        <n-button size="small" :loading="loading" @click="refreshHistory">
          刷新记录
        </n-button>
      </div>
    </header>

    <n-spin :show="loading">
      <section class="readiness-section">
        <div class="section-title">
          <div>
            <h3>发布检查</h3>
            <span>{{ readinessSummary }}</span>
          </div>
          <div class="readiness-actions">
            <span class="readiness-state" :class="readinessClass">{{ readinessLabel }}</span>
            <n-button size="tiny" secondary :loading="checking" @click="runPublishCheck">
              {{ checkResult ? '重新检查' : '执行发布检查' }}
            </n-button>
          </div>
        </div>

        <div v-if="checkResult?.selection?.dependencyMessages?.length" class="dependency-note">
          <strong>自动补齐依赖</strong>
          <span>{{ checkResult.selection.dependencyMessages.join('；') }}</span>
        </div>

        <div v-if="!checkResult" class="publish-check-placeholder">
          <strong>查看历史不会自动扫描全部应用资产</strong>
          <span>点击“执行发布检查”可提前查看阻断项；直接发布时，后端仍会执行最终权威检查。</span>
        </div>
        <n-empty v-else-if="!issues.length" size="small" description="当前没有发布阻断项或提醒" class="empty-state" />
        <div v-else class="issue-table">
          <button
            v-for="issue in issues"
            :key="`${issue.issueCode}-${issue.assetId || ''}`"
            type="button"
            class="issue-row"
            @click="emit('navigate', issue.actionPanel || issue.sectionKey || 'overview')"
          >
            <span class="issue-level" :class="`is-${String(issue.level).toLowerCase()}`">
              {{ issue.level === 'BLOCK' ? '阻断' : '提醒' }}
            </span>
            <span class="issue-copy"><strong>{{ issue.title }}</strong><small>{{ issue.message }}</small></span>
            <span class="issue-asset">{{ issue.assetCode || issue.assetType || '应用' }}</span>
            <span class="row-action">去处理</span>
          </button>
        </div>
      </section>

      <section class="history-section">
        <div class="section-title">
          <div><h3>不可变应用版本</h3><span>回滚会生成新版本，不覆盖历史快照</span></div>
          <span>{{ versions.length }} 个版本</span>
        </div>
        <div class="version-table">
          <div class="table-header version-grid">
            <span>版本</span><span>状态</span><span>发布时间</span><span>发布摘要</span><span>操作</span>
          </div>
          <div v-for="item in versions" :key="item.id" class="version-grid table-row">
            <strong>v{{ item.versionNo }}</strong>
            <DictTag dict-type="ai_business_application_publish_status" :value="item.publishStatus" :bordered="false" />
            <span>{{ item.publishedTime || '-' }}</span>
            <span class="summary-cell">{{ item.publishSummary || '-' }}</span>
            <span class="row-actions">
              <a class="cursor-pointer text-primary" @click="openVersion(item)">详情</a>
              <a
                v-if="item.versionNo !== application?.lastPublishVersion"
                class="cursor-pointer text-warning"
                @click="confirmRollback(item)"
              >回滚</a>
            </span>
          </div>
          <n-empty v-if="!versions.length" size="small" description="尚未生成应用级发布版本" class="empty-state" />
        </div>
      </section>

      <section class="history-section">
        <div class="section-title">
          <div><h3>发布运行记录</h3><span>部分失败会保留已完成步骤，可从失败位置继续</span></div>
          <span>{{ runs.length }} 条记录</span>
        </div>
        <div class="run-table">
          <div class="table-header run-grid">
            <span>目标版本</span><span>类型</span><span>状态</span><span>当前步骤</span><span>尝试</span><span>操作</span>
          </div>
          <div v-for="item in runs" :key="item.id" class="run-grid table-row">
            <strong>v{{ item.targetVersionNo }}</strong>
            <span>{{ item.operationType === 'ROLLBACK' ? `回滚 v${item.sourceVersionNo}` : '协调发布' }}</span>
            <DictTag dict-type="ai_business_application_publish_status" :value="item.runStatus" :bordered="false" />
            <span>{{ stepLabel(item.currentStep) }}</span>
            <span>{{ item.attemptCount || 1 }} 次</span>
            <span class="row-actions">
              <a
                v-if="['PARTIAL', 'FAILED'].includes(item.runStatus)"
                class="cursor-pointer text-warning"
                @click="recoverRun(item)"
              >恢复</a>
              <a class="cursor-pointer text-primary" @click="showRunSteps(item)">步骤</a>
            </span>
          </div>
          <n-empty v-if="!runs.length" size="small" description="尚无应用级发布运行记录" class="empty-state" />
        </div>
      </section>
    </n-spin>

    <ApplicationVersionDrawer
      v-model:show="versionDrawerVisible"
      :application-id="application?.id"
      :version-no="selectedVersionNo"
    />

    <n-modal v-model:show="stepModalVisible" preset="card" title="发布步骤" class="step-modal">
      <div class="step-list">
        <div v-for="step in selectedRun?.steps || []" :key="step.stepCode" class="step-row">
          <span class="step-dot" :class="`is-${String(step.status).toLowerCase()}`" />
          <span class="step-copy"><strong>{{ step.stepName }}</strong><small>{{ step.message || stepLabel(step.stepCode) }}</small></span>
          <span>{{ step.status }}</span>
        </div>
      </div>
      <n-alert v-if="selectedRun?.errorSummary" type="error" :bordered="false" title="最近失败原因">
        {{ selectedRun.errorSummary }}
      </n-alert>
    </n-modal>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import {
  businessApplicationPublishRuns,
  businessApplicationVersions,
  checkBusinessApplicationPublish,
  publishBusinessApplication,
  recoverBusinessApplicationPublish,
  rollbackBusinessApplication,
} from '@/api/business-application'
import ApplicationVersionDrawer from './ApplicationVersionDrawer.vue'

const props = defineProps({
  application: {
    type: Object,
    required: true,
  },
  publishRequestToken: {
    type: Number,
    default: 0,
  },
})

const emit = defineEmits(['changed', 'navigate', 'publishRequestConsumed'])
const loading = ref(false)
const checking = ref(false)
const publishing = ref(false)
const checkResult = ref(null)
const versions = ref([])
const runs = ref([])
const versionDrawerVisible = ref(false)
const selectedVersionNo = ref(null)
const stepModalVisible = ref(false)
const selectedRun = ref(null)

const issues = computed(() => checkResult.value?.issues || [])
const readinessLabel = computed(() => {
  if (!checkResult.value)
    return '待检查'
  if (checkResult.value.status === 'BLOCKED')
    return '暂不可发布'
  if (checkResult.value.status === 'WARNING')
    return '可发布，有提醒'
  return '可以发布'
})
const readinessClass = computed(() => `is-${String(checkResult.value?.status || 'pending').toLowerCase()}`)
const readinessSummary = computed(() => {
  if (!checkResult.value)
    return '尚未执行发布检查'
  return `${checkResult.value.blockingCount || 0} 项阻断，${checkResult.value.warningCount || 0} 项提醒`
})

watch(() => props.application?.id, () => {
  checkResult.value = null
  refreshHistory()
})
watch(() => props.publishRequestToken, consumePublishRequest)
onMounted(() => {
  refreshHistory()
  consumePublishRequest(props.publishRequestToken)
})

async function refreshHistory() {
  if (!props.application?.id || loading.value)
    return
  loading.value = true
  try {
    const [versionResponse, runResponse] = await Promise.all([
      businessApplicationVersions(props.application.id),
      businessApplicationPublishRuns(props.application.id),
    ])
    versions.value = versionResponse.data || []
    runs.value = runResponse.data || []
  }
  finally {
    loading.value = false
  }
}

async function runPublishCheck() {
  if (!props.application?.id || checking.value)
    return checkResult.value
  checking.value = true
  try {
    const response = await checkBusinessApplicationPublish(props.application.id, {})
    checkResult.value = response.data || null
    return checkResult.value
  }
  finally {
    checking.value = false
  }
}

function preparePublish() {
  if (publishing.value || checking.value)
    return
  const result = checkResult.value
  if (result && !result.publishable) {
    window.$message.warning(`当前存在 ${result?.blockingCount || 0} 项发布阻断，请处理后重试`)
    return
  }
  confirmPublish(result)
}

function consumePublishRequest(token) {
  if (!token)
    return
  emit('publishRequestConsumed')
  preparePublish()
}

function confirmPublish(check) {
  const warningCount = Number(check?.warningCount || 0)
  let content = '发布会依次处理对象、入口和扩展，并生成不可变应用版本。'
  if (warningCount > 0) {
    content = `当前有 ${warningCount} 项提醒。${content}`
  }
  else if (!check) {
    content = '系统会先执行最终发布检查，通过后依次处理对象、入口和扩展，并生成不可变应用版本。'
  }
  window.$dialog.warning({
    title: '发布当前应用',
    content,
    positiveText: check ? '确认发布' : '检查并发布',
    negativeText: '取消',
    onPositiveClick: executePublish,
  })
}

async function executePublish() {
  if (publishing.value)
    return
  publishing.value = true
  try {
    const response = await publishBusinessApplication(props.application.id, {}, createIdempotencyKey('publish'))
    handleRunResult(response.data)
  }
  catch (error) {
    window.$message.error(resolvePublishError(error), { duration: 8000 })
  }
  finally {
    publishing.value = false
    checkResult.value = null
    try {
      await refreshHistory()
    }
    catch {
      // 发布结果已经单独反馈，历史刷新失败不覆盖真实发布结果。
    }
  }
}

function resolvePublishError(error) {
  const message = String(
    error?.message
    || error?.detail?.rawMessage
    || error?.error?.message
    || '发布请求未完成，请刷新发布记录确认结果',
  )
  if (/timeout|timed out|ECONNABORTED/i.test(message)) {
    return '发布请求等待超时，后台可能仍在处理。请先刷新发布记录确认结果，不要连续重复发布。'
  }
  return `发布失败：${message}`
}

function confirmRollback(version) {
  window.$dialog.warning({
    title: `回滚到 v${version.versionNo}`,
    content: '回滚只恢复设计和运行配置，并生成新的回滚版本；不会删除字段、反向执行 DDL 或回滚业务数据。',
    positiveText: '确认回滚',
    negativeText: '取消',
    onPositiveClick: () => executeRollback(version),
  })
}

async function executeRollback(version) {
  publishing.value = true
  try {
    const response = await rollbackBusinessApplication(
      props.application.id,
      version.versionNo,
      { remark: `从应用工作台回滚到 v${version.versionNo}` },
      createIdempotencyKey(`rollback-${version.versionNo}`),
    )
    handleRunResult(response.data)
  }
  finally {
    publishing.value = false
    checkResult.value = null
    await refreshHistory()
  }
}

async function recoverRun(run) {
  publishing.value = true
  try {
    const response = await recoverBusinessApplicationPublish(props.application.id, run.id)
    handleRunResult(response.data)
  }
  finally {
    publishing.value = false
    checkResult.value = null
    await refreshHistory()
  }
}

function handleRunResult(result) {
  if (result?.runStatus === 'SUCCESS') {
    window.$message.success(result.message || '应用发布成功')
    emit('changed')
    return
  }
  if (result?.recoverable) {
    window.$message.warning(result.message || '发布部分完成，请处理失败项后恢复')
    return
  }
  window.$message.error(result?.message || '应用发布未完成')
}

function openVersion(version) {
  selectedVersionNo.value = version.versionNo
  versionDrawerVisible.value = true
}

function showRunSteps(run) {
  selectedRun.value = run
  stepModalVisible.value = true
}

function createIdempotencyKey(prefix) {
  const random = globalThis.crypto?.randomUUID?.() || `${Date.now()}-${Math.random().toString(16).slice(2)}`
  return `app-${props.application.id}-${prefix}-${random}`.slice(0, 128)
}

function stepLabel(step) {
  return {
    PRECHECK: '发布预检查',
    SNAPSHOT: '准备快照',
    OBJECTS: '发布业务对象',
    ENTRIES: '切换页面入口',
    EXTENSIONS: '启用业务扩展',
    COMMIT: '提交应用版本',
  }[step] || step || '-'
}
</script>

<style scoped>
.publish-panel {
  display: grid;
  gap: 22px;
}

.panel-heading,
.section-title,
.heading-actions,
.row-actions {
  display: flex;
  align-items: center;
}

.panel-heading,
.section-title {
  justify-content: space-between;
  gap: 18px;
}

.panel-heading {
  align-items: flex-start;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-light, #e5e6eb);
}

.panel-heading h2,
.section-title h3 {
  margin: 0;
  color: var(--text-primary, #1d2129);
}

.panel-heading h2 {
  font-size: 18px;
}

.panel-heading p {
  margin: 6px 0 0;
  color: var(--text-tertiary, #86909c);
  line-height: 1.6;
}

.heading-actions,
.readiness-actions,
.row-actions {
  gap: 10px;
}

.readiness-actions {
  display: flex;
  align-items: center;
}

.readiness-section,
.history-section {
  display: grid;
  gap: 11px;
}

.section-title > div {
  display: grid;
  gap: 3px;
}

.section-title h3 {
  font-size: 14px;
}

.section-title span {
  color: var(--text-tertiary, #86909c);
  font-size: 12px;
}

.readiness-state {
  padding: 4px 9px;
  border: 1px solid #b7dfc2;
  border-radius: 5px;
  color: #1a7f37 !important;
  background: #dafbe1;
}

.readiness-state.is-warning {
  border-color: #eed888;
  color: #9a6700 !important;
  background: #fff8c5;
}

.readiness-state.is-blocked {
  border-color: #ffcecb;
  color: #cf222e !important;
  background: #ffebe9;
}

.readiness-state.is-pending {
  border-color: var(--border-default, #c9cdd4);
  color: var(--text-tertiary, #86909c) !important;
  background: var(--bg-secondary, #f7f8fa);
}

.dependency-note {
  display: grid;
  grid-template-columns: 100px minmax(0, 1fr);
  gap: 10px;
  padding: 9px 11px;
  border-left: 3px solid #bf8700;
  color: #6e4c00;
  background: #fff8c5;
  font-size: 12px;
}

.publish-check-placeholder {
  display: grid;
  gap: 4px;
  padding: 13px 14px;
  border: 1px dashed var(--border-default, #c9cdd4);
  border-radius: 7px;
  color: var(--text-tertiary, #86909c);
  background: var(--bg-secondary, #f7f8fa);
  font-size: 12px;
}

.publish-check-placeholder strong {
  color: var(--text-secondary, #4e5969);
  font-size: 13px;
}

.issue-table,
.version-table,
.run-table {
  overflow: hidden;
  border: 1px solid var(--border-default, #c9cdd4);
  border-radius: 7px;
}

.issue-row {
  display: grid;
  grid-template-columns: 52px minmax(0, 1fr) minmax(90px, 130px) 58px;
  gap: 12px;
  align-items: center;
  width: 100%;
  min-height: 58px;
  padding: 8px 12px;
  cursor: pointer;
  border: 0;
  border-bottom: 1px solid var(--border-light, #e5e6eb);
  color: var(--text-secondary, #4e5969);
  background: var(--bg-primary, #fff);
  text-align: left;
}

.issue-row:hover,
.table-row:hover {
  background: var(--bg-hover, #f2f3f5);
}

.issue-row:last-child,
.table-row:last-child {
  border-bottom: 0;
}

.issue-level {
  width: fit-content;
  padding: 2px 6px;
  border-radius: 4px;
  color: #9a6700;
  background: #fff8c5;
  font-size: 11px;
}

.issue-level.is-block {
  color: #cf222e;
  background: #ffebe9;
}

.issue-copy {
  display: grid;
  min-width: 0;
  gap: 2px;
}

.issue-copy strong {
  font-size: 13px;
}

.issue-copy small,
.issue-asset {
  overflow: hidden;
  color: var(--text-tertiary, #86909c);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.row-action {
  color: var(--primary-color, #165dff);
  font-size: 12px;
  text-align: right;
}

.table-header,
.table-row {
  display: grid;
  gap: 12px;
  align-items: center;
  min-height: 43px;
  padding: 7px 11px;
}

.table-header {
  color: var(--text-tertiary, #86909c);
  background: var(--bg-secondary, #f7f8fa);
  font-size: 12px;
}

.table-row {
  border-bottom: 1px solid var(--border-light, #e5e6eb);
  font-size: 12px;
}

.version-grid {
  grid-template-columns: 72px 100px 150px minmax(180px, 1fr) 96px;
}

.run-grid {
  grid-template-columns: 85px 110px 100px minmax(130px, 1fr) 70px 92px;
}

.summary-cell {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.empty-state {
  padding: 26px;
}

.step-modal {
  width: min(620px, calc(100vw - 32px));
}

.step-list {
  display: grid;
  margin-bottom: 16px;
  border: 1px solid var(--border-default, #c9cdd4);
  border-radius: 7px;
}

.step-row {
  display: grid;
  grid-template-columns: 10px minmax(0, 1fr) 80px;
  gap: 10px;
  align-items: center;
  min-height: 50px;
  padding: 7px 11px;
  border-bottom: 1px solid var(--border-light, #e5e6eb);
}

.step-row:last-child {
  border-bottom: 0;
}

.step-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #8c959f;
}

.step-dot.is-success {
  background: #2da44e;
}

.step-dot.is-running {
  background: #0969da;
}

.step-dot.is-failed {
  background: #cf222e;
}

.step-copy {
  display: grid;
  gap: 2px;
}

.step-copy strong {
  font-size: 13px;
}

.step-copy small {
  color: var(--text-tertiary, #86909c);
}

@media (max-width: 900px) {
  .version-table,
  .run-table {
    overflow-x: auto;
  }

  .version-grid {
    min-width: 720px;
  }

  .run-grid {
    min-width: 720px;
  }
}
</style>
