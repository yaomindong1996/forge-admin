<script setup>
/**
 * AddNodePopover — 节点类型选择弹窗内容
 *
 * 使用 NODE_MENU_GROUPS 渲染分组列表。
 * 点击某项时 emit('select', type)；外层负责关闭 Popover 与调用 useFlowDesigner.addNode。
 *
 * Props:
 *   - allowTypes  数组，限制可见节点类型（不在数组内的隐藏）。默认 null = 全部可用
 */
import { computed } from 'vue'
import { NODE_MENU_GROUPS } from '../constants/node-menu.js'

const props = defineProps({
  allowTypes: { type: Array, default: null },
})

const emit = defineEmits(['select'])

const groups = computed(() => {
  if (!props.allowTypes)
    return NODE_MENU_GROUPS
  return NODE_MENU_GROUPS
    .map(g => ({
      ...g,
      items: g.items.filter(it => props.allowTypes.includes(it.type)),
    }))
    .filter(g => g.items.length > 0)
})

function handleClick(type) {
  emit('select', type)
}
</script>

<template>
  <div class="add-node-popover w-60">
    <div v-for="group in groups" :key="group.label" class="mb-2 last:mb-0">
      <div class="text-xs px-2 py-1 text-gray-500">
        {{ group.label }}
      </div>
      <div class="grid grid-cols-2 gap-1">
        <button
          v-for="item in group.items"
          :key="item.type"
          class="text-sm flex items-center gap-2 rounded px-2 py-2 hover:bg-gray-100"
          :data-type="item.type"
          @click.stop="handleClick(item.type)"
        >
          <i :class="[item.icon, item.color]" class="text-base" />
          <span>{{ item.label }}</span>
        </button>
      </div>
    </div>
  </div>
</template>
