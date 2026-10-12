<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'

const props = defineProps({
  edgeId: { type: String, required: true },
  position: { type: Object, required: true },
  items: { type: Array, default: () => [] },
  readonly: { type: Boolean, default: false },
  dragging: { type: Boolean, default: false },
  active: { type: Boolean, default: false },
})

const emit = defineEmits(['select', 'dragTarget', 'dropNode'])
const rootRef = ref(null)
const popoverVisible = ref(false)

function toggle() {
  if (!props.readonly)
    popoverVisible.value = !popoverVisible.value
}

function handleSelect(type) {
  popoverVisible.value = false
  emit('select', { edgeId: props.edgeId, type })
}

function handleDragOver(event) {
  if (props.readonly)
    return
  event.preventDefault()
  event.stopPropagation()
  if (event.dataTransfer)
    event.dataTransfer.dropEffect = 'copy'
  emit('dragTarget', props.edgeId)
}

function handleDrop(event) {
  if (props.readonly)
    return
  event.preventDefault()
  event.stopPropagation()
  emit('dropNode', { edgeId: props.edgeId, event })
}

function handleClickOutside(event) {
  if (!rootRef.value?.contains(event.target))
    popoverVisible.value = false
}

onMounted(() => window.addEventListener('mousedown', handleClickOutside, true))
onBeforeUnmount(() => window.removeEventListener('mousedown', handleClickOutside, true))
</script>

<template>
  <div
    ref="rootRef"
    class="business-process-add-wrap absolute z-20"
    :class="{
      'is-dragging': dragging,
      'is-active-target': active,
    }"
    :style="{
      left: `${position.x}px`,
      top: `${position.y}px`,
      transform: 'translate(-50%, -50%)',
    }"
    data-business-insert-edge
    :data-edge-id="edgeId"
    @dragover="handleDragOver"
    @drop="handleDrop"
  >
    <button
      type="button"
      class="business-process-add-button"
      :disabled="readonly"
      :aria-expanded="popoverVisible"
      title="在此处添加节点"
      @click.stop="toggle"
    >
      <span aria-hidden="true">+</span>
    </button>

    <div v-if="popoverVisible" class="business-process-add-popover">
      <strong>添加节点</strong>
      <div class="business-process-add-grid">
        <button
          v-for="item in items"
          :key="item.type"
          type="button"
          class="business-process-add-option"
          :data-business-insert-type="item.type"
          @click.stop="handleSelect(item.type)"
        >
          <span :class="`tone-${item.tone}`" aria-hidden="true" />
          <span>
            <b>{{ item.label }}</b>
            <small>{{ item.type === 'APPROVAL' ? '等待审批结果' : '业务编排节点' }}</small>
          </span>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.business-process-add-wrap {
  width: 32px;
  height: 32px;
}

.business-process-add-button {
  display: flex;
  width: 28px;
  height: 28px;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(100, 116, 139, 0.32);
  border-radius: 999px;
  background: var(--card-color, #fff);
  color: var(--text-secondary);
  font-size: 19px;
  line-height: 1;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.08);
  transition:
    border-color 150ms ease,
    background-color 150ms ease,
    color 150ms ease,
    box-shadow 150ms ease,
    transform 150ms ease;
}

.business-process-add-button:hover:not(:disabled),
.business-process-add-button[aria-expanded='true'] {
  border-color: var(--primary-color, #2563eb);
  color: var(--primary-color, #2563eb);
  box-shadow: 0 7px 18px rgba(37, 99, 235, 0.16);
  transform: scale(1.06);
}

.is-dragging .business-process-add-button {
  border-style: dashed;
  border-color: rgba(37, 99, 235, 0.55);
  background: rgba(239, 246, 255, 0.96);
  color: var(--primary-color, #2563eb);
}

.is-active-target .business-process-add-button {
  border-style: solid;
  border-color: var(--primary-color, #2563eb);
  background: var(--primary-color, #2563eb);
  color: #fff;
  box-shadow:
    0 0 0 7px rgba(37, 99, 235, 0.12),
    0 10px 24px rgba(37, 99, 235, 0.22);
  transform: scale(1.12);
}

.business-process-add-popover {
  position: absolute;
  top: calc(100% + 8px);
  left: 50%;
  z-index: 40;
  width: 270px;
  border: 1px solid rgba(148, 163, 184, 0.3);
  border-radius: 8px;
  background: var(--card-color, #fff);
  padding: 10px;
  box-shadow: 0 18px 44px rgba(15, 23, 42, 0.16);
  transform: translateX(-50%);
}

.business-process-add-popover > strong {
  display: block;
  margin: 1px 2px 9px;
  color: var(--text-color-2, #475569);
  font-size: 12px;
}

.business-process-add-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 7px;
}

.business-process-add-option {
  display: grid;
  min-width: 0;
  grid-template-columns: 4px minmax(0, 1fr);
  gap: 8px;
  align-items: center;
  border: 1px solid rgba(148, 163, 184, 0.24);
  border-radius: 6px;
  padding: 8px;
  text-align: left;
}

.business-process-add-option:hover {
  border-color: rgba(37, 99, 235, 0.38);
  background: rgba(37, 99, 235, 0.04);
}

.business-process-add-option > span:first-child {
  width: 4px;
  height: 30px;
  border-radius: 3px;
  background: #64748b;
}

.business-process-add-option .tone-condition {
  background: #c17a16;
}

.business-process-add-option .tone-action,
.business-process-add-option .tone-sub-process {
  background: #2563eb;
}

.business-process-add-option .tone-approval {
  background: #7c3aed;
}

.business-process-add-option b,
.business-process-add-option small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.business-process-add-option b {
  color: var(--text-color-1, #0f172a);
  font-size: 12px;
}

.business-process-add-option small {
  margin-top: 2px;
  color: var(--text-color-3, #64748b);
  font-size: 10px;
}

@media (prefers-reduced-motion: reduce) {
  .business-process-add-button {
    transition: none;
  }
}
</style>
