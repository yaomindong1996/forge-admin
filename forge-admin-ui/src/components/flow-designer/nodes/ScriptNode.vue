<script setup>
/**
 * ScriptNode — 脚本任务卡片
 */
import { computed } from 'vue'
import NodeCard from './NodeCard.vue'

const props = defineProps({
  node: { type: Object, required: true },
  selected: Boolean,
  status: { type: String, default: null },
  readonly: Boolean,
  showAdd: Boolean,
})
defineEmits(['click', 'delete', 'contextMenu', 'add', 'connect'])

const subtitle = computed(() => {
  const c = props.node?.config || {}
  const fmt = c.scriptFormat || 'javascript'
  if (!c.script)
    return `${fmt} · 点击配置脚本`
  const preview = String(c.script).split('\n')[0].slice(0, 30)
  return `${fmt} · ${preview}${c.script.length > 30 ? '…' : ''}`
})
</script>

<template>
  <NodeCard
    :node="node" :selected="selected" :status="status" :readonly="readonly" :show-add="showAdd"
    icon="i-lucide:code" color-var="info" :subtitle="subtitle"
    @click="$emit('click', $event)" @delete="$emit('delete', $event)" @context-menu="$emit('contextMenu', $event)"
    @add="$emit('add', $event)"
    @connect="$emit('connect')"
  />
</template>
