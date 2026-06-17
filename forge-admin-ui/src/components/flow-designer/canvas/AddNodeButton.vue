<script setup>
/**
 * AddNodeButton — 节点之间 / 分支首部显示的 "+" 按钮
 *
 * Props:
 *   - position    { x, y } 画布坐标系下的中心位置
 *   - size        按钮直径，默认 24
 *   - allowTypes  传给 AddNodePopover 的类型限制
 *   - readonly    禁用
 *
 * Events:
 *   - select(type) 用户选择节点类型后触发；外层负责调用 useFlowDesigner.addNode
 */
import { ref } from 'vue'
import AddNodePopover from './AddNodePopover.vue'

const props = defineProps({
  position: { type: Object, required: true },
  size: { type: Number, default: 24 },
  allowTypes: { type: Array, default: null },
  readonly: { type: Boolean, default: false },
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
  // 简单实现：点击非按钮 / 弹窗区域时收起
  if (!event.target.closest('.add-node-button-wrap'))
    popoverVisible.value = false
}

if (typeof window !== 'undefined')
  window.addEventListener('mousedown', handleClickOutside, true)
</script>

<template>
  <div
    class="add-node-button-wrap absolute z-20"
    :style="{
      left: `${position.x - size / 2}px`,
      top: `${position.y - size / 2}px`,
    }"
  >
    <button
      class="add-node-btn flex items-center justify-center border border-gray-300 rounded-full border-dashed bg-white text-gray-500 transition hover:border-primary hover:text-primary"
      :class="{ 'opacity-50 cursor-not-allowed': readonly }"
      :style="{ width: `${size}px`, height: `${size}px` }"
      :disabled="readonly"
      :aria-expanded="popoverVisible"
      @click.stop="toggle"
    >
      <i class="i-mdi-plus text-sm" />
    </button>
    <div
      v-if="popoverVisible"
      class="add-node-popover-anchor absolute left-1/2 top-full z-30 mt-2 border border-gray-200 rounded-md bg-white p-2 shadow-lg -translate-x-1/2"
    >
      <AddNodePopover :allow-types="allowTypes" @select="handleSelect" />
    </div>
  </div>
</template>
