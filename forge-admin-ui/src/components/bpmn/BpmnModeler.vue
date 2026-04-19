<template>
  <div class="bpmn-modeler-container">
    <!-- 工具栏 -->
    <div class="toolbar">
      <n-space>
        <n-button-group>
          <n-button size="small" :disabled="!canUndo" @click="handleUndo">
            <template #icon>
              <i class="i-material-symbols:undo" />
            </template>
            撤销
          </n-button>
          <n-button size="small" :disabled="!canRedo" @click="handleRedo">
            <template #icon>
              <i class="i-material-symbols:redo" />
            </template>
            重做
          </n-button>
        </n-button-group>

        <n-divider vertical />

        <n-button size="small" @click="handleZoomIn">
          <template #icon>
            <i class="i-material-symbols:zoom-in" />
          </template>
          放大
        </n-button>
        <n-button size="small" @click="handleZoomOut">
          <template #icon>
            <i class="i-material-symbols:zoom-out" />
          </template>
          缩小
        </n-button>
        <n-button size="small" @click="handleZoomReset">
          <template #icon>
            <i class="i-material-symbols:fit-screen" />
          </template>
          适应屏幕
        </n-button>

        <n-divider vertical />

        <n-button size="small" type="primary" @click="handleDownloadBpmn">
          <template #icon>
            <i class="i-material-symbols:download" />
          </template>
          导出BPMN
        </n-button>
        <n-button size="small" @click="handleDownloadSvg">
          <template #icon>
            <i class="i-material-symbols:image" />
          </template>
          导出SVG
        </n-button>
      </n-space>
    </div>

    <!-- 设计器主体 -->
    <div class="modeler-wrapper">
      <!-- 画布区域 -->
      <div ref="canvasRef" class="canvas-container" />

      <!-- 右侧属性面板 -->
      <div v-show="selectedElement" class="properties-panel">
        <div class="properties-header">
          <span>属性设置</span>
          <n-button text size="small" @click="selectedElement = null">
            <i class="i-material-symbols:close" />
          </n-button>
        </div>
        <div v-if="selectedElement" class="properties-content">
          <n-form :model="elementProperties" label-placement="top" size="small">
            <n-form-item label="ID">
              <n-input v-model:value="elementProperties.id" @blur="updateElementId" />
            </n-form-item>
            <n-form-item label="名称">
              <n-input v-model:value="elementProperties.name" @blur="updateElementName" />
            </n-form-item>

            <!-- 用户任务属性 -->
            <template v-if="selectedElement?.type === 'bpmn:UserTask'">
              <n-divider>审批设置</n-divider>
              <n-form-item label="审批人">
                <n-select
                  v-model:value="elementProperties.assignee"
                  :options="assigneeOptions"
                  placeholder="选择审批人"
                  @update:value="updateUserTaskAssignee"
                />
              </n-form-item>
              <n-form-item label="候选用户">
                <n-input
                  v-model:value="elementProperties.candidateUsers"
                  placeholder="用户ID，逗号分隔"
                  @blur="updateCandidateUsers"
                />
              </n-form-item>
              <n-form-item label="候选组">
                <n-input
                  v-model:value="elementProperties.candidateGroups"
                  placeholder="组ID，逗号分隔"
                  @blur="updateCandidateGroups"
                />
              </n-form-item>
            </template>

            <!-- 服务任务属性 -->
            <template v-if="selectedElement?.type === 'bpmn:ServiceTask'">
              <n-divider>服务设置</n-divider>
              <n-form-item label="实现类型">
                <n-select
                  v-model:value="elementProperties.implementationType"
                  :options="implementationTypeOptions"
                  @update:value="updateServiceImplementation"
                />
              </n-form-item>
              <n-form-item label="类名/表达式">
                <n-input
                  v-model:value="elementProperties.implementation"
                  @blur="updateServiceImplementation"
                />
              </n-form-item>
            </template>

            <!-- 序列流属性 -->
            <template v-if="selectedElement?.type === 'bpmn:SequenceFlow'">
              <n-divider>流转条件</n-divider>
              <n-form-item label="条件表达式">
                <n-input
                  v-model:value="elementProperties.condition"
                  type="textarea"
                  :rows="3"
                  placeholder="${approved == true}"
                  @blur="updateCondition"
                />
              </n-form-item>
            </template>
          </n-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import BpmnModeler from 'bpmn-js/lib/Modeler'
import { nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import 'bpmn-js/dist/assets/diagram-js.css'
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn.css'
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn-codes.css'
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn-embedded.css'

const props = defineProps({
  xml: {
    type: String,
    default: '',
  },
  readOnly: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['save', 'change', 'ready'])

// refs
const canvasRef = ref(null)

// modeler instance
let modeler = null
let isInitialized = false

// state
const canUndo = ref(false)
const canRedo = ref(false)
const selectedElement = ref(null)
const elementProperties = reactive({
  id: '',
  name: '',
  assignee: '',
  candidateUsers: '',
  candidateGroups: '',
  implementationType: 'class',
  implementation: '',
  condition: '',
})

// 选项数据
const assigneeOptions = [
  { label: '发起人', value: '${initiator}' },
  { label: '上级领导', value: '${leader}' },
  { label: '部门经理', value: '${deptManager}' },
  { label: 'HR', value: '${hr}' },
  { label: '自定义表达式', value: 'custom' },
]

const implementationTypeOptions = [
  { label: 'Java类', value: 'class' },
  { label: '表达式', value: 'expression' },
  { label: '委托表达式', value: 'delegateExpression' },
]

// 默认 BPMN XML - 必须包含 BPMNDiagram 图形信息
const defaultXml = `<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
                  xmlns:flowable="http://flowable.org/bpmn"
                  targetNamespace="http://flowable.org/processdef">
  <bpmn:process id="Process_1" name="新流程" isExecutable="true">
    <bpmn:startEvent id="StartEvent_1" name="开始"/>
  </bpmn:process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="Process_1">
      <bpmndi:BPMNShape id="StartEvent_1_di" bpmnElement="StartEvent_1">
        <dc:Bounds x="180" y="160" width="36" height="36"/>
      </bpmndi:BPMNShape>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</bpmn:definitions>`

// 检查 XML 是否包含图形信息
function hasDiagramInfo(xml) {
  return xml && (xml.includes('BPMNDiagram') || xml.includes('bpmndi:BPMNDiagram'))
}

// 初始化
onMounted(async () => {
  await nextTick()
  await initModeler()
})

onUnmounted(() => {
  if (modeler) {
    modeler.destroy()
  }
})

// 初始化 modeler
async function initModeler() {
  modeler = new BpmnModeler({
    container: canvasRef.value,
    keyboard: {
      bindTo: document,
    },
  })

  // 监听事件
  const eventBus = modeler.get('eventBus')

  // 元素选择事件
  eventBus.on('selection.changed', (e) => {
    const { newSelection } = e
    if (newSelection.length === 1) {
      selectedElement.value = newSelection[0]
      loadElementProperties(newSelection[0])
    }
    else {
      selectedElement.value = null
    }
  })

  // 元素变化事件
  eventBus.on('element.changed', () => {
    updateUndoRedoState()
    emitChange()
  })

  // 命令栈变化
  eventBus.on('commandStack.changed', () => {
    updateUndoRedoState()
  })

  // 导入 XML - 确保使用有效的 XML
  const xmlToImport = props.xml && props.xml.trim() ? props.xml : defaultXml
  await importXML(xmlToImport)

  isInitialized = true
  emit('ready', modeler)
}

// 导入 XML
async function importXML(xml) {
  // 确保使用有效的 XML
  if (!xml || !xml.trim()) {
    console.warn('XML 为空，使用默认模板')
    xml = defaultXml
  }

  // 检查是否包含图形信息，如果没有则使用默认模板
  if (!hasDiagramInfo(xml)) {
    console.warn('XML 缺少图形信息(BPMNDiagram)，使用默认模板')
    xml = defaultXml
  }

  try {
    const result = await modeler.importXML(xml)
    console.log('BPMN 导入成功')
    const canvas = modeler.get('canvas')
    canvas.zoom('fit-viewport')
  }
  catch (error) {
    console.error('导入 BPMN 失败:', error)
    console.error('失败的 XML 内容:', xml)
  }
}

// 更新撤销重做状态
function updateUndoRedoState() {
  const commandStack = modeler.get('commandStack')
  canUndo.value = commandStack.canUndo()
  canRedo.value = commandStack.canRedo()
}

// 加载元素属性
function loadElementProperties(element) {
  const bo = element.businessObject
  elementProperties.id = bo.id || ''
  elementProperties.name = bo.name || ''

  // 用户任务属性
  if (element.type === 'bpmn:UserTask') {
    const extensionElements = bo.extensionElements?.values || []
    const assigneeExt = extensionElements.find(ext => ext.$type === 'flowable:Assignee')
    elementProperties.assignee = bo.assignee || assigneeExt?.value || ''
    elementProperties.candidateUsers = bo.candidateUsers || ''
    elementProperties.candidateGroups = bo.candidateGroups || ''
  }

  // 服务任务属性
  if (element.type === 'bpmn:ServiceTask') {
    elementProperties.implementation = bo['flowable:class'] || bo['flowable:expression'] || bo['flowable:delegateExpression'] || ''
    elementProperties.implementationType = bo['flowable:class'] ? 'class' : bo['flowable:expression'] ? 'expression' : 'delegateExpression'
  }

  // 序列流条件
  if (element.type === 'bpmn:SequenceFlow') {
    const conditionExpression = bo.conditionExpression
    elementProperties.condition = conditionExpression ? conditionExpression.body : ''
  }
}

// 更新元素 ID
function updateElementId() {
  if (!selectedElement.value)
    return

  const modeling = modeler.get('modeling')
  modeling.updateProperties(selectedElement.value, {
    id: elementProperties.id,
  })
}

// 更新元素名称
function updateElementName() {
  if (!selectedElement.value)
    return

  const modeling = modeler.get('modeling')
  modeling.updateProperties(selectedElement.value, {
    name: elementProperties.name,
  })
}

// 更新用户任务审批人
function updateUserTaskAssignee() {
  if (!selectedElement.value)
    return

  const modeling = modeler.get('modeling')
  modeling.updateProperties(selectedElement.value, {
    'flowable:assignee': elementProperties.assignee,
  })
}

// 更新候选用户
function updateCandidateUsers() {
  if (!selectedElement.value)
    return

  const modeling = modeler.get('modeling')
  modeling.updateProperties(selectedElement.value, {
    'flowable:candidateUsers': elementProperties.candidateUsers,
  })
}

// 更新候选组
function updateCandidateGroups() {
  if (!selectedElement.value)
    return

  const modeling = modeler.get('modeling')
  modeling.updateProperties(selectedElement.value, {
    'flowable:candidateGroups': elementProperties.candidateGroups,
  })
}

// 更新服务任务实现
function updateServiceImplementation() {
  if (!selectedElement.value)
    return

  const modeling = modeler.get('modeling')
  const props = {
    'flowable:class': null,
    'flowable:expression': null,
    'flowable:delegateExpression': null,
  }

  const key = `flowable:${elementProperties.implementationType}`
  props[key] = elementProperties.implementation

  modeling.updateProperties(selectedElement.value, props)
}

// 更新流转条件
function updateCondition() {
  if (!selectedElement.value)
    return

  const modeling = modeler.get('modeling')

  if (elementProperties.condition) {
    modeling.updateProperties(selectedElement.value, {
      conditionExpression: {
        $type: 'bpmn:FormalExpression',
        body: elementProperties.condition,
      },
    })
  }
  else {
    modeling.updateProperties(selectedElement.value, {
      conditionExpression: null,
    })
  }
}

// 撤销
function handleUndo() {
  const commandStack = modeler.get('commandStack')
  commandStack.undo()
}

// 重做
function handleRedo() {
  const commandStack = modeler.get('commandStack')
  commandStack.redo()
}

// 放大
function handleZoomIn() {
  const canvas = modeler.get('canvas')
  canvas.zoom(canvas.zoom() * 1.1)
}

// 缩小
function handleZoomOut() {
  const canvas = modeler.get('canvas')
  canvas.zoom(canvas.zoom() * 0.9)
}

// 重置缩放
function handleZoomReset() {
  const canvas = modeler.get('canvas')
  canvas.zoom('fit-viewport')
}

// 导出 BPMN XML
async function handleDownloadBpmn() {
  try {
    const { xml } = await modeler.saveXML({ format: true })
    downloadFile(xml, 'process.bpmn', 'application/xml')
  }
  catch (error) {
    console.error('导出 BPMN 失败:', error)
  }
}

// 导出 SVG
async function handleDownloadSvg() {
  try {
    const { svg } = await modeler.saveSVG()
    downloadFile(svg, 'process.svg', 'image/svg+xml')
  }
  catch (error) {
    console.error('导出 SVG 失败:', error)
  }
}

// 下载文件
function downloadFile(content, filename, type) {
  const blob = new Blob([content], { type })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}

// 获取 XML
async function getXML() {
  try {
    const { xml } = await modeler.saveXML({ format: true })
    return xml
  }
  catch (error) {
    console.error('获取 XML 失败:', error)
    return null
  }
}

// 获取 SVG
async function getSVG() {
  try {
    const { svg } = await modeler.saveSVG()
    return svg
  }
  catch (error) {
    console.error('获取 SVG 失败:', error)
    return null
  }
}

// 触发变更事件
async function emitChange() {
  const xml = await getXML()
  emit('change', xml)
}

// 暴露方法
defineExpose({
  getXML,
  getSVG,
  importXML,
})

// 监听 xml prop 变化
watch(() => props.xml, (newXml) => {
  // 只有在初始化完成后才响应 prop 变化
  if (isInitialized && modeler) {
    const xmlToImport = newXml && newXml.trim() ? newXml : defaultXml
    importXML(xmlToImport)
  }
})
</script>

<style scoped>
.bpmn-modeler-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}

.toolbar {
  padding: 12px 16px;
  background: #f5f5f5;
  border-bottom: 1px solid #e0e0e0;
}

.modeler-wrapper {
  flex: 1;
  display: flex;
  overflow: hidden;
}

.canvas-container {
  flex: 1;
  overflow: hidden;
}

/* bpmn-js 内置工具面板样式覆盖 */
.canvas-container :deep(.djs-palette) {
  background: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
}

.canvas-container :deep(.djs-palette-entries) {
  padding: 8px;
}

.canvas-container :deep(.djs-palette-entry) {
  cursor: pointer;
  padding: 8px;
  border-radius: 4px;
}

.canvas-container :deep(.djs-palette-entry:hover) {
  background: #f0f0f0;
}

.properties-panel {
  width: 280px;
  background: #fafafa;
  border-left: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;
}

.properties-header {
  padding: 12px 16px;
  font-weight: 600;
  background: #f0f0f0;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.properties-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}
</style>
