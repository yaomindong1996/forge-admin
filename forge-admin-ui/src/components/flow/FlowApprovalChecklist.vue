<template>
  <div
    v-if="responsibilityDescription || normalizedPoints.length || legacyApprovalPoint"
    class="flow-approval-checklist"
    data-testid="flow-approval-checklist"
  >
    <section v-if="responsibilityDescription" class="responsibility-panel">
      <div class="checklist-section-title">
        <i class="i-material-symbols:assignment-outline" />
        <span>审批职责</span>
      </div>
      <p>{{ responsibilityDescription }}</p>
    </section>

    <section v-if="normalizedPoints.length" class="approval-points-panel">
      <div class="checklist-section-head">
        <div class="checklist-section-title">
          <i class="i-material-symbols:fact-check-outline" />
          <span>审批要点</span>
        </div>
        <span v-if="!readonly" class="checklist-progress" :class="{ complete: requiredComplete }">
          必审 {{ checkedRequiredCount }}/{{ requiredCount }}
        </span>
      </div>
      <div class="approval-point-list">
        <label
          v-for="point in normalizedPoints"
          :key="point.id"
          class="approval-point-item"
          :class="{ checked: Boolean(modelValue?.[point.id]), disabled: readonly }"
        >
          <NCheckbox
            :checked="Boolean(modelValue?.[point.id])"
            :disabled="readonly"
            :aria-label="point.content"
            @update:checked="checked => updatePoint(point.id, checked)"
          />
          <span class="approval-point-content">{{ point.content }}</span>
          <span class="approval-point-type" :class="point.required ? 'required' : 'optional'">
            {{ point.required ? '必审' : '非必审' }}
          </span>
        </label>
      </div>
      <p v-if="!readonly && !requiredComplete && requiredCount" class="checklist-required-tip">
        <i class="i-material-symbols:info-outline" />
        审批通过前须完成全部必审项
      </p>
    </section>

    <section v-else-if="legacyApprovalPoint" class="legacy-approval-point">
      <div class="checklist-section-title">
        <i class="i-material-symbols:fact-check-outline" />
        <span>审批要点</span>
      </div>
      <p>{{ legacyApprovalPoint }}</p>
    </section>
  </div>
</template>

<script setup>
import { NCheckbox } from 'naive-ui'
import { computed } from 'vue'

const props = defineProps({
  responsibilityDescription: { type: String, default: '' },
  approvalPoints: { type: Array, default: () => [] },
  legacyApprovalPoint: { type: String, default: '' },
  modelValue: { type: Object, default: () => ({}) },
  readonly: { type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue'])

const normalizedPoints = computed(() => (props.approvalPoints || [])
  .filter(point => point?.id && point?.content)
  .slice()
  .sort((left, right) => Number(left.sort || 0) - Number(right.sort || 0)))
const requiredPoints = computed(() => normalizedPoints.value.filter(point => point.required === true))
const requiredCount = computed(() => requiredPoints.value.length)
const checkedRequiredCount = computed(() => requiredPoints.value
  .filter(point => Boolean(props.modelValue?.[point.id]))
  .length)
const requiredComplete = computed(() => checkedRequiredCount.value === requiredCount.value)

function updatePoint(itemId, checked) {
  if (props.readonly)
    return
  emit('update:modelValue', {
    ...(props.modelValue || {}),
    [itemId]: Boolean(checked),
  })
}

defineExpose({
  requiredComplete,
  requiredCount,
})
</script>

<style scoped>
.flow-approval-checklist {
  display: grid;
  gap: 12px;
  margin-bottom: 16px;
}

.responsibility-panel,
.approval-points-panel,
.legacy-approval-point {
  min-width: 0;
  padding: 13px 14px;
  border: 1px solid #dde5ee;
  border-radius: 6px;
  background: var(--bg-secondary);
}

.responsibility-panel {
  border-left: 3px solid #2563eb;
}

.approval-points-panel,
.legacy-approval-point {
  border-left: 3px solid #c47a0a;
  background: #fffaf0;
}

.checklist-section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.checklist-section-title {
  display: flex;
  align-items: center;
  gap: 7px;
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 700;
}

.checklist-section-title i {
  color: #2563eb;
  font-size: 17px;
}

.approval-points-panel .checklist-section-title i,
.legacy-approval-point .checklist-section-title i {
  color: #a15c00;
}

.responsibility-panel p,
.legacy-approval-point p {
  margin: 7px 0 0;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
}

.checklist-progress {
  padding: 2px 7px;
  border-radius: 4px;
  background: #fceaea;
  color: #b42318;
  font-size: 11px;
  font-weight: 650;
  white-space: nowrap;
}

.checklist-progress.complete {
  background: #e8f7ee;
  color: #18794e;
}

.approval-point-list {
  display: grid;
  gap: 8px;
  margin-top: 11px;
}

.approval-point-item {
  display: grid;
  grid-template-columns: 22px minmax(0, 1fr) auto;
  align-items: start;
  gap: 8px;
  min-width: 0;
  padding: 9px 10px;
  border: 1px solid #eadfc9;
  border-radius: 5px;
  background: var(--bg-primary);
  cursor: pointer;
}

.approval-point-item:hover:not(.disabled) {
  border-color: #d6a24b;
}

.approval-point-item.checked {
  border-color: #a9d4bb;
  background: #f4fbf7;
}

.approval-point-item.disabled {
  cursor: default;
}

.approval-point-content {
  min-width: 0;
  color: var(--text-primary);
  font-size: 13px;
  line-height: 1.55;
  overflow-wrap: anywhere;
}

.approval-point-type {
  padding: 1px 6px;
  border-radius: 4px;
  font-size: 11px;
  white-space: nowrap;
}

.approval-point-type.required {
  background: #fceaea;
  color: #b42318;
}

.approval-point-type.optional {
  background: #eef2f6;
  color: #5f6b7a;
}

.checklist-required-tip {
  display: flex;
  align-items: center;
  gap: 5px;
  margin: 10px 0 0;
  color: #9a5700;
  font-size: 12px;
}

@media (max-width: 640px) {
  .checklist-section-head {
    align-items: flex-start;
  }

  .approval-point-item {
    grid-template-columns: 22px minmax(0, 1fr);
  }

  .approval-point-type {
    grid-column: 2;
    width: fit-content;
  }
}
</style>
