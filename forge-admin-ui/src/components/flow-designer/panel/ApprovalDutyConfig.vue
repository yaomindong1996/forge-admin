<script setup>
/**
 * 节点审批职责与审批要点。
 * 写入 BPMN flowable:responsibilityDescription / flowable:approvalPoints。
 */
import { computed } from 'vue'

const props = defineProps({
  config: { type: Object, required: true },
  readonly: Boolean,
})

const emit = defineEmits(['update:config'])

const responsibilityDescription = computed({
  get: () => props.config?.responsibilityDescription || '',
  set: value => emit('update:config', { responsibilityDescription: value }),
})

const approvalPoints = computed(() => normalizeApprovalPoints(props.config?.approvalPoints))

function addApprovalPoint() {
  if (props.readonly)
    return
  const next = [...approvalPoints.value, {
    id: createPointId(),
    content: '',
    required: true,
    sort: approvalPoints.value.length + 1,
  }]
  emit('update:config', { approvalPoints: reindex(next) })
}

function updatePoint(index, patch) {
  const next = approvalPoints.value.map((point, i) => (i === index ? { ...point, ...patch } : point))
  emit('update:config', { approvalPoints: reindex(next) })
}

function removeApprovalPoint(index) {
  emit('update:config', {
    approvalPoints: reindex(approvalPoints.value.filter((_, i) => i !== index)),
  })
}

function moveApprovalPoint(index, offset) {
  const target = index + offset
  if (target < 0 || target >= approvalPoints.value.length)
    return
  const next = [...approvalPoints.value]
  const [point] = next.splice(index, 1)
  next.splice(target, 0, point)
  emit('update:config', { approvalPoints: reindex(next) })
}

function normalizeApprovalPoints(source = []) {
  return (Array.isArray(source) ? source : [])
    .map((item, index) => ({
      id: String(item?.id || createPointId()),
      content: String(item?.content || '').trim(),
      required: item?.required === true,
      sort: Number.isFinite(Number(item?.sort)) ? Number(item.sort) : index + 1,
    }))
}

function reindex(points = []) {
  return points.map((point, index) => ({
    ...point,
    sort: index + 1,
  }))
}

function createPointId() {
  return `point-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 8)}`
}
</script>

<template>
  <div class="approval-duty-config">
    <n-form-item label="审批职责" label-placement="top" :show-feedback="false">
      <n-input
        v-model:value="responsibilityDescription"
        type="textarea"
        :autosize="{ minRows: 2, maxRows: 5 }"
        maxlength="500"
        show-count
        :disabled="readonly"
        placeholder="说明本节点审批人的办理职责和注意事项"
      />
    </n-form-item>

    <n-form-item label="审批要点" label-placement="top" :show-feedback="false">
      <div class="approval-point-editor">
        <div v-if="approvalPoints.length" class="approval-point-list">
          <div
            v-for="(point, index) in approvalPoints"
            :key="point.id"
            class="approval-point-row"
          >
            <span class="approval-point-order">{{ index + 1 }}</span>
            <n-input
              :value="point.content"
              maxlength="200"
              placeholder="请输入需要核查的内容"
              :disabled="readonly"
              @update:value="value => updatePoint(index, { content: value })"
            />
            <n-checkbox
              :checked="point.required"
              :disabled="readonly"
              @update:checked="value => updatePoint(index, { required: value })"
            >
              必审
            </n-checkbox>
            <div class="approval-point-actions">
              <n-button
                quaternary
                circle
                size="tiny"
                :disabled="readonly || index === 0"
                aria-label="上移审批要点"
                @click="moveApprovalPoint(index, -1)"
              >
                <template #icon>
                  <i class="i-material-symbols:arrow-upward" />
                </template>
              </n-button>
              <n-button
                quaternary
                circle
                size="tiny"
                :disabled="readonly || index === approvalPoints.length - 1"
                aria-label="下移审批要点"
                @click="moveApprovalPoint(index, 1)"
              >
                <template #icon>
                  <i class="i-material-symbols:arrow-downward" />
                </template>
              </n-button>
              <n-button
                quaternary
                circle
                size="tiny"
                type="error"
                :disabled="readonly"
                aria-label="删除审批要点"
                @click="removeApprovalPoint(index)"
              >
                <template #icon>
                  <i class="i-material-symbols:delete-outline" />
                </template>
              </n-button>
            </div>
          </div>
        </div>
        <n-button dashed block :disabled="readonly" @click="addApprovalPoint">
          <template #icon>
            <i class="i-material-symbols:add" />
          </template>
          添加审批要点
        </n-button>
        <div class="config-hint">
          必审项在审批通过前必须勾选；非必审项只做核查提示。
        </div>
      </div>
    </n-form-item>
  </div>
</template>

<style scoped>
.approval-duty-config {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.approval-point-editor {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
}

.approval-point-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.approval-point-row {
  display: grid;
  grid-template-columns: 22px minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.approval-point-order {
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 600;
  text-align: center;
}

.approval-point-actions {
  display: inline-flex;
  align-items: center;
}

@media (max-width: 560px) {
  .approval-point-row {
    grid-template-columns: 22px minmax(0, 1fr);
  }

  .approval-point-row :deep(.n-checkbox),
  .approval-point-actions {
    grid-column: 2;
  }
}
</style>
