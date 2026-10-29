<script setup>
/**
 * 画布底部居中工具条。
 * 添加节点菜单只从这里弹出，不挡画布连线；有选中节点时提示插入位置。
 */
import { useEventListener } from '@vueuse/core'
import { NPopover } from 'naive-ui'
import { computed, ref } from 'vue'
import { useFlowOverlayTarget } from '../composables/useFlowOverlayTarget.js'
import AddNodePopover from './AddNodePopover.vue'

const props = defineProps({
  tool: { type: String, default: 'select' },
  readonly: Boolean,
  /** 当前可作为「之后插入」锚点的节点名称；空表示未选中可用节点 */
  addAnchorLabel: { type: String, default: '' },
})

const emit = defineEmits(['update:tool', 'autoLayout', 'delete', 'addNode', 'connect'])

const addOpen = ref(false)
const overlayTarget = useFlowOverlayTarget()

const tools = [
  { id: 'select', label: '选择', icon: 'i-lucide:mouse-pointer-2' },
  { id: 'pan', label: '平移画布', icon: 'i-lucide:hand' },
  { id: 'add', label: '添加节点', icon: 'i-lucide:plus', action: 'add' },
  { id: 'connect', label: '连线', icon: 'i-lucide:spline' },
  { id: 'layout', label: '自动布局', icon: 'i-lucide:layout-dashboard', action: 'autoLayout' },
  { id: 'delete', label: '删除', icon: 'i-lucide:trash-2' },
]

const addTitle = computed(() => (props.addAnchorLabel ? '在选中节点后添加' : '添加节点'))
const addHint = computed(() => {
  if (props.addAnchorLabel)
    return `将插入到「${props.addAnchorLabel}」之后`
  return '未选中节点时，普通节点会放到画布中央，可用连线接上'
})
const addButtonTitle = computed(() => {
  if (props.addAnchorLabel)
    return `添加节点（在「${props.addAnchorLabel}」之后）`
  return '添加节点'
})

useEventListener('resize', () => {
  addOpen.value = false
})
useEventListener('wheel', (event) => {
  if (!event.target?.closest?.('.n-popover, .canvas-mode-toolbar'))
    addOpen.value = false
}, { capture: true, passive: true })

function onClick(item) {
  if (props.readonly)
    return
  if (item.action === 'autoLayout') {
    emit('autoLayout')
    return
  }
  if (item.action === 'add')
    return
  addOpen.value = false
  emit('update:tool', item.id)
}

function handleSelect(type) {
  addOpen.value = false
  emit('addNode', type)
}

function handleConnect() {
  addOpen.value = false
  emit('connect')
  emit('update:tool', 'connect')
}
</script>

<template>
  <div class="canvas-mode-toolbar" role="toolbar" aria-label="画布工具">
    <span
      v-if="addAnchorLabel"
      class="canvas-mode-anchor"
      :title="`当前锚点：${addAnchorLabel}`"
    >
      {{ addAnchorLabel }}
    </span>
    <template v-for="item in tools" :key="item.id">
      <NPopover
        v-if="item.action === 'add'"
        v-model:show="addOpen"
        trigger="click"
        placement="top"
        :disabled="readonly"
        :theme-overrides="{ space: '10px' }"
        :show-arrow="true"
        :style="{ padding: '14px' }"
        :animated="false"
        :to="overlayTarget || undefined"
      >
        <template #trigger>
          <button
            type="button"
            class="canvas-mode-btn canvas-mode-btn--add"
            :class="{ 'is-active': addOpen, 'has-anchor': Boolean(addAnchorLabel) }"
            :disabled="readonly"
            :title="addButtonTitle"
            :aria-label="addButtonTitle"
            :aria-expanded="addOpen"
            @keydown.esc="addOpen = false"
          >
            <i :class="item.icon" />
          </button>
        </template>
        <div @keydown.esc="addOpen = false">
          <AddNodePopover
            :title="addTitle"
            :hint="addHint"
            show-connect
            @select="handleSelect"
            @connect="handleConnect"
          />
        </div>
      </NPopover>
      <button
        v-else
        type="button"
        class="canvas-mode-btn"
        :class="{ 'is-active': tool === item.id && !item.action }"
        :disabled="readonly"
        :title="item.label"
        :aria-label="item.label"
        @click="onClick(item)"
      >
        <i :class="item.icon" />
      </button>
    </template>
  </div>
</template>

<style scoped>
.canvas-mode-toolbar {
  position: absolute;
  bottom: 24px;
  left: 50%;
  z-index: 12;
  display: flex;
  align-items: center;
  gap: 2px;
  padding: 4px;
  border: 1px solid var(--border-light, #e5e7eb);
  border-radius: 8px;
  background: var(--bg-primary, #fff);
  box-shadow: 0 2px 10px rgb(0 0 0 / 6%);
  transform: translateX(-50%);
  pointer-events: auto;
}
.canvas-mode-anchor {
  max-width: 120px;
  margin: 0 4px 0 2px;
  overflow: hidden;
  border-radius: 4px;
  background: color-mix(in srgb, var(--primary-color, #356de0) 8%, #fff);
  color: var(--primary-color, #356de0);
  font-size: 11px;
  font-weight: 500;
  line-height: 24px;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding: 0 8px;
}
.canvas-mode-btn {
  display: inline-flex;
  width: 32px;
  height: 32px;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: var(--text-secondary, #4e5969);
  font-size: 15px;
  cursor: pointer;
}
.canvas-mode-btn:hover:not(:disabled) {
  background: var(--bg-secondary, #f6f8fb);
  color: var(--text-primary, #1d2129);
}
.canvas-mode-btn.is-active {
  background: color-mix(in srgb, var(--primary-color, #356de0) 12%, #fff);
  color: var(--primary-color, #356de0);
}
.canvas-mode-btn--add {
  color: var(--primary-color, #356de0);
}
.canvas-mode-btn--add.has-anchor {
  background: color-mix(in srgb, var(--primary-color, #356de0) 12%, #fff);
}
.canvas-mode-btn:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}
@media (max-width: 760px) {
  .canvas-mode-toolbar {
    bottom: 16px;
  }
  .canvas-mode-anchor {
    max-width: 72px;
  }
}
</style>
