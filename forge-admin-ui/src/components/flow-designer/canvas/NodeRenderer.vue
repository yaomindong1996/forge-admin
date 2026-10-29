<script setup>
/**
 * NodeRenderer — 按 nodeType 调度卡片；四边中心暴露连线锚点。
 * 选中态在卡片内「添加」；底部工具栏「＋」仍作兜底。
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
  showPorts: { type: Boolean, default: true },
  portsActive: Boolean,
  connectSource: Boolean,
  /** source=强调出线；target=强调入线；null=四边都可 */
  reattachWhich: { type: String, default: null },
})

const emit = defineEmits(['click', 'delete', 'contextMenu', 'pointerDown', 'portPointerDown', 'add', 'connect'])

const SIDES = [
  { side: 'top', title: '上边锚点' },
  { side: 'right', title: '右边锚点' },
  { side: 'bottom', title: '下边锚点' },
  { side: 'left', title: '左边锚点' },
]

function onWrapPointerDown(event) {
  emit('pointerDown', event, props.node)
}

function onPortPointerDown(event, side) {
  emit('portPointerDown', event, props.node, side)
}

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
  callActivity: CallActivityNode,
  subProcess: SubProcessNode,
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

const visiblePorts = computed(() => {
  if (!props.showPorts || props.readonly)
    return []
  return SIDES.filter(({ side }) => {
    // 结束节点只允许入边（顶边），连线模式也不铺四边，避免小圆上挤成一团
    if (props.node?.nodeType === 'end')
      return side === 'top'
    if (props.node?.nodeType === 'start' && side === 'top')
      return props.portsActive
    return true
  })
})

/** 选中普通卡片时在卡内显示「添加」；结束/网关/连线模式除外 */
const showAdd = computed(() => {
  if (props.readonly || props.portsActive || !props.selected)
    return false
  const t = props.node?.nodeType
  if (!t || t === 'end' || t === 'condition' || t === 'parallel' || t === 'inclusive')
    return false
  return true
})

function handleAdd(type) {
  emit('add', props.node, type)
}

function handleConnect() {
  emit('connect')
}
</script>

<template>
  <div
    class="node-renderer-wrap"
    :class="{
      'is-draggable': !readonly && !portsActive,
      'is-ports-active': portsActive,
      'is-selected': selected,
      'is-connect-source': connectSource,
    }"
    :style="wrapStyle"
    :data-node-id="node?.id"
    @pointerdown="onWrapPointerDown"
  >
    <component
      :is="Component"
      :node="node"
      :selected="selected"
      :status="status"
      :readonly="readonly"
      :show-add="showAdd"
      :width="position?.width"
      :height="position?.height"
      :outgoing-count="isBranch ? outgoingCount : undefined"
      @click="$emit('click', $event)"
      @delete="$emit('delete', $event)"
      @context-menu="$emit('contextMenu', $event)"
      @add="handleAdd"
      @connect="handleConnect"
    />
    <button
      v-for="p in visiblePorts"
      :key="p.side"
      type="button"
      class="node-port"
      :class="[
        `node-port--${p.side}`,
        { 'is-active': portsActive },
      ]"
      :title="p.title"
      :aria-label="p.title"
      @pointerdown.stop.prevent="onPortPointerDown($event, p.side)"
    />
  </div>
</template>

<style scoped>
.node-renderer-wrap {
  position: relative;
  /* 高于连线层(z=1)，保证卡片始终压在线上 */
  z-index: 4;
}
.node-renderer-wrap.is-selected {
  z-index: 5;
}
.node-renderer-wrap.is-draggable {
  cursor: grab;
}
.node-renderer-wrap.is-draggable:active {
  cursor: grabbing;
}
.node-renderer-wrap.is-ports-active {
  cursor: crosshair;
}
.node-renderer-wrap.is-connect-source {
  outline: 2px solid var(--primary-color, #356de0);
  outline-offset: 2px;
  border-radius: 8px;
}
/* 连线模式：跟卡片同形的圆角虚线框，禁止 50% 椭圆套在长卡片上 */
.node-renderer-wrap.is-ports-active::after {
  position: absolute;
  inset: -4px;
  border: 1.5px dashed var(--primary-color, #356de0);
  border-radius: 8px;
  opacity: 0.5;
  pointer-events: none;
  content: '';
}
/* 仅结束小圆用圆形框 */
.node-renderer-wrap.is-ports-active:has([data-node-type='end'])::after,
.node-renderer-wrap.is-connect-source:has([data-node-type='end']) {
  border-radius: 50%;
}
/* 整边可点：透明热区覆盖四边；中心小圆仅作视觉提示，落点按指针投影 */
.node-port {
  position: absolute;
  z-index: 3;
  margin: 0;
  border: none;
  border-radius: 0;
  background: transparent;
  padding: 0;
  cursor: crosshair;
  opacity: 0;
  transition: opacity 120ms ease;
}
.node-port::after {
  position: absolute;
  width: 6px;
  height: 6px;
  border: 1.5px solid var(--primary-color, #356de0);
  border-radius: 50%;
  background: var(--bg-primary, #fff);
  content: '';
  pointer-events: none;
}
.node-renderer-wrap:hover .node-port,
.node-renderer-wrap.is-selected .node-port,
.node-renderer-wrap.is-ports-active .node-port {
  opacity: 1;
}
.node-port.is-active::after {
  width: 7px;
  height: 7px;
  box-shadow: 0 0 0 2px color-mix(in srgb, var(--primary-color, #356de0) 28%, transparent);
}
/* 热区略伸出便于点中；可见圆点中心落在卡片四边中点，与连线端点同心 */
.node-port--top {
  top: -6px;
  left: 0;
  width: 100%;
  height: 12px;
}
.node-port--top::after {
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}
.node-port--bottom {
  bottom: -6px;
  left: 0;
  width: 100%;
  height: 12px;
}
.node-port--bottom::after {
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}
.node-port--left {
  top: 0;
  left: -6px;
  width: 12px;
  height: 100%;
}
.node-port--left::after {
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}
.node-port--right {
  top: 0;
  right: -6px;
  width: 12px;
  height: 100%;
}
.node-port--right::after {
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}
</style>
