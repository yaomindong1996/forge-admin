<script setup>
/**
 * 白底紧凑双行节点。选中时在卡片内右侧放「添加」，菜单上浮不挡连线。
 */
import { useEventListener } from '@vueuse/core'
import { NPopover } from 'naive-ui'
import { computed, ref } from 'vue'
import AddNodePopover from '../canvas/AddNodePopover.vue'
import { useFlowOverlayTarget } from '../composables/useFlowOverlayTarget.js'
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
  /** 选中态在卡片内显示「添加」入口 */
  showAdd: { type: Boolean, default: false },
  width: { type: Number, default: 200 },
  height: { type: Number, default: 56 },
})
const emit = defineEmits(['click', 'delete', 'contextMenu', 'add', 'connect'])

const addOpen = ref(false)
const overlayTarget = useFlowOverlayTarget()
const presentation = computed(() => getNodePresentation(props.node?.nodeType))
const cardStyle = computed(() => ({
  'width': `${props.width}px`,
  'minHeight': `${props.height}px`,
  '--flow-node-color': presentation.value.color,
}))

const STATUS_BADGE = {
  completed: { label: '已完成', class: 'is-completed' },
  running: { label: '审批中', class: 'is-running' },
  pending: { label: '待办', class: 'is-pending' },
  rejected: { label: '已驳回', class: 'is-rejected' },
  skipped: { label: '已跳过', class: 'is-pending' },
}
const statusBadge = computed(() => STATUS_BADGE[props.status] || null)

useEventListener('resize', () => {
  addOpen.value = false
})
useEventListener('wheel', (event) => {
  if (!event.target?.closest?.('.n-popover'))
    addOpen.value = false
}, { capture: true, passive: true })

function handleContextMenu(event) {
  event.preventDefault()
  const rect = event.currentTarget?.getBoundingClientRect()
  const point = event.clientX || event.clientY
    ? event
    : { clientX: rect?.left || 0, clientY: rect?.bottom || 0 }
  emit('contextMenu', { event: point, node: props.node })
}

function handleAddSelect(type) {
  addOpen.value = false
  emit('add', type)
}

function handleConnect() {
  addOpen.value = false
  emit('connect')
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

    <!-- 选中态：卡片内「添加」，占用原空白区，不在画布外叠层 -->
    <div
      v-if="showAdd && !readonly"
      class="flow-node-add"
      @pointerdown.stop
      @click.stop
    >
      <NPopover
        v-model:show="addOpen"
        trigger="click"
        placement="bottom-end"
        :theme-overrides="{ space: '8px' }"
        :show-arrow="true"
        :style="{ padding: '12px' }"
        :animated="false"
        :to="overlayTarget || undefined"
      >
        <template #trigger>
          <button
            type="button"
            class="flow-node-add-btn"
            :class="{ 'is-open': addOpen }"
            :aria-expanded="addOpen"
            aria-label="在此节点后添加"
            title="添加"
            @keydown.esc="addOpen = false"
          >
            <i class="i-lucide:plus" />
          </button>
        </template>
        <div @keydown.esc="addOpen = false">
          <AddNodePopover
            title="在此之后添加"
            :hint="`插入到「${node?.name || '当前节点'}」之后`"
            show-connect
            @select="handleAddSelect"
            @connect="handleConnect"
          />
        </div>
      </NPopover>
    </div>

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
  gap: 2px;
  box-sizing: border-box;
  padding: 8px 10px;
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
  gap: 8px;
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
  width: 28px;
  height: 28px;
  align-items: center;
  justify-content: center;
  border-radius: 5px;
  background: color-mix(in srgb, var(--flow-node-color) 14%, var(--bg-primary, #fff));
  color: var(--flow-node-color);
  font-size: 16px;
}
.flow-node-main {
  display: flex;
  flex: 1;
  min-width: 0;
  flex-direction: column;
  gap: 2px;
}
.flow-node-heading {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 4px;
}
.flow-node-name {
  overflow: hidden;
  font-size: 13px;
  font-weight: 500;
  line-height: 1.3;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.flow-node-summary {
  overflow: hidden;
  color: var(--text-tertiary, #86909c);
  font-size: 11px;
  line-height: 1.3;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.flow-node-extra {
  color: var(--text-secondary, #4e5969);
  font-size: 11px;
}
.flow-node-add {
  flex: none;
}
.flow-node-add-btn,
.flow-node-more {
  display: inline-flex;
  flex: none;
  width: 22px;
  height: 22px;
  align-items: center;
  justify-content: center;
  padding: 0;
  border: none;
  border-radius: 4px;
  background: transparent;
  color: var(--text-tertiary, #86909c);
  font-size: 14px;
  line-height: 1;
  cursor: pointer;
}
.flow-node-add-btn:hover,
.flow-node-add-btn.is-open,
.flow-node-more:hover {
  background: var(--bg-secondary, #f6f8fb);
  color: var(--primary-color, #356de0);
}
.flow-node-chevron {
  flex: none;
  width: 14px;
  color: var(--text-tertiary, #86909c);
  font-size: 14px;
}
.flow-node-open:focus-visible,
.flow-node-more:focus-visible,
.flow-node-add-btn:focus-visible {
  outline: 2px solid var(--primary-color, #356de0);
  outline-offset: 2px;
}
.flow-node-status {
  width: fit-content;
  padding: 1px 4px;
  border-radius: 3px;
  font-size: 10px;
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
