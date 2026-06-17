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
})
defineEmits(['click', 'delete', 'context-menu'])

const summary = computed(() => {
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
    :node="node" :selected="selected" :status="status" :readonly="readonly"
    icon="i-mdi-code-tags" color-var="info"
    @click="$emit('click', $event)" @delete="$emit('delete', $event)" @context-menu="$emit('context-menu', $event)"
  >
    {{ summary }}
  </NodeCard>
</template>
