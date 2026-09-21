<template>
  <div v-if="event" class="data-audit-event-detail">
    <header class="detail-header">
      <div class="detail-identity">
        <h3 class="detail-title">
          {{ event.recordLabel || event.recordId || '未命名记录' }}
        </h3>
        <p class="detail-subtitle">
          {{ event.objectName || '业务对象' }}
        </p>
      </div>
      <div class="detail-badges">
        <DictTag :options="eventTypeOptions" :value="event.eventType" size="small" />
        <DictTag :options="sourceOptions" :value="event.sourceType" size="small" />
        <DictTag
          v-if="event.actorType"
          :options="actorOptions"
          :value="event.actorType"
          size="small"
        />
      </div>
    </header>

    <div class="detail-summary">
      <div class="summary-item">
        <span class="summary-label">操作者</span>
        <strong>{{ event.actorName || event.actorId || '系统' }}</strong>
      </div>
      <div class="summary-item">
        <span class="summary-label">发生时间</span>
        <strong>{{ event.occurredAt || '—' }}</strong>
      </div>
      <div class="summary-item">
        <span class="summary-label">修订号</span>
        <strong>第 {{ event.revision ?? '—' }} 次</strong>
      </div>
      <div class="summary-item">
        <span class="summary-label">可见字段</span>
        <strong>{{ event.visibleFieldCount ?? event.changedFieldCount ?? fields.length }} 项</strong>
      </div>
    </div>

    <div v-if="event.changeReason" class="detail-reason">
      <span class="reason-label">变更原因</span>
      <p>{{ event.changeReason }}</p>
    </div>

    <section v-if="hasContextMeta" class="detail-meta">
      <h4>关联上下文</h4>
      <dl>
        <div v-if="event.flowInstanceId">
          <dt>流程实例</dt>
          <dd><code>{{ event.flowInstanceId }}</code></dd>
        </div>
        <div v-if="event.taskId">
          <dt>任务 ID</dt>
          <dd><code>{{ event.taskId }}</code></dd>
        </div>
        <div v-if="event.operationId">
          <dt>操作 ID</dt>
          <dd><code>{{ event.operationId }}</code></dd>
        </div>
        <div v-if="event.correlationId">
          <dt>关联 ID</dt>
          <dd><code>{{ event.correlationId }}</code></dd>
        </div>
      </dl>
    </section>

    <section class="detail-diff-section">
      <div class="detail-diff-head">
        <h4>字段变化</h4>
        <span>{{ fields.length }} 项可见变化</span>
      </div>
      <DataAuditEventDiff
        :fields="fields"
        :loading="loading"
        :event-type="event.eventType"
        :expanded="expanded"
        @toggle="$emit('toggle')"
        @reveal="$emit('reveal', $event)"
      />
    </section>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables'
import DataAuditEventDiff from './DataAuditEventDiff.vue'

const props = defineProps({
  event: { type: Object, default: null },
  fields: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  expanded: { type: Boolean, default: false },
})

defineEmits(['reveal', 'toggle'])

const { dict } = useDict(
  'sys_data_audit_event_type',
  'sys_data_audit_source_type',
  'sys_data_audit_actor_type',
)

const eventTypeOptions = computed(() => dict.value.sys_data_audit_event_type || [])
const sourceOptions = computed(() => dict.value.sys_data_audit_source_type || [])
const actorOptions = computed(() => dict.value.sys_data_audit_actor_type || [])

const hasContextMeta = computed(() => Boolean(
  props.event?.flowInstanceId
  || props.event?.taskId
  || props.event?.operationId
  || props.event?.correlationId,
))
</script>

<style scoped>
.data-audit-event-detail {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-light, #e5e7eb);
}

.detail-identity {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.detail-title {
  margin: 0;
  color: var(--text-primary, #111827);
  font-size: 16px;
  font-weight: 650;
  line-height: 1.35;
  word-break: break-word;
}

.detail-subtitle {
  margin: 0;
  color: var(--text-tertiary, #64748b);
  font-size: 12px;
  line-height: 1.45;
}

.detail-meta code {
  padding: 0 4px;
  border-radius: 3px;
  background: var(--gray-100, #f1f5f9);
  color: var(--text-secondary, #475569);
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 11px;
}

.detail-badges {
  flex-shrink: 0;
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: flex-end;
  gap: 6px;
}

.detail-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.summary-item {
  min-width: 0;
  padding: 10px 12px;
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 6px;
  background: var(--gray-50, #f8fafc);
}

.summary-label {
  display: block;
  margin-bottom: 4px;
  color: var(--text-tertiary, #64748b);
  font-size: 12px;
}

.summary-item strong {
  display: block;
  overflow: hidden;
  color: var(--text-primary, #111827);
  font-size: 13px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-reason {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 10px 12px;
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 6px;
  background: var(--bg-primary, #fff);
}

.reason-label {
  color: var(--text-tertiary, #64748b);
  font-size: 12px;
}

.detail-reason p {
  margin: 0;
  color: var(--text-primary, #111827);
  font-size: 13px;
  line-height: 1.55;
  white-space: pre-wrap;
  word-break: break-word;
}

.detail-meta {
  padding: 12px 14px;
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 6px;
  background: var(--bg-primary, #fff);
}

.detail-meta h4,
.detail-diff-head h4 {
  margin: 0;
  color: var(--text-primary, #111827);
  font-size: 14px;
  font-weight: 650;
}

.detail-meta h4 {
  margin-bottom: 10px;
}

.detail-meta dl {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 14px;
  margin: 0;
}

.detail-meta dt {
  margin-bottom: 4px;
  color: var(--text-tertiary, #64748b);
  font-size: 12px;
}

.detail-meta dd {
  margin: 0;
  color: var(--text-primary, #111827);
  font-size: 13px;
  word-break: break-all;
}

.detail-diff-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.detail-diff-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.detail-diff-head span {
  color: var(--text-tertiary, #64748b);
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}

@media (max-width: 900px) {
  .detail-header {
    flex-direction: column;
  }

  .detail-badges {
    justify-content: flex-start;
  }

  .detail-summary,
  .detail-meta dl {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 560px) {
  .detail-summary,
  .detail-meta dl {
    grid-template-columns: 1fr;
  }
}
</style>
