<template>
  <div class="business-publish-checklist">
    <div class="publish-check-head">
      <div>
        <h3>发布检查</h3>
        <p>按字段、页面、关系级联、数据表和运行配置检查发布风险。</p>
      </div>
      <n-space size="small">
        <n-button size="small" secondary :loading="checking" @click="refresh">
          重新检查
        </n-button>
        <n-button size="small" type="success" :disabled="publishing || !publishable" @click="handlePublish">
          {{ publishing ? '发布中' : '发布对象' }}
        </n-button>
      </n-space>
    </div>

    <div class="publish-check-body">
      <div v-if="checking" class="publish-loading-mask">
        <span class="loading-dot" />
      </div>
      <div class="publish-check-content" :class="{ loading: checking }">
        <div class="publish-summary">
          <section class="publish-status-panel" :class="`status-${String(checkResult.overallStatus || 'WARN').toLowerCase()}`">
            <span>总体状态</span>
            <strong>{{ levelLabel(checkResult.overallStatus) }}</strong>
            <p>{{ publishable ? '当前对象满足发布条件。' : '存在阻断项，请先按修复入口处理。' }}</p>
            <n-checkbox v-if="requiresTableSync" v-model:checked="syncTable" size="small">
              同步数据表结构
            </n-checkbox>
            <n-space size="small">
              <n-button size="small" type="success" :disabled="publishing || !publishable" @click="handlePublish">
                {{ publishing ? '发布中' : '发布对象' }}
              </n-button>
              <n-button size="small" secondary :disabled="!runtimeInfo?.canOpen" @click="$emit('openRuntime')">
                打开应用
              </n-button>
            </n-space>
          </section>

          <button
            v-for="group in checkGroups"
            :key="group.level"
            type="button"
            class="summary-metric"
            :class="[{ active: activeCheckLevel === group.level }, `metric-${group.level.toLowerCase()}`]"
            @click="activeCheckLevel = group.level"
          >
            <span>{{ group.label }}</span>
            <strong>{{ group.items.length }}</strong>
            <em>{{ group.description }}</em>
          </button>
        </div>

        <div class="publish-workspace">
          <main class="check-workspace">
            <div class="check-toolbar">
              <div>
                <h4>{{ activeCheckGroup.label }}</h4>
                <p>{{ activeCheckGroup.description }}</p>
              </div>
              <n-radio-group v-model:value="activeCheckLevel" size="small" class="check-level-tabs">
                <n-radio-button v-for="group in checkGroups" :key="group.level" :value="group.level">
                  {{ group.label }} {{ group.items.length }}
                </n-radio-button>
              </n-radio-group>
            </div>

            <div v-if="activeCheckGroup.items.length" class="check-item-list">
              <article
                v-for="item in activeCheckGroup.items"
                :key="`${item.itemCode}-${item.fieldCode || item.zoneKey || ''}`"
                class="check-item-card"
              >
                <div class="check-item-main">
                  <n-tag size="small" :type="levelType(item.level)" :bordered="false">
                    {{ levelLabel(item.level) }}
                  </n-tag>
                  <div>
                    <strong>{{ item.title || item.itemCode }}</strong>
                    <p>{{ item.message || '-' }}</p>
                  </div>
                </div>
                <n-button
                  v-if="item.fixActionLabel"
                  size="small"
                  secondary
                  @click="$emit('fix', resolveFixPanel(item), item)"
                >
                  {{ item.fixActionLabel }}
                </n-button>
              </article>
            </div>
            <n-empty v-else description="当前分组没有检查项" />
          </main>

          <aside class="version-panel">
            <div class="version-card-head">
              <h4>设计版本</h4>
              <n-button size="tiny" quaternary :loading="versionLoading" @click="loadVersions">
                刷新
              </n-button>
            </div>
            <div class="version-panel-body">
              <div v-if="versionLoading" class="version-loading-mask">
                <span class="loading-dot" />
              </div>
              <n-empty v-if="!versions.length" description="暂无设计版本" />
              <div v-else class="version-list">
                <article v-for="item in versions" :key="item.id" class="version-item">
                  <div>
                    <strong>版本 {{ item.versionNo || item.publishVersion || item.id }}</strong>
                    <span>{{ formatTime(item.createTime) }}</span>
                    <p>{{ versionContent(item) }}</p>
                  </div>
                  <n-popconfirm @positive-click="rollbackVersion(item.id)">
                    <template #trigger>
                      <n-button text size="tiny" class="text-warning">
                        回滚
                      </n-button>
                    </template>
                    确认回滚到版本 {{ item.versionNo }}？
                  </n-popconfirm>
                </article>
              </div>
            </div>
          </aside>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useMessage } from 'naive-ui'
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import {
  businessObjectDesignVersions,
  businessObjectPublishCheck,
  rollbackBusinessObjectDesignVersion,
} from '@/api/business-app'

const props = defineProps({
  objectId: {
    type: [Number, String],
    default: null,
  },
  runtimeInfo: {
    type: Object,
    default: null,
  },
  publishing: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['checkUpdated', 'fix', 'publish', 'openRuntime', 'rolledBack'])

const message = useMessage()
const checking = ref(false)
const versionLoading = ref(false)
const activeCheckLevel = ref('BLOCK')
const checkResult = ref(createEmptyCheck())
const versions = ref([])
const syncTable = ref(false)
let alive = true
let checkRequestSeq = 0
let versionRequestSeq = 0

const publishable = computed(() => checkResult.value.publishable !== false && (checkResult.value.blockCount || 0) === 0)
const requiresTableSync = computed(() => (checkResult.value.items || []).some(item =>
  item?.fixAction === 'SYNC_TABLE' || item?.itemCode === 'TABLE_MISSING' || item?.itemCode === 'TABLE_COLUMN_MISSING'))
const checkGroups = computed(() => [
  {
    level: 'BLOCK',
    label: '阻断项',
    description: '必须修复后才能发布。',
    type: 'error',
    items: checkResult.value.blockItems || [],
  },
  {
    level: 'WARN',
    label: '警告项',
    description: '不阻断发布，但建议处理。',
    type: 'warning',
    items: checkResult.value.warnItems || [],
  },
  {
    level: 'PASS',
    label: '通过项',
    description: '已通过的发布检查。',
    type: 'success',
    items: checkResult.value.passItems || [],
  },
])
const activeCheckGroup = computed(() => checkGroups.value.find(group => group.level === activeCheckLevel.value) || checkGroups.value[0])

watch(() => props.objectId, () => {
  refresh()
}, { immediate: true })

watch(requiresTableSync, (value) => {
  syncTable.value = !!value
}, { immediate: true })

onBeforeUnmount(() => {
  alive = false
})

async function refresh() {
  if (!props.objectId) {
    checkResult.value = createEmptyCheck()
    versions.value = []
    emit('checkUpdated', checkResult.value)
    return
  }
  await Promise.all([loadCheck(), loadVersions()])
}

async function loadCheck() {
  if (!props.objectId)
    return
  const requestSeq = ++checkRequestSeq
  checking.value = true
  try {
    const res = await businessObjectPublishCheck(props.objectId)
    if (!alive || requestSeq !== checkRequestSeq)
      return
    checkResult.value = {
      ...createEmptyCheck(),
      ...(res.data || {}),
    }
    activeCheckLevel.value = resolveFocusLevel(checkResult.value)
    emit('checkUpdated', checkResult.value)
  }
  finally {
    if (alive && requestSeq === checkRequestSeq)
      checking.value = false
  }
}

async function loadVersions() {
  if (!props.objectId)
    return
  const requestSeq = ++versionRequestSeq
  versionLoading.value = true
  try {
    const res = await businessObjectDesignVersions(props.objectId)
    if (!alive || requestSeq !== versionRequestSeq)
      return
    versions.value = res.data || []
  }
  finally {
    if (alive && requestSeq === versionRequestSeq)
      versionLoading.value = false
  }
}

async function rollbackVersion(versionId) {
  if (!props.objectId || !versionId)
    return
  await rollbackBusinessObjectDesignVersion(props.objectId, versionId)
  if (!alive)
    return
  message.success('设计版本已回滚')
  emit('rolledBack')
  await refresh()
}

function handlePublish() {
  if (!publishable.value) {
    message.warning('存在阻断项，修复后再发布')
    return
  }
  if (requiresTableSync.value && !syncTable.value) {
    message.warning('请先确认同步数据表结构')
    return
  }
  emit('publish', { syncTable: syncTable.value })
}

function resolveFixPanel(item = {}) {
  const target = item.fixTarget || item.zoneKey || ''
  if (['fields', 'field', 'FIELD'].includes(target) || item.category === 'FIELD')
    return 'fields'
  if (target === 'list' || target === 'table' || target === 'search')
    return 'list'
  if (target === 'detail')
    return 'detail'
  if (target === 'relations' || item.category === 'RELATION' || item.category === 'LINKAGE')
    return 'relations'
  if (target === 'publish')
    return 'publish'
  if (item.category === 'RUNTIME' || item.category === 'TABLE')
    return 'advanced'
  if (item.category === 'PERMISSION')
    return 'permission'
  return target || 'form'
}

function levelType(level) {
  if (level === 'PASS')
    return 'success'
  if (level === 'WARN')
    return 'warning'
  if (level === 'BLOCK')
    return 'error'
  return 'default'
}

function levelLabel(level) {
  const labels = {
    PASS: '通过',
    WARN: '警告',
    BLOCK: '阻断',
  }
  return labels[level] || level || '未知'
}

function resolveFocusLevel(result = {}) {
  if ((result.blockCount || 0) > 0)
    return 'BLOCK'
  if ((result.warnCount || 0) > 0)
    return 'WARN'
  return 'PASS'
}

function versionContent(item) {
  const status = item.publishStatus || 'PUBLISHED'
  const remark = item.remark || (item.versionType === 'ROLLBACK' ? '回滚快照' : '发布快照')
  const config = item.configKey ? ` · ${item.configKey}` : ''
  return `${remark} · ${status}${config}`
}

function formatTime(value) {
  if (!value)
    return ''
  return new Date(value).toLocaleString()
}

function createEmptyCheck() {
  return {
    overallStatus: 'WARN',
    publishable: false,
    passCount: 0,
    warnCount: 0,
    blockCount: 0,
    items: [],
    passItems: [],
    warnItems: [],
    blockItems: [],
  }
}

defineExpose({
  refresh,
  loadCheck,
  loadVersions,
  publishable,
})
</script>

<style scoped>
.business-publish-checklist {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  min-height: calc(100vh - 106px);
  background: #f8fafc;
}

.publish-check-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid #e5e7eb;
  padding: 14px 16px;
}

.publish-check-head h3,
.check-workspace h4,
.version-panel h4 {
  margin: 0;
  color: #111827;
  font-size: 15px;
}

.publish-check-head p,
.publish-status-panel p,
.check-toolbar p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

.publish-check-body {
  position: relative;
  min-height: 0;
  overflow: auto;
  padding: 14px;
}

.publish-loading-mask,
.version-loading-mask {
  position: absolute;
  z-index: 5;
  display: grid;
  place-items: center;
  inset: 0;
  background: rgba(248, 250, 252, 0.58);
}

.loading-dot {
  width: 24px;
  height: 24px;
  border: 2px solid #cbd5e1;
  border-top-color: #2563eb;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.publish-check-content.loading {
  pointer-events: none;
}

.publish-summary {
  display: grid;
  grid-template-columns: minmax(240px, 1.2fr) repeat(3, minmax(112px, 0.8fr));
  gap: 12px;
  margin-bottom: 12px;
}

.publish-status-panel,
.summary-metric,
.check-workspace,
.version-panel {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.publish-status-panel {
  display: grid;
  align-content: start;
  gap: 10px;
  min-width: 0;
  min-height: 150px;
  border-left: 4px solid #f59e0b;
  padding: 16px;
}

.publish-status-panel.status-pass {
  border-left-color: #16a34a;
}

.publish-status-panel.status-block {
  border-left-color: #dc2626;
}

.publish-status-panel > span,
.summary-metric span,
.version-item span {
  color: #64748b;
  font-size: 12px;
}

.publish-status-panel strong {
  display: block;
  overflow: hidden;
  max-width: 100%;
  color: #111827;
  font-size: clamp(20px, 3vw, 26px);
  line-height: 1;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.summary-metric {
  display: grid;
  align-content: start;
  gap: 8px;
  min-width: 0;
  min-height: 150px;
  cursor: pointer;
  text-align: left;
  padding: 16px;
}

.summary-metric:hover,
.summary-metric.active {
  border-color: #2563eb;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.07);
}

.summary-metric strong {
  display: block;
  overflow: hidden;
  max-width: 100%;
  color: #111827;
  font-size: clamp(20px, 3.2vw, 30px);
  line-height: 1;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.summary-metric em {
  overflow: hidden;
  color: #64748b;
  font-size: 12px;
  font-style: normal;
  text-overflow: ellipsis;
}

.metric-block.active {
  border-color: #dc2626;
}

.metric-warn.active {
  border-color: #f59e0b;
}

.metric-pass.active {
  border-color: #16a34a;
}

.publish-workspace {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 12px;
}

.check-workspace,
.version-panel {
  min-width: 0;
  padding: 14px;
}

.check-toolbar,
.check-item-card,
.version-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.check-toolbar {
  align-items: center;
  border-bottom: 1px solid #eef2f7;
  padding-bottom: 12px;
}

.check-level-tabs {
  width: min(420px, 50%);
}

.check-item-list {
  display: grid;
  gap: 8px;
  margin-top: 12px;
}

.check-item-card {
  align-items: center;
  border: 1px solid #eef2f7;
  border-radius: 8px;
  background: #fdfefe;
  padding: 10px 12px;
}

.check-item-main {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  min-width: 0;
}

.check-item-main strong {
  display: block;
  color: #111827;
  font-size: 13px;
}

.check-item-main p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
}

.version-list {
  display: grid;
  gap: 8px;
  max-height: calc(100vh - 330px);
  overflow: auto;
  margin-top: 12px;
}

.version-panel-body {
  position: relative;
  min-height: 120px;
}

.version-item {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  border: 1px solid #eef2f7;
  border-radius: 8px;
  background: #fdfefe;
  padding: 10px;
}

.version-item strong {
  display: block;
  color: #111827;
  font-size: 13px;
}

.version-item span {
  display: block;
  margin-top: 3px;
}

.version-item p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
}

@media (max-width: 1100px) {
  .publish-summary,
  .publish-workspace {
    grid-template-columns: 1fr;
  }

  .check-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .check-level-tabs {
    width: 100%;
  }
}
</style>
