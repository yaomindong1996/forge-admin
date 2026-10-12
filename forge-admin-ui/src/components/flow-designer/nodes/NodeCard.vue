<script setup>
/**
 * NodeCard — 节点卡片基类
 *
 * 统一布局：标题行图标 + 节点名称，下面展示摘要面板。
 * 视觉参考钉钉审批流卡片：轻阴影、浅边框、摘要灰底、选中态青绿色描边。
 *
 * Props:
 *   - node / selected / status / readonly
 *   - icon        顶部图标（i-mdi-xxx）
 *   - colorVar    配色变量：primary / success / warning / info / error / gray
 *   - subtitle    副标题（节点说明，浅灰小字）
 *   - width / height
 *   - showActions 是否显示删除按钮
 *   - deletable   是否可删除（start/end 不可）
 */
import { computed } from 'vue'

const props = defineProps({
  node: { type: Object, required: true },
  selected: { type: Boolean, default: false },
  status: { type: String, default: null },
  readonly: { type: Boolean, default: false },
  icon: { type: String, default: 'i-mdi-checkbox-blank-circle' },
  colorVar: { type: String, default: 'primary' },
  subtitle: { type: String, default: '' },
  showActions: { type: Boolean, default: true },
  deletable: { type: Boolean, default: true },
  width: { type: Number, default: 224 },
  height: { type: Number, default: 82 },
})

const emit = defineEmits(['click', 'delete', 'contextMenu'])

const COLOR_META = {
  primary: { color: '#2563eb', soft: '#dbeafe', content: '#2563eb' },
  success: { color: 'var(--text-secondary)', soft: '#e2e8f0', content: 'var(--text-secondary)' },
  warning: { color: '#f59e0b', soft: '#fef3c7', content: '#d97706' },
  info: { color: '#4f46e5', soft: '#e0e7ff', content: '#4f46e5' },
  error: { color: '#dc2626', soft: '#fee2e2', content: '#dc2626' },
  gray: { color: 'var(--text-secondary)', soft: '#e2e8f0', content: 'var(--text-secondary)' },
  teal: { color: '#0d9488', soft: '#ccfbf1', content: '#0f766e' },
}

const STATUS_BADGE = {
  completed: { label: '已完成', class: 'bg-green-100 text-green-700' },
  running: { label: '审批中', class: 'bg-blue-100 text-blue-700' },
  pending: { label: '待办', class: 'bg-gray-100 text-gray-500' },
  rejected: { label: '已驳回', class: 'bg-red-100 text-red-700' },
  skipped: { label: '已跳过', class: 'bg-gray-100 text-gray-400' },
}

const colorMeta = computed(() => COLOR_META[props.colorVar] || COLOR_META.primary)
const cardStyle = computed(() => ({
  'width': `${props.width}px`,
  'minHeight': `${props.height}px`,
  '--flow-node-color': colorMeta.value.color,
  '--flow-node-soft': colorMeta.value.soft,
  '--flow-node-content-color': colorMeta.value.content,
}))
const statusBadge = computed(() => STATUS_BADGE[props.status] || null)
const canDelete = computed(() => props.deletable && props.showActions && !props.readonly)

function handleClick() {
  emit('click', props.node)
}

function handleDelete(e) {
  e.stopPropagation()
  if (canDelete.value)
    emit('delete', props.node)
}

function handleContextMenu(e) {
  e.preventDefault()
  emit('contextMenu', { event: e, node: props.node })
}
</script>

<template>
  <div
    class="flow-node-card flex flex-col cursor-pointer bg-white transition-all duration-200"
    :class="[
      selected ? 'is-selected' : '',
      readonly ? 'is-readonly' : '',
    ]"
    :style="cardStyle"
    :data-node-id="node?.id"
    :data-node-type="node?.nodeType"
    @click="handleClick"
    @contextmenu="handleContextMenu"
  >
    <div class="flow-node-title w-full flex items-center gap-2">
      <span
        class="flow-node-icon flex shrink-0 items-center justify-center"
      >
        <i :class="icon" />
      </span>
      <div class="flow-node-main min-w-0 flex flex-col flex-1 gap-1">
        <div class="flow-node-heading min-w-0 flex items-center gap-1.5">
          <span class="flow-node-name text-xs truncate text-slate-700 font-semibold">
            {{ node?.name || '未命名节点' }}
          </span>
          <slot v-if="!readonly" name="title-extra" />
        </div>
        <div v-if="statusBadge" class="flow-node-meta flex items-center">
          <span
            class="flow-node-status shrink-0 rounded-sm px-1.5 py-0.5 text-[10px] font-medium"
            :class="statusBadge.class"
          >{{ statusBadge.label }}</span>
        </div>
      </div>

      <button
        v-if="canDelete"
        class="flow-node-delete shrink-0 rounded-sm p-0.5 text-slate-400 transition-colors hover:bg-slate-200 hover:text-slate-600"
        aria-label="删除节点"
        @click.stop="handleDelete"
      >
        <i class="i-lucide:x text-sm" />
      </button>
    </div>

    <div class="flow-node-body">
      <div v-if="subtitle && !readonly" class="flow-node-summary">
        {{ subtitle }}
      </div>
      <div v-if="$slots.default && !readonly" class="flow-node-extra">
        <slot />
      </div>
      <i class="i-lucide:chevron-right flow-node-chevron" />
    </div>
  </div>
</template>

<style scoped>
.flow-node-card {
  overflow: hidden;
  border: 1px solid var(--border-light);
  border-left: 3px solid var(--flow-node-color);
  border-radius: 4px;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.08);
}

.flow-node-card:not(.is-readonly):hover {
  border-color: var(--border-light);
  border-left-color: var(--flow-node-color);
  box-shadow: 0 3px 8px rgba(15, 23, 42, 0.1);
}

.flow-node-card.is-selected {
  border-color: #3b82f6;
  border-left-color: var(--flow-node-color);
  box-shadow:
    0 0 0 1px rgba(59, 130, 246, 0.9),
    0 3px 10px rgba(15, 23, 42, 0.12);
}

.flow-node-card.is-readonly {
  cursor: default;
}

.flow-node-icon {
  width: 22px;
  height: 22px;
  border-radius: 3px;
  background: var(--flow-node-soft);
  color: var(--flow-node-color);
}

.flow-node-icon i {
  width: 13px;
  height: 13px;
  font-size: 13px;
}

.flow-node-title {
  min-height: 32px;
  padding: 6px 10px;
  border-bottom: 1px solid #f1f5f9;
  background: rgba(248, 250, 252, 0.86);
}

.flow-node-delete {
  opacity: 0;
}

.flow-node-card:hover .flow-node-delete,
.flow-node-card.is-selected .flow-node-delete {
  opacity: 1;
}

.flow-node-body {
  display: flex;
  min-height: 44px;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 10px 12px;
  background: var(--bg-primary);
}

.flow-node-summary {
  min-width: 0;
  flex: 1;
  overflow: hidden;
  color: var(--flow-node-content-color);
  font-size: 12px;
  font-weight: 500;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.flow-node-extra {
  min-width: 0;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.35;
}

.flow-node-chevron {
  width: 16px;
  height: 16px;
  flex: none;
  color: #cbd5e1;
}

.flow-node-card.is-readonly {
  justify-content: stretch;
}

.flow-node-card.is-readonly .flow-node-title {
  align-items: center;
  min-height: 34px;
}

.flow-node-card.is-readonly .flow-node-icon {
  width: 24px;
  height: 24px;
  border-radius: 3px;
}

.flow-node-card.is-readonly .flow-node-icon i {
  font-size: 14px;
  line-height: 1;
}

.flow-node-card.is-readonly .flow-node-name {
  line-height: 1.35;
}

.flow-node-card.is-readonly .flow-node-summary {
  color: var(--text-secondary);
}

@media (prefers-reduced-motion: reduce) {
  .flow-node-card {
    transition: none;
  }

  .flow-node-card:not(.is-readonly):hover {
    transform: none;
  }
}
</style>
