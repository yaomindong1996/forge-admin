<script setup>
/** 右键和卡片更多入口共用系统 Dropdown，复用主题、避让和键盘交互。 */
import { NDropdown } from 'naive-ui'
import { computed, h } from 'vue'
import { useFlowOverlayTarget } from '../composables/useFlowOverlayTarget.js'

const props = defineProps({
  visible: Boolean,
  position: { type: Object, default: () => ({ x: 0, y: 0 }) },
  node: { type: Object, default: null },
  canMoveUp: { type: Boolean, default: true },
  canMoveDown: { type: Boolean, default: true },
  readonly: Boolean,
})
const emit = defineEmits(['close', 'action'])
const overlayTarget = useFlowOverlayTarget()
const options = computed(() => {
  const items = [item('edit', '编辑', 'i-lucide:pencil')]
  const fixed = ['start', 'end'].includes(props.node?.nodeType)
  if (props.readonly || fixed)
    return items
  if (props.node?.nodeType !== 'advanced')
    items.push(item('copy', '复制', 'i-lucide:copy'))
  if (props.node?.nodeType === 'approver')
    items.push(item('merge', '合并审批节点', 'i-lucide:git-merge'))
  if (props.canMoveUp)
    items.push(item('move-up', '上移', 'i-lucide:arrow-up'))
  if (props.canMoveDown)
    items.push(item('move-down', '下移', 'i-lucide:arrow-down'))
  items.push({ type: 'divider', key: 'divider' })
  items.push({
    ...item('delete', '删除', 'i-lucide:trash-2'),
    props: { style: { color: 'var(--error-color, #f53f3f)' } },
  })
  return items
})
function item(key, label, icon) {
  return { key, label, icon: () => h('i', { class: icon }) }
}
function handleAction(key) {
  emit('action', key, props.node)
  emit('close')
}
</script>

<template>
  <NDropdown
    trigger="manual" :show="visible && Boolean(node)" :x="position.x" :y="position.y"
    :options="options" placement="bottom-start" :style="{ minWidth: '148px' }"
    :to="overlayTarget || undefined"
    @select="handleAction" @clickoutside="$emit('close')" @update:show="!$event && $emit('close')"
  />
</template>
