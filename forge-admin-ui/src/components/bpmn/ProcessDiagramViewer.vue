<template>
  <div class="process-diagram-viewer">
    <!-- 加载状态 -->
    <n-spin :show="loading" class="w-full">
      <div v-if="diagramInfo" class="diagram-wrapper">
        <!-- 流程状态标签 -->
        <div class="process-status-bar">
          <n-tag :type="statusType" size="small">
            {{ statusText }}
          </n-tag>
          <span v-if="diagramInfo.startUserName" class="start-user">
            发起人：{{ diagramInfo.startUserName }}
          </span>
          <span v-if="diagramInfo.startTime" class="start-time">
            发起时间：{{ formatDate(diagramInfo.startTime) }}
          </span>
        </div>

        <!-- BPMN 流程图容器 -->
        <div ref="containerRef" class="diagram-container">
          <div ref="canvasRef" class="bpmn-canvas" />
        </div>

        <!-- 图例 -->
        <div class="legend">
          <div class="legend-item">
            <span class="legend-color completed" />
            <span>已完成</span>
          </div>
          <div class="legend-item">
            <span class="legend-color running" />
            <span>处理中</span>
          </div>
          <div class="legend-item">
            <span class="legend-color pending" />
            <span>待处理</span>
          </div>
        </div>
      </div>

      <n-empty v-else-if="!loading" description="暂无流程图数据" />
    </n-spin>

    <!-- 节点详情悬浮提示 -->
    <Teleport to="body">
      <div
        v-if="tooltipVisible && currentNode"
        class="node-tooltip"
        :style="tooltipStyle"
      >
        <div class="tooltip-header">
          <span class="node-name">{{ currentNode.nodeName || currentNode.nodeId }}</span>
          <n-tag :type="getNodeStatusType(currentNode.status)" size="small">
            {{ getNodeStatusText(currentNode.status) }}
          </n-tag>
        </div>
        <n-divider style="margin: 8px 0" />
        <div class="tooltip-content">
          <div class="info-row">
            <span class="label">节点类型：</span>
            <span class="value">{{ getNodeTypeName(currentNode.nodeType) }}</span>
          </div>
          <!-- 处理人信息 -->
          <div v-if="currentNode.assigneeNames?.length" class="info-row">
            <span class="label">处理人：</span>
            <div class="assignee-list">
              <div v-for="(name, index) in currentNode.assigneeNames" :key="index" class="assignee-item">
                <span class="name">{{ name }}</span>
                <span v-if="currentNode.assigneeOrgs?.[index]" class="org">（{{ currentNode.assigneeOrgs[index] }}）</span>
              </div>
            </div>
          </div>
          <!-- 候选人信息 -->
          <div v-else-if="currentNode.candidateUserIds?.length" class="info-row">
            <span class="label">候选人：</span>
            <div class="assignee-list">
              <div v-for="(name, index) in (currentNode.assigneeNames || currentNode.candidateUserIds)" :key="index" class="assignee-item">
                <span class="name">{{ name }}</span>
                <span v-if="currentNode.assigneeOrgs?.[index]" class="org">（{{ currentNode.assigneeOrgs[index] }}）</span>
              </div>
            </div>
          </div>
          <!-- 兼容旧数据 -->
          <div v-else-if="currentNode.assigneeIds?.length" class="info-row">
            <span class="label">处理人：</span>
            <span class="value">{{ currentNode.assigneeIds.join(', ') }}</span>
          </div>
          <div v-if="currentNode.startTime" class="info-row">
            <span class="label">开始时间：</span>
            <span class="value">{{ formatDate(currentNode.startTime) }}</span>
          </div>
          <div v-if="currentNode.endTime" class="info-row">
            <span class="label">完成时间：</span>
            <span class="value">{{ formatDate(currentNode.endTime) }}</span>
          </div>
          <div v-if="currentNode.duration" class="info-row">
            <span class="label">处理时长：</span>
            <span class="value">{{ formatDuration(currentNode.duration) }}</span>
          </div>
          <div v-if="currentNode.comment" class="info-row">
            <span class="label">审批意见：</span>
            <span class="value">{{ currentNode.comment }}</span>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import BpmnJS from 'bpmn-js/lib/NavigatedViewer'
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import flowApi from '@/api/flow'

const props = defineProps({
  processInstanceId: {
    type: String,
    required: true,
  },
})

const emit = defineEmits(['node-click', 'loaded'])

// 状态
const loading = ref(false)
const diagramInfo = ref(null)
const containerRef = ref(null)
const canvasRef = ref(null)
let bpmnViewer = null

// 悬浮提示
const tooltipVisible = ref(false)
const currentNode = ref(null)
const tooltipPosition = ref({ x: 0, y: 0 })

// 计算属性
const statusType = computed(() => {
  if (!diagramInfo.value)
    return 'default'
  const status = diagramInfo.value.status
  switch (status) {
    case 'completed':
      return 'success'
    case 'running':
      return 'warning'
    case 'terminated':
      return 'error'
    default:
      return 'default'
  }
})

const statusText = computed(() => {
  if (!diagramInfo.value)
    return ''
  const status = diagramInfo.value.status
  switch (status) {
    case 'completed':
      return '已完成'
    case 'running':
      return '进行中'
    case 'terminated':
      return '已终止'
    default:
      return status
  }
})

const tooltipStyle = computed(() => ({
  left: `${tooltipPosition.value.x}px`,
  top: `${tooltipPosition.value.y}px`,
}))

// 方法
async function fetchDiagramInfo() {
  if (!props.processInstanceId)
    return

  loading.value = true

  try {
    const res = await flowApi.getProcessDiagramInfo(props.processInstanceId)
    if (res.code === 200) {
      diagramInfo.value = res.data
      emit('loaded', res.data)

      // 等待 DOM 更新后渲染 BPMN
      await nextTick()
      if (res.data.bpmnXml) {
        await renderBpmn(res.data.bpmnXml, res.data.nodes)
      }
    }
  }
  catch (error) {
    console.error('获取流程图详情失败:', error)
  }
  finally {
    loading.value = false
  }
}

async function renderBpmn(bpmnXml, nodes) {
  if (!canvasRef.value)
    return

  // 销毁旧的 viewer
  if (bpmnViewer) {
    bpmnViewer.destroy()
  }

  // 创建新的 viewer
  bpmnViewer = new BpmnJS({
    container: canvasRef.value,
    keyboard: {
      bindTo: window,
    },
  })

  try {
    // 导入 BPMN XML
    await bpmnViewer.importXML(bpmnXml)

    // 获取 canvas
    const canvas = bpmnViewer.get('canvas')
    const elementRegistry = bpmnViewer.get('elementRegistry')
    const overlays = bpmnViewer.get('overlays')

    // 自适应画布
    canvas.zoom('fit-viewport', 'auto')

    // 构建节点状态映射
    const nodeStatusMap = new Map()
    if (nodes) {
      nodes.forEach((node) => {
        nodeStatusMap.set(node.nodeId, node)
      })
    }

    // 遍历所有元素，添加样式
    elementRegistry.forEach((element) => {
      const nodeId = element.id
      const nodeInfo = nodeStatusMap.get(nodeId)

      if (nodeInfo) {
        // 添加状态样式
        canvas.addMarker(nodeId, `status-${nodeInfo.status}`)

        // 为用户任务添加处理人标记
        if (element.type === 'bpmn:UserTask' && nodeInfo.assigneeNames?.length) {
          const assigneeText = nodeInfo.assigneeNames.slice(0, 2).join(', ')
          const moreText = nodeInfo.assigneeNames.length > 2 ? `+${nodeInfo.assigneeNames.length - 2}` : ''

          overlays.add(nodeId, 'assignee', {
            position: 'bottom',
            html: `<div class="assignee-overlay">${assigneeText}${moreText}</div>`,
          })
        }
      }
    })

    // 添加点击事件
    const eventBus = bpmnViewer.get('eventBus')
    eventBus.on('element.hover', (event) => {
      const element = event.element
      const nodeInfo = nodeStatusMap.get(element.id)
      if (nodeInfo) {
        showNodeTooltip(nodeInfo, event.originalEvent)
      }
    })

    eventBus.on('element.out', () => {
      hideNodeTooltip()
    })

    eventBus.on('element.click', (event) => {
      const element = event.element
      const nodeInfo = nodeStatusMap.get(element.id)
      if (nodeInfo) {
        emit('node-click', nodeInfo)
      }
    })
  }
  catch (error) {
    console.error('渲染 BPMN 失败:', error)
  }
}

function showNodeTooltip(node, event) {
  currentNode.value = node
  tooltipVisible.value = true

  tooltipPosition.value = {
    x: event.clientX + 15,
    y: event.clientY + 15,
  }
}

function hideNodeTooltip() {
  tooltipVisible.value = false
  currentNode.value = null
}

function getNodeStatusType(status) {
  switch (status) {
    case 'completed':
      return 'success'
    case 'running':
      return 'warning'
    case 'pending':
      return 'default'
    case 'skipped':
      return 'info'
    default:
      return 'default'
  }
}

function getNodeStatusText(status) {
  switch (status) {
    case 'completed':
      return '已完成'
    case 'running':
      return '处理中'
    case 'pending':
      return '待处理'
    case 'skipped':
      return '已跳过'
    default:
      return status
  }
}

function getNodeTypeName(type) {
  const typeMap = {
    StartEvent: '开始节点',
    EndEvent: '结束节点',
    UserTask: '用户任务',
    ServiceTask: '服务任务',
    ScriptTask: '脚本任务',
    ExclusiveGateway: '排他网关',
    ParallelGateway: '并行网关',
    InclusiveGateway: '包含网关',
    SequenceFlow: '连线',
  }
  return typeMap[type] || type
}

function formatDate(date) {
  if (!date)
    return ''
  const d = new Date(date)
  return d.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function formatDuration(ms) {
  if (!ms)
    return ''
  const seconds = Math.floor(ms / 1000)
  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)
  const days = Math.floor(hours / 24)

  if (days > 0) {
    return `${days}天 ${hours % 24}小时`
  }
  else if (hours > 0) {
    return `${hours}小时 ${minutes % 60}分钟`
  }
  else if (minutes > 0) {
    return `${minutes}分钟`
  }
  else {
    return `${seconds}秒`
  }
}

// 鼠标移动更新提示框位置
function handleMouseMove(event) {
  if (tooltipVisible.value) {
    tooltipPosition.value = {
      x: event.clientX + 15,
      y: event.clientY + 15,
    }
  }
}

// 监听 processInstanceId 变化
watch(() => props.processInstanceId, () => {
  fetchDiagramInfo()
}, { immediate: true })

onMounted(() => {
  document.addEventListener('mousemove', handleMouseMove)
})

onUnmounted(() => {
  document.removeEventListener('mousemove', handleMouseMove)
  if (bpmnViewer) {
    bpmnViewer.destroy()
  }
})
</script>

<style scoped>
@import 'bpmn-js/dist/assets/diagram-js.css';
@import 'bpmn-js/dist/assets/bpmn-font/css/bpmn.css';
@import 'bpmn-js/dist/assets/bpmn-font/css/bpmn-codes.css';
@import 'bpmn-js/dist/assets/bpmn-font/css/bpmn-embedded.css';

.process-diagram-viewer {
  width: 100%;
  min-height: 400px;
}

.diagram-wrapper {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.process-status-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 13px;
  color: #606266;
}

.start-user,
.start-time {
  color: #909399;
}

.diagram-container {
  position: relative;
  background: #fafafa;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  overflow: hidden;
}

.bpmn-canvas {
  width: 100%;
  height: 500px;
}

/* 节点状态样式 */
:deep(.status-completed) {
  fill: #f0f9eb !important;
  stroke: #67c23a !important;
}

:deep(.status-completed .djs-visual > rect),
:deep(.status-completed .djs-visual > circle) {
  fill: #f0f9eb !important;
  stroke: #67c23a !important;
  stroke-width: 2px;
}

:deep(.status-running) {
  fill: #fdf6ec !important;
  stroke: #e6a23c !important;
}

:deep(.status-running .djs-visual > rect),
:deep(.status-running .djs-visual > circle) {
  fill: #fdf6ec !important;
  stroke: #e6a23c !important;
  stroke-width: 2px;
  animation: pulse-border 2s infinite;
}

:deep(.status-pending) {
  fill: #f4f4f5 !important;
  stroke: #909399 !important;
}

:deep(.status-pending .djs-visual > rect),
:deep(.status-pending .djs-visual > circle) {
  fill: #f4f4f5 !important;
  stroke: #909399 !important;
  stroke-width: 2px;
}

@keyframes pulse-border {
  0% {
    stroke-opacity: 1;
  }
  50% {
    stroke-opacity: 0.5;
  }
  100% {
    stroke-opacity: 1;
  }
}

/* 处理人标记样式 */
:deep(.assignee-overlay) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 11px;
  white-space: nowrap;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.4);
}

.legend {
  display: flex;
  justify-content: center;
  gap: 24px;
  padding: 8px;
  background: #f5f7fa;
  border-radius: 4px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #606266;
}

.legend-color {
  width: 16px;
  height: 16px;
  border-radius: 4px;
  border: 2px solid;
}

.legend-color.completed {
  border-color: #67c23a;
  background: rgba(103, 194, 58, 0.1);
}

.legend-color.running {
  border-color: #e6a23c;
  background: rgba(230, 162, 60, 0.1);
}

.legend-color.pending {
  border-color: #909399;
  background: rgba(144, 147, 153, 0.05);
}

/* 悬浮提示样式 */
.node-tooltip {
  position: fixed;
  z-index: 9999;
  min-width: 200px;
  max-width: 320px;
  padding: 12px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  border: 1px solid #e4e7ed;
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.tooltip-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.node-name {
  font-weight: 600;
  font-size: 14px;
  color: #303133;
}

.tooltip-content {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.info-row {
  display: flex;
  font-size: 12px;
  line-height: 1.5;
}

.info-row .label {
  flex-shrink: 0;
  color: #909399;
  width: 70px;
}

.info-row .value {
  color: #606266;
  word-break: break-all;
}

.assignee-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.assignee-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.assignee-item .name {
  color: #303133;
  font-weight: 500;
}

.assignee-item .org {
  color: #909399;
  font-size: 11px;
}
</style>
