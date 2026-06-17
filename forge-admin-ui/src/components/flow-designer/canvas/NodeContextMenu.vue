<script setup>
/**
 * NodeContextMenu — 节点右键菜单
 *
 * 用法：FlowCanvas / NodeRenderer 监听节点的 context-menu 事件，传入
 * { event, node }，本组件按节点类型渲染对应操作（编辑 / 复制 / 上移 / 下移 / 删除）。
 *
 * Props:
 *   - visible    是否显示
 *   - position   { x, y } 屏幕坐标
 *   - node       目标节点
 *   - canMoveUp  是否可向上移动（外层结合 useFlowDesigner.getIncomingEdges 判断）
 *   - canMoveDown
 *   - readonly
 *
 * Events:
 *   - close
 *   - action(actionId, node)   actionId: 'edit' | 'copy' | 'move-up' | 'move-down' | 'delete'
 */
import { computed, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  visible: Boolean,
  position: { type: Object, default: () => ({ x: 0, y: 0 }) },
  node: { type: Object, default: null },
  canMoveUp: { type: Boolean, default: true },
  canMoveDown: { type: Boolean, default: true },
  readonly: Boolean,
})

const emit = defineEmits(['close', 'action'])

const isStartOrEnd = computed(() => ['start', 'end'].includes(props.node?.nodeType))
const isAdvanced = computed(() => props.node?.nodeType === 'advanced')

function handleAction(id) {
  emit('action', id, props.node)
  emit('close')
}

function handleClickOutside(event) {
  if (!event.target.closest('.node-context-menu'))
    emit('close')
}

function handleEsc(event) {
  if (event.key === 'Escape')
    emit('close')
}

onMounted(() => {
  if (typeof window !== 'undefined') {
    window.addEventListener('mousedown', handleClickOutside, true)
    window.addEventListener('keydown', handleEsc)
  }
})

onUnmounted(() => {
  if (typeof window !== 'undefined') {
    window.removeEventListener('mousedown', handleClickOutside, true)
    window.removeEventListener('keydown', handleEsc)
  }
})
</script>

<template>
  <div
    v-if="visible && node"
    class="node-context-menu text-sm fixed z-50 min-w-32 border border-gray-200 rounded-md bg-white py-1 shadow-lg"
    :style="{ left: `${position.x}px`, top: `${position.y}px` }"
    @click.stop
  >
    <button
      class="w-full flex items-center gap-2 px-3 py-1.5 text-gray-700 hover:bg-gray-50"
      @click="handleAction('edit')"
    >
      <i class="i-mdi-pencil-outline text-base text-primary" />
      <span>编辑</span>
    </button>
    <button
      v-if="!readonly && !isStartOrEnd && !isAdvanced"
      class="w-full flex items-center gap-2 px-3 py-1.5 text-gray-700 hover:bg-gray-50"
      @click="handleAction('copy')"
    >
      <i class="i-mdi-content-copy text-base text-info" />
      <span>复制</span>
    </button>
    <button
      v-if="!readonly && canMoveUp && !isStartOrEnd"
      class="w-full flex items-center gap-2 px-3 py-1.5 text-gray-700 hover:bg-gray-50"
      @click="handleAction('move-up')"
    >
      <i class="i-mdi-arrow-up text-base text-info" />
      <span>上移</span>
    </button>
    <button
      v-if="!readonly && canMoveDown && !isStartOrEnd"
      class="w-full flex items-center gap-2 px-3 py-1.5 text-gray-700 hover:bg-gray-50"
      @click="handleAction('move-down')"
    >
      <i class="i-mdi-arrow-down text-base text-info" />
      <span>下移</span>
    </button>
    <div v-if="!readonly && !isStartOrEnd" class="my-1 h-px bg-gray-100" />
    <button
      v-if="!readonly && !isStartOrEnd"
      class="w-full flex items-center gap-2 px-3 py-1.5 text-error hover:bg-error/5"
      @click="handleAction('delete')"
    >
      <i class="i-mdi-trash-can-outline text-base" />
      <span>删除</span>
    </button>
  </div>
</template>
