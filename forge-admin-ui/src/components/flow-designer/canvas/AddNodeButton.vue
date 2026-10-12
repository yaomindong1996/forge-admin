<script setup>
/**
 * AddNodeButton — 节点之间的 "+" 添加按钮
 *
 * 样式参考钉钉：极简白色图标按钮
 * 点击后弹出 AddNodePopover 选择节点类型
 */
import { onBeforeUnmount, onMounted, ref } from 'vue'
import AddNodePopover from './AddNodePopover.vue'

const props = defineProps({
  position: { type: Object, required: true },
  allowTypes: { type: Array, default: null },
  readonly: { type: Boolean, default: false },
  label: { type: String, default: '添加' },
})

const emit = defineEmits(['select'])

const popoverVisible = ref(false)

function toggle() {
  if (props.readonly)
    return
  popoverVisible.value = !popoverVisible.value
}

function handleSelect(type) {
  popoverVisible.value = false
  emit('select', type)
}

function handleClickOutside(event) {
  if (!event.target.closest('.add-node-button-wrap'))
    popoverVisible.value = false
}

onMounted(() => {
  window.addEventListener('mousedown', handleClickOutside, true)
})

onBeforeUnmount(() => {
  window.removeEventListener('mousedown', handleClickOutside, true)
})
</script>

<template>
  <div
    class="add-node-button-wrap absolute z-20"
    :class="{ 'is-open': popoverVisible }"
    :style="{
      left: `${position.x}px`,
      top: `${position.y}px`,
      transform: 'translate(-50%, -50%)',
    }"
  >
    <button
      class="add-node-btn flex items-center justify-center bg-white transition-all duration-200"
      :class="{ 'opacity-40 cursor-not-allowed': readonly }"
      :disabled="readonly"
      :aria-expanded="popoverVisible"
      :aria-label="`添加${label}节点`"
      :title="`添加${label}节点`"
      @click.stop="toggle"
    >
      <i class="i-lucide:plus text-sm" />
    </button>
    <div
      v-if="popoverVisible"
      class="add-node-popover-anchor absolute left-1/2 top-full z-90 mt-2 bg-white p-3 shadow-xl -translate-x-1/2"
    >
      <AddNodePopover :allow-types="allowTypes" @select="handleSelect" />
    </div>
  </div>
</template>

<style scoped>
.add-node-button-wrap {
  width: 20px;
  height: 20px;
}

.add-node-button-wrap.is-open {
  z-index: 90;
}

.add-node-btn {
  width: 20px;
  height: 20px;
  border: 1px solid var(--border-light);
  border-radius: 4px;
  color: var(--text-tertiary);
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.1);
}

.add-node-btn:hover:not(:disabled),
.add-node-btn[aria-expanded='true'] {
  border-color: #2563eb;
  background: #eff6ff;
  color: #2563eb;
  box-shadow: 0 2px 6px rgba(37, 99, 235, 0.14);
}

.add-node-popover-anchor {
  z-index: 91;
  border: 1px solid var(--border-light);
  border-radius: 8px;
  box-shadow: 0 20px 48px rgba(15, 23, 42, 0.16);
}

@media (prefers-reduced-motion: reduce) {
  .add-node-btn {
    transition: none;
  }

  .add-node-btn:hover:not(:disabled),
  .add-node-btn[aria-expanded='true'] {
    transform: none;
  }
}
</style>
