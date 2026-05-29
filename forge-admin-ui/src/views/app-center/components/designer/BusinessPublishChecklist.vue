<template>
  <div class="business-publish-checklist">
    <div class="publish-check-head">
      <div>
        <h3>发布检查</h3>
        <p>按字段、页面、关系、数据表和运行配置检查发布风险。</p>
      </div>
      <n-space size="small">
        <n-button size="small" secondary :loading="checking" @click="refresh">
          重新检查
        </n-button>
        <n-button size="small" type="success" :loading="publishing" :disabled="!publishable" @click="handlePublish">
          发布对象
        </n-button>
      </n-space>
    </div>

    <div class="publish-check-body">
      <main class="check-list-pane">
        <n-spin :show="checking">
          <div class="check-overview">
            <div>
              <span>通过</span>
              <strong>{{ checkResult.passCount || 0 }}</strong>
            </div>
            <div>
              <span>警告</span>
              <strong>{{ checkResult.warnCount || 0 }}</strong>
            </div>
            <div>
              <span>阻断</span>
              <strong>{{ checkResult.blockCount || 0 }}</strong>
            </div>
          </div>

          <section v-for="group in checkGroups" :key="group.level" class="check-group">
            <header>
              <div>
                <h4>{{ group.label }}</h4>
                <p>{{ group.description }}</p>
              </div>
              <n-tag :type="group.type" :bordered="false">
                {{ group.items.length }}
              </n-tag>
            </header>
            <div v-if="group.items.length" class="check-item-list">
              <article v-for="item in group.items" :key="`${item.itemCode}-${item.fieldCode || item.zoneKey || ''}`" class="check-item-card">
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
            <n-empty v-else description="暂无检查项" />
          </section>
        </n-spin>
      </main>

      <aside class="publish-side-pane">
        <section class="publish-action-card">
          <div class="publish-status-row">
            <span>总体状态</span>
            <n-tag :type="levelType(checkResult.overallStatus)" :bordered="false">
              {{ levelLabel(checkResult.overallStatus) }}
            </n-tag>
          </div>
          <p>{{ publishable ? '当前对象满足发布条件。' : '存在阻断项，请先按修复入口处理。' }}</p>
          <n-space vertical size="small">
            <n-button type="success" block :loading="publishing" :disabled="!publishable" @click="handlePublish">
              发布对象
            </n-button>
            <n-button secondary block :disabled="!runtimeInfo?.canOpen" @click="$emit('openRuntime')">
              打开应用
            </n-button>
          </n-space>
        </section>

        <section class="version-card">
          <div class="version-card-head">
            <h4>设计版本</h4>
            <n-button size="tiny" quaternary :loading="versionLoading" @click="loadVersions">
              刷新
            </n-button>
          </div>
          <n-spin :show="versionLoading">
            <n-empty v-if="!versions.length" description="暂无设计版本" />
            <n-timeline v-else>
              <n-timeline-item
                v-for="item in versions"
                :key="item.id"
                :type="item.versionType === 'ROLLBACK' ? 'warning' : 'success'"
                :title="`版本 ${item.versionNo || item.publishVersion || item.id}`"
                :content="versionContent(item)"
                :time="formatTime(item.createTime)"
              >
                <template #footer>
                  <n-popconfirm @positive-click="rollbackVersion(item.id)">
                    <template #trigger>
                      <n-button text size="tiny" class="text-warning">
                        回滚到此版本
                      </n-button>
                    </template>
                    确认回滚到版本 {{ item.versionNo }}？
                  </n-popconfirm>
                </template>
              </n-timeline-item>
            </n-timeline>
          </n-spin>
        </section>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { useMessage } from 'naive-ui'
import { computed, onMounted, ref, watch } from 'vue'
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
const checkResult = ref(createEmptyCheck())
const versions = ref([])

const publishable = computed(() => checkResult.value.publishable !== false && (checkResult.value.blockCount || 0) === 0)
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

watch(() => props.objectId, () => {
  refresh()
}, { immediate: true })

onMounted(() => {
  refresh()
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
  checking.value = true
  try {
    const res = await businessObjectPublishCheck(props.objectId)
    checkResult.value = {
      ...createEmptyCheck(),
      ...(res.data || {}),
    }
    emit('checkUpdated', checkResult.value)
  }
  finally {
    checking.value = false
  }
}

async function loadVersions() {
  if (!props.objectId)
    return
  versionLoading.value = true
  try {
    const res = await businessObjectDesignVersions(props.objectId)
    versions.value = res.data || []
  }
  finally {
    versionLoading.value = false
  }
}

async function rollbackVersion(versionId) {
  if (!props.objectId || !versionId)
    return
  await rollbackBusinessObjectDesignVersion(props.objectId, versionId)
  message.success('设计版本已回滚')
  emit('rolledBack')
  await refresh()
}

function handlePublish() {
  if (!publishable.value) {
    message.warning('存在阻断项，修复后再发布')
    return
  }
  emit('publish')
}

function resolveFixPanel(item = {}) {
  const target = item.fixTarget || item.zoneKey || ''
  if (['fields', 'field', 'FIELD'].includes(target) || item.category === 'FIELD')
    return 'fields'
  if (target === 'list' || target === 'table' || target === 'search')
    return 'list'
  if (target === 'detail')
    return 'detail'
  if (target === 'relations' || item.category === 'RELATION')
    return 'relations'
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
.check-group h4,
.version-card h4 {
  margin: 0;
  color: #111827;
  font-size: 15px;
}

.publish-check-head p,
.check-group header p,
.publish-action-card p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

.publish-check-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  min-height: 0;
}

.check-list-pane {
  min-width: 0;
  overflow: auto;
  background: #f8fafc;
  padding: 14px;
}

.check-overview {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-bottom: 12px;
}

.check-overview div,
.check-group,
.publish-action-card,
.version-card {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 14px;
}

.check-overview span {
  display: block;
  color: #64748b;
  font-size: 12px;
}

.check-overview strong {
  display: block;
  margin-top: 4px;
  color: #111827;
  font-size: 22px;
}

.check-group + .check-group {
  margin-top: 12px;
}

.check-group header,
.check-item-card,
.publish-status-row,
.version-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
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

.publish-side-pane {
  display: grid;
  align-content: start;
  gap: 12px;
  border-left: 1px solid #e5e7eb;
  background: #fbfcfe;
  padding: 12px;
}

.publish-status-row {
  align-items: center;
}

.publish-status-row span {
  color: #64748b;
  font-size: 12px;
}

@media (max-width: 1100px) {
  .publish-check-body {
    grid-template-columns: 1fr;
  }

  .publish-side-pane {
    border-left: 0;
    border-top: 1px solid #e5e7eb;
  }
}
</style>
