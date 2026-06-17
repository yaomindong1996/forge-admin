<script setup>
/**
 * DingFlowViewer — 钉钉样式流程查看器
 *
 * 接口与 ProcessDiagramViewer 兼容：
 *   Props:
 *     - processInstanceId   流程实例 ID
 *     - compact             紧凑模式（顶部信息条 + 图例可隐藏）
 *     - bpmnXml             直接传入 BPMN XML（跳过自动 fetch）
 *     - nodeInstanceList    直接传入节点实例信息（跳过自动 fetch）
 *
 * 内部行为：
 *   - 当 processInstanceId 变化时，调用 flowApi.getProcessDiagramInfo 获取 bpmnXml + nodeInstanceList
 *   - 把 nodeInstanceList 转换为 nodeStatusMap = { [bpmnElementId]: { status, ... } }
 *   - 用 DingFlowDesigner readonly 模式渲染（status 通过 NodeRenderer 透传到 NodeCard）
 *   - 节点点击 → 弹出 NodeDetailPopover 展示该节点的运行实例信息
 *
 * 因为 NodeRenderer 已支持 status props，本组件主要负责数据获取与状态映射。
 */
import { computed, ref, watch } from 'vue'
import flowApi from '@/api/flow'
import {
  EdgeLayer,
  FlowCanvas,
  layoutFlow,
  NodeRenderer,
} from '../canvas/index.js'
import { useFlowDesigner } from '../composables/index.js'
import { convertBpmnToJson } from '../converter/index.js'
import NodeDetailPopover from './NodeDetailPopover.vue'

const props = defineProps({
  processInstanceId: { type: [String, Number], default: null },
  compact: { type: Boolean, default: false },
  bpmnXml: { type: String, default: '' },
  nodeInstanceList: { type: Array, default: null },
})

const emit = defineEmits(['ready', 'error'])

const designer = useFlowDesigner()
const loading = ref(false)
const renderError = ref(null)

const diagramInfo = ref({
  bpmnXml: '',
  status: null,
  startUserName: '',
  startTime: null,
  endTime: null,
  nodeInstanceList: [],
})

const nodeStatusMap = computed(() => {
  const m = {}
  for (const inst of diagramInfo.value.nodeInstanceList || []) {
    if (inst?.nodeId)
      m[inst.nodeId] = inst
  }
  return m
})

const layoutResult = ref({
  nodePositions: new Map(),
  edgePaths: new Map(),
  canvasBounds: { minX: 0, minY: 0, maxX: 0, maxY: 0 },
})

watch(designer.flowJson, () => {
  layoutResult.value = layoutFlow(designer.flowJson.value)
}, { immediate: true })

/* ---------- 数据加载 ---------- */

async function loadFromApi(processInstanceId) {
  if (!processInstanceId)
    return
  loading.value = true
  renderError.value = null
  try {
    const res = await flowApi.getProcessDiagramInfo(processInstanceId)
    if (res?.code === 200 || res?.code === 0 || res?.data) {
      const data = res.data || res
      diagramInfo.value = {
        bpmnXml: data.bpmnXml || '',
        status: data.status || null,
        startUserName: data.startUserName || '',
        startTime: data.startTime || null,
        endTime: data.endTime || null,
        nodeInstanceList: data.nodeInstanceList || [],
      }
      applyXml(diagramInfo.value.bpmnXml)
      emit('ready', diagramInfo.value)
    }
    else {
      throw new Error(res?.message || '加载流程图失败')
    }
  }
  catch (err) {
    renderError.value = err
    console.error('[DingFlowViewer] 加载流程图失败', err)
    emit('error', err)
  }
  finally {
    loading.value = false
  }
}

function applyXml(xml) {
  if (!xml) {
    designer.reset()
    return
  }
  try {
    const json = convertBpmnToJson(xml)
    designer.loadJson(json)
  }
  catch (err) {
    console.error('[DingFlowViewer] BPMN XML 解析失败', err)
    renderError.value = err
  }
}

/* ---------- 数据来源切换 ---------- */

watch(
  () => [props.processInstanceId, props.bpmnXml, props.nodeInstanceList],
  ([pid, xml, list]) => {
    if (xml) {
      // 直接给入 XML 模式
      diagramInfo.value = {
        bpmnXml: xml,
        status: null,
        startUserName: '',
        startTime: null,
        endTime: null,
        nodeInstanceList: list || [],
      }
      applyXml(xml)
    }
    else if (pid) {
      loadFromApi(pid)
    }
  },
  { immediate: true },
)

/* ---------- 节点点击 → 弹出详情 ---------- */

const popover = ref({ visible: false, position: { x: 0, y: 0 }, node: null })

function handleNodeClick(node) {
  designer.selectedNodeId.value = node.id
  // 用节点屏幕位置作为气泡定位（这里用一个相对粗略的位置：节点右侧 + 20px）
  // 真实位置需结合 viewport 缩放，但 jsdom + 简单实现先够用
  const target = document.querySelector(`[data-node-id="${node.id}"]`)
  if (target) {
    const rect = target.getBoundingClientRect()
    popover.value = {
      visible: true,
      position: { x: rect.right + 12, y: rect.top },
      node,
    }
  }
  else {
    popover.value = { visible: true, position: { x: 100, y: 100 }, node }
  }
}

function popoverTaskInfo(node) {
  const inst = nodeStatusMap.value[node?.id]
  if (!inst)
    return null
  return {
    status: inst.status,
    assigneeName: inst.assigneeName || inst.assignee,
    startTime: inst.startTime,
    endTime: inst.endTime,
    result: inst.result,
    comment: inst.comment,
  }
}

const popoverTaskInfoComputed = computed(() => popoverTaskInfo(popover.value.node))

/* ---------- 流程总状态文案 ---------- */

const STATUS_TEXT = {
  RUNNING: { label: '审批中', type: 'info' },
  COMPLETED: { label: '已完成', type: 'success' },
  CANCELED: { label: '已取消', type: 'default' },
  REJECTED: { label: '已驳回', type: 'error' },
}

const overallStatus = computed(() => {
  const s = diagramInfo.value.status
  if (!s)
    return null
  return STATUS_TEXT[s] || { label: String(s), type: 'default' }
})

defineExpose({
  designer,
  diagramInfo,
  loading,
  renderError,
  nodeStatusMap,
  /** 给单测 / 外部 ref 用，便于手动切换 BPMN XML */
  applyXml,
  loadFromApi,
})
</script>

<template>
  <div class="ding-flow-viewer relative h-full w-full" :class="{ compact }">
    <!-- 顶部状态条 -->
    <div v-if="!compact && overallStatus" class="text-sm flex items-center gap-3 px-4 py-2">
      <span
        class="text-xs inline-flex items-center rounded px-2 py-0.5"
        :class="{
          'bg-info/10 text-info': overallStatus.type === 'info',
          'bg-success/10 text-success': overallStatus.type === 'success',
          'bg-error/10 text-error': overallStatus.type === 'error',
          'bg-gray-100 text-gray-500': overallStatus.type === 'default',
        }"
      >
        {{ overallStatus.label }}
      </span>
      <span v-if="diagramInfo.startUserName" class="text-gray-500">
        发起人：{{ diagramInfo.startUserName }}
      </span>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="text-sm absolute inset-0 z-10 flex items-center justify-center bg-white/60 text-gray-400">
      正在加载流程图…
    </div>

    <!-- 渲染失败 -->
    <div v-if="renderError && !loading" class="text-sm flex items-center justify-center p-8 text-error">
      流程图加载失败：{{ renderError.message || renderError }}
    </div>

    <!-- 钉钉风格画布（readonly） -->
    <FlowCanvas v-if="!renderError" readonly>
      <template #edges>
        <EdgeLayer
          :edges="designer.flowJson.value.edges"
          :paths="layoutResult.edgePaths"
          :canvas-bounds="layoutResult.canvasBounds"
          :node-statuses="nodeStatusMap"
        />
      </template>
      <template #nodes>
        <NodeRenderer
          v-for="node in designer.flowJson.value.nodes"
          :key="node.id"
          :node="node"
          :position="layoutResult.nodePositions.get(node.id)"
          :selected="designer.selectedNodeId.value === node.id"
          :status="nodeStatusMap[node.id]?.status"
          :readonly="true"
          :outgoing-count="designer.getOutgoingEdges(node.id).length"
          @click="handleNodeClick"
        />
      </template>
    </FlowCanvas>

    <!-- 节点详情气泡 -->
    <NodeDetailPopover
      :visible="popover.visible"
      :position="popover.position"
      :node="popover.node"
      :task-info="popoverTaskInfoComputed"
      @close="popover.visible = false"
    />
  </div>
</template>
