<script setup>
/** 白底双行节点。配置入口与更多操作分离，避免嵌套按钮及仅悬停才可用的操作。 */
import { computed } from 'vue'
import { getNodePresentation } from '../constants/node-presentation.js'

const props = defineProps({
  node: { type: Object, required: true },
  selected: Boolean,
  status: { type: String, default: null },
  readonly: Boolean,
  icon: { type: String, default: '' },
  colorVar: { type: String, default: 'primary' },
  subtitle: { type: String, default: '' },
  showActions: { type: Boolean, default: true },
  deletable: { type: Boolean, default: true },
  width: { type: Number, default: 224 },
  height: { type: Number, default: 82 },
})
const emit = defineEmits(['click', 'delete', 'contextMenu'])
const presentation = computed(() => getNodePresentation(props.node?.nodeType))
const cardStyle = computed(() => ({
  'width': `${props.width}px`,
  'minHeight': `${props.height}px`,
  '--flow-node-color': presentation.value.color,
}))
// 查看器保留运行状态语义，设计态的节点类型颜色与运行状态互不替代。
const STATUS_BADGE = {
  completed: { label: '已完成', class: 'is-completed' },
  running: { label: '审批中', class: 'is-running' },
  pending: { label: '待办', class: 'is-pending' },
  rejected: { label: '已驳回', class: 'is-rejected' },
  skipped: { label: '已跳过', class: 'is-pending' },
}
const statusBadge = computed(() => STATUS_BADGE[props.status] || null)
function handleContextMenu(event) {
  event.preventDefault()
  const rect = event.currentTarget?.getBoundingClientRect()
  const point = event.clientX || event.clientY
    ? event
    : { clientX: rect?.left || 0, clientY: rect?.bottom || 0 }
  emit('contextMenu', { event: point, node: props.node })
}
</script>

<template>
  <div
    class="flow-node-card"
    :class="{ 'is-selected': selected, 'is-readonly': readonly }"
    :style="cardStyle"
    :data-node-id="node?.id"
    :data-node-type="node?.nodeType"
    @click="$emit('click', node)"
    @contextmenu="handleContextMenu"
  >
    <!-- 节点主信息 -->
    <button type="button" class="flow-node-open" :aria-label="`配置${node?.name || '节点'}`">
      <span class="flow-node-icon" aria-hidden="true">
        <i :class="presentation.icon || icon" />
      </span>
      <span class="flow-node-main">
        <span class="flow-node-heading">
          <span class="flow-node-name" :title="node?.name">{{ node?.name || '未命名节点' }}</span>
          <slot v-if="!readonly" name="title-extra" />
        </span>
        <span v-if="statusBadge" class="flow-node-status flow-node-meta" :class="statusBadge.class">
          {{ statusBadge.label }}
        </span>
        <span v-else-if="subtitle && !readonly" class="flow-node-summary" :title="subtitle">{{ subtitle }}</span>
        <span v-if="$slots.default && !readonly" class="flow-node-extra"><slot /></span>
      </span>
    </button>
    <!-- 复用已有复制、排序、删除菜单，不更改节点业务操作 -->
    <button
      v-if="showActions && deletable && !readonly"
      type="button" class="flow-node-more" aria-label="节点更多操作" title="节点更多操作"
      @click.stop="handleContextMenu"
    >
      <i class="i-lucide:ellipsis" />
    </button>
    <i class="flow-node-chevron i-lucide:chevron-right" aria-hidden="true" />
  </div>
</template>

<style scoped>
.flow-node-card {
  display: flex;
  align-items: center;
  gap: 4px;
  box-sizing: border-box;
  padding: 12px;
  border: 1px solid var(--border-light, #dfe4ec);
  border-radius: 6px;
  background: var(--bg-primary, #fff);
  color: var(--text-primary, #1d2129);
  cursor: pointer;
  transition:
    border-color 160ms,
    background-color 160ms;
}
.flow-node-card:hover {
  border-color: color-mix(in srgb, var(--primary-color, #356de0) 50%, var(--border-light, #dfe4ec));
}
.flow-node-card.is-selected {
  border-color: var(--primary-color, #356de0);
  outline: 1px solid var(--primary-color, #356de0);
}
.flow-node-open {
  display: flex;
  flex: 1;
  min-width: 0;
  align-items: center;
  gap: 12px;
  padding: 0;
  border: none;
  background: transparent;
  color: inherit;
  text-align: left;
  cursor: inherit;
}
.flow-node-icon {
  display: inline-flex;
  flex: none;
  width: 38px;
  height: 38px;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  background: color-mix(in srgb, var(--flow-node-color) 14%, var(--bg-primary, #fff));
  color: var(--flow-node-color);
  font-size: 22px;
}
.flow-node-main {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-direction: column;
  gap: 5px;
}
.flow-node-heading {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 6px;
}
.flow-node-name {
  overflow: hidden;
  font-size: 14px;
  font-weight: 500;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.flow-node-summary {
  overflow: hidden;
  color: var(--text-tertiary, #86909c);
  font-size: 12px;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.flow-node-extra {
  color: var(--text-secondary, #4e5969);
  font-size: 12px;
}
.flow-node-more {
  display: inline-flex;
  flex: none;
  width: 26px;
  height: 28px;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 4px;
  background: transparent;
  color: var(--text-tertiary, #86909c);
  cursor: pointer;
}
.flow-node-more:hover {
  background: var(--bg-secondary, #f6f8fb);
  color: var(--text-primary, #1d2129);
}
.flow-node-chevron {
  flex: none;
  color: var(--text-tertiary, #86909c);
  font-size: 16px;
}
.flow-node-open:focus-visible,
.flow-node-more:focus-visible {
  outline: 2px solid var(--primary-color, #356de0);
  outline-offset: 3px;
}
.flow-node-status {
  width: fit-content;
  padding: 2px 5px;
  border-radius: 3px;
  font-size: 11px;
}
.is-completed {
  background: #e8f5ee;
  color: #357a56;
}
.is-running {
  background: #e8f0fd;
  color: #356de0;
}
.is-pending {
  background: var(--bg-secondary, #f6f8fb);
  color: var(--text-tertiary, #86909c);
}
.is-rejected {
  background: #fceeed;
  color: #c44b4b;
}
@media (prefers-reduced-motion: reduce) {
  .flow-node-card {
    transition: none;
  }
}
</style>
