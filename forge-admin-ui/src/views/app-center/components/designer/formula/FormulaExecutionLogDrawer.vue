<template>
  <n-modal
    :show="show"
    preset="card"
    class="formula-log-modal"
    title="公式执行日志"
    :bordered="false"
    @update:show="emit('update:show', $event)"
  >
    <div class="formula-log-panel">
      <section class="log-filters">
        <n-input
          v-model:value="query.traceId"
          clearable
          placeholder="Trace ID"
          @keyup.enter="loadLogs(1)"
        />
        <n-select
          v-model:value="query.success"
          :options="successOptions"
          clearable
          placeholder="执行状态"
          @update:value="loadLogs(1)"
        />
        <n-button :loading="loading" type="primary" @click="loadLogs(1)">
          查询
        </n-button>
      </section>

      <section class="log-layout">
        <div class="log-list">
          <n-empty v-if="!logs.length && !loading" size="small" description="暂无执行日志" />
          <button
            v-for="item in logs"
            :key="item.id"
            type="button"
            class="log-row"
            :class="{ active: selectedLog?.id === item.id, failed: item.success === false }"
            @click="openDetail(item)"
          >
            <div>
              <strong>{{ item.fieldCode || '未知字段' }}</strong>
              <span>{{ item.traceId || '-' }}</span>
            </div>
            <div>
              <n-tag size="small" :type="item.success === false ? 'error' : 'success'" :bordered="false">
                {{ item.success === false ? '失败' : '成功' }}
              </n-tag>
              <em>{{ formatTime(item.createTime) }}</em>
            </div>
          </button>
          <div v-if="logs.length || total" class="log-pagination">
            <span>{{ pageRangeText }}</span>
            <n-pagination
              v-model:page="pageNum"
              v-model:page-size="pageSize"
              :item-count="total"
              :page-sizes="pageSizeOptions"
              size="small"
              show-size-picker
              show-quick-jumper
              @update:page="loadLogs"
              @update:page-size="handlePageSizeChange"
            />
          </div>
        </div>

        <aside class="log-detail">
          <n-empty v-if="!selectedLog" size="small" description="选择一条日志查看详情" />
          <template v-else>
            <header>
              <div>
                <span>字段</span>
                <strong>{{ selectedLog.fieldCode || '-' }}</strong>
              </div>
              <n-tag size="small" :type="selectedLog.success === false ? 'error' : 'success'" :bordered="false">
                {{ selectedLog.success === false ? '失败' : '成功' }}
              </n-tag>
            </header>
            <dl>
              <div>
                <dt>Trace</dt>
                <dd>{{ selectedLog.traceId || '-' }}</dd>
              </div>
              <div>
                <dt>对象</dt>
                <dd>{{ selectedLog.objectCode || '-' }}</dd>
              </div>
              <div>
                <dt>记录</dt>
                <dd>{{ selectedLog.recordId || '-' }}</dd>
              </div>
              <div>
                <dt>耗时</dt>
                <dd>{{ selectedLog.elapsedMs ?? '-' }}ms</dd>
              </div>
            </dl>
            <section>
              <span>表达式</span>
              <pre>{{ selectedLog.expression || '-' }}</pre>
            </section>
            <section>
              <span>输入快照</span>
              <pre>{{ selectedLog.inputSnapshot || '-' }}</pre>
            </section>
            <section>
              <span>输出</span>
              <pre>{{ selectedLog.outputValue || '-' }}</pre>
            </section>
            <section v-if="selectedLog.errorMessage">
              <span>错误</span>
              <pre class="error">{{ selectedLog.errorMessage }}</pre>
            </section>
          </template>
        </aside>
      </section>
    </div>
  </n-modal>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { getFormulaLogDetail, getFormulaLogPage } from '@/api/formula'

const props = defineProps({
  show: {
    type: Boolean,
    default: false,
  },
  objectCode: {
    type: String,
    default: '',
  },
  fieldCode: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['update:show'])

const loading = ref(false)
const logs = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const pageSizeOptions = [10, 20, 50]
const selectedLog = ref(null)
const query = reactive({
  traceId: '',
  success: null,
})

const successOptions = [
  { label: '成功', value: true },
  { label: '失败', value: false },
]

const pageRangeText = computed(() => {
  if (!total.value)
    return '共 0 条'
  const start = (pageNum.value - 1) * pageSize.value + 1
  const end = Math.min(pageNum.value * pageSize.value, total.value)
  return `${start}-${end} / 共 ${total.value} 条`
})

watch(
  () => props.show,
  (visible) => {
    if (visible)
      loadLogs(1)
  },
)

async function loadLogs(page = pageNum.value) {
  pageNum.value = page
  loading.value = true
  try {
    const res = await getFormulaLogPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      objectCode: props.objectCode || undefined,
      fieldCode: props.fieldCode || undefined,
      traceId: query.traceId || undefined,
      success: query.success === null ? undefined : query.success,
    })
    const pageData = res?.data ?? res ?? {}
    logs.value = pageData.records || []
    total.value = Number(pageData.total || 0)
    selectedLog.value = logs.value[0] || null
  }
  finally {
    loading.value = false
  }
}

function handlePageSizeChange(size) {
  pageSize.value = size
  loadLogs(1)
}

async function openDetail(item) {
  selectedLog.value = item
  if (!item?.id)
    return
  const res = await getFormulaLogDetail(item.id)
  selectedLog.value = res?.data ?? res ?? item
}

function formatTime(value) {
  if (!value)
    return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}
</script>

<style scoped>
.formula-log-modal {
  width: min(1040px, calc(100vw - 32px));
}

.formula-log-panel {
  display: grid;
  gap: 14px;
}

.log-filters {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 160px auto;
  gap: 10px;
}

.log-layout {
  display: grid;
  grid-template-columns: minmax(320px, 0.9fr) minmax(0, 1.1fr);
  gap: 14px;
  min-height: 460px;
}

.log-list,
.log-detail {
  min-width: 0;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  background: var(--bg-primary);
  padding: 10px;
}

.log-list {
  display: grid;
  align-content: start;
  gap: 8px;
}

.log-pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: 4px;
  border-top: 1px solid var(--border-light);
  padding-top: 10px;
}

.log-pagination > span {
  color: var(--text-secondary);
  font-size: 12px;
  white-space: nowrap;
}

.log-row {
  display: grid;
  gap: 8px;
  width: 100%;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  background: var(--bg-secondary);
  cursor: pointer;
  text-align: left;
  padding: 10px;
}

.log-row.active {
  border-color: #2563eb;
  background: #eef6ff;
}

.log-row.failed {
  border-color: #fecaca;
}

.log-row > div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-width: 0;
}

.log-row strong,
.log-detail strong {
  color: var(--text-primary);
  font-size: 13px;
}

.log-row span,
.log-row em,
.log-detail span,
.log-detail dt,
.log-detail dd {
  color: var(--text-secondary);
  font-size: 12px;
  font-style: normal;
}

.log-detail {
  display: grid;
  align-content: start;
  gap: 12px;
}

.log-detail header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.log-detail dl {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin: 0;
}

.log-detail dl > div {
  min-width: 0;
  border-radius: 6px;
  background: var(--bg-secondary);
  padding: 8px;
}

.log-detail dd {
  margin: 3px 0 0;
  color: var(--text-primary);
  word-break: break-all;
}

.log-detail section {
  display: grid;
  gap: 5px;
}

.log-detail pre {
  overflow: auto;
  max-height: 150px;
  margin: 0;
  border-radius: 6px;
  background: var(--bg-secondary);
  color: var(--text-primary);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  line-height: 1.6;
  padding: 8px;
}

.log-detail pre.error {
  background: #fef2f2;
  color: #991b1b;
}

@media (max-width: 860px) {
  .log-filters,
  .log-layout {
    grid-template-columns: minmax(0, 1fr);
  }

  .log-pagination {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
