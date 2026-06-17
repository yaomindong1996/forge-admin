<script setup>
/**
 * NodeRenderer — 按 nodeType 调度对应卡片组件
 *
 * 这个调度组件让 FlowCanvas / DingFlowDesigner 不需要显式 v-if 分发到 12 种节点。
 * 只需 <NodeRenderer :node="node" ... /> 即可。
 *
 * Props:
 *   - node             flowJson 节点对象
 *   - position         { x, y, width, height } 来自 layoutFlow
 *   - selected
 *   - status
 *   - readonly
 *   - outgoingCount    供 BranchNode 显示分支数（外层从 useFlowDesigner.getOutgoingEdges 取）
 *
 * Events 全部透传：click / delete / context-menu
 */
import { computed } from 'vue'
import AdvancedNode from '../nodes/AdvancedNode.vue'
import ApproverNode from '../nodes/ApproverNode.vue'
import BranchNode from '../nodes/BranchNode.vue'
import CallActivityNode from '../nodes/CallActivityNode.vue'
import CarbonCopyNode from '../nodes/CarbonCopyNode.vue'
import EndNode from '../nodes/EndNode.vue'
import ScriptNode from '../nodes/ScriptNode.vue'
import ServiceNode from '../nodes/ServiceNode.vue'
import StartNode from '../nodes/StartNode.vue'
import SubProcessNode from '../nodes/SubProcessNode.vue'

const props = defineProps({
  node: { type: Object, required: true },
  position: { type: Object, default: null },
  selected: Boolean,
  status: { type: String, default: null },
  readonly: Boolean,
  outgoingCount: { type: Number, default: 0 },
})

defineEmits(['click', 'delete', 'context-menu'])

const RENDERER_MAP = {
  start: StartNode,
  end: EndNode,
  approver: ApproverNode,
  carbonCopy: CarbonCopyNode,
  condition: BranchNode,
  parallel: BranchNode,
  inclusive: BranchNode,
  service: ServiceNode,
  script: ScriptNode,
  subProcess: SubProcessNode,
  callActivity: CallActivityNode,
  advanced: AdvancedNode,
}

const Component = computed(() => RENDERER_MAP[props.node?.nodeType] || AdvancedNode)

const isBranch = computed(() => ['condition', 'parallel', 'inclusive'].includes(props.node?.nodeType))

const wrapStyle = computed(() => {
  if (!props.position)
    return {}
  return {
    position: 'absolute',
    left: `${props.position.x}px`,
    top: `${props.position.y}px`,
  }
})
</script>

<template>
  <div class="node-renderer-wrap" :style="wrapStyle" :data-node-id="node?.id">
    <component
      :is="Component"
      :node="node"
      :selected="selected"
      :status="status"
      :readonly="readonly"
      :outgoing-count="isBranch ? outgoingCount : undefined"
      @click="$emit('click', $event)"
      @delete="$emit('delete', $event)"
      @context-menu="$emit('context-menu', $event)"
    />
  </div>
</template>
