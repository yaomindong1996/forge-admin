<template>
  <div ref="containerRef" class="interactive-diagram">
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

        <!-- 流程图容器 -->
        <div ref="diagramRef" class="diagram-container">
          <!-- 流程图图片 -->
          <img
            v-if="diagramInfo.diagramBase64"
            ref="imageRef"
            :src="diagramInfo.diagramBase64"
            alt="流程图"
            class="diagram-image"
            @load="onImageLoad"
          >

          <!-- 节点状态指示器层（只显示状态点，不覆盖整个节点） -->
          <div
            v-if="imageLoaded && nodePositions.length > 0"
            class="node-indicators"
            :style="overlayStyle"
          >
            <div
              v-for="node in nodePositions"
              :key="node.nodeId"
              class="node-indicator"
              :class="getNodeClass(node)"
              :style="getIndicatorStyle(node)"
              @mouseenter="showNodeTooltip(node, $event)"
              @mouseleave="hideNodeTooltip"
              @click="handleNodeClick(node)"
            >
              <!-- 节点状态图标 -->
              <div v-if="node.status === 'running'" class="status-icon running">
                <i class="i-material-symbols:sync" />
              </div>
              <div v-else-if="node.status === 'completed'" class="status-icon completed">
                <i class="i-material-symbols:check-circle" />
              </div>
              <div v-else class="status-icon pending">
                <i class="i-material-symbols:circle-outline" />
              </div>
            </div>
          </div>
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
          <!-- 处理人信息（优先显示姓名和组织） -->
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
          <!-- 兼容旧数据：只显示ID -->
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
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
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
const imageLoaded = ref(false)
const containerRef = ref(null)
const diagramRef = ref(null)
const imageRef = ref(null)

// 悬浮提示
const tooltipVisible = ref(false)
const currentNode = ref(null)
const tooltipPosition = ref({ x: 0, y: 0 })

// 图片缩放比例
const scale = ref(1)

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

const overlayStyle = computed(() => {
  if (!imageRef.value)
    return {}
  return {
    width: `${imageRef.value.clientWidth}px`,
    height: `${imageRef.value.clientHeight}px`,
  }
})

// 节点位置（需要根据图片缩放计算）
const nodePositions = computed(() => {
  if (!diagramInfo.value?.nodes || !imageLoaded.value)
    return []

  return diagramInfo.value.nodes
    .filter(node => node.x !== undefined && node.y !== undefined)
    .map(node => ({
      ...node,
      // 计算相对于图片的位置
      scaledX: node.x * scale.value,
      scaledY: node.y * scale.value,
      scaledWidth: (node.width || 100) * scale.value,
      scaledHeight: (node.height || 80) * scale.value,
    }))
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
  imageLoaded.value = false

  try {
    console.log('[InteractiveProcessDiagram] 获取流程图详情, processInstanceId:', props.processInstanceId)
    const res = await flowApi.getProcessDiagramInfo(props.processInstanceId)
    console.log('[InteractiveProcessDiagram] API 返回结果:', res)

    if (res.code === 200) {
      diagramInfo.value = res.data
      console.log('[InteractiveProcessDiagram] diagramInfo:', diagramInfo.value)
      console.log('[InteractiveProcessDiagram] nodes 数量:', res.data?.nodes?.length)
      if (res.data?.nodes?.length > 0) {
        console.log('[InteractiveProcessDiagram] 第一个节点:', res.data.nodes[0])
      }
      emit('loaded', res.data)
    }
    else {
      console.error('[InteractiveProcessDiagram] API 返回错误:', res.message)
    }
  }
  catch (error) {
    console.error('[InteractiveProcessDiagram] 获取流程图详情失败:', error)
  }
  finally {
    loading.value = false
  }
}

function onImageLoad() {
  imageLoaded.value = true
  // 计算缩放比例（假设原始图片是按照 BPMN 坐标绘制的）
  if (imageRef.value && diagramInfo.value?.nodes?.length > 0) {
    // 找到最大的坐标值来计算缩放
    const maxX = Math.max(...diagramInfo.value.nodes.map(n => (n.x || 0) + (n.width || 100)))
    const maxY = Math.max(...diagramInfo.value.nodes.map(n => (n.y || 0) + (n.height || 80)))

    if (maxX > 0 && maxY > 0) {
      scale.value = Math.min(
        imageRef.value.clientWidth / maxX,
        imageRef.value.clientHeight / maxY,
      )
    }
  }
}

function getNodeClass(node) {
  return {
    [`status-${node.status}`]: true,
    'is-user-task': node.nodeType === 'UserTask',
  }
}

function getNodeStyle(node) {
  return {
    left: `${node.scaledX}px`,
    top: `${node.scaledY}px`,
    width: `${node.scaledWidth}px`,
    height: `${node.scaledHeight}px`,
  }
}

function getIndicatorStyle(node) {
  // 状态指示器放在节点右上角
  return {
    left: `${node.scaledX + node.scaledWidth - 12}px`,
    top: `${node.scaledY - 12}px`,
  }
}

function showNodeTooltip(node, event) {
  currentNode.value = node
  tooltipVisible.value = true

  // 计算提示框位置
  const rect = event.target.getBoundingClientRect()
  tooltipPosition.value = {
    x: rect.left + rect.width / 2,
    y: rect.bottom + 10,
  }
}

function hideNodeTooltip() {
  tooltipVisible.value = false
  currentNode.value = null
}

function handleNodeClick(node) {
  emit('node-click', node)
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

// 监听 processInstanceId 变化
watch(() => props.processInstanceId, () => {
  fetchDiagramInfo()
}, { immediate: true })

// 鼠标移动更新提示框位置
function handleMouseMove(event) {
  if (tooltipVisible.value) {
    tooltipPosition.value = {
      x: event.clientX + 15,
      y: event.clientY + 15,
    }
  }
}

onMounted(() => {
  document.addEventListener('mousemove', handleMouseMove)
})

onUnmounted(() => {
  document.removeEventListener('mousemove', handleMouseMove)
})
</script>

<style scoped>
.interactive-diagram {
  width: 100%;
  min-height: 300px;
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
  display: flex;
  justify-content: center;
  overflow: auto;
  background: #fafafa;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 16px;
}

.diagram-image {
  max-width: 100%;
  max-height: 600px;
  object-fit: contain;
}

.node-indicators {
  position: absolute;
  top: 16px;
  left: 16px;
  pointer-events: none;
}

.node-indicator {
  position: absolute;
  cursor: pointer;
  pointer-events: auto;
  transition: all 0.2s ease;
  z-index: 10;
}

.status-icon {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  border: 2px solid #fff;
  transition: all 0.2s ease;
}

.status-icon.completed {
  color: #67c23a;
  background: #f0f9eb;
  border-color: #67c23a;
}

.status-icon.running {
  color: #e6a23c;
  background: #fdf6ec;
  border-color: #e6a23c;
  animation: pulse 2s infinite;
}

.status-icon.pending {
  color: #909399;
  background: #f4f4f5;
  border-color: #d3d4d6;
}

.node-indicator:hover .status-icon {
  transform: scale(1.2);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

@keyframes pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(230, 162, 60, 0.4);
  }
  70% {
    box-shadow: 0 0 0 10px rgba(230, 162, 60, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(230, 162, 60, 0);
  }
}

@keyframes blink {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
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
  transform: translateX(-50%);
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateX(-50%) translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateX(-50%) translateY(0);
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
