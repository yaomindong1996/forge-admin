<template>
  <div class="data-audit-diff">
    <n-spin :show="loading">
      <n-empty v-if="!loading && !fields.length" :description="emptyText" size="small" />
      <div v-else-if="!expanded" class="diff-collapsed">
        <button type="button" class="diff-collapsed-btn" @click="$emit('toggle')">
          <span>查看具体字段</span>
          <span class="diff-collapsed-count">{{ fieldSummary }}</span>
        </button>
      </div>
      <div v-else class="diff-content">
        <section v-for="group in fieldGroups" :key="group.key" class="diff-section">
          <div class="diff-section-head">
            <span class="diff-section-label">{{ group.label }}</span>
            <span class="diff-section-count">{{ group.total }} 项</span>
          </div>

          <div class="diff-list">
            <div class="diff-list-head">
              <span>字段</span>
              <span>{{ headings.before }}</span>
              <span class="diff-arrow-col" aria-hidden="true" />
              <span>{{ headings.after }}</span>
            </div>

            <div
              v-for="field in group.fields"
              :key="field.id"
              class="diff-row"
            >
              <div class="diff-field">
                <div class="diff-field-title">
                  <strong>{{ fieldTitle(field) }}</strong>
                  <DictTag
                    v-if="field.sourceType"
                    :options="sourceOptions"
                    :value="field.sourceType"
                    size="small"
                  />
                </div>
              </div>

              <div
                class="diff-value is-before"
                :class="[`is-${resolveValue(field.before).kind}`]"
              >
                <span class="diff-value-text">{{ resolveValue(field.before).text }}</span>
              </div>

              <div class="diff-arrow" aria-hidden="true">
                <span class="diff-arrow-mark">→</span>
              </div>

              <div
                class="diff-value is-after"
                :class="[`is-${resolveValue(field.after).kind}`]"
              >
                <span class="diff-value-text">{{ resolveValue(field.after).text }}</span>
                <button
                  v-if="field.canReveal"
                  type="button"
                  class="diff-reveal"
                  @click="$emit('reveal', field)"
                >
                  查看原值
                </button>
              </div>
            </div>
          </div>
        </section>

        <div class="diff-more">
          <button type="button" class="diff-more-btn" @click="$emit('toggle')">
            收起字段
          </button>
        </div>
      </div>
    </n-spin>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import DictTag from '@/components/DictTag.vue'
import { useDict } from '@/composables'
import {
  auditDiffHeadings,
  auditFieldSummary,
  auditFieldTitle,
  DEFAULT_AUDIT_DIFF_LIMIT,
  groupAuditFields,
  resolveAuditValueView,
} from './data-audit-display'

const props = defineProps({
  fields: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  emptyText: { type: String, default: '没有可见的字段变化' },
  eventType: { type: String, default: 'UPDATE' },
  expanded: { type: Boolean, default: false },
  previewLimit: { type: Number, default: DEFAULT_AUDIT_DIFF_LIMIT },
})

defineEmits(['reveal', 'toggle'])

const { dict } = useDict('sys_data_audit_source_type')
const sourceOptions = computed(() => dict.value.sys_data_audit_source_type || [])
const headings = computed(() => auditDiffHeadings(props.eventType))
const fieldGroups = computed(() => groupAuditFields(props.fields, true, props.previewLimit))
const fieldSummary = computed(() => auditFieldSummary(props.fields, props.fields.length))

function fieldTitle(field) {
  return auditFieldTitle(field)
}

function resolveValue(view) {
  return resolveAuditValueView(view)
}
</script>

<style scoped>
.data-audit-diff {
  min-height: 0;
}

.diff-collapsed-btn {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
  padding: 8px 12px;
  border: 1px dashed var(--border-light, #e5e7eb);
  border-radius: 6px;
  background: var(--gray-50, #f8fafc);
  color: var(--primary-color, #165dff);
  font-size: 12px;
  cursor: pointer;
}

.diff-collapsed-btn:hover {
  border-color: color-mix(in srgb, var(--primary-color, #165dff) 35%, var(--border-light, #e5e7eb));
  background: color-mix(in srgb, var(--primary-color, #165dff) 5%, #fff);
}

.diff-collapsed-count {
  color: var(--text-tertiary, #64748b);
  font-variant-numeric: tabular-nums;
}

.diff-content {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.diff-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.diff-section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.diff-section-label {
  color: var(--text-primary, #111827);
  font-size: 13px;
  font-weight: 600;
}

.diff-section-count {
  color: var(--text-tertiary, #64748b);
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}

.diff-list {
  overflow: hidden;
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 6px;
  background: var(--bg-primary, #fff);
}

.diff-list-head,
.diff-row {
  display: grid;
  grid-template-columns: minmax(148px, 1.05fr) minmax(0, 1fr) 28px minmax(0, 1fr);
  gap: 8px;
  align-items: start;
  padding: 9px 12px;
}

.diff-list-head {
  background: var(--gray-100, #f6f8fb);
  color: var(--text-tertiary, #64748b);
  font-size: 12px;
  font-weight: 600;
}

.diff-row + .diff-row {
  border-top: 1px solid var(--border-light, #e5e7eb);
}

.diff-row:hover {
  background: color-mix(in srgb, var(--gray-100, #f6f8fb) 55%, transparent);
}

.diff-field {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding-top: 2px;
}

.diff-field-title {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
}

.diff-field-title strong {
  color: var(--text-primary, #111827);
  font-size: 13px;
  font-weight: 600;
  line-height: 1.35;
}

.diff-value {
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  padding: 7px 9px;
  border-radius: 4px;
  border: 1px solid transparent;
  font-size: 13px;
  line-height: 1.5;
  word-break: break-word;
  white-space: pre-wrap;
}

.diff-value.is-before {
  background: var(--gray-50, #f8fafc);
  border-color: var(--border-light, #e5e7eb);
  color: var(--text-secondary, #475569);
}

.diff-value.is-after {
  background: color-mix(in srgb, var(--primary-color, #165dff) 6%, #fff);
  border-color: color-mix(in srgb, var(--primary-color, #165dff) 18%, var(--border-light, #e5e7eb));
  color: var(--text-primary, #111827);
}

.diff-value.is-null .diff-value-text,
.diff-value.is-absent .diff-value-text,
.diff-value.is-empty .diff-value-text,
.diff-value.is-omitted .diff-value-text,
.diff-value.is-masked .diff-value-text {
  color: var(--text-tertiary, #94a3b8);
  font-style: italic;
}

.diff-arrow {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 34px;
  color: var(--text-tertiary, #94a3b8);
  font-size: 12px;
}

.diff-arrow-mark {
  line-height: 1;
}

.diff-reveal {
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--primary-color, #165dff);
  font-size: 12px;
  cursor: pointer;
}

.diff-reveal:hover {
  color: color-mix(in srgb, var(--primary-color, #165dff) 78%, #000);
}

.diff-more {
  display: flex;
  justify-content: center;
  padding-top: 2px;
}

.diff-more-btn {
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--primary-color, #165dff);
  font-size: 12px;
  cursor: pointer;
}

.diff-more-btn:hover {
  color: color-mix(in srgb, var(--primary-color, #165dff) 78%, #000);
}

@media (max-width: 720px) {
  .diff-list-head {
    display: none;
  }

  .diff-row {
    grid-template-columns: 1fr;
    gap: 8px;
  }

  .diff-arrow {
    min-height: auto;
    justify-content: flex-start;
    padding-left: 2px;
  }
}
</style>
