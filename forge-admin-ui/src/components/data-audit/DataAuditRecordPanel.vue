<template>
  <div class="data-audit-record-panel">
    <n-alert v-if="!available" type="info" :bordered="false" class="panel-alert">
      当前记录尚无可查看的变更历史。
    </n-alert>
    <template v-else>
      <n-alert
        v-if="!enabled && historyAvailable"
        type="warning"
        :bordered="false"
        class="panel-alert"
      >
        当前对象已停用新的变更采集，下面仍保留停用前的历史记录。
      </n-alert>

      <div class="panel-toolbar">
        <div class="toolbar-filters">
          <n-select
            v-model:value="selectedFieldCode"
            clearable
            filterable
            size="small"
            :loading="fieldOptionsLoading"
            :options="fieldOptions"
            placeholder="筛选变更字段"
            class="toolbar-field"
          />
          <n-date-picker
            v-model:value="timeRange"
            type="datetimerange"
            clearable
            size="small"
            class="toolbar-time"
          />
        </div>
        <n-button size="small" type="primary" @click="reload">
          查询
        </n-button>
      </div>

      <n-spin :show="store.loading">
        <n-empty
          v-if="!store.loading && !store.events.length"
          :description="emptyDescription"
          size="small"
          class="panel-empty"
        />

        <div v-else class="audit-feed">
          <article
            v-for="event in store.events"
            :key="event.id"
            class="audit-event"
            :class="{ 'is-expanded': isExpanded(event.id) }"
          >
            <div class="event-time-col">
              <span class="event-date">{{ splitOccurredAt(event.occurredAt).date }}</span>
              <span class="event-clock">{{ splitOccurredAt(event.occurredAt).time }}</span>
            </div>

            <div class="event-body">
              <header class="event-header">
                <div class="event-actor">
                  <span class="actor-avatar" aria-hidden="true">
                    {{ actorInitial(event.actorName || event.actorId) }}
                  </span>
                  <div class="actor-copy">
                    <div class="actor-line">
                      <strong>{{ event.actorName || event.actorId || '系统操作' }}</strong>
                      <DictTag :options="eventTypeOptions" :value="event.eventType" size="small" />
                      <DictTag :options="sourceOptions" :value="event.sourceType" size="small" />
                    </div>
                    <div class="event-meta">
                      <span>第 {{ event.revision ?? '—' }} 次记录</span>
                      <span class="meta-sep">·</span>
                      <span>{{ eventChangeSummary(event) }}</span>
                    </div>
                  </div>
                </div>
              </header>

              <p v-if="event.changeReason" class="event-reason">
                <span>变更原因</span>
                {{ event.changeReason }}
              </p>

              <div class="event-diff-wrap">
                <DataAuditEventDiff
                  :fields="previewFields(event)"
                  :loading="isFieldLoading(event.id)"
                  :empty-text="fieldEmptyText(event.id)"
                  :event-type="event.eventType"
                  :expanded="isExpanded(event.id)"
                  :preview-limit="compactPreviewLimit"
                  @toggle="toggleEvent(event)"
                  @reveal="field => handleReveal(event, field)"
                />
              </div>
            </div>
          </article>
        </div>

        <div v-if="store.events.length" class="panel-pager">
          <span>共 {{ store.total }} 次变更</span>
          <n-pagination
            v-if="store.total > 10"
            :page="store.pageNum"
            :page-size="eventPageSize"
            :item-count="store.total"
            :page-sizes="[10, 20, 50]"
            show-size-picker
            size="small"
            @update:page="page => load(page)"
            @update:page-size="handlePageSizeChange"
          />
        </div>
      </n-spin>
    </template>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { dataAuditObjectFieldOptions } from '@/api/data-audit'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables'
import { useDataAuditStore } from '@/stores/data-audit/dataAuditStore'
import {
  actorInitial,
  auditFieldSummary,
  DEFAULT_AUDIT_DIFF_LIMIT,
} from './data-audit-display'
import { promptAuditReason } from './data-audit-submit'
import DataAuditEventDiff from './DataAuditEventDiff.vue'

const props = defineProps({
  objectId: { type: [String, Number], default: '' },
  recordId: { type: [String, Number], default: '' },
  enabled: { type: Boolean, default: false },
  historyAvailable: { type: Boolean, default: false },
  accessMode: { type: String, default: 'RECORD' },
  taskId: { type: String, default: '' },
})

const store = useDataAuditStore()
const { dict } = useDict('sys_data_audit_event_type', 'sys_data_audit_source_type')
const eventTypeOptions = computed(() => dict.value.sys_data_audit_event_type || [])
const sourceOptions = computed(() => dict.value.sys_data_audit_source_type || [])
const selectedFieldCode = ref(null)
const fieldOptions = ref([])
const fieldOptionsLoading = ref(false)
const timeRange = ref(null)
const fieldLoadingIds = ref([])
const fieldLoadErrorIds = ref([])
const expandedEventIds = ref([])
const eventPageSize = ref(10)
const compactPreviewLimit = DEFAULT_AUDIT_DIFF_LIMIT

const available = computed(() => props.enabled || props.historyAvailable)
const emptyDescription = computed(() => {
  if (store.emptyReason === 'error')
    return store.error || '加载失败'
  if (selectedFieldCode.value || timeRange.value)
    return '没有符合筛选条件的变更记录'
  return '已启用审计，但暂无变更记录'
})

async function load(page = 1) {
  if (!available.value || !props.objectId || !props.recordId)
    return
  const filters = {
    fieldCode: selectedFieldCode.value || undefined,
    taskId: props.taskId || undefined,
  }
  if (Array.isArray(timeRange.value) && timeRange.value.length === 2) {
    filters.startTime = formatTime(timeRange.value[0])
    filters.endTime = formatTime(timeRange.value[1])
  }
  expandedEventIds.value = []
  fieldLoadErrorIds.value = []
  store.fieldsByEvent = {}
  await store.loadRecordEvents({
    objectId: props.objectId,
    recordId: String(props.recordId),
    filters,
    page,
    size: eventPageSize.value,
    accessMode: props.accessMode,
  })
  await loadVisibleEventFields()
}

function reload() {
  load(1)
}

function handlePageSizeChange(size) {
  eventPageSize.value = size
  load(1)
}

function formatTime(value) {
  const date = value instanceof Date ? value : new Date(value)
  const pad = n => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

function splitOccurredAt(value) {
  const text = String(value || '').trim()
  if (!text)
    return { date: '—', time: '' }
  const [date, time = ''] = text.split(/\s+/)
  return { date, time }
}

function isExpanded(eventId) {
  return expandedEventIds.value.includes(String(eventId))
}

function isFieldLoading(eventId) {
  return fieldLoadingIds.value.includes(String(eventId))
}

function eventChangeSummary(event) {
  const fields = store.fieldsByEvent[String(event.id)] || []
  return auditFieldSummary(fields, event.visibleFieldCount ?? event.changedFieldCount)
}

function previewFields(event) {
  return store.fieldsByEvent[String(event.id)] || []
}

function fieldEmptyText(eventId) {
  if (fieldLoadErrorIds.value.includes(String(eventId)))
    return '字段变化加载失败，请重新查询'
  return '本次变更没有可见字段'
}

function toggleEvent(event) {
  const eventId = String(event.id)
  if (isExpanded(eventId)) {
    expandedEventIds.value = expandedEventIds.value.filter(id => id !== eventId)
    return
  }
  expandedEventIds.value = [...expandedEventIds.value, eventId]
}

async function loadVisibleEventFields() {
  const eventIds = store.events.map(event => String(event.id)).filter(Boolean)
  fieldLoadingIds.value = eventIds
  fieldLoadErrorIds.value = []
  try {
    const results = await Promise.allSettled(eventIds.map(eventId => store.loadEventFields(eventId, {
      accessMode: props.accessMode,
      fieldCode: selectedFieldCode.value || undefined,
    })))
    fieldLoadErrorIds.value = results
      .map((result, index) => result.status === 'rejected' ? eventIds[index] : '')
      .filter(Boolean)
  }
  finally {
    fieldLoadingIds.value = []
  }
}

async function loadFieldOptions() {
  selectedFieldCode.value = null
  fieldOptions.value = []
  if (!props.objectId)
    return
  fieldOptionsLoading.value = true
  try {
    const response = await dataAuditObjectFieldOptions(props.objectId)
    fieldOptions.value = (response.code === 200 ? (response.data || []) : []).map(field => ({
      label: field.fieldLabel || '未命名字段',
      value: String(field.fieldCode),
    }))
  }
  catch (error) {
    window.$message?.error?.(error?.message || '加载页面字段失败')
  }
  finally {
    fieldOptionsLoading.value = false
  }
}

async function handleReveal(event, field) {
  const reason = await promptAuditReason('请填写查看原值原因')
  if (reason === false)
    return
  try {
    const data = await store.revealField(event.id, field.id, reason, props.accessMode)
    const list = store.fieldsByEvent[String(event.id)] || []
    store.fieldsByEvent = {
      ...store.fieldsByEvent,
      [String(event.id)]: list.map((item) => {
        if (String(item.id) !== String(field.id))
          return item
        return { ...item, before: data.before, after: data.after, masked: false, canReveal: false }
      }),
    }
  }
  catch (error) {
    window.$message?.error(error?.message || '查看原值失败')
  }
}

watch(() => props.objectId, loadFieldOptions, { immediate: true })

watch(
  () => [props.objectId, props.recordId, props.enabled, props.historyAvailable],
  () => {
    store.clear()
    if (available.value)
      load(1)
  },
  { immediate: true },
)
</script>

<style scoped>
.data-audit-record-panel {
  min-height: 240px;
  padding-top: 4px;
}

.panel-alert {
  margin-bottom: 10px;
}

.panel-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 12px;
  padding: 10px 12px;
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 6px;
  background: var(--gray-50, #f8fafc);
}

.toolbar-filters {
  display: flex;
  flex: 1;
  flex-wrap: wrap;
  gap: 8px;
  min-width: 0;
}

.toolbar-field {
  width: 200px;
  max-width: 100%;
}

.toolbar-time {
  width: min(320px, 100%);
}

.panel-empty {
  padding: 28px 0;
}

.audit-feed {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.audit-event {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr);
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 6px;
  background: var(--bg-primary, #fff);
  transition:
    border-color 140ms ease,
    background-color 140ms ease;
}

.audit-event.is-expanded {
  border-color: color-mix(in srgb, var(--primary-color, #165dff) 28%, var(--border-light, #e5e7eb));
  background: color-mix(in srgb, var(--primary-color, #165dff) 3%, #fff);
}

.event-time-col {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding-top: 2px;
}

.event-date {
  color: var(--text-primary, #111827);
  font-size: 12px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.event-clock {
  color: var(--text-tertiary, #64748b);
  font-size: 11px;
  font-variant-numeric: tabular-nums;
}

.event-body {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.event-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.event-actor {
  display: flex;
  min-width: 0;
  gap: 10px;
}

.actor-avatar {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 4px;
  background: var(--gray-100, #f1f5f9);
  color: var(--text-secondary, #475569);
  font-size: 12px;
  font-weight: 650;
}

.actor-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.actor-line {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px 8px;
}

.actor-line strong {
  color: var(--text-primary, #111827);
  font-size: 13px;
  font-weight: 600;
}

.event-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px 6px;
  color: var(--text-tertiary, #64748b);
  font-size: 12px;
}

.meta-sep {
  opacity: 0.55;
}

.event-reason {
  margin: 0;
  padding: 8px 10px;
  border-radius: 4px;
  background: var(--gray-50, #f8fafc);
  border: 1px solid var(--border-light, #e5e7eb);
  color: var(--text-secondary, #475569);
  font-size: 12px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

.event-reason span {
  margin-right: 6px;
  color: var(--text-tertiary, #64748b);
}

.event-diff-wrap {
  min-width: 0;
}

.panel-pager {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 12px;
  color: var(--text-tertiary, #64748b);
  font-size: 12px;
}

@media (max-width: 720px) {
  .audit-event {
    grid-template-columns: 1fr;
    gap: 8px;
  }

  .event-time-col {
    flex-direction: row;
    align-items: baseline;
    gap: 8px;
  }

  .toolbar-field,
  .toolbar-time {
    width: 100%;
  }
}
</style>
