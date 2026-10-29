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
  showAdd: Boolean,
})
defineEmits(['click', 'delete', 'contextMenu', 'add', 'connect'])

const subtitle = computed(() => {
  const t = props.node?.bpmnElementType || 'bpmn:Activity'
  return `${t}（仅查看 XML）`
})
</script>

<template>
  <NodeCard
    :node="node" :selected="selected" :status="status" :readonly="readonly" :show-add="showAdd"
    icon="i-lucide:shield" color-var="warning" :subtitle="subtitle"
    @click="$emit('click', $event)" @delete="$emit('delete', $event)" @context-menu="$emit('contextMenu', $event)"
    @add="$emit('add', $event)"
    @connect="$emit('connect')"
  >
    <template #title-extra>
      <span class="shrink-0 rounded bg-orange-100 px-1.5 py-0.5 text-[10px] text-orange-600">
        高级
      </span>
    </template>
  </NodeCard>
</template>
