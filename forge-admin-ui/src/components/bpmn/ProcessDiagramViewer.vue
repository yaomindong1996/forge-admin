<template>
  <div class="process-diagram-viewer">
    <!-- 加载状态 -->
    <n-spin :show="loading" class="w-full">
      <div v-if="diagramInfo" class="diagram-wrapper">
        <!-- 流程状态标签 -->
        <div class="process-status-bar">
          <div class="status-info">
            <n-tag :type="statusType" size="medium" :bordered="false">
              {{ statusText }}
            </n-tag>
          </div>
          <div v-if="diagramInfo.startUserName" class="meta-info">
            <span class="meta-label">发起人</span>
            <span class="meta-value">{{ diagramInfo.startUserName }}</span>
          </div>
          <div v-if="diagramInfo.startTime" class="meta-info">
            <span class="meta-label">发起时间</span>
            <span class="meta-value">{{ formatDate(diagramInfo.startTime) }}</span>
          </div>
        </div>

        <!-- BPMN 流程图容器 -->
        <div ref="containerRef" class="diagram-container">
          <div ref="canvasRef" class="bpmn-canvas" />
        </div>

        <!-- 图例 -->
        <div class="legend">
          <div class="legend-item">
            <span class="legend-dot completed" />
            <span>已完成</span>
          </div>
          <div class="legend-item">
            <span class="legend-dot running" />
            <span>处理中</span>
          </div>
          <div class="legend-item">
            <span class="legend-dot pending" />
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
          <n-tag :type="getNodeStatusType(currentNode.status)" size="small" :bordered="false">
            {{ getNodeStatusText(currentNode.status) }}
          </n-tag>
        </div>
        <n-divider style="margin: 8px 0" />
        <div class="tooltip-content">
          <div class="info-row">
            <span class="label">节点类型</span>
            <span class="value">{{ getNodeTypeName(currentNode.nodeType) }}</span>
          </div>
          <!-- 处理人信息 -->
          <div v-if="currentNode.assigneeNames?.length" class="info-row">
            <span class="label">处理人</span>
            <div class="assignee-list">
              <n-tag
                v-for="(name, index) in currentNode.assigneeNames"
                :key="index"
                size="small"
                type="info"
                :bordered="false"
              >
                {{ name }}
                <span v-if="currentNode.assigneeOrgs?.[index]" class="org-suffix">
                  · {{ currentNode.assigneeOrgs[index] }}
                </span>
              </n-tag>
            </div>
          </div>
          <!-- 候选人信息 -->
          <div v-else-if="currentNode.candidateUserIds?.length" class="info-row">
            <span class="label">候选人</span>
            <div class="assignee-list">
              <n-tag
                v-for="(name, index) in (currentNode.assigneeNames || currentNode.candidateUserIds)"
                :key="index"
                size="small"
                type="default"
                :bordered="false"
              >
                {{ name }}
                <span v-if="currentNode.assigneeOrgs?.[index]" class="org-suffix">
                  · {{ currentNode.assigneeOrgs[index] }}
                </span>
              </n-tag>
            </div>
          </div>
          <!-- 兼容旧数据 -->
          <div v-else-if="currentNode.assigneeIds?.length" class="info-row">
            <span class="label">处理人</span>
            <span class="value">{{ currentNode.assigneeIds.join(', ') }}</span>
          </div>
          <div v-if="currentNode.startTime" class="info-row">
            <span class="label">开始时间</span>
            <span class="value">{{ formatDate(currentNode.startTime) }}</span>
          </div>
          <div v-if="currentNode.endTime" class="info-row">
            <span class="label">完成时间</span>
            <span class="value">{{ formatDate(currentNode.endTime) }}</span>
          </div>
          <div v-if="currentNode.duration" class="info-row">
            <span class="label">处理时长</span>
            <span class="value">{{ formatDuration(currentNode.duration) }}</span>
          </div>
          <div v-if="currentNode.comment" class="info-row comment-row">
            <span class="label">审批意见</span>
            <span class="value comment-text">{{ currentNode.comment }}</span>
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
  gap: 20px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #667eea15 0%, #764ba215 100%);
  border-radius: 8px;
  border: 1px solid #e4e7ed;
}

.status-info {
  display: flex;
  align-items: center;
}

.meta-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.meta-label {
  color: #909399;
  font-weight: 500;
}

.meta-value {
  color: #303133;
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
  fill: #f0f9ff !important;
  stroke: #18a058 !important;
}

:deep(.status-completed .djs-visual > rect),
:deep(.status-completed .djs-visual > circle),
:deep(.status-completed .djs-visual > polygon) {
  fill: #f0f9ff !important;
  stroke: #18a058 !important;
  stroke-width: 2.5px;
  filter: drop-shadow(0 2px 4px rgba(24, 160, 88, 0.2));
}

:deep(.status-running) {
  fill: #fffbeb !important;
  stroke: #f0a020 !important;
}

:deep(.status-running .djs-visual > rect),
:deep(.status-running .djs-visual > circle),
:deep(.status-running .djs-visual > polygon) {
  fill: #fffbeb !important;
  stroke: #f0a020 !important;
  stroke-width: 2.5px;
  animation: pulse-border 2s ease-in-out infinite;
  filter: drop-shadow(0 2px 6px rgba(240, 160, 32, 0.3));
}

:deep(.status-pending) {
  fill: #fafafa !important;
  stroke: #d0d0d0 !important;
}

:deep(.status-pending .djs-visual > rect),
:deep(.status-pending .djs-visual > circle),
:deep(.status-pending .djs-visual > polygon) {
  fill: #fafafa !important;
  stroke: #d0d0d0 !important;
  stroke-width: 2px;
}

@keyframes pulse-border {
  0%,
  100% {
    stroke-opacity: 1;
    filter: drop-shadow(0 2px 6px rgba(240, 160, 32, 0.3));
  }
  50% {
    stroke-opacity: 0.7;
    filter: drop-shadow(0 3px 10px rgba(240, 160, 32, 0.5));
  }
}

/* 处理人标记样式 */
:deep(.assignee-overlay) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  padding: 5px 10px;
  border-radius: 12px;
  font-size: 11px;
  white-space: nowrap;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.4);
  backdrop-filter: blur(4px);
  font-weight: 500;
}

.legend {
  display: flex;
  justify-content: center;
  gap: 24px;
  padding: 10px 16px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(8px);
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #606266;
}

.legend-color {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  position: relative;
}

.legend-color::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.legend-color.completed {
  background: rgba(103, 194, 58, 0.2);
  color: #67c23a;
}

.legend-color.running {
  background: rgba(230, 162, 60, 0.2);
  color: #e6a23c;
  animation: legendPulse 2s ease-in-out infinite;
}

.legend-color.pending {
  background: rgba(144, 147, 153, 0.2);
  color: #909399;
}

@keyframes legendPulse {
  0%,
  100% {
    box-shadow: 0 0 0 0 currentColor;
    opacity: 1;
  }
  50% {
    box-shadow: 0 0 0 4px transparent;
    opacity: 0.8;
  }
}

/* 悬浮提示样式 */
.node-tooltip {
  position: fixed;
  z-index: 9999;
  min-width: 220px;
  max-width: 340px;
  padding: 0;
  background: #fff;
  border-radius: 12px;
  box-shadow:
    0 8px 24px rgba(0, 0, 0, 0.12),
    0 0 0 1px rgba(0, 0, 0, 0.05);
  animation: fadeIn 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-8px) scale(0.96);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.tooltip-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 12px 14px;
  background: linear-gradient(135deg, #667eea08 0%, #764ba208 100%);
  border-bottom: 1px solid #f0f0f0;
}

.node-name {
  font-weight: 600;
  font-size: 14px;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 6px;
}

.node-name::before {
  content: '📍';
  font-size: 16px;
}

.tooltip-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px 14px;
}

.info-row {
  display: flex;
  font-size: 12px;
  line-height: 1.6;
  align-items: flex-start;
}

.info-row .label {
  flex-shrink: 0;
  color: #909399;
  width: 75px;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 4px;
}

.info-row .label::before {
  content: '•';
  color: #667eea;
  font-weight: bold;
}

.info-row .value {
  color: #606266;
  word-break: break-all;
  flex: 1;
}

.assignee-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.assignee-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 8px;
  background: #f8f9fa;
  border-radius: 6px;
  transition: all 0.2s;
}

.assignee-item:hover {
  background: #f0f2f5;
  transform: translateX(2px);
}

.assignee-item::before {
  content: '👤';
  font-size: 14px;
}

.assignee-item .name {
  color: #303133;
  font-weight: 500;
  font-size: 12px;
}

.assignee-item .org {
  color: #909399;
  font-size: 11px;
  padding: 2px 6px;
  background: #fff;
  border-radius: 4px;
  margin-left: auto;
}

.org-suffix {
  color: #909399;
  font-size: 11px;
}

.comment-row {
  flex-direction: column;
  gap: 6px;
  padding: 10px;
  background: #fafbfc;
  border-radius: 8px;
  border-left: 3px solid #667eea;
}

.comment-row .label {
  width: auto;
  color: #667eea;
  font-weight: 600;
}

.comment-text {
  color: #303133;
  line-height: 1.6;
  font-style: italic;
}
</style>
