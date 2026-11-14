<script setup>
/**
 * EndNode — 结束节点
 * 命中盒仅灰圆大小，文案溢出画在下方，保证连线贴到圆点。
 * 只读查看器复用同一圆点（布局按 20px 圆点计算），运行状态只改变圆点颜色。
 */
import { computed } from 'vue'

const props = defineProps({
  node: { type: Object, required: true },
  selected: Boolean,
  status: { type: String, default: null },
  readonly: Boolean,
  width: { type: Number, default: 20 },
  height: { type: Number, default: 20 },
})

defineEmits(['click', 'delete', 'contextMenu'])

const STATUS_LABEL = { completed: '已结束', running: '进行中', pending: '未到达', rejected: '已驳回', skipped: '已跳过' }

const subtitle = computed(() => {
  return props.node?.config?.endType === 'terminate' ? '强制终止流程' : '正常结束'
})
const title = computed(() => {
  const statusLabel = STATUS_LABEL[props.status]
  return statusLabel ? `${subtitle.value}（${statusLabel}）` : subtitle.value
})
</script>

<template>
  <button
    type="button" class="flow-end-marker"
    :class="{ 'is-selected': selected, 'is-readonly': readonly, [`is-${status}`]: !!status }"
    :style="{ width: `${width}px`, height: `${height}px` }"
    :data-node-id="node.id" data-node-type="end"
    :aria-label="readonly ? `${node.name || '流程结束'}：${title}` : `配置${node.name || '流程结束'}`"
    :title="title" @click="$emit('click', node)"
  >
    <span class="flow-end-dot" />
    <span class="flow-end-label">{{ node.name || '结束' }}</span>
  </button>
</template>

<style scoped>
.flow-end-marker {
  position: relative;
  display: block;
  padding: 0;
  border: none;
  background: transparent;
  cursor: pointer;
}
.flow-end-marker.is-readonly {
  cursor: default;
}
.flow-end-dot {
  display: block;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: var(--text-tertiary, #86909c);
}
.flow-end-marker.is-completed .flow-end-dot {
  background: var(--success-color, #16a34a);
}
.flow-end-marker.is-rejected .flow-end-dot {
  background: var(--error-color, #dc2626);
}
.flow-end-label {
  position: absolute;
  top: calc(100% + 6px);
  left: 50%;
  color: var(--text-tertiary, #86909c);
  font-size: 12px;
  line-height: 1;
  white-space: nowrap;
  pointer-events: none;
  transform: translateX(-50%);
}
.flow-end-marker.is-selected .flow-end-dot {
  outline: 2px solid var(--primary-color, #356de0);
  outline-offset: 3px;
}
.flow-end-marker:focus-visible {
  outline: 2px solid var(--primary-color, #356de0);
  outline-offset: 2px;
}
</style>
