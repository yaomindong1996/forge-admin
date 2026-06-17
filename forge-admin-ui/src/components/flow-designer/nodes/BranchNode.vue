<script setup>
/**
 * BranchNode — 网关节点（condition / parallel / inclusive 共享样式）
 *
 * 网关在钉钉样式画布中通常显示为简短的菱形/圆角小卡片，但为统一渲染，本组件按
 * NodeCard 形式展示，标识网关类型 + 出边数量。
 */
import { computed } from 'vue'
import NodeCard from './NodeCard.vue'

const props = defineProps({
  node: { type: Object, required: true },
  outgoingCount: { type: Number, default: 0 },
  selected: Boolean,
  status: { type: String, default: null },
  readonly: Boolean,
})

defineEmits(['click', 'delete', 'context-menu'])

const META = {
  condition: { icon: 'i-mdi-source-branch', color: 'warning', label: '条件分支' },
  parallel: { icon: 'i-mdi-call-split', color: 'success', label: '并行分支' },
  inclusive: { icon: 'i-mdi-set-merge', color: 'success', label: '包容分支' },
}

const meta = computed(() => META[props.node?.nodeType] || META.condition)

const summary = computed(() => {
  const t = props.node?.nodeType
  const n = props.outgoingCount || 0
  if (t === 'condition')
    return n > 0 ? `${n} 条分支（按条件路由）` : '点击配置分支条件'
  if (t === 'parallel')
    return n > 0 ? `${n} 条并行分支（同时执行）` : '点击配置并行分支'
  if (t === 'inclusive')
    return n > 0 ? `${n} 条包容分支（条件 OR）` : '点击配置包容分支'
  return ''
})
</script>

<template>
  <NodeCard
    :node="node"
    :selected="selected"
    :status="status"
    :readonly="readonly"
    :icon="meta.icon"
    :color-var="meta.color"
    @click="$emit('click', $event)"
    @delete="$emit('delete', $event)"
    @context-menu="$emit('context-menu', $event)"
  >
    <template #title-extra>
      <span class="text-xs rounded bg-gray-100 px-1.5 py-0.5 text-gray-500">
        {{ meta.label }}
      </span>
    </template>
    {{ summary }}
  </NodeCard>
</template>
