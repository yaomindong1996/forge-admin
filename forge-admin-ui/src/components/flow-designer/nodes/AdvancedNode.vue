<script setup>
/**
 * AdvancedNode — 高级节点（advanced 兜底）卡片
 *
 * 显示原 BPMN 元素类型，提示用户该节点不被钉钉样式直接支持，仅可查看 / 删除 / 编辑 rawXml。
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
  const t = props.node?.bpmnElementType || 'bpmn:Activity'
  return `${t}（仅查看 XML）`
})
</script>

<template>
  <NodeCard
    :node="node" :selected="selected" :status="status" :readonly="readonly"
    icon="i-mdi-shield-alert-outline" color-var="warning"
    @click="$emit('click', $event)" @delete="$emit('delete', $event)" @context-menu="$emit('context-menu', $event)"
  >
    <template #title-extra>
      <span class="text-xs rounded bg-warning/10 px-1.5 py-0.5 text-warning">
        高级
      </span>
    </template>
    {{ summary }}
  </NodeCard>
</template>
